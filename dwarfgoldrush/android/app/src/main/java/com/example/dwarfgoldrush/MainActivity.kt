package com.example.dwarfgoldrush

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.VideoView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import coil.decode.ImageDecoderDecoder
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

// --- Configuration ---
const val YOUR_REWARDED_AD_UNIT_ID = "ca-app-pub-1262202142447431/7738751536"
const val ADS_TO_FILL_BAR = 20
val MINING_DURATION_MS = TimeUnit.HOURS.toMillis(12)
val COOLDOWN_DURATION_MS = TimeUnit.HOURS.toMillis(4)

// --- Data Classes for State ---
data class MiningState(val adCount: Int, val miningStartedAt: Long, val cooldownStartedAt: Long)
data class UserProfile(val name: String, val walletAddress: String, val isLoggedIn: Boolean)
data class ReferralInfo(val wallet: String, val count: Int)

// --- Main Activity and Navigation Setup ---
object AppRoutes {
    const val INITIAL = "initial"
    const val LOGIN = "login"
    const val RULES = "rules"
    const val LOADING = "loading"
    const val MINING = "mining"
    const val ADMIN = "admin" // Secret admin route
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this) {}
        setContent {
            DwarfGoldRushTheme {
                val context = LocalContext.current
                val userProfile = loadUserProfile(context)
                val startDestination = if (userProfile.isLoggedIn) AppRoutes.LOADING else AppRoutes.INITIAL
                AppNavigation(startDestination)
            }
        }
    }
}

@Composable
fun AppNavigation(startDestination: String) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = startDestination) {
        composable(AppRoutes.INITIAL) { InitialScreen(navController) }
        composable(AppRoutes.LOGIN) { LoginScreen(navController) }
        composable(AppRoutes.RULES) { RulesScreen(navController) }
        composable(AppRoutes.LOADING) { LoadingScreen(navController) }
        composable(AppRoutes.MINING) { MiningScreen(navController) }
        composable(AppRoutes.ADMIN) { AdminScreen(navController) }
    }
}

// --- Screens ---
@Composable
fun InitialScreen(navController: NavController) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.tela_inicial),
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Dwarf Gold Rush", style = MaterialTheme.typography.headlineLarge, color = Color.White)
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { navController.navigate(AppRoutes.LOGIN) }) { Text("Login") }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { navController.navigate(AppRoutes.RULES) }) { Text("Regras") }
        }
    }
}

@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var walletAddress by remember { mutableStateOf("") }
    var referralCode by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Página de Login", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome") })
        OutlinedTextField(value = surname, onValueChange = { surname = it }, label = { Text("Sobrenome") })
        OutlinedTextField(value = walletAddress, onValueChange = { walletAddress = it }, label = { Text("Endereço da Wallet BSC Testnet") })
        OutlinedTextField(value = referralCode, onValueChange = { referralCode = it }, label = { Text("Código de Referência (Opcional)") })
        Spacer(modifier = Modifier.height(16.dp))
        Text("Aviso: usar wallet carteira bnb bsc testnet para fazer o login", textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = {
            if (name.lowercase() == "admin") {
                navController.navigate(AppRoutes.ADMIN)
                return@Button
            }
            if (name.isNotBlank() && walletAddress.isNotBlank()) {
                val profile = UserProfile(name = "$name $surname", walletAddress = walletAddress, isLoggedIn = true)
                saveUserProfile(context, profile)
                if (referralCode.isNotBlank()) {
                    incrementReferralCount(context, referralCode)
                }
                navController.navigate(AppRoutes.LOADING)
            } else {
                Toast.makeText(context, "Preencha o nome e a carteira!", Toast.LENGTH_SHORT).show()
            }
        }) {
            Text("Entrar")
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = { navController.navigate(AppRoutes.RULES) }) {
            Text("Ver Regras")
        }
    }
}

@Composable
fun LoadingScreen(navController: NavController) {
    LaunchedEffect(Unit) {
        delay(3000) // Wait for 3 seconds
        navController.navigate(AppRoutes.MINING) {
            popUpTo(AppRoutes.INITIAL) { inclusive = true }
        }
    }
    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Image(
            painter = rememberAsyncImagePainter(
                model = R.drawable.loading,
                imageLoader = coil.ImageLoader.Builder(LocalContext.current).components { add(ImageDecoderDecoder.Factory()) }.build()
            ),
            contentDescription = "Loading GIF",
            modifier = Modifier.size(150.dp)
        )
    }
}

