package prasad.vennam.moneypilot.ui.legal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalScreen(
    title: String,
    content: String,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
        ) {
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

object LegalContent {
    const val TERMS_OF_SERVICE = """
        Terms of Service
        
        Last updated: June 05, 2025
        
        1. Agreement to Terms
        By using MoneyPilot, you agree to be bound by these Terms. If you don't agree, please do not use the app.
        
        2. Description of Service
        MoneyPilot is a personal finance management tool that helps users track expenses, income, investments, and loans.
        
        3. Privacy Policy
        Your use of the app is also governed by our Privacy Policy.
        
        4. User Accounts
        You are responsible for maintaining the confidentiality of your account information.
        
        5. Data Security
        We use industry-standard security measures to protect your data. However, no method of transmission over the internet is 100% secure.
        
        6. Disclaimer of Warranties
        MoneyPilot is provided "as is" without any warranties.
        
        7. Limitation of Liability
        We are not liable for any financial losses or damages resulting from the use of the app.
        
        8. Changes to Terms
        We reserve the right to modify these terms at any time.
    """

    const val PRIVACY_POLICY = """
        Privacy Policy
        
        Last updated: June 05, 2025
        
        1. Information We Collect
        We collect transaction data, budget information, and profile details that you provide.
        
        2. How We Use Your Information
        We use your data to provide financial insights, track your budget, and improve app functionality.
        
        3. Data Syncing
        If you sign in with Google, your data may be synced with your private Google Drive/Sheets for backup purposes.
        
        4. Data Sharing
        We do not sell your personal data to third parties.
        
        5. AI Analysis
        We use on-device AI to provide financial insights. This analysis happens locally on your device.
        
        6. Security
        We implement technical and organizational measures to protect your personal information.
        
        7. Your Rights
        You can delete your account and all associated data at any time through the app settings.
        
        8. Contact Us
        If you have questions about this policy, contact us through the Help & FAQ section.
    """
}
