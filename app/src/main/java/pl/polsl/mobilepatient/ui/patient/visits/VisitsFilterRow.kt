package pl.polsl.mobilepatient.ui.patient.visits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun VisitsFilterRow(
    selectedFilter: String,
    onSelectFilter: (String) -> Unit,
    allCount: Int,
    upcomingCount: Int,
    completedCount: Int,
    cancelledCount: Int,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item {
            FilterChip(
                selected = selectedFilter == "UPCOMING",
                onClick = { onSelectFilter("UPCOMING") },
                label = { Text("Nadchodzące ($upcomingCount)") },
                shape = RoundedCornerShape(10.dp),
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == "COMPLETED",
                onClick = { onSelectFilter("COMPLETED") },
                label = { Text("Zrealizowane ($completedCount)") },
                shape = RoundedCornerShape(10.dp),
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == "CANCELLED",
                onClick = { onSelectFilter("CANCELLED") },
                label = { Text("Odwołane ($cancelledCount)") },
                shape = RoundedCornerShape(10.dp),
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == "ALL",
                onClick = { onSelectFilter("ALL") },
                label = { Text("Wszystkie ($allCount)") },
                shape = RoundedCornerShape(10.dp),
            )
        }
    }
}
