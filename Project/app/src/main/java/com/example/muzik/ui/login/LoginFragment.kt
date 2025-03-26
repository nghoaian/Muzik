package com.example.muzik.ui.login

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.example.muzik.R
import com.example.muzik.ui.home.background_color
import com.google.android.gms.auth.api.identity.Identity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.example.muzik.ui.home.IndeterminateCircularIndicator
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {
    private val viewModel: LoginViewModel by viewModels()
    lateinit var navController: NavController

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                RenderLogin(viewModel, navController = findNavController())
            }
        }
    }
}

@Composable
fun RenderLogin(viewModel: LoginViewModel, navController: NavController) {
    val colorStops = arrayOf(
        0f to Color.DarkGray,
        0.2f to background_color,
        1f to Color.Black
    )

    val context = LocalContext.current
    val state = viewModel.state.collectAsState()
    val googleAuthUiClient by lazy {
        GoogleAuthUIClient(
            context = context,
            oneTapClient = Identity.getSignInClient(context)
        )
    }

    LaunchedEffect(key1 = Unit) {
        if (googleAuthUiClient.getSignedInUser() != null) {
            navController.navigate(R.id.navigation_home)
            val curUser = googleAuthUiClient.getSignedInUser()
            val sharedPref = context.getSharedPreferences("MuzikPrefs", MODE_PRIVATE)
            val editor = sharedPref.edit()
            editor.putBoolean("log_status", true)
            editor.putString("userID", curUser?.userId)
            editor.putString("username", curUser?.username)
            editor.putString("avatarURL", curUser?.avatarUrl)
            editor.apply()
        }
        else {}
    }

    LaunchedEffect(key1 = state.value.isSignInSuccessful) {
        if (state.value.isSignInSuccessful) {
            Toast.makeText(context, "Welcome!", Toast.LENGTH_SHORT).show()
            val curUser = googleAuthUiClient.getSignedInUser()

            state.value.createUserInFirestore(curUser?.userId.toString(),
                curUser?.username.toString(), curUser?.avatarUrl.toString()
            )

            val sharedPref = context.getSharedPreferences("MuzikPrefs", MODE_PRIVATE)
            val editor = sharedPref.edit()
            editor.putBoolean("log_status", true)
            editor.putString("userID", curUser?.userId)
            editor.putString("username", curUser?.username)
            editor.putString("avatarURL", curUser?.avatarUrl)
            editor.apply()

            navController.navigate(R.id.navigation_home)
        }
        else {}
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { result ->
            if (result.resultCode == RESULT_OK) {
                val intent = result.data
                if (intent != null) {
                    viewModel.viewModelScope.launch {
                        val signInResult = googleAuthUiClient.signInWithIntent(intent)
                        viewModel.onSignInResult(signInResult)
                    }
                }
            }
        }
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent, // Màu nền mặc định
        contentColor = Color.White, // Màu chữ
        content = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(colorStops = colorStops))
            ) {
                Column () {
                    Box (
                        modifier = Modifier
                            .fillMaxHeight(0.4f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_logo),
                            contentDescription = "Logo App",
                            modifier = Modifier.size(128.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Box (
                        modifier = Modifier
                            .fillMaxHeight(0.25f)
                            .fillMaxWidth(),

                    ) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = "Online Music App. \nFree on Muzik.",
                            textAlign = TextAlign.Center,
                            fontSize = 28.sp,
                            fontFamily = FontFamily(Font(R.font.magistral_bold)),
                            lineHeight = 32.sp
                        )
                    }

                    Column (
                        modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box() {
                            Button(
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1ED760),
                                ),
                                onClick = {
                                    navController.navigate(R.id.navigation_home)
                                    Toast.makeText(context, "Log in to enjoy unlimited functionality", Toast.LENGTH_LONG).show()

                                    val sharedPref = context.getSharedPreferences("MuzikPrefs", MODE_PRIVATE)
                                    val editor = sharedPref.edit()
                                    editor.putBoolean("log_status", true)
                                    editor.putString("userID", "")
                                    editor.putString("username", "Guest")
                                    editor.putString("avatarURL", "")
                                    editor.apply()
                                }) {
                                Text(
                                    text = "Guest",
                                    color = Color.Black,
                                    fontSize = 16.sp,
                                    fontFamily = FontFamily(Font(R.font.magistral_bold))
                                )
                            }
                        }

                        Box() {
                            OutlinedButton(
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .height(48.dp),
                                onClick = {
                                    viewModel.viewModelScope.launch {
                                        val signInIntentSender = googleAuthUiClient.signIn()
                                        launcher.launch(
                                            IntentSenderRequest.Builder(signInIntentSender ?: return@launch).build()
                                        )
                                    }
                                }
                            ) {
                                Image(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .fillMaxWidth(0.1f),
                                    painter = painterResource(id = R.drawable.ic_google),
                                    contentDescription = "Google Icon",
                                )
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = "Login with Google",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center,
                                    fontFamily = FontFamily(Font(R.font.magistral_bold))
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}