@Composable
fun RulesScreen(navController: NavController) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("REGRAS DO DWARF GOLD RUSH", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("• cada inscrito pela sua referencia ganha 1 ecopacto")
        Text("• login diario para cada escrito na sua referencia ganhe 5 ecopacto por mes com recompensa de fidelidade")
        Text("• contrato bsc testnet 0xf79D541E8316381f4a88315Bf4F6E967FF5B9D3e")
        Text("• ecopacto contrato ( dec 18) 0x3a713ad93a08AE1A7331b2Be5c1DaAD3f2Bf732e")
        Text("• previsao para lançamento do ecopacto ( 27/05/2026 )")
        Spacer(modifier = Modifier.weight(1f))
        Row {
            Button(onClick = { navController.popBackStack() }) { Text("Tela Inicial") }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = { navController.navigate(AppRoutes.MINING) }) { Text("Tela de Mineração") }
        }
    }
}

@Composable
fun MiningScreen(navController: NavController) {
    val context = LocalContext.current
    val userProfile = loadUserProfile(context)
    var state by remember { mutableStateOf(loadMiningState(context)) }
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = System.currentTimeMillis()
        }
    }

    val isMining = state.miningStartedAt > 0 && currentTime - state.miningStartedAt < MINING_DURATION_MS
    val isCooldown = state.cooldownStartedAt > 0 && currentTime - state.cooldownStartedAt < COOLDOWN_DURATION_MS

    LaunchedEffect(isMining, isCooldown) {
        if (state.miningStartedAt > 0 && !isMining) {
            state = state.copy(miningStartedAt = 0, adCount = 0, cooldownStartedAt = System.currentTimeMillis())
            saveMiningState(context, state)
        }
        if (state.cooldownStartedAt > 0 && !isCooldown) {
            state = state.copy(cooldownStartedAt = 0)
            saveMiningState(context, state)
        }
    }

    val activity = context as Activity

    Box(modifier = Modifier.fillMaxSize()) {
        VideoPlayer(videoUri = Uri.parse("android.resource://" + context.packageName + "/" + R.raw.tela_mineracao))

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                 Column(horizontalAlignment = Alignment.End) {
                    Text("Usuário: ${userProfile.name}", color = Color.White)
                    Text("Wallet: ${userProfile.walletAddress}", color = Color.White, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Nuggets: 123", style = MaterialTheme.typography.headlineLarge, color = Color.White)
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                when {
                    isMining -> {
                        val remaining = MINING_DURATION_MS - (currentTime - state.miningStartedAt)
                        Text("Minerando...", color = Color.Green, fontSize = 20.sp)
                        Text(formatDuration(remaining), color = Color.White)
                    }
                    isCooldown -> {
                        val remaining = COOLDOWN_DURATION_MS - (currentTime - state.cooldownStartedAt)
                        Text("Recarregando...", color = Color.Yellow, fontSize = 20.sp)
                        Text(formatDuration(remaining), color = Color.White)
                    }
                    else -> {
                        Button(onClick = {
                            loadRewardedAd(activity,
                                onAdLoaded = { rewardedAd ->
                                    rewardedAd.show(activity) {
                                        val newCount = state.adCount + 1
                                        var newMiningStartTime = 0L
                                        if (newCount >= ADS_TO_FILL_BAR) {
                                            newMiningStartTime = System.currentTimeMillis()
                                        }
                                        state = state.copy(adCount = newCount, miningStartedAt = newMiningStartTime)
                                        saveMiningState(context, state)
                                        Toast.makeText(context, "Recompensa recebida!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onAdFailed = {
                                    Toast.makeText(context, "Falha ao carregar anúncio.", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }) { Text("Ver Anúncio") }
                    }
                }
                Text("Progresso: ${state.adCount}/$ADS_TO_FILL_BAR", color = Color.White)
                LinearProgressIndicator(progress = state.adCount.toFloat() / ADS_TO_FILL_BAR.toFloat(), modifier = Modifier.fillMaxWidth(0.5f))
            }
        }
    }
}

@Composable
fun AdminScreen(navController: NavController) {
    val context = LocalContext.current
    val referrals = getAllReferrals(context)

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Relatório de Referências", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        if (referrals.isEmpty()) {
            Text("Nenhuma referência encontrada.")
        } else {
            LazyColumn {
                items(referrals) {
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Wallet: ${it.wallet}")
                            Text("Inscritos: ${it.count}")
                        }
                    }
                }
            }
        }
    }
}

