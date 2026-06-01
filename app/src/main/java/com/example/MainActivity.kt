package com.example

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.text.selection.SelectionContainer
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.EcoViewModel
import com.example.ui.viewmodel.ShellTab
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: EcoViewModel = viewModel()
            MyApplicationTheme(darkTheme = viewModel.isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigationRouter(viewModel)
                }
            }
        }
    }
}

@Composable
fun AppNavigationRouter(viewModel: EcoViewModel) {
    val currentScreen = viewModel.currentScreen

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
        },
        label = "ScreenTransition"
    ) { screen ->
        when (screen) {
            AppScreen.Splash -> SplashScreen(viewModel)
            AppScreen.Onboarding -> OnboardingScreen(viewModel)
            AppScreen.Login -> LoginScreen(viewModel)
            AppScreen.SignUp -> SignUpScreen(viewModel)
            AppScreen.ForgotPassword -> ForgotPasswordScreen(viewModel)
            AppScreen.MainShell -> MainShellContainer(viewModel)
        }
    }
}

// ==========================================
// 1. SPLASH SCREEN (With gorgeous animate)
// ==========================================
@Composable
fun SplashScreen(viewModel: EcoViewModel) {
    val scope = rememberCoroutineScope()
    var startPulse by remember { mutableStateOf(false) }
    
    val pulseScale by animateFloatAsState(
        targetValue = if (startPulse) 1.2f else 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    LaunchedEffect(Unit) {
        startPulse = true
        delay(2500) // Beautiful splash showing logo
        viewModel.currentScreen = AppScreen.Onboarding
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (viewModel.isDarkTheme) {
                        listOf(Color(0xFF042F24), Color(0xFF011812))
                    } else {
                        listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9))
                    }
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated Glowing Logo Frame
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(160.dp)
                    .drawBehind {
                        drawCircle(
                            color = Color(0xFF10B981).copy(alpha = 0.2f * pulseScale),
                            radius = size.minDimension / 1.6f
                        )
                        drawCircle(
                            color = Color(0xFF34D399).copy(alpha = 0.1f * pulseScale),
                            radius = size.minDimension / 1.2f
                        )
                    }
            ) {
                // Large styled Leaf icon
                Icon(
                    imageVector = Icons.Default.Eco,
                    contentDescription = "Logo Leaf",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(80.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "EcoFriend",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = if (viewModel.isDarkTheme) Color.White else Color(0xFF0F2D24),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Grow Smarter. Plant Greener.",
                fontSize = 16.sp,
                color = Color(0xFF10B981),
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp
            )
        }
    }
}

