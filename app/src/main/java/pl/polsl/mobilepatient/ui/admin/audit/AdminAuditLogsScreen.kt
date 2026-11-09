package pl.polsl.mobilepatient.ui.admin.audit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.polsl.mobilepatient.data.dto.AuditLogWithActor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAuditLogsScreen(
    onBack: () -> Unit,
    viewModel: AdminAuditLogsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedLogForDetails by remember { mutableStateOf<AuditLogWithActor?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    if (selectedLogForDetails != null) {
        AuditLogDetailDialog(
            log = selectedLogForDetails!!,
            onDismiss = { selectedLogForDetails = null }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Dziennik audytu",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Niezaprzeczalność operacji w systemie",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Wróć"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadAuditLogs() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Odśwież"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Szukaj akcji, aktora, tabeli, UUID...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Wyczyść")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AuditFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = uiState.selectedFilter == filter,
                        onClick = { viewModel.selectFilter(filter) },
                        label = { Text(filter.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(12.dp),
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Box(
//                        modifier = Modifier
//                            .size(36.dp)
//                            .background(
//                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
//                                shape = CircleShape
//                            ),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        Icon(
//                            imageVector = Icons.Default.Security,
//                            contentDescription = null,
//                            tint = MaterialTheme.colorScheme.primary,
//                            modifier = Modifier.size(18.dp)
//                        )
//                    }

//                    Spacer(modifier = Modifier.width(10.dp))

//                    Column(modifier = Modifier.weight(1f)) {
//                        Text(
//                            text = "Wpisy w dzienniku: ${uiState.filteredLogs.size}",
//                            style = MaterialTheme.typography.labelLarge,
//                            fontWeight = FontWeight.Bold,
//                            color = MaterialTheme.colorScheme.onSurface
//                        )
//                        Text(
//                            text = "Wymóg T11: rejestracja autora, zasobu i czasu",
//                            style = MaterialTheme.typography.bodySmall,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant
//                        )
//                    }
//
//                    Button(
//                        onClick = { viewModel.generateTestAuditEvent() },
//                        enabled = !uiState.isGeneratingTestEvent,
//                        shape = RoundedCornerShape(10.dp),
//                        colors = ButtonDefaults.buttonColors(
//                            containerColor = MaterialTheme.colorScheme.primary
//                        )
//                    ) {
//                        if (uiState.isGeneratingTestEvent) {
//                            CircularProgressIndicator(
//                                modifier = Modifier.size(16.dp),
//                                color = MaterialTheme.colorScheme.onPrimary,
//                                strokeWidth = 2.dp
//                            )
//                        } else {
//                            Icon(
//                                imageVector = Icons.Default.AddAlert,
//                                contentDescription = null,
//                                modifier = Modifier.size(16.dp)
//                            )
//                            Spacer(modifier = Modifier.width(6.dp))
//                            Text(
//                                text = "Test T11",
//                                style = MaterialTheme.typography.labelMedium
//                            )
//                        }
//                    }
                //}
            }

            if (uiState.isLoading && uiState.logs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (uiState.filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Text(
                            text = if (uiState.searchQuery.isBlank()) "Brak logów w tej grupie" else "Brak wyników wyszukiwania",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Zmień kryteria wyszukiwania lub wybierz inną zakładkę logów.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = uiState.filteredLogs,
                        key = { it.id }
                    ) { logItem ->
                        AuditLogItemCard(
                            log = logItem,
                            onClick = { selectedLogForDetails = logItem }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditLogItemCard(
    log: AuditLogWithActor,
    onClick: () -> Unit
) {
    val actionColors = getActionColors(log.action)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = actionColors.first
                ) {
                    Text(
                        text = log.action,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = actionColors.second,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Text(
                    text = formatAuditDate(log.created_at),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = log.actorName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!log.actorRole.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = log.actorRole.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Zasób: ${log.entity_type}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (log.entity_id != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${log.entity_id.take(8)}...)",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

private fun formatAuditDate(isoDate: String): String {
    return try {
        val cleaned = isoDate.replace('T', ' ').substringBefore('.')
        if (cleaned.length >= 16) cleaned.substring(0, 16) else cleaned
    } catch (_: Exception) {
        isoDate
    }
}

@Composable
private fun getActionColors(action: String): Pair<Color, Color> {
    return when {
        action.startsWith("CREATE") || action.startsWith("ADD") -> {
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
        }

        action.startsWith("DELETE") || action.startsWith("CANCEL") -> {
            MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.error
        }

        action.startsWith("UPDATE") -> {
            MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.secondary
        }

        action.contains("TEST") -> {
            MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        }

        else -> {
            MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        }
    }
}
