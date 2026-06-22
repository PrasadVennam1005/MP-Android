package prasad.vennam.moneypilot.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconName: String,
    val color: Long,
    val isExpense: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis(),
) {
    companion object {
        val DEFAULT_CATEGORIES =
            listOf(
                // Expenses
                Category(name = "Food", iconName = "restaurant", color = 0xFFF44336, isExpense = true),
                Category(name = "Transport", iconName = "directions_car", color = 0xFF2196F3, isExpense = true),
                Category(name = "Shopping", iconName = "shopping_cart", color = 0xFFE91E63, isExpense = true),
                Category(name = "Entertainment", iconName = "movie", color = 0xFFFF9800, isExpense = true),
                Category(name = "Health", iconName = "medical_services", color = 0xFF4CAF50, isExpense = true),
                Category(name = "Utilities", iconName = "lightbulb", color = 0xFF00BCD4, isExpense = true),
                Category(name = "Housing", iconName = "home", color = 0xFF795548, isExpense = true),
                Category(name = "Education", iconName = "school", color = 0xFF3F51B5, isExpense = true),
                Category(name = "Gifts", iconName = "card_giftcard", color = 0xFF9C27B0, isExpense = true),
                Category(name = "Travel", iconName = "flight", color = 0xFF009688, isExpense = true),
                Category(name = "Insurance", iconName = "security", color = 0xFF607D8B, isExpense = true),
                Category(name = "Bills", iconName = "receipt", color = 0xFF8BC34A, isExpense = true),
                // Income
                Category(name = "Salary", iconName = "payments", color = 0xFF4CAF50, isExpense = false),
                Category(name = "Freelance", iconName = "work", color = 0xFF8BC34A, isExpense = false),
                Category(name = "Investments", iconName = "trending_up", color = 0xFF00BCD4, isExpense = false),
                Category(name = "Rental", iconName = "apartment", color = 0xFFFFC107, isExpense = false),
                Category(name = "Gifts", iconName = "redeem", color = 0xFFE91E63, isExpense = false),
                Category(name = "Refund", iconName = "history", color = 0xFF607D8B, isExpense = false),
            )
    }
}
