package prasad.vennam.moneypilot.ui.scanner

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.data.entity.Category
import prasad.vennam.moneypilot.data.entity.Transaction
import prasad.vennam.moneypilot.data.entity.TransactionType
import prasad.vennam.moneypilot.ui.viewmodel.TransactionViewModel
import prasad.vennam.moneypilot.util.AnalyticsHelper
import prasad.vennam.moneypilot.util.LocalCurrencyCode
import prasad.vennam.moneypilot.util.ParsedReceipt
import prasad.vennam.moneypilot.util.PermissionGate
import prasad.vennam.moneypilot.util.ReceiptParser
import prasad.vennam.moneypilot.util.inPaisa
import java.util.Currency
import java.util.concurrent.Executors

@Composable
fun ReceiptScannerScreen(
    onNavigateBack: () -> Unit,
    transactionViewModel: TransactionViewModel,
    analyticsHelper: AnalyticsHelper,
) {
    PermissionGate(
        permission = Manifest.permission.CAMERA,
        rationale = "Camera access is required to scan receipts and automatically extract expense details.",
    ) {
        ReceiptScannerContent(onNavigateBack, transactionViewModel, analyticsHelper)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScannerContent(
    onNavigateBack: () -> Unit,
    transactionViewModel: TransactionViewModel,
    analyticsHelper: AnalyticsHelper,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
            cameraExecutor.shutdown()
            recognizer.close()
        }
    }

    var isScanning by remember { mutableStateOf(true) }
    var detectedData by remember { mutableStateOf<ParsedReceipt?>(null) }
    var showResultsSheet by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val previewView = remember { PreviewView(context) }

    // Gallery Picker Launcher
    val galleryLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
            uri?.let {
                val inputImage = InputImage.fromFilePath(context, it)
                recognizer
                    .process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val result = ReceiptParser.parse(visionText)

                        analyticsHelper.logEvent(
                            "scanner_gallery_upload",
                            mapOf(
                                "success" to (result.amount != null),
                                "merchant_found" to (result.merchant != null),
                            ),
                        )

                        if (result.amount != null) {
                            detectedData = result
                            isScanning = false
                            showResultsSheet = true
                        } else {
                            Toast
                                .makeText(
                                    context,
                                    context.run { getString(R.string.could_not_detect_amount_in_this_image_please_try_another) },
                                    Toast.LENGTH_LONG,
                                ).show()
                        }
                    }
            }
        }

    LaunchedEffect(isScanning) {
        if (isScanning) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val provider = cameraProviderFuture.get()
                cameraProvider = provider
                val preview =
                    Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                val imageAnalysis =
                    ImageAnalysis
                        .Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor) { imageProxy ->
                                processImageProxy(imageProxy, recognizer) { result ->
                                    if (result.amount != null && isScanning) {
                                        analyticsHelper.logEvent(
                                            "scanner_live_scan_success",
                                            mapOf(
                                                "merchant_found" to (result.merchant != null),
                                            ),
                                        )

                                        detectedData = result
                                        isScanning = false
                                        showResultsSheet = true
                                    }
                                }
                            }
                        }

                try {
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis,
                    )
                } catch (e: Exception) {
                    // Ignore use case binding failures
                }
            }, ContextCompat.getMainExecutor(context))
        } else {
            cameraProvider?.unbindAll()
        }
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.scan_receipt),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                        Icon(Icons.Rounded.Collections, contentDescription = stringResource(R.string.gallery), tint = Color.White)
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.5f),
                        titleContentColor = Color.White,
                    ),
            )
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize(),
            )

            // Perfectly centered 3/4 screen overlay
            ScannerOverlay(modifier = Modifier.fillMaxSize())

            if (isScanning) {
                Text(
                    stringResource(R.string.position_receipt_within_the_frame),
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .padding(top = 350.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }

    val currentDetectedData = detectedData
    if (showResultsSheet && currentDetectedData != null) {
        val categories by transactionViewModel.allCategories.collectAsState()
        ReceiptResultsBottomSheet(
            detectedData = currentDetectedData,
            categories = categories,
            onDismiss = {
                showResultsSheet = false
                isScanning = true
            },
            onSave = { transaction ->
                transactionViewModel.saveTransaction(transaction)
                showResultsSheet = false
                showSuccessDialog = true
            },
        )
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text(stringResource(R.string.expense_saved), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.would_you_like_to_scan)) },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        isScanning = true
                        detectedData = null
                    },
                    shape = MaterialTheme.shapes.large,
                ) { Text(stringResource(R.string.scan_another)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSuccessDialog = false
                    onNavigateBack()
                }) { Text(stringResource(R.string.go_to_dashboard)) }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptResultsBottomSheet(
    detectedData: ParsedReceipt,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit,
) {
    val currencyCode = LocalCurrencyCode.current
    val currencySymbol = remember(currencyCode) { Currency.getInstance(currencyCode).symbol }
    var merchant by remember { mutableStateOf(detectedData.merchant ?: "") }
    var amount by remember { mutableStateOf(detectedData.amount?.toString() ?: "") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var timestamp by remember { mutableStateOf(detectedData.date ?: System.currentTimeMillis()) }
    var showCategoryMenu by remember { mutableStateOf(false) }

    val amountVal = amount.toDoubleOrNull()
    val isAmountError = amount.isNotEmpty() && (amountVal == null || amountVal <= 0.0)
    val isFormValid = amount.isNotBlank() && !isAmountError && selectedCategoryId != null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
                    .verticalScroll(rememberScrollState()),
        ) {
            // Header
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.confirm_details),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                )
                IconButton(
                    onClick = onDismiss,
                    modifier =
                        Modifier
                            .size(32.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                CircleShape,
                            ),
                ) {
                    Icon(Icons.Rounded.Close, null, modifier = Modifier.size(18.dp))
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(bottom = 24.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )

            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Merchant
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text(stringResource(R.string.merchant)) },
                    leadingIcon = { Icon(Icons.Rounded.Store, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                )

                // Amount
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) amount = it },
                    label = { Text(stringResource(R.string.amount_1)) },
                    leadingIcon = {
                        Text(
                            currencySymbol,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    },
                    isError = isAmountError,
                    supportingText =
                        if (isAmountError) {
                            { Text(stringResource(R.string.amount_error_desc)) }
                        } else {
                            null
                        },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                )

                // Category Picker
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = categories.find { it.id == selectedCategoryId }?.name ?: stringResource(R.string.select_category),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.category)) },
                        leadingIcon = { Icon(Icons.Rounded.Category, null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        trailingIcon = {
                            IconButton(onClick = {
                                showCategoryMenu = true
                            }) { Icon(Icons.Rounded.ArrowDropDown, contentDescription = stringResource(R.string.select_category)) }
                        },
                    )
                    Box(
                        modifier =
                            Modifier
                                .matchParentSize()
                                .clickable { showCategoryMenu = true },
                    )
                    DropdownMenu(
                        expanded = showCategoryMenu,
                        onDismissRequest = { showCategoryMenu = false },
                        modifier = Modifier.fillMaxWidth(0.8f),
                    ) {
                        categories.filter { it.isExpense }.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                leadingIcon = {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.Label,
                                        null,
                                        tint = Color(category.color),
                                        modifier = Modifier.size(20.dp),
                                    )
                                },
                                onClick = {
                                    selectedCategoryId = category.id
                                    showCategoryMenu = false
                                },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (isFormValid) {
                            onSave(
                                Transaction(
                                    amount = (amount.toDoubleOrNull() ?: 0.0).inPaisa,
                                    timestamp = timestamp,
                                    categoryId = selectedCategoryId,
                                    note = merchant,
                                    type = TransactionType.EXPENSE,
                                ),
                            )
                        }
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                    shape = MaterialTheme.shapes.large,
                    enabled = isFormValid,
                ) {
                    Icon(Icons.Rounded.Check, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.save_expense), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun processImageProxy(
    imageProxy: ImageProxy,
    recognizer: com.google.mlkit.vision.text.TextRecognizer,
    onResult: (ParsedReceipt) -> Unit,
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        recognizer
            .process(image)
            .addOnCompleteListener {
                imageProxy.close()
            }.addOnSuccessListener { visionText ->
                val result = ReceiptParser.parse(visionText)
                onResult(result)
            }.addOnFailureListener {
                onResult(ParsedReceipt())
            }
    } else {
        imageProxy.close()
    }
}

@Composable
fun ScannerOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = 3.dp.toPx()
        val cornerRadius = 24.dp.toPx()

        // Perfectly centered 3/4 screen overlay
        val overlayWidth = size.width * 0.85f
        val overlayHeight = size.height * 0.65f
        val left = (size.width - overlayWidth) / 2
        val top = (size.height - overlayHeight) / 2

        drawRect(
            color = Color.Black.copy(alpha = 0.6f),
            size = size,
        )

        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(overlayWidth, overlayHeight),
            cornerRadius = CornerRadius(cornerRadius),
            blendMode = BlendMode.Clear,
        )

        drawRoundRect(
            color = Color.White,
            topLeft = Offset(left, top),
            size = Size(overlayWidth, overlayHeight),
            cornerRadius = CornerRadius(cornerRadius),
            style = Stroke(width = strokeWidth),
        )
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    icon: ImageVector,
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(value, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
        }
    }
}
