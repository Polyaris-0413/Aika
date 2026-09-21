package com.aika.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.aika.app.ui.SettingsScreen
import com.aika.app.ui.TodoScreen
import com.aika.app.ui.theme.AikaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AikaTheme {
                AikaApp()
            }
        }
    }
}

/** 底栏分页 */
private enum class AikaTab { Tasks, Settings }

/**
 * 应用外壳:底栏做分页导航,各页自带顶栏。
 * 顶栏不放这里是因为两页的顶栏内容不同(待办页有统计和添加按钮),放在各页里更内聚。
 */
@Composable
fun AikaApp() {
    // 旋转等配置变更会重建 Activity,当前分页要留住
    var currentTab by rememberSaveable { mutableStateOf(AikaTab.Tasks) }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab == AikaTab.Tasks,
                    onClick = { currentTab = AikaTab.Tasks },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_checklist),
                            contentDescription = null,
                        )
                    },
                    label = { Text(stringResource(R.string.tab_tasks)) },
                )
                NavigationBarItem(
                    selected = currentTab == AikaTab.Settings,
                    onClick = { currentTab = AikaTab.Settings },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = null,
                        )
                    },
                    label = { Text(stringResource(R.string.tab_settings)) },
                )
            }
        },
    ) { innerPadding ->
        when (currentTab) {
            AikaTab.Tasks -> TodoScreen(contentPadding = innerPadding)
            AikaTab.Settings -> SettingsScreen(contentPadding = innerPadding)
        }
    }
}
