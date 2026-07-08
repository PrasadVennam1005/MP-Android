package prasad.vennam.moneypilot.ui.cosplit

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import prasad.vennam.moneypilot.feature.cosplit.ui.CoSplitViewModel
import prasad.vennam.moneypilot.feature.cosplit.util.AiReceiptLineItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoSplitAddExpenseScreen(
    viewModel: CoSplitViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val group by viewModel.selectedGroup.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val scope = rememberCoroutineScope()

    if (group == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No group selected")
        }
        return
    }

    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var paidBy by remember { mutableStateOf(userEmail) }
    var splitType by remember { mutableStateOf("EQUAL") } // EQUAL, EXACT, PERCENT

    // Map: memberEmail -> split value (amount, percentage, or inclusion flag 1.0/0.0)
    val splitDetails = remember { mutableStateMapOf<String, Double>() }

    // Initialize splits on startup or when group changes
    LaunchedEffect(group) {
        group?.members?.forEach { member ->
            splitDetails[member] = 1.0 // for EQUAL, 1.0 means included, 0.0 means excluded
        }
    }

    // Receipt Line Item Splits States
    var showReceiptSplitSheet by remember { mutableStateOf(false) }
    var receiptLineItems by remember { mutableStateOf<List<AiReceiptLineItem>>(emptyList()) }
    // Map: itemIndex -> Set of memberEmails who share this item
    val itemAssignments = remember { mutableStateMapOf<Int, Set<String>>() }

    // Launcher for receipt gallery image pick
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val inputImage = InputImage.fromFilePath(context, uri)
                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    recognizer.process(inputImage)
                        .addOnSuccessListener { visionText ->
                            val ocrText = visionText.text
                            if (ocrText.isNotBlank()) {
                                viewModel.parseReceiptToLineItems(ocrText) { parsedItems ->
                                    if (!parsedItems.isNullOrEmpty()) {
                                        receiptLineItems = parsedItems
                                        itemAssignments.clear()
                                        parsedItems.indices.forEach { idx ->
                                            itemAssignments[idx] = emptySet()
                                        }
                                        showReceiptSplitSheet = true
                                    } else {
                                        Toast.makeText(context, "Could not extract receipt items via AI", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } else {
                                Toast.makeText(context, "No text detected in this image", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "OCR Text Recognition failed", Toast.LENGTH_SHORT).show()
                        }
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to load image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Expense", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        enabled = !isAiLoading
                    ) {
                        if (isAiLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Rounded.DocumentScanner, contentDescription = "Scan Receipt")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Total Amount (₹)") },
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text("Paid By", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                DropdownSelector(
                    options = group!!.members,
                    selected = paidBy,
                    displayMap = group!!.memberNames,
                    onSelected = { paidBy = it }
                )
            }

            item {
                Text("Split Options", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                TabRow(
                    selectedTabIndex = when (splitType) {
                        "EQUAL" -> 0
                        "EXACT" -> 1
                        "PERCENT" -> 2
                        else -> 0
                    }
                ) {
                    Tab(selected = splitType == "EQUAL", onClick = { splitType = "EQUAL" }, text = { Text("Equally") })
                    Tab(selected = splitType == "EXACT", onClick = { splitType = "EXACT" }, text = { Text("Exact") })
                    Tab(selected = splitType == "PERCENT", onClick = { splitType = "PERCENT" }, text = { Text("Percent") })
                }
            }

            item {
                Text("Split Details", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            items(group!!.members) { member ->
                val totalAmount = amountText.toDoubleOrNull() ?: 0.0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val memberName = group!!.memberNames[member] ?: member
                    Text(
                        text = memberName,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )

                    when (splitType) {
                        "EQUAL" -> {
                            val included = splitDetails[member] ?: 1.0
                            Checkbox(
                                checked = included == 1.0,
                                onCheckedChange = { check ->
                                    splitDetails[member] = if (check) 1.0 else 0.0
                                }
                            )
                        }
                        "EXACT" -> {
                            var textVal by remember(member) {
                                mutableStateOf(String.format("%.2f", splitDetails[member] ?: 0.0))
                            }
                            OutlinedTextField(
                                value = textVal,
                                onValueChange = { newValue ->
                                    textVal = newValue
                                    val num = newValue.toDoubleOrNull() ?: 0.0
                                    splitDetails[member] = num
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.width(120.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                maxLines = 1
                            )
                        }
                        "PERCENT" -> {
                            var textVal by remember(member) {
                                mutableStateOf(String.format("%.1f", splitDetails[member] ?: 0.0))
                            }
                            OutlinedTextField(
                                value = textVal,
                                onValueChange = { newValue ->
                                    textVal = newValue
                                    val num = newValue.toDoubleOrNull() ?: 0.0
                                    splitDetails[member] = num
                                },
                                suffix = { Text("%") },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.width(120.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull() ?: 0.0
                        if (description.isNotBlank() && amount > 0.0) {
                            val detailsMap = splitDetails.toMap()
                            viewModel.addExpense(
                                description,
                                amount,
                                paidBy,
                                splitType,
                                detailsMap
                            ) { success ->
                                if (success) {
                                    onNavigateBack()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = description.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0.0
                ) {
                    Text("Save Expense", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showReceiptSplitSheet) {
        AiSplitReviewSheet(
            lineItems = receiptLineItems,
            members = group!!.members,
            memberNames = group!!.memberNames,
            itemAssignments = itemAssignments,
            onDismiss = { showReceiptSplitSheet = false },
            onAssignItem = { itemIdx, emails ->
                itemAssignments[itemIdx] = emails
            },
            onApply = {
                // Apply assignments to EXACT splits
                val sumMap = group!!.members.associateWith { 0.0 }.toMutableMap()
                var sumTotal = 0.0

                itemAssignments.forEach { (itemIdx, emails) ->
                    val price = receiptLineItems[itemIdx].price
                    sumTotal += price
                    if (emails.isNotEmpty()) {
                        val share = price / emails.size
                        emails.forEach { email ->
                            sumMap[email] = (sumMap[email] ?: 0.0) + share
                        }
                    } else {
                        // If no one is selected, split equally to everyone in group
                        val share = price / group!!.members.size
                        group!!.members.forEach { email ->
                            sumMap[email] = (sumMap[email] ?: 0.0) + share
                        }
                    }
                }

                splitType = "EXACT"
                amountText = String.format("%.2f", sumTotal)
                splitDetails.clear()
                sumMap.forEach { (email, amt) ->
                    splitDetails[email] = amt
                }

                showReceiptSplitSheet = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiSplitReviewSheet(
    lineItems: List<AiReceiptLineItem>,
    members: List<String>,
    memberNames: Map<String, String>,
    itemAssignments: Map<Int, Set<String>>,
    onDismiss: () -> Unit,
    onAssignItem: (Int, Set<String>) -> Unit,
    onApply: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Receipt Click-to-Split", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                Text("Select members who shared each item:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(lineItems.size) { index ->
                        val item = lineItems[index]
                        val selectedEmails = itemAssignments[index] ?: emptySet()

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(item.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                    Text("₹${item.price}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    members.forEach { email ->
                                        val isAssigned = selectedEmails.contains(email)
                                        FilterChip(
                                            selected = isAssigned,
                                            onClick = {
                                                val nextSet = if (isAssigned) {
                                                    selectedEmails - email
                                                } else {
                                                    selectedEmails + email
                                                }
                                                onAssignItem(index, nextSet)
                                            },
                                            label = { 
                                                val displayName = memberNames[email] ?: email.substringBefore("@")
                                                Text(displayName, fontSize = 11.sp) 
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onApply, shape = RoundedCornerShape(10.dp)) {
                Text("Apply splits")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
