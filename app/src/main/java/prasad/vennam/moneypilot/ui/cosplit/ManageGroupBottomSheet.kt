package prasad.vennam.moneypilot.ui.cosplit

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.ui.components.BaseBottomSheet

@Composable
fun ManageGroupBottomSheet(
    isCreateMode: Boolean,
    initialGroupName: String = "",
    initialMembers: List<String>,
    initialMemberNamesMap: Map<String, String>,
    currentUserEmail: String,
    onFetchUserProfile: suspend (String) -> String?,
    onDismiss: () -> Unit,
    onSave: (groupName: String, members: List<String>, memberNamesMap: Map<String, String>) -> Unit,
    isSaving: Boolean = false
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var groupName by remember { mutableStateOf(initialGroupName) }
    var newMemberEmail by remember { mutableStateOf("") }
    val currentMembersList = remember { mutableStateListOf(*initialMembers.toTypedArray()) }
    val currentMemberNamesMap = remember { mutableStateMapOf(*initialMemberNamesMap.toList().toTypedArray()) }
    
    var manualNamePromptEmail by remember { mutableStateOf<String?>(null) }
    var manualName by remember { mutableStateOf("") }
    var isFetchingName by remember { mutableStateOf(false) }

    if (manualNamePromptEmail != null) {
        AlertDialog(
            onDismissRequest = { manualNamePromptEmail = null },
            title = { Text("Enter Name") },
            text = {
                OutlinedTextField(
                    value = manualName,
                    onValueChange = { manualName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (manualName.isNotBlank()) {
                            val emailToAdd = manualNamePromptEmail!!
                            currentMembersList.add(emailToAdd)
                            currentMemberNamesMap[emailToAdd] = manualName.trim()
                            manualName = ""
                            manualNamePromptEmail = null
                            newMemberEmail = ""
                        }
                    },
                    enabled = manualName.isNotBlank()
                ) {
                    Text(stringResource(R.string.add))
                }
            },
            dismissButton = {
                TextButton(onClick = { manualNamePromptEmail = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    BaseBottomSheet(
        onDismissRequest = onDismiss,
        title = if (isCreateMode) "Create Group" else stringResource(R.string.manage_members)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (isCreateMode) {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group Name") },
                    placeholder = { Text("Trip to Hawaii") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
            } else {
                Text(
                    text = stringResource(R.string.manage_members_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            OutlinedTextField(
                value = newMemberEmail,
                onValueChange = { newMemberEmail = it },
                label = { Text(stringResource(R.string.email_address)) },
                placeholder = { Text(stringResource(R.string.email_address_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            val email = newMemberEmail.trim().lowercase()
                            if (email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                if (!currentMembersList.contains(email)) {
                                    isFetchingName = true
                                    coroutineScope.launch {
                                        val profileName = onFetchUserProfile(email)
                                        isFetchingName = false
                                        if (profileName != null) {
                                            currentMembersList.add(email)
                                            currentMemberNamesMap[email] = profileName
                                            newMemberEmail = ""
                                        } else {
                                            manualNamePromptEmail = email
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, context.getString(R.string.member_already_in_group), Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, context.getString(R.string.invalid_email), Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = newMemberEmail.isNotBlank() && !isFetchingName
                    ) {
                        if (isFetchingName) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = stringResource(R.string.add_member),
                                tint = if (newMemberEmail.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.current_members_count, currentMembersList.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .heightIn(max = 200.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                currentMembersList.forEach { email ->
                    val isCurrentUser = email == currentUserEmail
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val displayName = currentMemberNamesMap[email]
                            if (displayName != null) {
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            val memberName = currentMemberNamesMap[email] ?: email
                            Text(
                                text = memberName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isCurrentUser) {
                                Text(
                                    text = "You",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        if (!isCurrentUser) {
                            IconButton(
                                onClick = { currentMembersList.remove(email) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.remove_member),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = { onSave(groupName, currentMembersList.toList(), currentMemberNamesMap.toMap()) },
                    enabled = !isSaving && currentMembersList.isNotEmpty() && (!isCreateMode || groupName.isNotBlank()),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(if (isCreateMode) "Create" else stringResource(R.string.save))
                    }
                }
            }
        }
    }
}
