package com.example.pullit.ui.more

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.pullit.ui.LocalStrings
import com.example.pullit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyInfoScreen(navController: NavController) {
    val S = LocalStrings.current
    val context = LocalContext.current

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {},
                windowInsets = WindowInsets.statusBars,
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, S.back)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Outlined.PrivacyTip,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    S.privacyTitle,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    S.privacySubtitle,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Disclosure items
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DisclosureItem(
                    icon = Icons.Outlined.PhoneAndroid,
                    title = S.privacyLocalStorageTitle,
                    detail = S.privacyLocalStorageDetail
                )
                DisclosureItem(
                    icon = Icons.Outlined.CloudUpload,
                    title = S.privacyDataSentTitle,
                    detail = S.privacyDataSentDetail
                )
                DisclosureItem(
                    icon = Icons.Outlined.Memory,
                    title = S.privacyThirdPartyTitle,
                    detail = S.privacyThirdPartyDetail
                )
                DisclosureItem(
                    icon = Icons.Outlined.Delete,
                    title = S.privacyDataDeletionTitle,
                    detail = S.privacyDataDeletionDetail
                )
                DisclosureItem(
                    icon = Icons.Outlined.Lock,
                    title = S.privacyEncryptionTitle,
                    detail = S.privacyEncryptionDetail
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Privacy policy link
            TextButton(
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://pullit-landing.up.railway.app/privacy.html"))
                    )
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    S.privacyPolicyLink,
                    color = Primary,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Agree button
            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text(
                    S.privacyAgreeButton,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color.White
                )
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

@Composable
private fun DisclosureItem(
    icon: ImageVector,
    title: String,
    detail: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier
                .size(24.dp)
                .padding(top = 2.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                detail,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}
