package prasad.vennam.moneypilot.ui.news

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.ui.viewmodel.NewsViewModel

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsWebViewScreen(
    url: String,
    title: String,
    onBack: () -> Unit,
    showBookmark: Boolean = true,
    viewModel: NewsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var currentUrl by remember { mutableStateOf(url) }
    var currentTitle by remember { mutableStateOf(title) }
    var progress by remember { mutableIntStateOf(0) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    val isBookmarked =
        remember(uiState.bookmarks, currentUrl) {
            uiState.bookmarks.any { it.url == currentUrl }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = currentUrl,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    // Bookmark toggle button
                    if (showBookmark) {
                        IconButton(
                            onClick = {
                                if (isBookmarked) {
                                    viewModel.removeBookmarkByUrl(currentUrl)
                                } else {
                                    viewModel.addBookmark(
                                        title = currentTitle,
                                        url = currentUrl,
                                        currencyCode = uiState.selectedCurrency,
                                    )
                                }
                            },
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                contentDescription = stringResource(R.string.bookmark),
                                tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    // Share button
                    IconButton(
                        onClick = {
                            try {
                                val sendIntent =
                                    Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, "$currentTitle\n$currentUrl")
                                        type = "text/plain"
                                    }
                                val shareIntent = Intent.createChooser(sendIntent, context.getString(R.string.share_article))
                                context.startActivity(shareIntent)
                            } catch (e: Exception) {
                                // ignore
                            }
                        },
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = stringResource(R.string.share))
                    }

                    // Open Externally button
                    IconButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // ignore
                            }
                        },
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = stringResource(R.string.open_in_browser))
                    }

                    // Reload page button
                    IconButton(
                        onClick = {
                            webViewInstance?.reload()
                        },
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.refresh))
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            // Horizontal Loading Progress bar
            AnimatedVisibility(
                visible = progress < 100,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                )
            }

            // Embedded Android WebView
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams =
                            ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            builtInZoomControls = true
                            displayZoomControls = false
                            setSupportZoom(true)
                        }

                        webViewClient =
                            object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                ): Boolean {
                                    val requestUrl = request?.url?.toString() ?: return false
                                    if (requestUrl.startsWith("http://") || requestUrl.startsWith("https://")) {
                                        return false
                                    }

                                    // Block local file/content access schemes from external web contexts
                                    if (requestUrl.startsWith("file://") || requestUrl.startsWith("content://")) {
                                        return true
                                    }

                                    return try {
                                        val intent =
                                            if (requestUrl.startsWith("intent://")) {
                                                Intent.parseUri(requestUrl, Intent.URI_INTENT_SCHEME).apply {
                                                    // Enforce browsable category, prevent targeting local components
                                                    addCategory(Intent.CATEGORY_BROWSABLE)
                                                    component = null
                                                    selector = null
                                                }
                                            } else {
                                                val uri = Uri.parse(requestUrl)
                                                val allowedSchemes = listOf("tel", "mailto", "sms", "geo", "market")
                                                if (uri.scheme in allowedSchemes) {
                                                    Intent(Intent.ACTION_VIEW, uri)
                                                } else {
                                                    null
                                                }
                                            }

                                        if (intent != null) {
                                            context.startActivity(intent)
                                        }
                                        true
                                    } catch (e: Exception) {
                                        true
                                    }
                                }

                                override fun onPageFinished(
                                    view: WebView?,
                                    url: String?,
                                ) {
                                    super.onPageFinished(view, url)
                                    if (url != null) {
                                        currentUrl = url
                                    }
                                }
                            }

                        webChromeClient =
                            object : WebChromeClient() {
                                override fun onProgressChanged(
                                    view: WebView?,
                                    newProgress: Int,
                                ) {
                                    progress = newProgress
                                }

                                override fun onReceivedTitle(
                                    view: WebView?,
                                    title: String?,
                                ) {
                                    if (!title.isNullOrBlank()) {
                                        currentTitle = title
                                    }
                                }
                            }

                        webViewInstance = this
                        loadUrl(url)
                    }
                },
                update = { webView ->
                    webViewInstance = webView
                },
                modifier =
                    Modifier
                        .fillMaxSize()
                        .weight(1f),
            )
        }
    }
}
