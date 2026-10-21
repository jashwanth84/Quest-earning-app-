package com.example.ui.screens.games

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GemCyan
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import kotlinx.coroutines.delay
import kotlin.random.Random

private val ALL_SYMBOLS = listOf(
    "💎", "👑", "🪙", "🏆", "⚡", "🔮", "🗝️", "🎁",
    "🛡️", "🗡️", "🌟", "🍀", "🎯", "🔥", "🚀", "🪐",
    "⚓", "🧭", "🏹", "🎪", "🦄", "🐉", "🪄", "🔔"
)

enum class GameState {
    PLAYING,
    WON,
    TIME_UP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageFinderScreen(
    currentGems: Long,
    onRoundWon: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var roundNumber by remember { mutableIntStateOf(1) }
    var targetSymbol by remember { mutableStateOf("") }
    var gridItems by remember { mutableStateOf<List<String>>(emptyList()) }
    var targetIndex by remember { mutableIntStateOf(-1) }
    var timeLeftSeconds by remember { mutableIntStateOf(15) }
    var gameState by remember { mutableStateOf(GameState.PLAYING) }
    var selectedCellIndex by remember { mutableIntStateOf(-1) }

    fun setupNewRound() {
        val pool = ALL_SYMBOLS.shuffled()
        val target = pool.first()
        val otherSymbols = pool.drop(1).take(15)
        val all16 = (listOf(target) + otherSymbols).shuffled()

        targetSymbol = target
        gridItems = all16
        targetIndex = all16.indexOf(target)
        timeLeftSeconds = 15
        gameState = GameState.PLAYING
        selectedCellIndex = -1
    }

    LaunchedEffect(roundNumber) {
        setupNewRound()
    }

    // Countdown Timer (15 seconds)
    LaunchedEffect(gameState, roundNumber) {
        if (gameState == GameState.PLAYING) {
            while (timeLeftSeconds > 0 && gameState == GameState.PLAYING) {
                delay(1000L)
                if (gameState == GameState.PLAYING) {
                    timeLeftSeconds--
                }
            }
            if (timeLeftSeconds == 0 && gameState == GameState.PLAYING) {
                gameState = GameState.TIME_UP
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Image Finder",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("btn_back_image_finder")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Current Gems Badge
                    Row(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Diamond,
                            contentDescription = null,
                            tint = GemCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "$currentGems Gems",
                            color = GemCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Target Prompt & Timer Header
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkCardBg),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Find the Target Symbol:",
                            fontSize = 14.sp,
                            color = TextSecondaryDark,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Large target symbol card
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkSurfaceVariant)
                                .border(2.dp, GoldPrimary, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = targetSymbol,
                                fontSize = 38.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Timer bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = if (timeLeftSeconds <= 5) CrimsonError else GoldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            LinearProgressIndicator(
                                progress = { timeLeftSeconds / 15f },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (timeLeftSeconds <= 5) CrimsonError else GoldPrimary,
                                trackColor = DarkSurfaceVariant
                            )
                            Text(
                                text = "${timeLeftSeconds}s",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (timeLeftSeconds <= 5) CrimsonError else GoldPrimary
                            )
                        }
                    }
                }
            }

            // 4x4 Grid
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(gridItems.size) { index ->
                        val symbol = gridItems[index]
                        val isCorrect = index == targetIndex
                        val isSelected = index == selectedCellIndex

                        val bgColor = when {
                            gameState == GameState.WON && isCorrect -> EmeraldSuccess.copy(alpha = 0.35f)
                            isSelected && !isCorrect -> CrimsonError.copy(alpha = 0.35f)
                            else -> DarkSurfaceVariant
                        }

                        val borderColor = when {
                            gameState == GameState.WON && isCorrect -> EmeraldSuccess
                            isSelected && !isCorrect -> CrimsonError
                            else -> Color.Transparent
                        }

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(bgColor)
                                .border(2.dp, borderColor, RoundedCornerShape(12.dp))
                                .clickable(enabled = gameState == GameState.PLAYING) {
                                    selectedCellIndex = index
                                    if (isCorrect) {
                                        gameState = GameState.WON
                                        onRoundWon()
                                    }
                                }
                                .testTag("grid_cell_$index"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = symbol,
                                fontSize = 28.sp
                            )
                        }
                    }
                }
            }

            // Outcome and Action
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (gameState) {
                    GameState.WON -> {
                        Text(
                            text = "🎉 Target Found! +1 Gem Rewarded!",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldSuccess,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { roundNumber++ },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_next_round")
                        ) {
                            Text(
                                text = "Next Round (Round ${roundNumber + 1})",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    GameState.TIME_UP -> {
                        Text(
                            text = "⏰ Time's Up! You missed the target.",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = CrimsonError,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { roundNumber++ },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_try_again")
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Try Again",
                                color = TextPrimaryDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    GameState.PLAYING -> {
                        Text(
                            text = "Tap the matching symbol before the timer expires!",
                            fontSize = 13.sp,
                            color = TextSecondaryDark,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
