package prasad.vennam.moneypilot.ui.budget.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.vector.ImageVector
import prasad.vennam.moneypilot.data.entity.Budget
import prasad.vennam.moneypilot.data.entity.Category

data class BudgetItemState(
    val budget: Budget,
    val category: Category?,
    val spent: Double
)

fun getCategoryIcon(name: String?): ImageVector {
    return when (name) {
        "restaurant" -> Icons.Rounded.Restaurant
        "directions_car" -> Icons.Rounded.DirectionsCar
        "shopping_cart" -> Icons.Rounded.ShoppingCart
        "movie" -> Icons.Rounded.Movie
        "medical_services" -> Icons.Rounded.MedicalServices
        "lightbulb" -> Icons.Rounded.Lightbulb
        "home" -> Icons.Rounded.Home
        "school" -> Icons.Rounded.School
        "card_giftcard" -> Icons.Rounded.CardGiftcard
        "flight" -> Icons.Rounded.Flight
        "security" -> Icons.Rounded.Security
        "receipt" -> Icons.Rounded.Receipt
        "payments" -> Icons.Rounded.Payments
        "work" -> Icons.Rounded.Work
        "trending_up" -> Icons.AutoMirrored.Rounded.TrendingUp
        "apartment" -> Icons.Rounded.Apartment
        "redeem" -> Icons.Rounded.Redeem
        "history" -> Icons.Rounded.History
        else -> Icons.Rounded.Category
    }
}
