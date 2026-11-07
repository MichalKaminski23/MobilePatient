package pl.polsl.mobilepatient.ui.patient.scans

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.createBitmap
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.polsl.mobilepatient.data.dto.MedicalFileDto
import java.io.File

@Composable
fun ScanPreviewDialog(
    file: MedicalFileDto,
    previewBytes: ByteArray?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val isPdf = file.mime_type.contains("pdf", ignoreCase = true) ||
            file.file_name.endsWith(".pdf", ignoreCase = true)

    val imageBitmap = remember(previewBytes, isPdf) {
        if (!isPdf && previewBytes != null) {
            BitmapFactory.decodeByteArray(previewBytes, 0, previewBytes.size)
        } else {
            null
        }
    }

    var pdfPages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var currentPdfPageIndex by remember { mutableIntStateOf(0) }
    var isPdfRendering by remember { mutableStateOf(false) }

    LaunchedEffect(previewBytes, isPdf) {
        if (isPdf && previewBytes != null && previewBytes.isNotEmpty()) {
            isPdfRendering = true
            withContext(Dispatchers.IO) {
                try {
                    val tempFile = File.createTempFile("pdf_preview_", ".pdf", context.cacheDir)
                    tempFile.writeBytes(previewBytes)
                    val pfd =
                        ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = PdfRenderer(pfd)
                    val pages = mutableListOf<Bitmap>()
                    val total = renderer.pageCount.coerceAtMost(10)
                    for (i in 0 until total) {
                        val page = renderer.openPage(i)
                        val width = (page.width * 2).coerceAtMost(2048)
                        val height = (page.height * 2).coerceAtMost(2048)
                        val bmp = createBitmap(width, height)
                        bmp.eraseColor(android.graphics.Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        pages.add(bmp)
                        page.close()
                    }
                    renderer.close()
                    pfd.close()
                    tempFile.delete()

                    withContext(Dispatchers.Main) {
                        pdfPages = pages
                        currentPdfPageIndex = 0
                        isPdfRendering = false
                    }
                } catch (_: Exception) {
                    withContext(Dispatchers.Main) {
                        isPdfRendering = false
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isPdf) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    modifier = Modifier.padding(end = 8.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(
                                            horizontal = 6.dp,
                                            vertical = 2.dp
                                        ),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            Icons.Default.PictureAsPdf,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(12.dp),
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "PDF",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                        )
                                    }
                                }
                            }
                            Text(
                                text = file.file_name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                            )
                        }
                        Text(
                            text = "Data dodania: ${file.created_at?.take(10) ?: ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Zamknij",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }

//                Surface(
//                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(horizontal = 16.dp, vertical = 4.dp),
//                    shape = RoundedCornerShape(8.dp),
//                ) {
//                    Row(
//                        modifier = Modifier.padding(8.dp),
//                        verticalAlignment = Alignment.CenterVertically,
//                    ) {
//                        Icon(
//                            imageVector = Icons.Default.Security,
//                            contentDescription = null,
//                            tint = MaterialTheme.colorScheme.primary,
//                            modifier = Modifier.size(16.dp),
//                        )
//                        Spacer(modifier = Modifier.width(6.dp))
//                        Text(
//                            text = "Połączenie szyfrowane HTTPS (pobieranie z autoryzacją Bearer)",
//                            style = MaterialTheme.typography.labelSmall,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant,
//                        )
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        isLoading || isPdfRendering -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (isPdfRendering) "Renderowanie stron PDF..." else "Pobieranie zabezpieczonego pliku...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        isPdf && pdfPages.isNotEmpty() -> {
                            Image(
                                bitmap = pdfPages[currentPdfPageIndex].asImageBitmap(),
                                contentDescription = "Strona ${currentPdfPageIndex + 1}",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White),
                            )
                        }

                        imageBitmap != null -> {
                            Image(
                                bitmap = imageBitmap.asImageBitmap(),
                                contentDescription = file.file_name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp)),
                            )
                        }

                        !file.signed_url.isNullOrEmpty() && !isPdf -> {
                            AsyncImage(
                                model = file.signed_url,
                                contentDescription = file.file_name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp)),
                            )
                        }

                        else -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(48.dp),
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Nie udało się wygenerować podglądu pliku.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                if (isPdf && pdfPages.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = {
                                if (currentPdfPageIndex > 0) currentPdfPageIndex--
                            },
                            enabled = currentPdfPageIndex > 0,
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Poprzednia strona"
                            )
                        }

                        Text(
                            text = "Strona ${currentPdfPageIndex + 1} z ${pdfPages.size}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )

                        IconButton(
                            onClick = {
                                if (currentPdfPageIndex < pdfPages.size - 1) currentPdfPageIndex++
                            },
                            enabled = currentPdfPageIndex < pdfPages.size - 1,
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Następna strona"
                            )
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Zamknij podgląd", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
