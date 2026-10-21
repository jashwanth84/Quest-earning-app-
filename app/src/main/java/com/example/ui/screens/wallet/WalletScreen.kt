package com.example.ui.screens.wallet

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ads.AdManager
import com.example.data.model.AppConfig
import com.example.data.model.PayoutMethod
import com.example.data.model.UserProfile
import com.example.data.model.WithdrawalRecord
import com.example.ui.components.DynamicAdBanner
import com.example.ui.components.formatNumber
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WalletScreen(
    userProfile: UserProfile?,
    appConfig: AppConfig,
    payoutMethods: List<PayoutMethod>,
    withdrawals: List<WithdrawalRecord>,
    adManager: AdManager,
    onSubmitWithdrawal: (PayoutMethod, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedMethodForWithdrawal by remember { mutableStateOf<PayoutMethod?>(null) }
    var accountInput by remember { mutableStateOf("") }

    val userCoins = userProfile?.coins ?: 0L

    Surface(
        color = DarkBackground,
        modifier = modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Balance Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = "Redeemable Coins",
                        fontSize = 13.sp,
                        color = TextSecondaryDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = formatNumber(userCoins),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = GoldPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = TextSecondaryDark,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = appConfig.conversionRateText.ifBlank { "1000 Coins = $1.00 USD" },
                            fontSize = 12.sp,
                            color = TextSecondaryDark
                        )
                    }
                }
            }

            // Tabs: Redeem Methods | Withdrawal History
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkSurface,
                contentColor = GoldPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = GoldPrimary,
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Payout Methods", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                    modifier = Modifier.testTag("tab_payout_methods")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("History (${withdrawals.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                    modifier = Modifier.testTag("tab_withdrawal_history")
                )
            }

            if (selectedTab == 0) {
                // Payout Methods
                if (payoutMethods.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payment,
                                contentDescription = null,
                                tint = TextSecondaryDark,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "No Payout Methods Configured",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "The administrator has not added any withdrawal methods yet. Please check back later.",
                                fontSize = 13.sp,
                                color = TextSecondaryDark,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(payoutMethods, key = { it.id }) { method ->
                            PayoutMethodCard(
                                method = method,
                                userCoins = userCoins,
                                onSelect = {
                                    if (userCoins < method.minCoins) {
                                        Toast.makeText(
                                            context,
                                            "You need at least ${method.minCoins} Coins to redeem this method",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        selectedMethodForWithdrawal = method
                                        accountInput = ""
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                // Withdrawal History
                if (withdrawals.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = TextSecondaryDark,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "No Withdrawal History",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Your submitted payouts and status updates will appear here.",
                                fontSize = 13.sp,
                                color = TextSecondaryDark
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(withdrawals, key = { it.id }) { record ->
                            WithdrawalRecordCard(record = record)
                        }
                    }
                }
            }

            // Bottom banner ad
            DynamicAdBanner(
                placement = "WALLET_BANNER",
                adManager = adManager
            )
        }

        // Withdrawal Input Dialog
        if (selectedMethodForWithdrawal != null) {
            val method = selectedMethodForWithdrawal!!
            AlertDialog(
                onDismissRequest = { selectedMethodForWithdrawal = null },
                containerColor = DarkSurface,
                title = {
                    Text(
                        text = "Withdraw ${method.currency} ${method.cashAmount}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Deducts ${method.minCoins} Coins immediately. Admin review takes 24-48 hours.",
                            fontSize = 13.sp,
                            color = TextSecondaryDark
                        )
                        OutlinedTextField(
                            value = accountInput,
                            onValueChange = { accountInput = it },
                            label = { Text(method.inputLabel) },
                            placeholder = { Text(method.inputPlaceholder) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = TextSecondaryDark,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_account_detail")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (accountInput.isNotBlank()) {
                                onSubmitWithdrawal(method, accountInput)
                                selectedMethodForWithdrawal = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_confirm_withdrawal")
                    ) {
                        Text(
                            text = "Submit Request",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedMethodForWithdrawal = null }) {
                        Text("Cancel", color = TextSecondaryDark)
                    }
                }
            )
        }
    }
}

@Composable
private fun PayoutMethodCard(
    method: PayoutMethod,
    userCoins: Long,
    onSelect: () -> Unit
) {
    val canRedeem = userCoins >= method.minCoins

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCardBg),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .testTag("payout_method_${method.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (canRedeem) GoldPrimary.copy(alpha = 0.2f) else DarkSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getPayoutIcon(method.iconType),
                    contentDescription = null,
                    tint = if (canRedeem) GoldPrimary else TextSecondaryDark,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = method.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Text(
                    text = "${method.currency} ${method.cashAmount}",
                    fontSize = 13.sp,
                    color = GoldPrimary,
                    fontWeight = FontWeight.Bold
                )
                if (method.subtitle.isNotBlank()) {
                    Text(
                        text = method.subtitle,
                        fontSize = 11.sp,
                        color = TextSecondaryDark
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (canRedeem) Color(0xFF2E2413) else DarkSurfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${method.minCoins} Coins",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (canRedeem) GoldPrimary else TextSecondaryDark
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (canRedeem) "Ready" else "Need ${method.minCoins - userCoins}",
                    fontSize = 10.sp,
                    color = if (canRedeem) EmeraldSuccess else CrimsonError,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun WithdrawalRecordCard(record: WithdrawalRecord) {
    val dateStr = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.US).format(Date(record.createdAt))
    val statusColor = when (record.status.uppercase()) {
        "APPROVED" -> EmeraldSuccess
        "REJECTED" -> CrimsonError
        else -> GoldPrimary
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCardBg),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.methodTitle.ifBlank { "Payout Request" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Text(
                    text = "Account: ${record.accountDetail}",
                    fontSize = 12.sp,
                    color = TextSecondaryDark
                )
                Text(
                    text = dateStr,
                    fontSize = 10.sp,
                    color = TextSecondaryDark.copy(alpha = 0.7f)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$${record.cashAmount}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = GoldPrimary
                )
                Text(
                    text = "-${record.coinsDeducted} Coins",
                    fontSize = 11.sp,
                    color = TextSecondaryDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = record.status,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }
        }
    }
}

private fun getPayoutIcon(iconType: String): ImageVector {
    return when (iconType.lowercase()) {
        "bank", "upi" -> Icons.Default.AccountBalance
        "paypal", "wallet" -> Icons.Default.AccountBalanceWallet
        else -> Icons.Default.CreditCard
    }
}
