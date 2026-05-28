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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Sms
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.platform.testTag
import com.example.ui.components.ActiveCallOverlay
import com.example.util.ActiveCallState

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

                    if (authState is AuthState.Authenticated) {
                        val navController = rememberNavController()
                        val currentBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = currentBackStackEntry?.destination?.route
                        val showBottomBar = currentRoute == "contact_list" || currentRoute?.startsWith("dialer") == true || currentRoute?.startsWith("sms") == true
                        val currentLanguage by listViewModel.appLanguage.collectAsStateWithLifecycle()

                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = if (isShaderEnabled) Color.Transparent else MaterialTheme.colorScheme.background,
                            bottomBar = {
                                if (showBottomBar) {
                                    NavigationBar(
                                        containerColor = if (isShaderEnabled) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                                        modifier = Modifier.testTag("app_bottom_bar")
                                    ) {
                                        NavigationBarItem(
                                            selected = currentRoute == "contact_list",
                                            onClick = {
                                                if (currentRoute != "contact_list") {
                                                    navController.navigate("contact_list") {
                                                        popUpTo("contact_list") { inclusive = false }
                                                        launchSingleTop = true
                                                    }
                                                }
                                            },
                                            icon = { Icon(Icons.Default.ContactPhone, contentDescription = "Contacts") },
                                            label = { Text(if (themeEnabled) "" else "Contacts") },
                                            modifier = Modifier.testTag("nav_contacts_tab")
                                        )
                                        NavigationBarItem(
                                            selected = currentRoute?.startsWith("dialer") == true,
                                            onClick = {
                                                if (currentRoute?.startsWith("dialer") != true) {
                                                    navController.navigate("dialer") {
                                                        launchSingleTop = true
                                                    }
                                                }
                                            },
                                            icon = { Icon(Icons.Default.Dialpad, contentDescription = "Dialer") },
                                            label = { Text(if (themeEnabled) "" else "Dialer") },
                                            modifier = Modifier.testTag("nav_dialer_tab")
                                        )
                                        NavigationBarItem(
                                            selected = currentRoute?.startsWith("sms") == true,
                                            onClick = {
                                                if (currentRoute?.startsWith("sms") != true) {
                                                    navController.navigate("sms") {
                                                        launchSingleTop = true
                                                    }
                                                }
                                            },
                                            icon = { Icon(Icons.Default.Sms, contentDescription = "SMS") },
                                            label = { Text(if (themeEnabled) "" else "SMS") },
                                            modifier = Modifier.testTag("nav_sms_tab")
                                        )
                                    }
                                }
                            }
                        ) { innerPadding ->
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
                                        },
                                        onNavigateToDialer = { phone ->
                                            navController.navigate("dialer?number=${Uri.encode(phone)}") {
                                                launchSingleTop = true
                                            }
                                        },
                                        onNavigateToSms = { phone ->
                                            navController.navigate("sms?number=${Uri.encode(phone)}") {
                                                launchSingleTop = true
                                            }
                                        }
                                    )
                                }

                                composable(
                                    "dialer?number={number}",
                                    arguments = listOf(navArgument("number") {
                                        type = NavType.StringType
                                        nullable = true
                                        defaultValue = null
                                    })
                                ) { backStackEntry ->
                                    val number = backStackEntry.arguments?.getString("number")
                                    PhoneDialerScreen(
                                        currentLanguage = currentLanguage,
                                        prefilledNumber = number
                                    )
                                }

                                composable(
                                    "sms?number={number}",
                                    arguments = listOf(navArgument("number") {
                                        type = NavType.StringType
                                        nullable = true
                                        defaultValue = null
                                    })
                                ) { backStackEntry ->
                                    val number = backStackEntry.arguments?.getString("number")
                                    SmsMessagingScreen(
                                        currentLanguage = currentLanguage,
                                        prefilledNumber = number
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
                                        isShaderEnabled = isShaderEnabled,
                                        onNavigateToDialer = { phone ->
                                            navController.navigate("dialer?number=${Uri.encode(phone)}") {
                                                launchSingleTop = true
                                            }
                                        },
                                        onNavigateToSms = { phone ->
                                            navController.navigate("sms?number=${Uri.encode(phone)}") {
                                                launchSingleTop = true
                                            }
                                        }
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
                        }

                        // Ongoing system active call UI overlay
                        val activeCall by ActiveCallState.activeCall.collectAsStateWithLifecycle()
                        val callState by ActiveCallState.callState.collectAsStateWithLifecycle()
                        val callerNumber by ActiveCallState.callerNumber.collectAsStateWithLifecycle()

                        if (activeCall != null && callState != android.telecom.Call.STATE_DISCONNECTED) {
                            ActiveCallOverlay(
                                callerNumber = callerNumber,
                                callState = callState,
                                onAnswer = { ActiveCallState.answer() },
                                onHangup = { ActiveCallState.hangup() },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = if (isShaderEnabled) Color.Transparent else MaterialTheme.colorScheme.background
                        ) { innerPadding ->
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
