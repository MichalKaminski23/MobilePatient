package pl.polsl.mobilepatient.ui.patient.scans

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DocumentUploadDialog(
    initialBitmap: Bitmap?,
    initialUri: Uri?,
    isDirectPdf: Boolean = false,
    context: Context,
    onDismiss: () -> Unit,
    onConfirm: (fileName: String, mimeType: String, bytes: ByteArray) -> Unit,
) {
    val categories = listOf(
        "Badanie krwi / moczu",
        "Badanie obrazowe (RTG/USG/TK)",
        "Wypis ze szpitala",
        "E-recepta / Zalecenie",
        "Inny dokument medyczny",
    )

    var selectedCategory by remember { mutableStateOf(categories[0]) }
    var selectedFormat by remember { mutableStateOf(if (isDirectPdf) "PDF" else "JPG") }
    val dateStamp =
        remember { SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date()) }
    var customName by remember { mutableStateOf("") }

    val (pdfDisplayName, pdfByteSize) = remember(initialUri, isDirectPdf) {
        if (isDirectPdf && initialUri != null) {
            queryFileInfo(context, initialUri)
        } else {
            Pair("", 0L)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 680.dp)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (isDirectPdf) Icons.Default.PictureAsPdf else Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isDirectPdf) "Wgraj dokument PDF" else "Dodaj dokument / skan",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Zapisz i zaszyfruj w chmurze",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Zamknij",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDirectPdf) {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        ),
                    ) {
                        when {
                            isDirectPdf -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        modifier = Modifier.size(54.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.PictureAsPdf,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(32.dp),
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = pdfDisplayName.ifEmpty { "Dokument PDF" },
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (pdfByteSize > 0) {
                                                "Rozmiar: ${pdfByteSize / 1024} KB • Gotowy do wysyłki"
                                            } else {
                                                "Oryginalny plik PDF z urządzenia"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }

                            initialBitmap != null -> {
                                Image(
                                    bitmap = initialBitmap.asImageBitmap(),
                                    contentDescription = "Podgląd",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }

                            initialUri != null -> {
                                AsyncImage(
                                    model = initialUri,
                                    contentDescription = "Podgląd",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }

                            else -> {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        ),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "1",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Wybierz kategorię badania",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(categories) { category ->
                                    FilterChip(
                                        selected = selectedCategory == category,
                                        onClick = { selectedCategory = category },
                                        label = { Text(category, fontWeight = FontWeight.Medium) },
                                        shape = RoundedCornerShape(10.dp),
                                    )
                                }
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        ),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "2",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Format zapisu",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (isDirectPdf) {
                                FilterChip(
                                    selected = true,
                                    onClick = {},
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.PictureAsPdf,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    },
                                    label = {
                                        Text(
                                            "PDF (Oryginalny dokument)",
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    FilterChip(
                                        selected = selectedFormat == "JPG",
                                        onClick = { selectedFormat = "JPG" },
                                        label = {
                                            Text(
                                                "JPG",
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f),
                                    )
                                    FilterChip(
                                        selected = selectedFormat == "PNG",
                                        onClick = { selectedFormat = "PNG" },
                                        label = {
                                            Text(
                                                "PNG",
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f),
                                    )
                                    FilterChip(
                                        selected = selectedFormat == "PDF",
                                        onClick = { selectedFormat = "PDF" },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.PictureAsPdf,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.error,
                                            )
                                        },
                                        label = {
                                            Text(
                                                "PDF A4",
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1.2f),
                                    )
                                }
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        ),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "3",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Własna nazwa pliku (opcjonalnie)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = customName,
                                onValueChange = { customName = it },
                                label = { Text("Wpisz tytuł lub opis badania") },
                                placeholder = {
                                    Text(
                                        if (isDirectPdf && pdfDisplayName.isNotEmpty()) {
                                            pdfDisplayName.substringBeforeLast(".")
                                        } else {
                                            "np. Morfologia $selectedCategory"
                                        },
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Anuluj", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            val ext = when (selectedFormat) {
                                "PNG" -> ".png"
                                "PDF" -> ".pdf"
                                else -> ".jpg"
                            }
                            val mime = when (selectedFormat) {
                                "PNG" -> "image/png"
                                "PDF" -> "application/pdf"
                                else -> "image/jpeg"
                            }
                            val baseName = customName.trim().ifEmpty {
                                if (isDirectPdf && pdfDisplayName.isNotEmpty()) {
                                    pdfDisplayName.substringBeforeLast(".")
                                } else {
                                    selectedCategory
                                }
                            }
                            val finalFileName = "${baseName}_$dateStamp$ext"

                            val bytes = when {
                                isDirectPdf && initialUri != null -> {
                                    context.contentResolver.openInputStream(initialUri)
                                        ?.use { it.readBytes() } ?: ByteArray(0)
                                }

                                initialBitmap != null -> {
                                    when (selectedFormat) {
                                        "PDF" -> createPdfFromBitmap(initialBitmap)
                                        "PNG" -> {
                                            val stream = ByteArrayOutputStream()
                                            initialBitmap.compress(
                                                Bitmap.CompressFormat.PNG,
                                                100,
                                                stream
                                            )
                                            stream.toByteArray()
                                        }

                                        else -> {
                                            val stream = ByteArrayOutputStream()
                                            initialBitmap.compress(
                                                Bitmap.CompressFormat.JPEG,
                                                90,
                                                stream
                                            )
                                            stream.toByteArray()
                                        }
                                    }
                                }

                                initialUri != null -> {
                                    val input = context.contentResolver.openInputStream(initialUri)
                                    val bmp = BitmapFactory.decodeStream(input)
                                    input?.close()
                                    if (bmp != null) {
                                        when (selectedFormat) {
                                            "PDF" -> createPdfFromBitmap(bmp)
                                            "PNG" -> {
                                                val stream = ByteArrayOutputStream()
                                                bmp.compress(Bitmap.CompressFormat.PNG, 100, stream)
                                                stream.toByteArray()
                                            }

                                            else -> {
                                                val stream = ByteArrayOutputStream()
                                                bmp.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                                                stream.toByteArray()
                                            }
                                        }
                                    } else {
                                        context.contentResolver.openInputStream(initialUri)
                                            ?.use { it.readBytes() } ?: ByteArray(0)
                                    }
                                }

                                else -> ByteArray(0)
                            }

                            if (bytes.isNotEmpty()) {
                                onConfirm(finalFileName, mime, bytes)
                            }
                        },
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Zapisz", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun createPdfFromBitmap(bitmap: Bitmap): ByteArray {
    val document = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = document.startPage(pageInfo)
    val canvas = page.canvas

    val margin = 24f
    val availWidth = 595f - (2 * margin)
    val availHeight = 842f - (2 * margin)

    val scale = (availWidth / bitmap.width).coerceAtMost(availHeight / bitmap.height)
    val scaledWidth = bitmap.width * scale
    val scaledHeight = bitmap.height * scale
    val left = margin + (availWidth - scaledWidth) / 2f
    val top = margin + (availHeight - scaledHeight) / 2f

    val paint = Paint().apply { isFilterBitmap = true }
    val destRect = RectF(left, top, left + scaledWidth, top + scaledHeight)
    canvas.drawBitmap(bitmap, null, destRect, paint)
    document.finishPage(page)

    val stream = ByteArrayOutputStream()
    document.writeTo(stream)
    document.close()
    return stream.toByteArray()
}

private fun queryFileInfo(context: Context, uri: Uri): Pair<String, Long> {
    var name = ""
    var size = 0L
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) name = cursor.getString(nameIndex) ?: ""
                if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
            }
        }
    } catch (_: Exception) {
    }
    return Pair(name, size)
}
