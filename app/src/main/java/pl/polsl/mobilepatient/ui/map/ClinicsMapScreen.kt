package pl.polsl.mobilepatient.ui.map

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.polsl.mobilepatient.data.dto.ClinicDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicsMapScreen(
    onBack: () -> Unit,
    viewModel: ClinicsMapViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Placówki medyczne",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Lokalizacje, kontakt i dojazd",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Powrót"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.loadClinics() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Odśwież"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                PrimaryTabRow(
                    selectedTabIndex = uiState.viewMode.ordinal,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = uiState.viewMode == MapViewMode.MAP,
                        onClick = { viewModel.setViewMode(MapViewMode.MAP) },
                        text = { Text("Mapa") },
                        icon = {
                            Icon(
                                Icons.Default.Map,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    Tab(
                        selected = uiState.viewMode == MapViewMode.LIST,
                        onClick = { viewModel.setViewMode(MapViewMode.LIST) },
                        text = { Text("Lista placówek") },
                        icon = {
                            Icon(
                                Icons.AutoMirrored.Filled.ViewList,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.viewMode) {
                MapViewMode.LIST -> {
                    ClinicsListView(
                        clinics = uiState.clinics,
                        selectedClinic = uiState.selectedClinic,
                        onSelectClinic = {
                            viewModel.selectClinic(it)
                            viewModel.setViewMode(MapViewMode.MAP)
                        },
                        onCall = { openDialer(context, it) },
                        onNavigate = { openNavigation(context, it) }
                    )
                }

                MapViewMode.MAP -> {
                    OsmMapView(
                        clinic = uiState.selectedClinic ?: uiState.clinics.firstOrNull(),
                        modifier = Modifier.fillMaxSize()
                    )

                    FloatingActionButton(
                        onClick = {
                            uiState.clinics.firstOrNull()?.let { viewModel.selectClinic(it) }
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = Icons.Default.NearMe,
                            contentDescription = "Wycentruj na placówce"
                        )
                    }

                    ClinicsMapBottomOverlay(
                        clinics = uiState.clinics,
                        selectedClinic = uiState.selectedClinic,
                        onSelectClinic = { viewModel.selectClinic(it) },
                        onClearSelected = { viewModel.clearSelectedClinic() },
                        onCall = { openDialer(context, it) },
                        onNavigate = { openNavigation(context, it) },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            uiState.errorMessage?.let { msg ->
                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = { viewModel.loadClinics() },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Ponów")
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun OsmMapView(
    clinic: ClinicDto?,
    modifier: Modifier = Modifier
) {
    val lat = clinic?.latitude ?: 50.2885
    val lng = clinic?.longitude ?: 18.6775

    val delta = 0.007
    val minLng = lng - delta
    val minLat = lat - delta
    val maxLng = lng + delta
    val maxLat = lat + delta

    val embedUrl = remember(lat, lng) {
        "https://www.openstreetmap.org/export/embed.html?bbox=$minLng%2C$minLat%2C$maxLng%2C$maxLat&layer=mapnik&marker=$lat%2C$lng"
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    useWideViewPort = true
                    loadWithOverviewMode = true
                }
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                loadUrl(embedUrl)
            }
        },
        update = { webView ->
            if (webView.url != embedUrl) {
                webView.loadUrl(embedUrl)
            }
        }
    )
}

@Composable
private fun ClinicsMapBottomOverlay(
    clinics: List<ClinicDto>,
    selectedClinic: ClinicDto?,
    onSelectClinic: (ClinicDto) -> Unit,
    onClearSelected: () -> Unit,
    onCall: (String?) -> Unit,
    onNavigate: (ClinicDto) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (clinics.size > 1) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(clinics, key = { it.id }) { clinicItem ->
                    val isSelected = selectedClinic?.id == clinicItem.id
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        shadowElevation = 3.dp,
                        modifier = Modifier.clickable {
                            onSelectClinic(clinicItem)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalHospital,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = clinicItem.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = selectedClinic != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            selectedClinic?.let { clinic ->
                SelectedClinicCard(
                    clinic = clinic,
                    onClose = onClearSelected,
                    onCall = { onCall(clinic.phone) },
                    onNavigate = { onNavigate(clinic) }
                )
            }
        }
    }
}

@Composable
private fun SelectedClinicCard(
    clinic: ClinicDto,
    onClose: () -> Unit,
    onCall: () -> Unit,
    onNavigate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalHospital,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = clinic.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = clinic.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Zamknij",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!clinic.phone.isNullOrBlank()) {
                    FilledTonalButton(
                        onClick = onCall,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Zadzwoń")
                    }
                }

                Button(
                    onClick = onNavigate,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Directions,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Nawiguj")
                }
            }
        }
    }
}

@Composable
private fun ClinicsListView(
    clinics: List<ClinicDto>,
    selectedClinic: ClinicDto?,
    onSelectClinic: (ClinicDto) -> Unit,
    onCall: (String?) -> Unit,
    onNavigate: (ClinicDto) -> Unit
) {
    if (clinics.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Brak dostępnych placówek medycznych",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(clinics, key = { it.id }) { clinic ->
                val isSelected = selectedClinic?.id == clinic.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectClinic(clinic) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(
                            alpha = 0.3f
                        )
                        else MaterialTheme.colorScheme.surface
                    ),
                    border = if (isSelected) BorderStroke(
                        1.5.dp,
                        MaterialTheme.colorScheme.primary
                    ) else null,
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalHospital,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = clinic.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = clinic.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (!clinic.phone.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = clinic.phone,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!clinic.phone.isNullOrBlank()) {
                                OutlinedButton(
                                    onClick = { onCall(clinic.phone) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Zadzwoń")
                                }
                            }

                            FilledTonalButton(
                                onClick = { onNavigate(clinic) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Navigation,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pokaż / Nawiguj")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun openDialer(context: Context, phone: String?) {
    if (phone.isNullOrBlank()) {
        Toast.makeText(context, "Brak numeru telefonu dla tej placówki", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val intent = Intent(Intent.ACTION_DIAL, "tel:${phone.replace(" ", "")}".toUri())
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Nie można uruchomić aplikacji telefonu", Toast.LENGTH_SHORT).show()
    }
}

private fun openNavigation(context: Context, clinic: ClinicDto) {
    try {
        val encodedName = Uri.encode(clinic.name)
        val uri =
            "geo:${clinic.latitude},${clinic.longitude}?q=${clinic.latitude},${clinic.longitude}($encodedName)".toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Nie można uruchomić nawigacji", Toast.LENGTH_SHORT).show()
    }
}
