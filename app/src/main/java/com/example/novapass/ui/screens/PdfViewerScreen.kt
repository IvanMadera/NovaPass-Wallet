package com.example.novapass.ui.screens

import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(uri: Uri, pageIndex: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var fileDescriptor by remember { mutableStateOf<android.os.ParcelFileDescriptor?>(null) }
    var pageCount by remember { mutableIntStateOf(0) }
    val renderMutex = remember { Mutex() }

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    val transformableState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        if (scale == 1f) {
            offset = androidx.compose.ui.geometry.Offset.Zero
        } else {
            val maxX = (screenWidthPx * scale - screenWidthPx) / 2f
            val maxY = (screenHeightPx * scale - screenHeightPx) / 2f
            val newX = (offset.x + offsetChange.x * scale).coerceIn(-maxX, maxX)
            val newY = (offset.y + offsetChange.y * scale).coerceIn(-maxY, maxY)
            offset = androidx.compose.ui.geometry.Offset(newX, newY)
        }
    }

    DisposableEffect(uri) {
        try {
            val fd = context.contentResolver.openFileDescriptor(uri, "r")
            if (fd != null) {
                fileDescriptor = fd
                val renderer = PdfRenderer(fd)
                pdfRenderer = renderer
                pageCount = renderer.pageCount
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        onDispose {
            pdfRenderer?.close()
            fileDescriptor?.close()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Visualizar Boleto") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (pdfRenderer != null && pageCount > 0) {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .transformable(state = transformableState)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    PdfPageImage(
                        pdfRenderer = pdfRenderer!!,
                        pageIndex = pageIndex.coerceIn(0, pageCount - 1),
                        renderMutex = renderMutex
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Cargando o error al leer PDF")
            }
        }
    }
}

@Composable
fun PdfPageImage(pdfRenderer: PdfRenderer, pageIndex: Int, renderMutex: Mutex) {
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(pageIndex) {
        withContext(Dispatchers.IO) {
            renderMutex.withLock {
                try {
                    val page = pdfRenderer.openPage(pageIndex)
                    val width = page.width * 2
                    val height = page.height * 2
                    val currentBitmap = android.graphics.Bitmap.createBitmap(
                        width, height, android.graphics.Bitmap.Config.ARGB_8888
                    )
                    currentBitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(currentBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    bitmap = currentBitmap
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "Página $pageIndex del PDF",
            modifier = Modifier.fillMaxWidth()
        )
    } else {
        Box(
            modifier = Modifier.fillMaxWidth().height(400.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}
