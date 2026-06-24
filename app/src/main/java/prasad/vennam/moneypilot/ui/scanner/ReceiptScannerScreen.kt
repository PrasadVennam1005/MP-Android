package prasad.vennam.moneypilot.ui.scanner

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.view.MotionEvent
import android.widget.Toast
import androidx.camera.core.FocusMeteringAction
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import prasad.vennam.moneypilot.util.TrackScreen
import prasad.vennam.moneypilot.util.inPaisa
import java.util.Currency
import java.util.concurrent.Executors
import kotlinx.coroutines.launch
import com.google.mlkit.vision.text.Text
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import prasad.vennam.moneypilot.ads.AdConfig
import android.app.Activity
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ReceiptScannerScreen(
    onNavigateBack: () -> Unit,
    transactionViewModel: TransactionViewModel,
    analyticsHelper: AnalyticsHelper,
) {
    TrackScreen(analyticsHelper, "ReceiptScanner")
    val categories by transactionViewModel.allCategories.collectAsState()
    PermissionGate(
        permission = Manifest.permission.CAMERA,
        rationale = stringResource(R.string.camera_rationale),
    ) {
        ReceiptScannerContent(
            onNavigateBack = onNavigateBack,
            categories = categories,
            onSaveTransaction = { transactionViewModel.saveTransaction(it) },
            analyticsHelper = analyticsHelper,
            transactionViewModel = transactionViewModel,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScannerContent(
    onNavigateBack: () -> Unit,
    categories: List<Category>,
    onSaveTransaction: (Transaction) -> Unit,
    analyticsHelper: AnalyticsHelper,
    transactionViewModel: TransactionViewModel,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val scope = rememberCoroutineScope()

    var isScanning by remember { mutableStateOf(true) }
    var detectedData by remember { mutableStateOf<ParsedReceipt?>(null) }
    var showResultsSheet by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    val imageCapture = remember { ImageCapture.Builder().build() }

    val remainingScans by transactionViewModel.remainingAiScans.collectAsState()
    val isPremium by transactionViewModel.isPremium.collectAsState()

    var showUnlockDialog by remember { mutableStateOf(false) }
    var isAdLoading by remember { mutableStateOf(false) }
    var pendingVisionText by remember { mutableStateOf<Text?>(null) }
    var isGalleryScan by remember { mutableStateOf(false) }

    var camera by remember { mutableStateOf<Camera?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    var showShutter by remember { mutableStateOf(false) }

    LaunchedEffect(isTorchOn, camera) {
        try {
            camera?.cameraControl?.enableTorch(isTorchOn)
        } catch (_: Exception) {}
    }

    fun loadAndShowAd(onRewardEarned: () -> Unit, onFailure: () -> Unit) {
        val activity = context as? Activity
        if (activity == null) {
            onFailure()
            return
        }
        isAdLoading = true
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            AdConfig.rewardedAdUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    isAdLoading = false
                    onFailure()
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    isAdLoading = false
                    ad.show(activity) {
                        onRewardEarned()
                    }
                }
            }
        )
    }

    suspend fun processOcrResult(visionText: Text): ParsedReceipt {
        val rawText = visionText.text
        if (transactionViewModel.isAiReady && rawText.isNotBlank()) {
            val aiResult = transactionViewModel.parseReceiptText(rawText)
            if (aiResult != null && aiResult.amount != null) {
                return aiResult
            }
        }
        return ReceiptParser.parse(visionText)
    }

    fun logScanEvent(result: ParsedReceipt, isGallery: Boolean) {
        val eventName = if (isGallery) "scanner_gallery_upload" else "scanner_picture_captured"
        analyticsHelper.logEvent(
            eventName,
            mapOf(
                "success" to (result.amount != null),
                "merchant_found" to (result.merchant != null),
                "parsed_by_ai" to transactionViewModel.isAiReady,
            )
        )
    }

    fun handleScanResult(result: ParsedReceipt) {
        if (result.amount != null) {
            detectedData = result
            isScanning = false
            showResultsSheet = true
        } else {
            Toast.makeText(
                context,
                with(context) { getString(R.string.could_not_detect_amount_in_this_image_please_try_another) },
                Toast.LENGTH_LONG
            ).show()
        }
        isProcessing = false
    }

    fun handleOcrResult(visionText: Text, isGallery: Boolean) {
        val rawText = visionText.text
        if (rawText.isBlank()) {
            Toast.makeText(context, with(context) { getString(R.string.could_not_detect_amount_in_this_image_please_try_another) }, Toast.LENGTH_LONG).show()
            isProcessing = false
            return
        }

        if (transactionViewModel.isAiReady && !isPremium && remainingScans <= 0) {
            // Out of scans: prompt unlock dialog
            pendingVisionText = visionText
            isGalleryScan = isGallery
            showUnlockDialog = true
            isProcessing = false
        } else {
            // Proceed normally
            isProcessing = true
            scope.launch {
                val result = processOcrResult(visionText)
                logScanEvent(result, isGallery)
                handleScanResult(result)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
            cameraExecutor.shutdown()
            recognizer.close()
        }
    }

    val previewView = remember { PreviewView(context) }

    // Gallery Picker Launcher
    val galleryLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
            uri?.let {
                isProcessing = true
                scope.launch {
                    val inputImage = withContext(Dispatchers.IO) {
                        getDownscaledInputImage(context, it)
                    }
                    if (inputImage != null) {
                        recognizer
                            .process(inputImage)
                            .addOnSuccessListener { visionText ->
                                handleOcrResult(visionText, isGallery = true)
                            }
                            .addOnFailureListener {
                                isProcessing = false
                                Toast.makeText(context, with(context) { getString(R.string.scanning_failed) }, Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        isProcessing = false
                        Toast.makeText(context, with(context) { getString(R.string.scanning_failed) }, Toast.LENGTH_SHORT).show()
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

                try {
                    provider.unbindAll()
                    val boundCamera = provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture,
                    )
                    camera = boundCamera
                } catch (e: Exception) {
                    // Ignore use case binding failures
                }
            }, ContextCompat.getMainExecutor(context))
        } else {
            cameraProvider?.unbindAll()
            camera = null
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
                    IconButton(onClick = {
                        isTorchOn = !isTorchOn
                        analyticsHelper.logEvent("scanner_flash_toggled", mapOf("is_on" to isTorchOn))
                    }) {
                        Icon(
                            imageVector = if (isTorchOn) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                            contentDescription = stringResource(R.string.flash),
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = {
                        analyticsHelper.logEvent("scanner_gallery_opened")
                        galleryLauncher.launch("image/*")
                    }) {
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
                factory = { ctx ->
                    previewView.apply {
                        setOnTouchListener { view, event ->
                            if (event.action == MotionEvent.ACTION_DOWN) {
                                true
                            } else if (event.action == MotionEvent.ACTION_UP) {
                                val cameraControl = camera?.cameraControl
                                if (cameraControl != null) {
                                    val factory = meteringPointFactory
                                    val point = factory.createPoint(event.x, event.y)
                                    val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
                                        .setAutoCancelDuration(3, TimeUnit.SECONDS)
                                        .build()
                                    cameraControl.startFocusAndMetering(action)
                                }
                                view.performClick()
                                true
                            } else {
                                false
                            }
                        }
                    }
                },
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
                            .padding(top = 220.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (isScanning && !isProcessing) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 48.dp)
                            .size(72.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .padding(6.dp)
                            .background(Color.White, CircleShape)
                            .clickable {
                                analyticsHelper.logEvent("scanner_shutter_clicked")
                                isProcessing = true
                                showShutter = true
                                scope.launch {
                                    kotlinx.coroutines.delay(100.milliseconds)
                                    showShutter = false
                                }
                                val executor = ContextCompat.getMainExecutor(context)
                                imageCapture.takePicture(
                                    executor,
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        @androidx.annotation.OptIn(ExperimentalGetImage::class)
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            val mediaImage = image.image
                                            if (mediaImage != null) {
                                                val inputImage = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
                                                recognizer
                                                    .process(inputImage)
                                                    .addOnSuccessListener { visionText ->
                                                        handleOcrResult(visionText, isGallery = false)
                                                    }.addOnFailureListener {
                                                        isProcessing = false
                                                        Toast.makeText(context, with(context) { getString(R.string.scanning_failed) }, Toast.LENGTH_SHORT).show()
                                                    }.addOnCompleteListener {
                                                        image.close()
                                                    }
                                            } else {
                                                image.close()
                                                isProcessing = false
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            isProcessing = false
                                            Toast.makeText(context, with(context) { getString(R.string.capture_failed_formatted, exception.message) }, Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                )
                            },
                )
            }

            if (isProcessing) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.processing_receipt),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            if (showShutter) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                )
            }
        }
    }

    val currentDetectedData = detectedData
    if (showResultsSheet && currentDetectedData != null) {
        ReceiptResultsBottomSheet(
            detectedData = currentDetectedData,
            categories = categories,
            onPredictCategory = { transactionViewModel.predictCategoryForMerchant(it) },
            onDismiss = {
                showResultsSheet = false
                isScanning = true
            },
            onSave = { transaction ->
                analyticsHelper.logEvent("scanner_expense_saved")
                onSaveTransaction(transaction)
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

    if (showUnlockDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isAdLoading) {
                    showUnlockDialog = false
                    isProcessing = false
                    pendingVisionText = null
                }
            },
            title = {
                Text(
                    stringResource(R.string.unlock_scans_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(stringResource(R.string.unlock_scans_desc))
                    if (isAdLoading) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(stringResource(R.string.loading_ad))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        loadAndShowAd(
                            onRewardEarned = {
                                transactionViewModel.incrementAiScans(3)
                                Toast.makeText(context, with(context) { getString(R.string.unlocked_scans_toast) }, Toast.LENGTH_SHORT).show()
                                pendingVisionText?.let { visionText ->
                                    isProcessing = true
                                    scope.launch {
                                        val result = processOcrResult(visionText)
                                        logScanEvent(result, isGalleryScan)
                                        handleScanResult(result)
                                    }
                                }
                                pendingVisionText = null
                                showUnlockDialog = false
                            },
                            onFailure = {
                                Toast.makeText(context, with(context) { getString(R.string.ad_failed_to_load) }, Toast.LENGTH_LONG).show()
                                pendingVisionText?.let { visionText ->
                                    isProcessing = true
                                    scope.launch {
                                        val result = ReceiptParser.parse(visionText)
                                        logScanEvent(result, isGalleryScan)
                                        handleScanResult(result)
                                    }
                                }
                                pendingVisionText = null
                                showUnlockDialog = false
                            }
                        )
                    },
                    enabled = !isAdLoading,
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.watch_ad_btn))
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            pendingVisionText?.let { visionText ->
                                isProcessing = true
                                scope.launch {
                                    val result = ReceiptParser.parse(visionText)
                                    logScanEvent(result, isGalleryScan)
                                    handleScanResult(result)
                                }
                            }
                            pendingVisionText = null
                            showUnlockDialog = false
                        },
                        enabled = !isAdLoading
                    ) {
                        Text(stringResource(R.string.basic_scan_btn))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            showUnlockDialog = false
                            isProcessing = false
                            pendingVisionText = null
                        },
                        enabled = !isAdLoading
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptResultsBottomSheet(
    detectedData: ParsedReceipt,
    categories: List<Category>,
    onPredictCategory: (String) -> Long?,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit,
) {
    val currencyCode = LocalCurrencyCode.current
    val currencySymbol = remember(currencyCode) { Currency.getInstance(currencyCode).symbol }
    var merchant by remember { mutableStateOf(detectedData.merchant ?: "") }
    var amount by remember { mutableStateOf(detectedData.amount?.toString() ?: "") }
    var selectedCategoryId by remember {
        mutableStateOf<Long?>(
            detectedData.merchant?.let { onPredictCategory(it) }
        )
    }

    LaunchedEffect(merchant) {
        val predicted = onPredictCategory(merchant)
        if (predicted != null) {
            selectedCategoryId = predicted
        }
    }
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

private fun getDownscaledInputImage(context: android.content.Context, uri: Uri, maxDimension: Int = 2000): InputImage? {
    return try {
        // Step 1: Decode image size only
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, options)
        } ?: return null

        // Calculate scale factor
        var scale = 1
        val height = options.outHeight
        val width = options.outWidth
        if (height > maxDimension || width > maxDimension) {
            val maxDep = maxOf(height, width)
            scale = (maxDep / maxDimension.toDouble()).let { Math.round(it).toInt() }
        }

        // Step 2: Decode bitmap with sample size
        val outputOptions = BitmapFactory.Options().apply {
            inSampleSize = scale
        }
        var bitmap = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, outputOptions)
        } ?: return null

        // Step 3: Get orientation from EXIF
        var rotationDegrees = 0
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val exifInterface = ExifInterface(inputStream)
            val orientation = exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            rotationDegrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        }

        // Step 4: If rotated, perform rotation on the bitmap
        if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotatedBitmap != bitmap) {
                bitmap.recycle()
                bitmap = rotatedBitmap
            }
            rotationDegrees = 0
        }

        InputImage.fromBitmap(bitmap, rotationDegrees)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
