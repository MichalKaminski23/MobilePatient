package pl.polsl.mobilepatient.ui.patient.prescriptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PrescriptionsFilterRow(
    selectedFilter: String,
    onSelectFilter: (String) -> Unit,
    allCount: Int,
    activeCount: Int,
    fulfilledCount: Int,
    expiredCount: Int,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item {
            FilterChip(
                selected = selectedFilter == "ALL",
                onClick = { onSelectFilter("ALL") },
                label = { Text("Wszystkie ($allCount)") },
                shape = RoundedCornerShape(10.dp),
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == "active",
                onClick = { onSelectFilter("active") },
                label = { Text("Aktywne ($activeCount)") },
                shape = RoundedCornerShape(10.dp),
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == "fulfilled",
                onClick = { onSelectFilter("fulfilled") },
                label = { Text("Zrealizowane ($fulfilledCount)") },
                shape = RoundedCornerShape(10.dp),
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == "expired",
                onClick = { onSelectFilter("expired") },
                label = { Text("Wygasłe ($expiredCount)") },
                shape = RoundedCornerShape(10.dp),
            )
        }
    }
}
