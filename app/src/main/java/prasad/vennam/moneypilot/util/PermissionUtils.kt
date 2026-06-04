package prasad.vennam.moneypilot.util

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionGate(
    permission: String,
    rationale: String,
    onPermissionGranted: @Composable () -> Unit
) {
    val permissionState = rememberPermissionState(permission)

    if (permissionState.status.isGranted) {
        onPermissionGranted()
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val textToShow = if (permissionState.status.shouldShowRationale) {
                rationale
            } else {
                "This feature requires ${permission.split('.').last().lowercase()} permission to work."
            }

            Text(
                text = textToShow,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { permissionState.launchPermissionRequest() }) {
                Text("Grant Permission")
            }
        }
    }
}
