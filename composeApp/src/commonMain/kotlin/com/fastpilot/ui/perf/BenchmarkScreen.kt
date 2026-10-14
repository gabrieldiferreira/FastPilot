package com.fastpilot.ui.perf

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fastpilot.ui.theme.*
import org.koin.compose.getKoin

/** Shown instead of the app when it is launched in benchmark mode (see [LoadBenchmark]). */
@Composable
fun BenchmarkScreen(platform: String, spec: String) {
    val koin = getKoin()
    val benchmark = remember { LoadBenchmark(platform, koin, LoadBenchmark.Config.parse(spec)) }
    val progress by benchmark.progress.collectAsState()

    LaunchedEffect(benchmark) { benchmark.run(this) }

    Box(
        Modifier.fillMaxSize().background(CarbonBlack).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Benchmark de carregamento", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            // Static text only: an animated indicator would keep the CPU busy during the measurements
            Spacer(Modifier.height(16.dp))
            Text(progress, color = TextSecondary, fontSize = 13.sp)
        }
    }
}
