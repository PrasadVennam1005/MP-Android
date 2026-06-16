package prasad.vennam.moneypilot.ui.loans.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import prasad.vennam.moneypilot.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanTypeSelector(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val options = listOf(
        Triple(stringResource(R.string.home_loan), Icons.Rounded.Home, "Home"),
        Triple(stringResource(R.string.car_loan), Icons.Rounded.DirectionsCar, "Car"),
        Triple(stringResource(R.string.personal_loan), Icons.Rounded.Person, "Personal"),
        Triple(stringResource(R.string.other_loan), Icons.Rounded.Payments, "Other")
    )

    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth()
    ) {
        options.forEachIndexed { index, (label, icon, desc) ->
            SegmentedButton(
                selected = selectedTabIndex == index,
                onClick = { onTabSelected(index) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = desc,
                        modifier = Modifier.size(18.dp)
                    )
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            )
        }
    }
}
