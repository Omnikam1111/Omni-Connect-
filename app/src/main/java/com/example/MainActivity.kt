package com.example

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.DatabaseProvider
import com.example.data.repository.ContactRepository
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AuthState
import com.example.ui.viewmodel.ContactDetailViewModel
import com.example.ui.viewmodel.ContactListViewModel
import com.example.worker.InteractionTrackingWorker
import androidx.compose.ui.graphics.Color
import android.net.Uri
import android.content.Intent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()

        handleOAuthRedirect(intent)

        setContent {
            val listViewModel: ContactListViewModel = viewModel()
            
            // ✅ Initialize the ViewModel
            LaunchedEffect(Unit) {
                listViewModel.initialize()
            }
            
            val themeEnabled by listViewModel.isCustomThemeEnabled.collectAsStateWithLifecycle()
            val primaryColor by listViewModel.customPrimaryColor.collectAsStateWithLifecycle()
            val secondaryColor by listViewModel.customSecondaryColor.collectAsStateWithLifecycle()
            val backgroundColor by listViewModel.customBackgroundColor.collectAsStateWithLifecycle()
            val surfaceColor by listViewModel.customSurfaceColor.collectAsStateWithLifecycle()

            MyApplicationTheme(
                customThemeEnabled = themeEnabled,
                customPrimary = Color(primaryColor),
                customSecondary = Color(secondaryColor),
                customBackground = Color(backgroundColor),
                customSurface = Color(surfaceColor)
            ) {
                val authState by listViewModel.authState.collectAsStateWithLifecycle()
                val isShaderEnabled by listViewModel.isShaderBackgroundEnabled.collectAsStateWithLifecycle()
                val shaderPreset by listViewModel.selectedShaderPreset.collectAsStateWithLifecycle()
                val customShaderCode by listViewModel.customShaderCode.collectAsStateWithLifecycle()
                val shaderSeed by listViewModel.shaderSeed.collectAsStateWithLifecycle()

                androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                    if (isShaderEnabled) {
                        val shaderSource = remember(shaderPreset, customShaderCode, shaderSeed) {
                            com.example.util.ShaderPresets.getShaderCode(shaderPreset, shaderSeed, customShaderCode)
                        }
                        com.example.ui.components.GlShaderBackground(
                            shaderCode = shaderSource,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = if (isShaderEnabled) Color.Transparent else MaterialTheme.colorScheme.background
                    ) { innerPadding ->
                        if (authState is AuthState.Authenticated) {
                            val navController = rememberNavController()
                            val repository = remember { DatabaseProvider.getRepository(applicationContext) }

                            NavHost(
                                navController = navController,
                                startDestination = "contact_list",
                                modifier = Modifier.padding(innerPadding)
                            ) {
                                composable("contact_list") {
                                    ContactListScreen(
                                        viewModel = listViewModel,
                                        onContactSelect = { id ->
                                            if (navController.currentDestination?.route == "contact_list") {
                                                navController.navigate("contact_detail/$id")
                                            }
                                        },
                                        onNavigateToSettings = {
                                            if (navController.currentDestination?.route == "contact_list") {
                                                navController.navigate("settings")
                                            }
                                        },
                                        onAddContact = {
                                            if (navController.currentDestination?.route == "contact_list") {
                                                navController.navigate("contact_detail/0")
                                            }
                                        }
                                    )
                                }

                                composable(
                                    "contact_detail/{contactId}",
                                    arguments = listOf(navArgument("contactId") { type = NavType.LongType })
                                ) { backStackEntry ->
                                    val contactId = backStackEntry.arguments?.getLong("contactId") ?: 0L
                                    val detailViewModel: ContactDetailViewModel = viewModel(
                                        key = "contact_detail_key_$contactId",
                                        viewModelStoreOwner = backStackEntry
                                    )
                                    ContactDetailScreen(
                                        contactId = contactId,
                                        viewModel = detailViewModel,
                                        onNavigateBack = {
                                            navController.popBackStack()
                                        },
                                        isShaderEnabled = isShaderEnabled
                                    )
                                }

                                composable("settings") {
                                    SettingsScreen(
                                        viewModel = listViewModel,
                                        onNavigateBack = {
                                            navController.popBackStack()
                                        }
                                    )
                                }
                            }
                        } else {
                            AuthScreen(
                                viewModel = listViewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOAuthRedirect(intent)
    }

    private fun handleOAuthRedirect(intent: Intent?) {
        val uri: Uri? = intent?.data
        if (uri != null && uri.scheme == "com.aistudio.oauth" && uri.host == "callback") {
            val code = uri.getQueryParameter("code")
            val error = uri.getQueryParameter("error")
            if (code != null) {
                com.example.util.GoogleOAuthManager.exchangeCodeForTokens(applicationContext, code) { success, resultMsg ->
                    runOnUiThread {
                        android.widget.Toast.makeText(applicationContext, resultMsg, android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            } else if (error != null) {
                android.widget.Toast.makeText(applicationContext, "Google login failed: $error", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
}
