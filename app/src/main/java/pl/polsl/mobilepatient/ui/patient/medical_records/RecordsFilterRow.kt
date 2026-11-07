package pl.polsl.mobilepatient.ui.patient.medical_records

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
fun RecordsFilterRow(
    selectedFilter: String,
    onSelectFilter: (String) -> Unit,
    allCount: Int,
    visitsCount: Int,
    testsCount: Int,
    referralsCount: Int,
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
                selected = selectedFilter == "visit",
                onClick = { onSelectFilter("visit") },
                label = { Text("Wizyty ($visitsCount)") },
                shape = RoundedCornerShape(10.dp),
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == "test_result",
                onClick = { onSelectFilter("test_result") },
                label = { Text("Badania ($testsCount)") },
                shape = RoundedCornerShape(10.dp),
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == "referral",
                onClick = { onSelectFilter("referral") },
                label = { Text("Skierowania ($referralsCount)") },
                shape = RoundedCornerShape(10.dp),
            )
        }
    }
}