// --- Helper Functions ---
fun saveUserProfile(context: Context, profile: UserProfile) {
    val sharedPref = context.getSharedPreferences("DwarfGoldRushPrefs", Context.MODE_PRIVATE) ?: return
    with(sharedPref.edit()) {
        putString("userName", profile.name)
        putString("userWallet", profile.walletAddress)
        putBoolean("isLoggedIn", profile.isLoggedIn)
        apply()
    }
}

fun loadUserProfile(context: Context): UserProfile {
    val sharedPref = context.getSharedPreferences("DwarfGoldRushPrefs", Context.MODE_PRIVATE)
    return UserProfile(
        name = sharedPref.getString("userName", "") ?: "",
        walletAddress = sharedPref.getString("userWallet", "") ?: "",
        isLoggedIn = sharedPref.getBoolean("isLoggedIn", false)
    )
}

fun saveMiningState(context: Context, state: MiningState) {
    val sharedPref = context.getSharedPreferences("DwarfGoldRushPrefs", Context.MODE_PRIVATE) ?: return
    with(sharedPref.edit()) {
        putInt("adCount", state.adCount)
        putLong("miningStartedAt", state.miningStartedAt)
        putLong("cooldownStartedAt", state.cooldownStartedAt)
        apply()
    }
}

fun loadMiningState(context: Context): MiningState {
    val sharedPref = context.getSharedPreferences("DwarfGoldRushPrefs", Context.MODE_PRIVATE)
    return MiningState(
        adCount = sharedPref.getInt("adCount", 0),
        miningStartedAt = sharedPref.getLong("miningStartedAt", 0L),
        cooldownStartedAt = sharedPref.getLong("cooldownStartedAt", 0L)
    )
}

fun incrementReferralCount(context: Context, referrerWallet: String) {
    val sharedPref = context.getSharedPreferences("DwarfGoldRushPrefs", Context.MODE_PRIVATE) ?: return
    val key = "referral_count_$referrerWallet"
    val currentCount = sharedPref.getInt(key, 0)
    with(sharedPref.edit()) {
        putInt(key, currentCount + 1)
        apply()
    }
}

fun getAllReferrals(context: Context): List<ReferralInfo> {
    val sharedPref = context.getSharedPreferences("DwarfGoldRushPrefs", Context.MODE_PRIVATE)
    return sharedPref.all.mapNotNull { (key, value) ->
        if (key.startsWith("referral_count_") && value is Int) {
            val wallet = key.removePrefix("referral_count_")
            ReferralInfo(wallet, value)
        } else {
            null
        }
    }
}

fun formatDuration(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

fun loadRewardedAd(activity: Activity, onAdLoaded: (RewardedAd) -> Unit, onAdFailed: () -> Unit) {
    val adRequest = AdRequest.Builder().build()
    RewardedAd.load(activity, YOUR_REWARDED_AD_UNIT_ID, adRequest, object : RewardedAdLoadCallback() {
        override fun onAdFailedToLoad(adError: LoadAdError) {
            onAdFailed()
        }

        override fun onAdLoaded(rewardedAd: RewardedAd) {
            onAdLoaded(rewardedAd)
        }
    })
}

@Composable
fun VideoPlayer(videoUri: Uri) {
    AndroidView(
        factory = {
            VideoView(it).apply {
                setVideoURI(videoUri)
                setOnPreparedListener { mp ->
                    mp.isLooping = true
                    start()
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun DwarfGoldRushTheme(content: @Composable () -> Unit) {
    MaterialTheme {
        content()
    }
}

// --- Previews for Android Studio ---
@Preview(showBackground = true)
@Composable
fun InitialScreenPreview() {
    DwarfGoldRushTheme { InitialScreen(rememberNavController()) }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    DwarfGoldRushTheme { LoginScreen(rememberNavController()) }
}

@Preview(showBackground = true)
@Composable
fun LoadingScreenPreview() { DwarfGoldRushTheme{ LoadingScreen(rememberNavController())} }

@Preview(showBackground = true)
@Composable
fun RulesScreenPreview() {
    DwarfGoldRushTheme { RulesScreen(rememberNavController()) }
}

@Preview(showBackground = true)
@Composable
fun MiningScreenPreview() {
    DwarfGoldRushTheme { MiningScreen(rememberNavController()) }
}

@Preview(showBackground = true)
@Composable
fun AdminScreenPreview() {
    DwarfGoldRushTheme { AdminScreen(rememberNavController()) }
}
