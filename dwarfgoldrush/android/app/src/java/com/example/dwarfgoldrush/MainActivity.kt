package com.example.dwarfgoldrush

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dwarfgoldrush.ui.theme.DwarfGoldRushTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DwarfGoldRushTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DwarfGoldRushApp()
                }
            }
        }
    }
}

@Composable
fun DwarfGoldRushApp() {
    var nuggets by remember { mutableStateOf(0) }
    var totalMined by remember { mutableStateOf(0) }
    var cooldown by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Mock fetchUser - in a real app, this would make a network request
    LaunchedEffect(Unit) {
        // Simulate fetching initial user data
        // fetchUser()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("DWARF GOLD RUSH", style = MaterialTheme.typography.headlineLarge)

        Spacer(modifier = Modifier.height(32.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Nuggets: $nuggets")
            Text("Total: $totalMined")
            Text("ECOPACT: ${(totalMined / 20000.0).format(2)}")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    if (!cooldown) {
                        loading = true
                        cooldown = true
                        // Simulate mining operation
                        delay(1000) // Simulate network latency
                        val minedAmount = (1..10).random()
                        nuggets += minedAmount
                        totalMined += minedAmount
                        loading = false
                        delay(3000) // Cooldown period
                        cooldown = false
                    }
                }
            },
            enabled = !cooldown && !loading
        ) {
            Text(if (loading) "MINERANDO..." else "MINERAR")
        }
    }
}

// Extension to format double to a specific number of decimal places
fun Double.format(digits: Int) = "%.${digits}f".format(this)

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    DwarfGoldRushTheme {
        DwarfGoldRushApp()
    }
}