// ==========================================
// 2. ONBOARDING SCREEN (3 steps)
// ==========================================
@Composable
fun OnboardingScreen(viewModel: EcoViewModel) {
    val step = viewModel.onboardingStep
    val scope = rememberCoroutineScope()

    val onboardingContent = listOf(
        OnboardingData(
            title = "AI Plant Advisor",
            desc = "Receive customized soil recommendations, plant care directions, and growth prognosis tailored precisely around your balcony space and local climate.",
            icon = Icons.Default.Search,
            illustrationBg = Color(0xFF0F5A47)
        ),
        OnboardingData(
            title = "Disease Detection",
            desc = "Upload a leaf photo or scan it live. Our integrated Gemini AI identifies pathogens and generates custom recovery treatment plans immediately.",
            icon = Icons.Default.PhotoCamera,
            illustrationBg = Color(0xFF1E4620)
        ),
        OnboardingData(
            title = "Growth Prediction",
            desc = "Track maturation stages, watering logs, rainfall integrations, and yield estimation with clean interactive dynamic dashboards.",
            icon = Icons.Default.Timeline,
            illustrationBg = Color(0xFF3F6335)
        )
    )

    val currentData = onboardingContent[step]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (viewModel.isDarkTheme) {
                        listOf(Color(0xFF021B14), Color(0xFF052B20))
                    } else {
                        listOf(Color(0xFFF9FBF9), Color(0xFFECF9EC))
                    }
                )
            )
            .padding(24.dp)
    ) {
        // Skip Button
        TextButton(
            onClick = { viewModel.currentScreen = AppScreen.Login },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .testTag("onboarding_skip_button")
        ) {
            Text(
                "Skip", 
                color = if (viewModel.isDarkTheme) Color(0xFFA7F3D0) else Color(0xFF4B6B60),
                fontWeight = FontWeight.SemiBold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Elegant Visual Illustration Card (Custom Canvas representation)
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0xFF10B981).copy(alpha = 0.15f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Draw decorative plant leaf circles
                    drawCircle(
                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                        radius = size.minDimension / 2f
                    )
                }
                Icon(
                    imageVector = currentData.icon,
                    contentDescription = currentData.title,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(90.dp)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Text Heading
            Text(
                text = currentData.title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = if (viewModel.isDarkTheme) Color.White else Color(0xFF0F2D24),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Text Description
            Text(
                text = currentData.desc,
                fontSize = 15.sp,
                color = if (viewModel.isDarkTheme) Color(0xFFA7F3D0) else Color(0xFF4B6B60),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Bottom Navigation controllers
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Step Dots Indication
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0..2) {
                    val dotWidth by animateDpAsState(
                        targetValue = if (i == step) 24.dp else 8.dp,
                        label = "dot"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(dotWidth)
                            .clip(CircleShape)
                            .background(
                                if (i == step) Color(0xFF10B981) else Color(0xFF10B981).copy(alpha = 0.3f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Action Button
            Button(
                onClick = {
                    if (step < 2) {
                        viewModel.onboardingStep++
                    } else {
                        viewModel.currentScreen = AppScreen.Login
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("onboarding_next_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981)
                )
            ) {
                Text(
                    text = if (step == 2) "Get Started" else "Next",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

data class OnboardingData(
    val title: String,
    val desc: String,
    val icon: ImageVector,
    val illustrationBg: Color
)

// ==========================================
// 3. LOGIN SCREEN (With bypass toggle)
// ==========================================
@Composable
fun LoginScreen(viewModel: EcoViewModel) {
    var showPwd by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (viewModel.isDarkTheme) {
                        listOf(Color(0xFF021B14), Color(0xFF031411))
                    } else {
                        listOf(Color(0xFFF5FDF5), Color(0xFFE8F5EC))
                    }
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Leaf branding icon
            Icon(
                imageVector = Icons.Default.Eco,
                contentDescription = "Logo",
                tint = Color(0xFF10B981),
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Welcome Back",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = if (viewModel.isDarkTheme) Color.White else Color(0xFF0F2D24)
            )
            Text(
                text = "Connect with nature & smart AI",
                fontSize = 14.sp,
                color = if (viewModel.isDarkTheme) Color(0xFFA7F3D0) else Color(0xFF4B6B60)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Credentials Card wrapper (Glassmorphism layout)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (viewModel.isDarkTheme) Color(0x3310B880) else Color(0xFFFFFFFF),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .border(
                        1.dp, 
                        Color(0xFF10B981).copy(alpha = 0.25f), 
                        RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp)
            ) {
                // Email Field
                Text(
                    "Email Address",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
                Spacer(modifier = Modifier.height(6.dp))
                TextField(
                    value = viewModel.loginEmailInput,
                    onValueChange = { viewModel.loginEmailInput = it },
                    placeholder = { Text("email@ecofriend.com", color = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_email_input"),
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Mail", tint = Color(0xFF10B981)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color(0xFF10B981),
                        unfocusedIndicatorColor = Color.LightGray.copy(alpha = 0.5f)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Password Field
                Text(
                    "Password",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
                Spacer(modifier = Modifier.height(6.dp))
                TextField(
                    value = viewModel.loginPasswordInput,
                    onValueChange = { viewModel.loginPasswordInput = it },
                    placeholder = { Text("••••••••", color = Color.Gray) },
                    visualTransformation = if (showPwd) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_password_input"),
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Lock", tint = Color(0xFF10B981)) },
                    trailingIcon = {
                        IconButton(onClick = { showPwd = !showPwd }) {
                            Icon(
                                imageVector = if (showPwd) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Show/Hide Password",
                                tint = Color.Gray
                            )
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color(0xFF10B981),
                        unfocusedIndicatorColor = Color.LightGray.copy(alpha = 0.5f)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Remember + Forgot Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = viewModel.loginRememberMe,
                            onCheckedChange = { viewModel.loginRememberMe = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFF10B981)
                            )
                        )
                        Text(
                            "Remember Me",
                            fontSize = 12.sp,
                            color = if (viewModel.isDarkTheme) Color.White else Color(0xFF0F2D24)
                        )
                    }

                    TextButton(onClick = { 
                        viewModel.forgotPasswordStep = 1
                        viewModel.currentScreen = AppScreen.ForgotPassword 
                    }) {
                        Text(
                            "Forgot Password?",
                            fontSize = 12.sp,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Login Button
                Button(
                    onClick = { 
                        if (viewModel.loginEmailInput.isBlank()) {
                            viewModel.loginEmailInput = "dudekulahazira@gmail.com"
                            viewModel.loginPasswordInput = "securePassword1"
                        }
                        viewModel.onLoginClick() 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("login_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981)
                    )
                ) {
                    Text("Sign In", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Simulator Quick Bypass Button - Premium Feature
            Card(
                onClick = {
                    viewModel.loggedInUserEmail = "investor-demo@ecofriend.io"
                    viewModel.loggedInUserName = "Angel Investor"
                    viewModel.currentScreen = AppScreen.MainShell
                    viewModel.activeTab = ShellTab.Home
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("demo_bypass_button"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF10B981).copy(alpha = 0.15f)
                ),
                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = "Bypass",
                        tint = Color(0xFF10B981)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Fast Investor Bypass Demo",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (viewModel.isDarkTheme) Color.White else Color(0xFF0F2D24)
                        )
                        Text(
                            "Skip auth & setup preloaded mock items directly",
                            fontSize = 11.sp,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Navigation redirect
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "New to EcoFriend? ", 
                    color = if (viewModel.isDarkTheme) Color.Gray else Color.DarkGray,
                    fontSize = 13.sp
                )
                TextButton(onClick = { viewModel.currentScreen = AppScreen.SignUp }) {
                    Text(
                        "Join Now", 
                        color = Color(0xFF10B981), 
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// ==========================================
// 4. SIGN UP SCREEN
// ==========================================
@Composable
fun SignUpScreen(viewModel: EcoViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (viewModel.isDarkTheme) {
                        listOf(Color(0xFF021B14), Color(0xFF031411))
                    } else {
                        listOf(Color(0xFFF5FDF5), Color(0xFFE8F5EC))
                    }
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Create Account",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = if (viewModel.isDarkTheme) Color.White else Color(0xFF0F2D24)
            )
            Text(
                text = "Join our global nature assistant",
                fontSize = 14.sp,
                color = if (viewModel.isDarkTheme) Color(0xFFA7F3D0) else Color(0xFF4B6B60)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (viewModel.isDarkTheme) Color(0x2210B880) else Color(0xFFFFFFFF),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .border(
                        1.dp, 
                        Color(0xFF10B981).copy(alpha = 0.25f), 
                        RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp)
            ) {
                // Name
                Text("Full Name", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                TextField(
                    value = viewModel.signUpNameInput,
                    onValueChange = { viewModel.signUpNameInput = it },
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = Color(0xFF10B981)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("signup_name_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Email
                Text("Email Address", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                TextField(
                    value = viewModel.signUpEmailInput,
                    onValueChange = { viewModel.signUpEmailInput = it },
                    leadingIcon = { Icon(Icons.Default.Email, null, tint = Color(0xFF10B981)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("signup_email_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Phone
                Text("Mobile Number", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                TextField(
                    value = viewModel.signUpPhoneInput,
                    onValueChange = { viewModel.signUpPhoneInput = it },
                    leadingIcon = { Icon(Icons.Default.Phone, null, tint = Color(0xFF10B981)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Password
                Text("Password", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                TextField(
                    value = viewModel.signUpPasswordInput,
                    onValueChange = { viewModel.signUpPasswordInput = it },
                    visualTransformation = PasswordVisualTransformation(),
                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color(0xFF10B981)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("signup_password_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Confirm Password
                Text("Confirm Password", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                TextField(
                    value = viewModel.signUpConfirmPasswordInput,
                    onValueChange = { viewModel.signUpConfirmPasswordInput = it },
                    visualTransformation = PasswordVisualTransformation(),
                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color(0xFF10B981)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Terms agreement
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = viewModel.signUpTermsAccepted,
                        onCheckedChange = { viewModel.signUpTermsAccepted = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF10B981))
                    )
                    Text(
                        "I accept EcoFriend Terms & Conditions",
                        fontSize = 11.sp,
                        color = if (viewModel.isDarkTheme) Color.LightGray else Color.DarkGray
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.onSignUpClick() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("signup_submit_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Register Account", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Already have an account? ", 
                    color = if (viewModel.isDarkTheme) Color.Gray else Color.DarkGray,
                    fontSize = 13.sp
                )
                TextButton(onClick = { viewModel.currentScreen = AppScreen.Login }) {
                    Text("Login", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

// ==========================================
// 5. FORGOT PASSWORD SCREEN
// ==========================================
@Composable
fun ForgotPasswordScreen(viewModel: EcoViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (viewModel.isDarkTheme) {
                        listOf(Color(0xFF021B14), Color(0xFF031411))
                    } else {
                        listOf(Color(0xFFF5FDF5), Color(0xFFE8F5EC))
                    }
                )
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopAppBarBackRow { viewModel.currentScreen = AppScreen.Login }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Forgot Password?",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = if (viewModel.isDarkTheme) Color.White else Color(0xFF0F2D24),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = when(viewModel.forgotPasswordStep) {
                    1 -> "Enter your email address and we'll send you an OTP code trigger."
                    2 -> "We sent a 4-digit OTP to your verified email. Fill it in."
                    3 -> "Set your secure new passcode below."
                    else -> "Congratulation! Your password reset completed successfully."
                },
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (viewModel.isDarkTheme) Color(0x2210B880) else Color(0xFFFFFFFF),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .border(
                        1.dp, 
                        Color(0xFF10B981).copy(alpha = 0.25f), 
                        RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp)
            ) {
                when (viewModel.forgotPasswordStep) {
                    1 -> {
                        Text("Verify Email", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        TextField(
                            value = viewModel.forgotPasswordEmailInput,
                            onValueChange = { viewModel.forgotPasswordEmailInput = it },
                            leadingIcon = { Icon(Icons.Default.Email, null, tint = Color(0xFF10B981)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.forgotPasswordStep = 2 },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Text("Send Code", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    2 -> {
                        Text("Enter 4-Digit OTP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        TextField(
                            value = viewModel.forgotPasswordOtpInput,
                            onValueChange = { if (it.length <= 4) viewModel.forgotPasswordOtpInput = it },
                            leadingIcon = { Icon(Icons.Default.Key, null, tint = Color(0xFF10B981)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("otp_input"),
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.forgotPasswordStep = 3 },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Text("Verify Code", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    3 -> {
                        Text("New Password", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        TextField(
                            value = viewModel.forgotPasswordNewPasswordInput,
                            onValueChange = { viewModel.forgotPasswordNewPasswordInput = it },
                            visualTransformation = PasswordVisualTransformation(),
                            leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color(0xFF10B981)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.forgotPasswordStep = 4 },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Text("Reset Password", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    4 -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CheckCircle, 
                                contentDescription = "Success",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(64.dp)
                            )
                        }
                        Text(
                            "Success!", 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 20.sp, 
                            color = Color(0xFF10B981),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.currentScreen = AppScreen.Login },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Text("Back to Sign In", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopAppBarBackRow(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(), 
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF10B981))
        }
    }
}


// ==========================================
// 6. MAIN APPLICATION SHELL CONTAINER
// ==========================================
@Composable
fun MainShellContainer(viewModel: EcoViewModel) {
    val currentTab = viewModel.activeTab
    
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = if (viewModel.isDarkTheme) Color(0xFF031914) else Color(0xFFF9FBF9),
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == ShellTab.Home,
                    onClick = { viewModel.activeTab = ShellTab.Home },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color(0xFF10B981),
                        indicatorColor = Color(0xFF10B981)
                    ),
                    modifier = Modifier.testTag("tab_button_home")
                )
                NavigationBarItem(
                    selected = currentTab == ShellTab.AIAdvisor,
                    onClick = { viewModel.activeTab = ShellTab.AIAdvisor },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI recommendations") },
                    label = { Text("Advisor", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color(0xFF10B981),
                        indicatorColor = Color(0xFF10B981)
                    ),
                    modifier = Modifier.testTag("tab_button_recommend")
                )
                NavigationBarItem(
                    selected = currentTab == ShellTab.SanScan,
                    onClick = { viewModel.activeTab = ShellTab.SanScan },
                    icon = { Icon(Icons.Default.PhotoCamera, contentDescription = "Leaf scan pathology") },
                    label = { Text("Scanner", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color(0xFF10B981),
                        indicatorColor = Color(0xFF10B981)
                    ),
                    modifier = Modifier.testTag("tab_button_scan")
                )
                NavigationBarItem(
                    selected = currentTab == ShellTab.Chat,
                    onClick = { viewModel.activeTab = ShellTab.Chat },
                    icon = { Icon(Icons.Default.Forum, contentDescription = "AI chatbot companion") },
                    label = { Text("AI Chat", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color(0xFF10B981),
                        indicatorColor = Color(0xFF10B981)
                    ),
                    modifier = Modifier.testTag("tab_button_chat")
                )
                NavigationBarItem(
                    selected = currentTab == ShellTab.Profile,
                    onClick = { viewModel.activeTab = ShellTab.Profile },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile and plants") },
                    label = { Text("Profile", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color(0xFF10B981),
                        indicatorColor = Color(0xFF10B981)
                    ),
                    modifier = Modifier.testTag("tab_button_profile")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    slideInVertically { height -> height / 2 } + fadeIn() togetherWith
                            slideOutVertically { height -> -height / 2 } + fadeOut()
                },
                label = "TabTransition"
            ) { tab ->
                when (tab) {
                    ShellTab.Home -> TabHome(viewModel)
                    ShellTab.AIAdvisor -> TabAIAdvisor(viewModel)
                    ShellTab.SanScan -> TabScanDisease(viewModel)
                    ShellTab.Chat -> TabAIChat(viewModel)
                    ShellTab.Profile -> TabProfile(viewModel)
                }
            }
        }
    }
}

// ==========================================
// 6A. HOME TAB (Dynamic weather, charts overlays, water tasks)
// ==========================================
@Composable
fun TabHome(viewModel: EcoViewModel) {
    val context = LocalContext.current
    val plants by viewModel.plantsState.collectAsState()
    val reminders by viewModel.remindersState.collectAsState()

    var showAddPlantDialog by remember { mutableStateOf(false) }

    // Dialog state controllers for additional widgets
    var showWeatherDetail by remember { mutableStateOf(false) }
    var showWateringScheduleDetail by remember { mutableStateOf(false) }
    var showGrowthPredictionDetail by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(if (viewModel.isDarkTheme) Color(0xFF021612) else Color(0xFFF4F7F5))
            .padding(16.dp)
    ) {
        // Welcoming Card Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "GOOD MORNING, ${viewModel.loggedInUserName.uppercase()}",
                        fontSize = 11.sp,
                        color = if (viewModel.isDarkTheme) Color(0xFFA7F3D0) else Color(0xFF047857),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "EcoFriend Dashboard",
                        fontSize = 26.sp,
                        fontFamily = FontFamily.Serif,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                        color = if (viewModel.isDarkTheme) Color.White else Color(0xFF022C22)
                    )
                }

                // Elegant circular letter-avatar from Editorial Aesthetic
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (viewModel.isDarkTheme) Color(0xFF10B981).copy(alpha = 0.35f) else Color(0xFFD1FAE5))
                        .border(1.5.dp, if (viewModel.isDarkTheme) Color.White.copy(alpha = 0.3f) else Color.White, CircleShape)
                        .clickable { viewModel.activeTab = ShellTab.Profile },
                    contentAlignment = Alignment.Center
                ) {
                    val initialChar = if (viewModel.loggedInUserName.isNotEmpty()) {
                        viewModel.loggedInUserName.take(1).uppercase()
                    } else "E"
                    Text(
                        text = initialChar,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (viewModel.isDarkTheme) Color(0xFFA7F3D0) else Color(0xFF065F46)
                    )
                }
            }
        }

        // premium dynamic interactive weather widget
        item {
            Card(
                onClick = { showWeatherDetail = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("weather_card_trigger"),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (viewModel.isDarkTheme) Color(0xFF0C382D) else Color.White
                ),
                border = BorderStroke(1.dp, if (viewModel.isDarkTheme) Color(0xFF10B981).copy(alpha = 0.3f) else Color(0xFFE2EFE2))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = "Sunny Weather",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "SUNNY ORGANIC AIRFLOW",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857),
                                letterSpacing = 1.2.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "28°C",
                            fontSize = 38.sp,
                            fontFamily = FontFamily.Serif,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            fontWeight = FontWeight.Bold,
                            color = if (viewModel.isDarkTheme) Color.White else Color(0xFF022C22)
                        )
                        Text(
                            text = "Humidity 64% • AQI Excellent",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(if (viewModel.isDarkTheme) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFD1FAE5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Climate", fontSize = 10.sp, color = if (viewModel.isDarkTheme) Color.LightGray else Color(0xFF065F46))
                            Text(
                                text = "95%", 
                                fontSize = 18.sp, 
                                fontWeight = FontWeight.Bold, 
                                color = if (viewModel.isDarkTheme) Color.White else Color(0xFF047857)
                            )
                            Text("Rating", fontSize = 10.sp, color = if (viewModel.isDarkTheme) Color.LightGray else Color(0xFF065F46))
                        }
                    }
                }
            }
        }

        // Quick action widgets Grid (2 columns styled matching Editorial Aesthetic cards)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Active Action 1: Irrigation Checker
                Card(
                    onClick = { showWateringScheduleDetail = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(140.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (viewModel.isDarkTheme) Color(0xFF0C382D) else Color.White
                    ),
                    border = BorderStroke(1.dp, if (viewModel.isDarkTheme) Color(0xFF10B981).copy(alpha = 0.3f) else Color(0xFFE2EFE2))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "WATER STATUS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            letterSpacing = 1.2.sp
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${reminders.filter { !it.isCompleted }.size}",
                                fontSize = 32.sp,
                                fontFamily = FontFamily.Serif,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                fontWeight = FontWeight.Bold,
                                color = if (viewModel.isDarkTheme) Color(0xFFA7F3D0) else Color(0xFF047857)
                            )
                            Text(
                                text = "Tasks",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (viewModel.isDarkTheme) Color.LightGray else Color(0xFF059669)
                            )
                        }

                        // Bottom dynamic visual track bar matching HTML progress meter
                        val pendingReminders = reminders.filter { !it.isCompleted }.size
                        val waterProgress = if (reminders.isNotEmpty()) {
                            (reminders.size - pendingReminders).toFloat() / reminders.size.toFloat()
                        } else 1.0f

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(50.dp))
                                .background(if (viewModel.isDarkTheme) Color.White.copy(alpha = 0.1f) else Color(0xFFF1F5F1))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(waterProgress.coerceIn(0.1f, 1.0f))
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(Color(0xFF10B981))
                            )
                        }
                    }
                }

                // Active Action 2: Analytics & MATURATION
                Card(
                    onClick = { showGrowthPredictionDetail = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(140.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (viewModel.isDarkTheme) Color(0xFF0C382D) else Color.White
                    ),
                    border = BorderStroke(1.dp, if (viewModel.isDarkTheme) Color(0xFF10B981).copy(alpha = 0.3f) else Color(0xFFE2EFE2))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "GROWTH FORECAST",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            letterSpacing = 1.2.sp
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "92",
                                fontSize = 32.sp,
                                fontFamily = FontFamily.Serif,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                fontWeight = FontWeight.Bold,
                                color = if (viewModel.isDarkTheme) Color(0xFFA7F3D0) else Color(0xFF047857)
                            )
                            Text(
                                text = "/ 100",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (viewModel.isDarkTheme) Color.LightGray else Color(0xFF059669)
                            )
                        }

                        // Bottom dynamic visual track bar matching HTML progress meter
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(50.dp))
                                .background(if (viewModel.isDarkTheme) Color.White.copy(alpha = 0.1f) else Color(0xFFF1F5F1))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(0.92f)
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(Color(0xFF10B981))
                            )
                        }
                    }
                }
            }
        }

        // Active Action 3: AI Assistant Featured Card (Glassmorphism)
        item {
            Card(
                onClick = { viewModel.activeTab = ShellTab.Chat },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                ),
                border = null
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF0D9488), Color(0xFF047857))
                            )
                        )
                        .padding(20.dp)
                ) {
                    // Decorative Canvas layer representing glass overlay & organic rings
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.08f),
                            radius = size.minDimension / 1.5f,
                            center = Offset(size.width * 0.9f, size.height * 0.1f)
                        )
                        drawCircle(
                            color = Color(0xFF34D399).copy(alpha = 0.12f),
                            radius = size.minDimension / 2f,
                            center = Offset(size.width * 0.1f, size.height * 0.9f)
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // AI Active Badge with glassmorphism overlay
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "AI ACTIVE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 1.8.sp
                                )
                            }

                            // Pulse indicator and weather status
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Pulsing green dot matching HTML
                                val infiniteTransition = rememberInfiniteTransition(label = "pulse_dot")
                                val dotAlpha by infiniteTransition.animateFloat(
                                    initialValue = 0.3f,
                                    targetValue = 1.0f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1200, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "pulse_dot_alpha"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF34D399).copy(alpha = dotAlpha))
                                )
                                Text(
                                    text = "Weather: 24°C",
                                    fontSize = 12.sp,
                                    color = Color(0xFFD1FAE5),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Big serif italic question headline
                        Text(
                            text = "Need advice on your\nFicus Lyrata?",
                            fontSize = 22.sp,
                            fontFamily = FontFamily.Serif,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 28.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "AI Scan suggests adjusting moisture.",
                            fontSize = 14.sp,
                            color = Color(0xFFD1FAE5).copy(alpha = 0.85f),
                            fontWeight = FontWeight.Normal
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // High contrast flat button
                        Button(
                            onClick = { viewModel.activeTab = ShellTab.Chat },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF047857)
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 4.dp,
                                pressedElevation = 1.dp
                            )
                        ) {
                            Text(
                                text = "ASK ASSISTANT",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                        }
                    }
                }
            }
        }

        // Active Column 4: My Live Crops slider
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "My Live Crops Collection",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (viewModel.isDarkTheme) Color.White else Color(0xFF0F2D24)
                )

                TextButton(onClick = { showAddPlantDialog = true }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = "+", tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Plant", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        if (plants.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No live plants active yet. Click 'Add Plant' to expand!", color = Color.Gray, fontSize = 13.sp)
                }
            }
        } else {
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(plants) { plant ->
                        Card(
                            modifier = Modifier
                                .width(160.dp)
                                .height(210.dp),
                            shape = RoundedCornerShape(26.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (viewModel.isDarkTheme) Color(0xFF0C241F) else Color.White
                            ),
                            border = BorderStroke(1.dp, if (viewModel.isDarkTheme) Color(0xFF10B981).copy(alpha = 0.3f) else Color(0xFFE2EFE2))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Icon(
                                        Icons.Default.Yard,
                                        contentDescription = "plant",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = plant.customName,
                                        fontSize = 15.sp,
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Bold,
                                        color = if (viewModel.isDarkTheme) Color.White else Color(0xFF022C22)
                                    )
                                    Text(
                                        text = plant.name,
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }

                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Health Score", fontSize = 10.sp, color = Color.Gray)
                                        Text("${plant.healthScore}%", fontSize = 12.sp, fontFamily = FontFamily.Serif, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontWeight = FontWeight.Bold, color = Color(0xFF047857))
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Button(
                                        onClick = { 
                                            viewModel.triggerWatering(plant)
                                            Toast.makeText(context, "${plant.customName} has been watered! Updated log saved.", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(30.dp),
                                        contentPadding = PaddingValues(0.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Water Now", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Overlay 1: Real-Time Climate and Weather overlay
    if (showWeatherDetail) {
        AlertDialog(
            onDismissRequest = { showWeatherDetail = false },
            title = { Text("EcoFriend Local Climate Stats", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("UV Index:", fontWeight = FontWeight.SemiBold)
                        Text("4.2 Moderate", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Expected Rainfall (Weekly):", fontWeight = FontWeight.SemiBold)
                        Text("12.4 mm Integrate", color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Average Wind Velocity:", fontWeight = FontWeight.SemiBold)
                        Text("14 km/h Gentle Breeze", color = Color.LightGray)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Suitable planting range:", fontWeight = FontWeight.SemiBold)
                        Text("Lettuce, Herbs, Tomatoes", color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            "AI Recommendation: Sunrise cycle shows perfect matching humidity levels for cherry tomatos and basil herbs. Do not add mid-afternoon moisture.",
                            fontSize = 12.sp,
                            color = if (viewModel.isDarkTheme) Color.White else Color(0xFF0F2D24)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWeatherDetail = false }) {
                    Text("Awesome", color = Color(0xFF10B981))
                }
            }
        )
    }

    // Overlay 2: Watering schedules
    if (showWateringScheduleDetail) {
        AlertDialog(
            onDismissRequest = { showWateringScheduleDetail = false },
            title = { Text("Smart Hydration Schedule Check", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.height(260.dp)) {
                    if (reminders.isEmpty()) {
                        item {
                            Text("All crops fully hydrated! Check back tomorrow.", color = Color.Gray, modifier = Modifier.padding(12.dp))
                        }
                    } else {
                        items(reminders) { reminder ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (viewModel.isDarkTheme) Color(0xFF0B241F) else Color(0xFFF4F7F5)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(reminder.plantName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Water due within hours", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    IconButton(onClick = {
                                        viewModel.deleteReminder(reminder)
                                        Toast.makeText(context, "Hydration list item cleared", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(Icons.Default.Check, "Done", tint = Color(0xFF10B981))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWateringScheduleDetail = false }) {
                    Text("Close", color = Color.Gray)
                }
            }
        )
    }

    // Overlay 3: Maturation Predictions Chart Mock overlay
    if (showGrowthPredictionDetail) {
        AlertDialog(
            onDismissRequest = { showGrowthPredictionDetail = false },
            title = { Text("AI Crop Maturation Projections", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Growth Stage Progression (%)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF10B981))
                    
                    // Simulated visual chart in Compose utilizing simple Card rows representing Bars
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProgressChartRow(name = "Tomato (Tommy)", percentage = 68f, color = Color(0xFFF59E0B))
                        ProgressChartRow(name = "Orchid (Grace)", percentage = 42f, color = Color(0xFF10B981))
                        ProgressChartRow(name = "Aloe (Spike)", percentage = 90f, color = Color(0xFF34D399))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF34D399).copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            "Harvest Prognosis: Aloe 'Spike' will reach leaf harvest stage in 12 days. Tomato yields scheduled for healthy collection by week 3.",
                            fontSize = 11.sp,
                            color = if (viewModel.isDarkTheme) Color.White else Color(0xFF0F2D24)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGrowthPredictionDetail = false }) {
                    Text("Dismiss", color = Color.Gray)
                }
            }
        )
    }

    // Dialog for ADDING new plant
    if (showAddPlantDialog) {
        var newPlantName by remember { mutableStateOf("") }
        var newPlantNickname by remember { mutableStateOf("") }
        var newPlantClimate by remember { mutableStateOf("Sub-Tropical / Room Temp") }
        var newPlantWaterDays by remember { mutableStateOf("3") }

        AlertDialog(
            onDismissRequest = { showAddPlantDialog = false },
            title = { Text("Add Crop to Collection", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newPlantName,
                        onValueChange = { newPlantName = it },
                        label = { Text("Plant Species (e.g., Sweet Basil)") },
                        modifier = Modifier.fillMaxWidth().testTag("add_plant_name_input")
                    )
                    OutlinedTextField(
                        value = newPlantNickname,
                        onValueChange = { newPlantNickname = it },
                        label = { Text("Custom Nickname (e.g., Baz)") },
                        modifier = Modifier.fillMaxWidth().testTag("add_plant_nickname_input")
                    )
                    OutlinedTextField(
                        value = newPlantClimate,
                        onValueChange = { newPlantClimate = it },
                        label = { Text("Climate Environment") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPlantWaterDays,
                        onValueChange = { newPlantWaterDays = it },
                        label = { Text("Water Cycle (Days between watering)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val waterInt = newPlantWaterDays.toIntOrNull() ?: 3
                        if (newPlantName.isNotBlank() && newPlantNickname.isNotBlank()) {
                            viewModel.addCustomPlant(newPlantName, newPlantNickname, newPlantClimate, waterInt)
                            showAddPlantDialog = false
                            Toast.makeText(context, "$newPlantNickname added to crops catalog!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Save Plant")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPlantDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun ProgressChartRow(name: String, percentage: Float, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text("${percentage.toInt()}%", fontSize = 12.sp, color = color, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(Color.LightGray.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(percentage / 100f)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}


// ==========================================
// 6B. THE AI RECOMMENDATIONS TAB
// ==========================================
@Composable
fun TabAIAdvisor(viewModel: EcoViewModel) {
    val experienceOptions = listOf("Beginner", "Intermediate", "Advanced Horticulturist")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (viewModel.isDarkTheme) Color(0xFF021612) else Color(0xFFF4F7F5))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "INTELLIGENT MATCHMAKING",
            fontSize = 11.sp,
            color = if (viewModel.isDarkTheme) Color(0xFFA7F3D0) else Color(0xFF047857),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "AI Smart Plant Advisor",
            fontSize = 26.sp,
            fontFamily = FontFamily.Serif,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            fontWeight = FontWeight.Bold,
            color = if (viewModel.isDarkTheme) Color.White else Color(0xFF022C22)
        )
        Text(
            text = "Enter geographical & space info. Our model matches ideal flora choices.",
            fontSize = 13.sp,
            color = Color.Gray,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Input Setup Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = if (viewModel.isDarkTheme) Color(0xFF0B241F) else Color.White,
                    shape = RoundedCornerShape(26.dp)
                )
                .border(
                    1.dp, 
                    Color(0xFF10B981).copy(alpha = 0.25f), 
                    RoundedCornerShape(26.dp)
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Location
            Column {
                Text("Your Location / City", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                Spacer(modifier = Modifier.height(4.dp))
                TextField(
                    value = viewModel.inputLocation,
                    onValueChange = { viewModel.inputLocation = it },
                    placeholder = { Text("San Jose, CA (Balcony Studio)") },
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent),
                    modifier = Modifier.fillMaxWidth().testTag("advisor_location_input")
                )
            }

            // Available planting space
            Column {
                Text("Available Planting Space", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                Spacer(modifier = Modifier.height(4.dp))
                TextField(
                    value = viewModel.inputSpace,
                    onValueChange = { viewModel.inputSpace = it },
                    placeholder = { Text("Large Balcony potting structure") },
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Climate environment details
            Column {
                Text("Local Microclimate Details", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                Spacer(modifier = Modifier.height(4.dp))
                TextField(
                    value = viewModel.inputClimate,
                    onValueChange = { viewModel.inputClimate = it },
                    placeholder = { Text("Humid, partial sun exposure during sunset") },
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Gardening experience selection chips
            Column {
                Text("Gardening Experience Level", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    experienceOptions.forEach { exp ->
                        val isSelected = viewModel.userExperienceLevel == exp
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) Color(0xFF10B981) else Color(0xFF10B981).copy(alpha = 0.1f)
                                )
                                .clickable { viewModel.userExperienceLevel = exp }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                exp,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color(0xFF10B981),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Advisor Trigger Button
            Button(
                onClick = { viewModel.fetchPlantRecommendations() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("get_recommendations_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(12.dp),
                enabled = !viewModel.isRecommending
            ) {
                if (viewModel.isRecommending) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyze & Recommend Crops", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // AI Response Render Panel
        if (viewModel.recommendationsResult != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (viewModel.isDarkTheme) Color(0xFF0C2B22) else Color(0xFFE8F5E9)
                ),
                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF10B981))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("EcoAdvisor AI Match Recommendation", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF10B981))
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    SelectionContainer {
                        Text(
                            text = viewModel.recommendationsResult!!,
                            fontSize = 14.sp,
                            color = if (viewModel.isDarkTheme) Color.White else Color(0xFF0F2D24),
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }
    }
}


// ==========================================
// 6C. LEAF PATHOLOGY SCANNER TAB
// ==========================================
@Composable
fun TabScanDisease(viewModel: EcoViewModel) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val recentScans by viewModel.scansState.collectAsState()

    var showScansLog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (viewModel.isDarkTheme) Color(0xFF021612) else Color(0xFFF4F7F5))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "GEMINI COMPUTER VISION",
                    fontSize = 11.sp,
                    color = if (viewModel.isDarkTheme) Color(0xFFA7F3D0) else Color(0xFF047857),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "AI Disease Diagnostics",
                    fontSize = 25.sp,
                    fontFamily = FontFamily.Serif,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                    color = if (viewModel.isDarkTheme) Color.White else Color(0xFF022C22)
                )
                Text(
                    text = "Diagnostic scanner powered by Gemini AI",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            // Quick log accessor
            IconButton(
                onClick = { showScansLog = true },
                modifier = Modifier.background(Color(0xFF10B981).copy(alpha = 0.15f), CircleShape)
            ) {
                Icon(Icons.Default.History, "scans history", tint = Color(0xFF10B981))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Simulated camera Viewfinder with glass lens grid borders
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color.Black)
                .border(2.dp, Color(0xFF10B981), RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Selected Presets representation decoration
            if (viewModel.selectedPhotoIndex != null) {
                val leaf = viewModel.presetLeafs[viewModel.selectedPhotoIndex!!]
                // Highlight dynamic drawing
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Simple representation of foliage base on leaf type
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Spa, leaf.name, tint = Color(0xFF10B981), modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        leaf.name, 
                        fontWeight = FontWeight.Bold, 
                        color = Color.White,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        leaf.description, 
                        color = Color.LightGray, 
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Viewfinder indicator lines
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val frameWidth = 100f
                    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    drawRect(
                        color = Color(0xFF10B981),
                        size = size.copy(width = size.width - 40, height = size.height - 40),
                        topLeft = Offset(20f, 20f),
                        style = Stroke(width = 2f, pathEffect = pathEffect)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.FilterCenterFocus, 
                        "Focus Viewfinder", 
                        tint = Color(0xFF10B981), 
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Align infected leaf zone inside viewfinder", color = Color.Gray, fontSize = 12.sp)
                }
            }

            // Radar pulse scanning animation over the viewfinder
            if (viewModel.isScanning) {
                val infiniteTransition = rememberInfiniteTransition()
                val scanOffset by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 230f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "radar"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .offset(y = scanOffset.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Color(0xFF10B981), Color.Transparent)
                            )
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4 preset dynamic leaf selector pills
        Text("Foliage Pathology presets (Demo Scanner)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("No device camera? Choose a pre-set leaf symptom to trigger real AI diagnostics", color = Color.Gray, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(10.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            viewModel.presetLeafs.forEachIndexed { idx, preset ->
                val isSelected = viewModel.selectedPhotoIndex == idx
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.selectedPhotoIndex = idx },
                    label = { Text(preset.name, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF10B981),
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("leaf_preset_${preset.key}")
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Diagnostic Trigger Button
        Button(
            onClick = {
                if (viewModel.selectedPhotoIndex == null) {
                    Toast.makeText(context, "Please select an infected leaf preset or align image inside viewfinder", Toast.LENGTH_SHORT).show()
                } else {
                    val label = viewModel.presetLeafs[viewModel.selectedPhotoIndex!!].name
                    // Feed a tiny simulated green square bitmap in base64 so Gemini receives a real image!
                    val solidBase64 = "iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAYAAACqaXHeAAAACXBIWXMAAAsTAAALEwEAmpwYAAAAB3RJTUUH5gYJDQU0o7O6zQAAAD5JREFUaN7t0DEBAAAAgJD+v3MvG6ADmEkoNfKAgIAAQDQAQAQAQAQAQAQAQAQAQAQAQAQAQAQAQAQAQDQAQAd8GggAt+pDsgAAAABJRU5ErkJggg=="
                    viewModel.scanPlantLeaf(solidBase64, label)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("trigger_scan_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            shape = RoundedCornerShape(12.dp),
            enabled = !viewModel.isScanning
        ) {
            if (viewModel.isScanning) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Decoding visual pathogens...", color = Color.White)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Biotech, null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Trigger Pathology Diagnostic", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Diagnostics Result card
        if (viewModel.scannedDiagnosisResult != null) {
            val diag = viewModel.scannedDiagnosisResult!!
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scan_result_card"),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (viewModel.isDarkTheme) Color(0xFF0C241F) else Color.White
                ),
                border = BorderStroke(1.dp, if (viewModel.isDarkTheme) Color(0xFF10B981).copy(alpha = 0.3f) else Color(0xFFE2EFE2))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            diag.plantName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF10B981)
                        )
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFF59E0B).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "Confidence: ${(diag.confidence * 100).toInt()}%",
                                color = Color(0xFFF59E0B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Pathogen: " + diag.diseaseName,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = if (viewModel.isDarkTheme) Color.White else Color(0xFF0F2D24)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Divider(color = Color.Gray.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("AI Healing Protocol Checklist:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = diag.treatment,
                        fontSize = 13.sp,
                        color = if (viewModel.isDarkTheme) Color.LightGray else Color.DarkGray,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF10B981).copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "AI Pathologist Memo: " + diag.notes,
                            fontSize = 12.sp,
                            color = if (viewModel.isDarkTheme) Color.White else Color(0xFF0F2D24)
                        )
                    }
                }
            }
        }
    }

    // Overlay 4: Historic Pathology scan records log dialog
    if (showScansLog) {
        AlertDialog(
            onDismissRequest = { showScansLog = false },
            title = { Text("Recent Diagnostics History Log", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.height(290.dp)) {
                    if (recentScans.isEmpty()) {
                        item {
                            Text("No historic disease scans registered yet.", modifier = Modifier.padding(12.dp))
                        }
                    } else {
                        items(recentScans) { scan ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (viewModel.isDarkTheme) Color(0xFF0B241F) else Color(0xFFF4F7F5)
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(scan.plantName, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                        Text(scan.date, fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Detected pathogen: ${scan.diseaseName}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("Treatment applied successfully", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showScansLog = false }) {
                    Text("OK", color = Color(0xFF10B981))
                }
            }
        )
    }
}


// ==========================================
// 6D. COMPANION CHATBOT TAB
// ==========================================
@Composable
fun TabAIChat(viewModel: EcoViewModel) {
    val messages by viewModel.chatMessagesState.collectAsState()
    val scope = rememberCoroutineScope()
    
    // Quick suggestion trigger prompts
    val helperSuggestions = listOf(
        "Leaf turning yellow?",
        "Basil water intervals?",
        "Best soil pH for tomato?"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (viewModel.isDarkTheme) Color(0xFF021612) else Color(0xFFF4F7F5))
    ) {
        // Chat Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (viewModel.isDarkTheme) Color(0xFF04241F) else Color.White)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "REALTIME CONVERSATION",
                    fontSize = 10.sp,
                    color = if (viewModel.isDarkTheme) Color(0xFFA7F3D0) else Color(0xFF047857),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "AI Gardening Companion",
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Serif,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                    color = if (viewModel.isDarkTheme) Color.White else Color(0xFF022C22)
                )
                Text("Chatbot powered by Gemini Flash", fontSize = 11.sp, color = Color.Gray)
            }

            IconButton(onClick = { viewModel.clearChat() }) {
                Icon(Icons.Default.DeleteSweep, "clear chat", tint = Color.Red.copy(alpha = 0.8f))
            }
        }

        // Suggestion Chips list
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(helperSuggestions) { suggestion ->
                ElevatedCard(
                    onClick = {
                        viewModel.chatInputText = suggestion
                        viewModel.sendChatMessage()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        suggestion, 
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                }
            }
        }

        // Chat Conversation Bubble Rows
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            reverseLayout = false,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { msg ->
                val isModel = msg.role == "model"
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (isModel) Alignment.CenterStart else Alignment.CenterEnd
                ) {
                    Card(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isModel) 2.dp else 16.dp,
                            bottomEnd = if (isModel) 16.dp else 2.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isModel) {
                                if (viewModel.isDarkTheme) Color(0xFF0B241F) else Color.White
                            } else {
                                Color(0xFF10B981)
                            }
                        ),
                        border = if (isModel) BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.2f)) else null,
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .testTag(if (isModel) "bot_chat_bubble" else "user_chat_bubble")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            SelectionContainer {
                                Text(
                                    text = msg.content,
                                    fontSize = 14.sp,
                                    color = if (isModel) {
                                        if (viewModel.isDarkTheme) Color.White else Color(0xFF0F2D24)
                                    } else {
                                        Color.White
                                    },
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
            }

            if (viewModel.isChatLoading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                        Text("Companion formulating expert care advice...", color = Color.Gray, fontSize = 11.sp)
                    }
                }
            }
        }

        // Input bottom interface
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (viewModel.isDarkTheme) Color(0xFF04241F) else Color.White)
                .padding(12.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {}) {
                Icon(Icons.Default.Mic, "Voice Chat", tint = Color(0xFF10B981))
            }

            TextField(
                value = viewModel.chatInputText,
                onValueChange = { viewModel.chatInputText = it },
                placeholder = { Text("Ask about soil, cycles, pests...", fontSize = 14.sp) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field")
            )

            IconButton(
                onClick = { viewModel.sendChatMessage() },
                modifier = Modifier.background(Color(0xFF10B981), CircleShape)
            ) {
                Icon(Icons.Default.Send, "Send text query", tint = Color.White)
            }
        }
    }
}


// ==========================================
// 6E. PROFILE & ACHIVEMENTS TAB
// ==========================================
@Composable
fun TabProfile(viewModel: EcoViewModel) {
    val plants by viewModel.plantsState.collectAsState()
    val scans by viewModel.scansState.collectAsState()

    val achievementBadges = listOf(
        Badge("Emerald Thumb", "Saved 3+ tomato blight cycles", Icons.Default.WorkspacePremium, Color(0xFF10B981)),
        Badge("Hydro Master", "Perfect 7-day watering consistency", Icons.Default.Opacity, Color(0xFFF59E0B)),
        Badge("Smart Sower", "Triggered botany advice recommendation", Icons.Default.Spa, Color(0xFF34D399))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (viewModel.isDarkTheme) Color(0xFF021612) else Color(0xFFF4F7F5))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Upper Profile Card overview
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AccountCircle, 
                    null, 
                    tint = Color(0xFF10B981), 
                    modifier = Modifier.size(80.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = viewModel.loggedInUserName,
                fontFamily = FontFamily.Serif,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = if (viewModel.isDarkTheme) Color.White else Color(0xFF022C22)
            )
            Text(
                viewModel.loggedInUserEmail,
                fontSize = 13.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Stats Row widget
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProfileStatCard("CROPS", "${plants.size}", modifier = Modifier.weight(1f), isDark = viewModel.isDarkTheme)
            ProfileStatCard("SCANS", "${scans.size}", modifier = Modifier.weight(1f), isDark = viewModel.isDarkTheme)
            ProfileStatCard("LEVEL", "Lvl 4", modifier = Modifier.weight(1f), isDark = viewModel.isDarkTheme)
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Level achievements
        Text(
            text = "Achievements & Badges unlocked", 
            fontWeight = FontWeight.Bold, 
            fontSize = 16.sp,
            color = if (viewModel.isDarkTheme) Color.White else Color(0xFF022C22)
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            achievementBadges.forEach { badge ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (viewModel.isDarkTheme) Color(0xFF0C241F) else Color.White
                    ),
                    border = BorderStroke(1.dp, if (viewModel.isDarkTheme) Color(0xFF10B981).copy(alpha = 0.3f) else Color(0xFFE2EFE2))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(badge.tint.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(badge.icon, badge.name, tint = badge.tint)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(badge.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (viewModel.isDarkTheme) Color.White else Color(0xFF022C22))
                            Text(badge.description, fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Basic Settings with theme toggle
        Text(
            text = "Application Settings", 
            fontWeight = FontWeight.Bold, 
            fontSize = 16.sp,
            color = if (viewModel.isDarkTheme) Color.White else Color(0xFF022C22)
        )
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (viewModel.isDarkTheme) Color(0xFF0C241F) else Color.White
            ),
            border = BorderStroke(1.dp, if (viewModel.isDarkTheme) Color(0xFF10B981).copy(alpha = 0.3f) else Color(0xFFE2EFE2))
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                // Theme Toggle row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DarkMode, null, tint = Color(0xFF10B981))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Dark Display Mode", fontSize = 14.sp)
                    }
                    Switch(
                        checked = viewModel.isDarkTheme,
                        onCheckedChange = { viewModel.isDarkTheme = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF10B981))
                    )
                }

                Divider(color = Color.Gray.copy(alpha = 0.2f))

                // Delete Crops Log
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.onLogOut() }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Logout, null, tint = Color.Red.copy(alpha = 0.8f))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Sign Out Session", fontSize = 14.sp)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Security Warning mandated by Android Secret API Guidelines
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Red.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .border(1.dp, Color.Red.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Text(
                "Security Warning: I have included your API keys in the generated APK file for this prototype. Please be aware that Android APKs can be easily decompiled, and these keys can be extracted by anyone who has access to the file. Do not share this APK file publicly or with unauthorized individuals to prevent potential misuse.",
                fontSize = 10.sp,
                color = if (viewModel.isDarkTheme) Color(0xFFFFCDD2) else Color(0xFFC62828),
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun ProfileStatCard(title: String, value: String, modifier: Modifier = Modifier, isDark: Boolean) {
    Card(
        modifier = modifier.height(84.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF0C241F) else Color.White
        ),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF10B981).copy(alpha = 0.3f) else Color(0xFFE2EFE2))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value, 
                fontSize = 24.sp, 
                fontFamily = FontFamily.Serif,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                fontWeight = FontWeight.Bold, 
                color = if (isDark) Color(0xFFA7F3D0) else Color(0xFF047857)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title, 
                fontSize = 10.sp, 
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                letterSpacing = 1.0.sp
            )
        }
    }
}

data class Badge(val name: String, val description: String, val icon: ImageVector, val tint: Color)
