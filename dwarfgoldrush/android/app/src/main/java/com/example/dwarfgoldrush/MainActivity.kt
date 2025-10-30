package com.example.dwarfgoldrush

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

// --- Configuration ---
const val YOUR_REWARDED_AD_UNIT_ID = "ca-app-pub-1262202142447431/7738751536"
val MAX_MINING_TIME_MS = TimeUnit.HOURS.toMillis(12)
val MINING_TIME_PER_AD_MS = MAX_MINING_TIME_MS / 20
val COOLDOWN_DURATION_MS = TimeUnit.HOURS.toMillis(4)
const val NUGGETS_GOAL = 10000L
const val NUGGET_GAIN_INTERVAL_SECONDS = 20

// --- Data Classes ---
data class GameState(val miningTimeRemainingMs: Long, val cooldownStartedAt: Long, val nuggets: Long, val upgrades: Set<String>)
data class UserProfile(val name: String, val walletAddress: String, val isLoggedIn: Boolean)
data class ReferralInfo(val wallet: String, val count: Int)
data class UpgradeItem(val id: String, val name: String, val cost: Long, val description: String)

// --- Main Activity & Navigation ---
object AppRoutes {
    const val INITIAL = "initial"
    const val LOGIN = "login"
    const val MAIN = "main"
    const val ADMIN = "admin"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        MobileAds.initialize(this) {}
        setContent {
            DwarfGoldRushTheme {
                Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
                    val context = LocalContext.current
                    val userProfile = loadUserProfile(context)
                    val startDestination = if (userProfile.isLoggedIn) AppRoutes.MAIN else AppRoutes.INITIAL
                    AppNavigation(startDestination)
                }
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
        composable(AppRoutes.MAIN) { MainScreen(navController) }
        composable(AppRoutes.ADMIN) { AdminScreen(navController) }
    }
}

// --- Base UI ---
@Composable
fun ScreenWithBackground(content: @Composable BoxScope.() -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(painter = painterResource(id = R.drawable.tela_inicial), contentDescription = "Background", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)))
        content()
    }
}

@Composable
fun InfoBox(modifier: Modifier = Modifier, horizontalAlignment: Alignment.Horizontal = Alignment.Start, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = modifier.background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp)).padding(16.dp), horizontalAlignment = horizontalAlignment, content = content)
}

// --- Main Game Screen (Pager) ---
@OptIn(ExperimentalPagerApi::class)
@Composable
fun MainScreen(navController: NavController) {
    val pagerState = rememberPagerState()
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = pagerState.currentPage) {
            Tab(text = { Text("Mineração") }, selected = pagerState.currentPage == 0, onClick = { scope.launch { pagerState.animateScrollToPage(0) } })
            Tab(text = { Text("Loja") }, selected = pagerState.currentPage == 1, onClick = { scope.launch { pagerState.animateScrollToPage(1) } })
            Tab(text = { Text("Regras") }, selected = pagerState.currentPage == 2, onClick = { scope.launch { pagerState.animateScrollToPage(2) } })
        }
        HorizontalPager(count = 3, state = pagerState, modifier = Modifier.weight(1f)) { page ->
            when (page) {
                0 -> MiningScreenContent(navController)
                1 -> StoreScreenContent(navController)
                2 -> RulesScreenContent(navController)
            }
        }
    }
}

