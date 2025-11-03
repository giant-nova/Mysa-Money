package com.giantnovadevs.mysamoney


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.rememberNavController
import com.giantnovadevs.mysamoney.ui.components.AppDrawer
import com.giantnovadevs.mysamoney.ui.navigation.AppNavGraph
import com.giantnovadevs.mysamoney.ui.theme.MysaMoneyTheme
import kotlinx.coroutines.launch
import androidx.activity.viewModels
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.giantnovadevs.mysamoney.viewmodel.AuthViewModel
import com.giantnovadevs.mysamoney.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {

    // ✅ Get an instance of the SettingsViewModel
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // ✅ Collect the current theme from the ViewModel
            val currentTheme by settingsViewModel.currentTheme.collectAsState()
            val currentFont by settingsViewModel.currentFont.collectAsState()

            MysaMoneyTheme(
                palette = currentTheme,
                fontFamily = currentFont
            ) {
                val navController = rememberNavController()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                ModalNavigationDrawer(drawerState = drawerState, drawerContent = {
                    AppDrawer(navController) { scope.launch { drawerState.close() } }
                }) {
                    AppNavGraph(navController = navController,
                        settingsViewModel = settingsViewModel,
                        authViewModel = authViewModel,
                        onMenuClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        }
                    )
                }
            }
        }
    }
}