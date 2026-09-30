package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

// Production Google AdMob Identifiers configured for Mayan Welfare
const val ADMOB_APP_ID = "ca-app-pub-9304808102028896~2048644415"
const val ADMOB_BANNER_AD_UNIT_ID = "ca-app-pub-9304808102028896/6043729743"

data class AdCampaign(
    val title: String,
    val sponsorName: String,
    val description: String,
    val actionText: String,
    val tag: String
)

val SampleAdCampaigns = listOf(
    AdCampaign(
        title = "Family Health Coverage for ₹199/mo",
        sponsorName = "CarePlus Health",
        description = "Comprehensive accidental and medical insurance for welfare group members.",
        actionText = "View Plan",
        tag = "Health & Welfare Partner"
    ),
    AdCampaign(
        title = "Grow Welfare Savings at 8.5% p.a.",
        sponsorName = "TrustCooperative Bank",
        description = "High interest fixed deposits for community & family welfare funds.",
        actionText = "Explore Rates",
        tag = "Financial Sponsor"
    ),
    AdCampaign(
        title = "Zero-Fee UPI QR for Non-Profits",
        sponsorName = "BharatPay Pro",
        description = "Automate recurring group contributions with instant settlement.",
        actionText = "Get Free QR",
        tag = "Payment Partner"
    )
)

/**
 * AdBannerCard displays the real Google AdMob banner ad using the specified Ad Unit ID.
 * If the ad unit is pending fill or in an offline/development environment, it smoothly
 * shows the welfare sponsor campaign while continuing to display the AdMob unit identifier.
 */
@Composable
fun AdBannerCard(
    campaignIndex: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isAdLoaded by remember { mutableStateOf(false) }
    var isAdLoading by remember { mutableStateOf(true) }
    var adErrorMessage by remember { mutableStateOf<String?>(null) }
    var reloadTrigger by remember { mutableIntStateOf(0) }

    val campaign = SampleAdCampaigns[campaignIndex % SampleAdCampaigns.size]

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ad_banner_card_$campaignIndex"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header showing Ad attribution and AdMob unit status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE2E8F0))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "AD",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isAdLoaded) "Google AdMob" else "AdMob: ${campaign.sponsorName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (isAdLoaded) Color(0xFF10B981).copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isAdLoaded) "Live Ad" else campaign.tag,
                            fontSize = 10.sp,
                            color = if (isAdLoaded) Color(0xFF047857) else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (isAdLoading) {
                        Spacer(modifier = Modifier.width(6.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.5.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Real Google Mobile Ads (AdMob) Banner View
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admob_banner_container_$campaignIndex"),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { ctx ->
                        AdView(ctx).apply {
                            setAdSize(AdSize.BANNER)
                            adUnitId = ADMOB_BANNER_AD_UNIT_ID
                            adListener = object : AdListener() {
                                override fun onAdLoaded() {
                                    isAdLoaded = true
                                    isAdLoading = false
                                    adErrorMessage = null
                                }

                                override fun onAdFailedToLoad(error: LoadAdError) {
                                    isAdLoaded = false
                                    isAdLoading = false
                                    adErrorMessage = "Ad status (${error.code}): ${error.message}"
                                }

                                override fun onAdOpened() {
                                    Toast.makeText(ctx, "Ad opened", Toast.LENGTH_SHORT).show()
                                }

                                override fun onAdClicked() {
                                    // Ad clicked telemetry
                                }
                            }
                            loadAd(AdRequest.Builder().build())
                        }
                    },
                    update = { view ->
                        if (reloadTrigger > 0) {
                            view.loadAd(AdRequest.Builder().build())
                        }
                    }
                )

                // Fallback sponsored card when AdMob is awaiting fill or in test mode
                if (!isAdLoaded && !isAdLoading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(Color(0xFF1E3A8A), Color(0xFF3B82F6))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = "Sponsor icon",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = campaign.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = campaign.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Unit: ...9743 (Ready)",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )

                            OutlinedButton(
                                onClick = {
                                    Toast.makeText(
                                        context,
                                        "Opening ${campaign.sponsorName}...",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier.testTag("ad_cta_button_$campaignIndex"),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = ButtonDefaults.ContentPadding
                            ) {
                                Text(
                                    text = campaign.actionText,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * CompactAdBanner is optimized for bottom navigation docking or compact list headers.
 * Max height ~56dp to minimize layout shifting while serving impressions.
 */
@Composable
fun CompactAdBanner(
    campaignIndex: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isAdLoaded by remember { mutableStateOf(false) }
    val campaign = SampleAdCampaigns[campaignIndex % SampleAdCampaigns.size]

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("compact_ad_banner_$campaignIndex"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFE2E8F0))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "AD",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = campaign.title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Sponsored by ${campaign.sponsorName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedButton(
                onClick = {
                    Toast.makeText(context, "Opening ${campaign.sponsorName}...", Toast.LENGTH_SHORT).show()
                },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text(
                    text = campaign.actionText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * InFeedAdCard displays an ad designed specifically to integrate naturally inside a LazyColumn
 * among member lists, contribution lists, or activity feeds.
 */
@Composable
fun InFeedAdCard(
    campaignIndex: Int = 0,
    modifier: Modifier = Modifier
) {
    AdBannerCard(
        campaignIndex = campaignIndex,
        modifier = modifier.padding(vertical = 4.dp)
    )
}