// --- Screen Contents ---
@Composable
fun InitialScreen(navController: NavController) {
    ScreenWithBackground {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            InfoBox { Text("Dwarf Gold Rush", style = MaterialTheme.typography.headlineLarge, color = Color.White) }
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { navController.navigate(AppRoutes.LOGIN) }) { Text("Login") }
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
    
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color.White,
        unfocusedBorderColor = Color.White.copy(alpha = 0.7f),
        focusedLabelColor = Color.White,
        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
        cursorColor = Color.White
    )

    ScreenWithBackground {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            InfoBox {
                Text("Página de Login", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome") }, colors = textFieldColors, textStyle = TextStyle(color = MaterialTheme.colorScheme.primary))
                OutlinedTextField(value = surname, onValueChange = { surname = it }, label = { Text("Sobrenome") }, colors = textFieldColors, textStyle = TextStyle(color = MaterialTheme.colorScheme.primary))
                OutlinedTextField(value = walletAddress, onValueChange = { walletAddress = it }, label = { Text("Endereço da Wallet") }, colors = textFieldColors, textStyle = TextStyle(color = MaterialTheme.colorScheme.primary))
                OutlinedTextField(value = referralCode, onValueChange = { referralCode = it }, label = { Text("Código de Referência (Opcional)") }, colors = textFieldColors, textStyle = TextStyle(color = MaterialTheme.colorScheme.primary))
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = {
                if (name.lowercase() == "admin") {
                    navController.navigate(AppRoutes.ADMIN)
                    return@Button
                }
                if (name.isNotBlank() && walletAddress.isNotBlank()) {
                    val profile = UserProfile(name = "$name $surname", walletAddress = walletAddress, isLoggedIn = true)
                    saveUserProfile(context, profile)
                    if (referralCode.isNotBlank()) incrementReferralCount(context, referralCode)
                    navController.navigate(AppRoutes.MAIN) { popUpTo(AppRoutes.INITIAL) { inclusive = true } }
                } else {
                    Toast.makeText(context, "Preencha o nome e a carteira!", Toast.LENGTH_SHORT).show()
                }
            }) { Text("Entrar") }
        }
    }
}

@Composable
fun RulesScreenContent(navController: NavController) {
    ScreenWithBackground {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            InfoBox(modifier = Modifier.padding(16.dp)) {
                Text("REGRAS DO DWARF GOLD RUSH", style = MaterialTheme.typography.headlineMedium, color = Color.White, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))
                Text("• cada inscrito pela sua referencia ganha 1 ecopacto", color = Color.White, textAlign = TextAlign.Center)
                Text("• login diario para cada escrito na sua referencia ganhe 2 ecopacto por mes com recompensa de fidelidade", color = Color.White, textAlign = TextAlign.Center)
                Text("• bsc testnet", color = Color.White, textAlign = TextAlign.Center)
                Text("• RPC URL: https://data-seed-prebsc-1-s1.bnbchain.org:8545", color = Color.White, textAlign = TextAlign.Center)
                Text("• ChainID: 97", color = Color.White, textAlign = TextAlign.Center)
                Text("• Symbol: tBNB", color = Color.White, textAlign = TextAlign.Center)
                Text("• Explorer: https://testnet.bscscan.com/", color = Color.White, textAlign = TextAlign.Center)
                Text("• ecopacto contrato ( dec 18) 0x3a713ad93a08AE1A7331b2Be5c1DaAD3f2Bf732e", color = Color.White, textAlign = TextAlign.Center)
                Text("• previsao para lançamento do ecopacto ( ainda sem data definida )", color = Color.White, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun MiningScreenContent(navController: NavController) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val userProfile = loadUserProfile(context)
    var gameState by remember { mutableStateOf(loadGameState(context)) }
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    
    // This is the main state machine for the game timers
    LaunchedEffect(Unit) {
        var internalNuggetCounter = 0
        while (isActive) {
            delay(1000) // Tick every second
            val now = System.currentTimeMillis()
            var currentState = gameState

            if (currentState.isMining()) {
                // 1. Decrease mining time
                val newTime = (currentState.miningTimeRemainingMs - 1000).coerceAtLeast(0)
                currentState = currentState.copy(miningTimeRemainingMs = newTime)

                // 2. Handle nugget gain every 20 seconds
                internalNuggetCounter++
                if (internalNuggetCounter >= NUGGET_GAIN_INTERVAL_SECONDS) {
                    val nuggetsPerCycle = when {
                        currentState.upgrades.contains("upgrade_cart") -> 3L
                        currentState.upgrades.contains("upgrade_pickaxe") -> 2L
                        else -> 1L
                    }
                    val newNuggets = currentState.nuggets + nuggetsPerCycle
                    currentState = currentState.copy(nuggets = newNuggets)
                    internalNuggetCounter = 0 // Reset counter
                }

                // 3. Check for transition to cooldown
                if (newTime <= 0) {
                    currentState = currentState.copy(cooldownStartedAt = now)
                }
            } else {
                // 4. Check if cooldown is over
                val isCooldownNow = currentState.cooldownStartedAt > 0 && (now - currentState.cooldownStartedAt < COOLDOWN_DURATION_MS)
                if (isCooldownNow && (now - currentState.cooldownStartedAt >= COOLDOWN_DURATION_MS)) {
                    currentState = currentState.copy(cooldownStartedAt = 0L)
                }
            }

            // 5. Save state if it has changed
            if (currentState != gameState) {
                saveGameState(context, currentState)
                gameState = currentState
            }
            currentTime = now
        }
    }

    val activity = context as Activity
    val isCooldown = !gameState.isMining() && gameState.cooldownStartedAt > 0 && (currentTime - gameState.cooldownStartedAt < COOLDOWN_DURATION_MS)

    ScreenWithBackground {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.Top) {
                InfoBox(horizontalAlignment = Alignment.End) {
                    Text("Usuário: ${userProfile.name}", color = Color.White)
                    Text("Wallet: ${formatWalletAddress(userProfile.walletAddress)}", color = Color.White, fontSize = 12.sp)
                    TextButton(onClick = { 
                        val clip = ClipData.newPlainText("Referral Code", userProfile.walletAddress)
                        clipboardManager.setPrimaryClip(clip)
                        Toast.makeText(context, "Código de referência copiado!", Toast.LENGTH_SHORT).show()
                    }) { Text("Copiar Código") }
                }
            }

            InfoBox(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Nuggets: ${gameState.nuggets}", style = MaterialTheme.typography.headlineLarge, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = (gameState.nuggets.toFloat() / NUGGETS_GOAL.toFloat()).coerceIn(0f, 1f),
                    modifier = Modifier.height(8.dp).clip(RoundedCornerShape(4.dp)).fillMaxWidth(0.8f)
                )
            }

            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                when {
                    isCooldown -> {
                        val remaining = COOLDOWN_DURATION_MS - (currentTime - gameState.cooldownStartedAt)
                        Text("Recarregando...", color = Color.Yellow, fontSize = 20.sp)
                        Text(formatDuration(remaining), color = Color.White)
                    }
                    else -> {
                        Button(onClick = {
                            loadRewardedAd(activity,
                                onAdLoaded = { rewardedAd ->
                                    rewardedAd.show(activity) {
                                        val newTime = gameState.miningTimeRemainingMs + MINING_TIME_PER_AD_MS
                                        gameState = gameState.copy(miningTimeRemainingMs = newTime.coerceAtMost(MAX_MINING_TIME_MS))
                                        saveGameState(context, gameState)
                                        Toast.makeText(context, "+36 minutos de mineração!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onAdFailed = { Toast.makeText(context, "Falha ao carregar anúncio.", Toast.LENGTH_SHORT).show() }
                            )
                        }) { Text("Ver Anúncio") }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Tempo de Mineração", color = Color.White)
                LinearProgressIndicator(progress = gameState.miningTimeRemainingMs.toFloat() / MAX_MINING_TIME_MS.toFloat(), modifier = Modifier.fillMaxWidth(0.7f))
            }
        }
    }
}

@Composable
fun StoreScreenContent(navController: NavController) {
    val context = LocalContext.current
    var gameState by remember { mutableStateOf(loadGameState(context)) }

    LaunchedEffect(Unit) {
        while(isActive) {
            gameState = loadGameState(context)
            delay(1000)
        }
    }

    val storeItems = remember {
        listOf(
            UpgradeItem("upgrade_pickaxe", "Picareta Melhorada", 500, "Ganha 2 nuggets/20 seg"),
            UpgradeItem("upgrade_cart", "Carrinho de Mão Maior", 2000, "Ganha 3 nuggets/20 seg"),
            UpgradeItem("upgrade_map", "Mapa do Tesouro", 1000, "Recompensa instantânea de 5000 nuggets")
        )
    }

    ScreenWithBackground {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Loja de Melhorias", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Text("Seus Nuggets: ${gameState.nuggets}", color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                items(storeItems) { item ->
                    val isPurchased = gameState.upgrades.contains(item.id)
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.titleMedium)
                                Text(item.description, style = MaterialTheme.typography.bodySmall)
                                Text("Custo: ${item.cost} nuggets", style = MaterialTheme.typography.bodySmall)
                            }
                            Button(
                                onClick = { 
                                    if (gameState.nuggets >= item.cost && !isPurchased) {
                                        var newNuggets = gameState.nuggets - item.cost
                                        if (item.id == "upgrade_map") { newNuggets += 5000 }
                                        val newUpgrades = gameState.upgrades + item.id
                                        gameState = gameState.copy(nuggets = newNuggets, upgrades = newUpgrades)
                                        saveGameState(context, gameState)
                                        Toast.makeText(context, "${item.name} comprada!", Toast.LENGTH_SHORT).show()
                                    } 
                                },
                                enabled = gameState.nuggets >= item.cost && !isPurchased
                            ) { Text(if (isPurchased) "Comprado" else "Comprar") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminScreen(navController: NavController) {
    ScreenWithBackground {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("Relatório de Referências", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            val referrals = getAllReferrals(LocalContext.current)
            if (referrals.isEmpty()) {
                Text("Nenhuma referência encontrada.", color = Color.White)
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
}

// --- Helper Functions ---
fun GameState.isMining(): Boolean = this.miningTimeRemainingMs > 0

fun saveGameState(context: Context, state: GameState) {
    val sharedPref = context.getSharedPreferences("DwarfGoldRushPrefs", Context.MODE_PRIVATE) ?: return
    with(sharedPref.edit()) {
        putLong("miningTimeRemainingMs", state.miningTimeRemainingMs)
        putLong("cooldownStartedAt", state.cooldownStartedAt)
        putLong("userNuggets", state.nuggets)
        putStringSet("userUpgrades", state.upgrades)
        apply()
    }
}

fun loadGameState(context: Context): GameState {
    val sharedPref = context.getSharedPreferences("DwarfGoldRushPrefs", Context.MODE_PRIVATE)
    return GameState(
        miningTimeRemainingMs = sharedPref.getLong("miningTimeRemainingMs", 0L),
        cooldownStartedAt = sharedPref.getLong("cooldownStartedAt", 0L),
        nuggets = sharedPref.getLong("userNuggets", 0L),
        upgrades = sharedPref.getStringSet("userUpgrades", emptySet()) ?: emptySet()
    )
}

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

fun formatWalletAddress(address: String): String {
    if (address.length < 11) return address
    return "${address.take(6)}...${address.takeLast(4)}"
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
fun DwarfGoldRushTheme(content: @Composable () -> Unit) {
    MaterialTheme {
        content()
    }
}

// --- Previews ---
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
fun RulesScreenPreview() {
    DwarfGoldRushTheme { RulesScreenContent(rememberNavController()) }
}

@Preview(showBackground = true)
@Composable
fun MiningScreenPreview() {
    DwarfGoldRushTheme { MainScreen(rememberNavController()) }
}

@Preview(showBackground = true)
@Composable
fun AdminScreenPreview() {
    DwarfGoldRushTheme { AdminScreen(rememberNavController()) }
}
