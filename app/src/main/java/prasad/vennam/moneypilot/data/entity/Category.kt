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
                Category(id = 1, name = "Food", iconName = "restaurant", color = 0xFFF44336, isExpense = true),
                Category(id = 2, name = "Transport", iconName = "directions_car", color = 0xFF2196F3, isExpense = true),
                Category(id = 3, name = "Shopping", iconName = "shopping_cart", color = 0xFFE91E63, isExpense = true),
                Category(id = 4, name = "Entertainment", iconName = "movie", color = 0xFFFF9800, isExpense = true),
                Category(id = 5, name = "Health", iconName = "medical_services", color = 0xFF4CAF50, isExpense = true),
                Category(id = 6, name = "Utilities", iconName = "lightbulb", color = 0xFF00BCD4, isExpense = true),
                Category(id = 7, name = "Housing", iconName = "home", color = 0xFF795548, isExpense = true),
                Category(id = 8, name = "Education", iconName = "school", color = 0xFF3F51B5, isExpense = true),
                Category(id = 9, name = "Gifts", iconName = "card_giftcard", color = 0xFF9C27B0, isExpense = true),
                Category(id = 10, name = "Travel", iconName = "flight", color = 0xFF009688, isExpense = true),
                Category(id = 11, name = "Insurance", iconName = "security", color = 0xFF607D8B, isExpense = true),
                Category(id = 12, name = "Bills", iconName = "receipt", color = 0xFF8BC34A, isExpense = true),
                Category(id = 13, name = "Subscription", iconName = "subscriptions", color = 0xFF673AB7, isExpense = true),
                Category(id = 14, name = "Pets", iconName = "pets", color = 0xFF795548, isExpense = true),
                Category(id = 15, name = "Personal Care", iconName = "face", color = 0xFFE91E63, isExpense = true),
                Category(id = 16, name = "Groceries", iconName = "local_grocery_store", color = 0xFF009688, isExpense = true),
                Category(id = 17, name = "Dining", iconName = "local_dining", color = 0xFFFF5722, isExpense = true),
                Category(id = 18, name = "Fitness", iconName = "fitness_center", color = 0xFF3F51B5, isExpense = true),
                Category(id = 19, name = "Automotive", iconName = "directions_car", color = 0xFF607D8B, isExpense = true),
                Category(id = 20, name = "Apparel", iconName = "checkroom", color = 0xFFE91E63, isExpense = true),
                Category(id = 21, name = "Charity", iconName = "volunteer_activism", color = 0xFF9C27B0, isExpense = true),
                Category(id = 22, name = "Taxes", iconName = "account_balance", color = 0xFFF44336, isExpense = true),
                // Income
                Category(id = 23, name = "Salary", iconName = "payments", color = 0xFF4CAF50, isExpense = false),
                Category(id = 24, name = "Freelance", iconName = "work", color = 0xFF8BC34A, isExpense = false),
                Category(id = 25, name = "Investments", iconName = "trending_up", color = 0xFF00BCD4, isExpense = false),
                Category(id = 26, name = "Rental", iconName = "apartment", color = 0xFFFFC107, isExpense = false),
                Category(id = 27, name = "Gifts", iconName = "redeem", color = 0xFFE91E63, isExpense = false),
                Category(id = 28, name = "Refund", iconName = "history", color = 0xFF607D8B, isExpense = false),
                Category(id = 29, name = "Business", iconName = "storefront", color = 0xFF3F51B5, isExpense = false),
                Category(id = 30, name = "Dividend", iconName = "show_chart", color = 0xFF9C27B0, isExpense = false),
                Category(id = 31, name = "Bonus", iconName = "card_giftcard", color = 0xFFFF9800, isExpense = false),
                Category(id = 32, name = "Interest", iconName = "account_balance", color = 0xFF009688, isExpense = false),
                Category(id = 33, name = "Sale", iconName = "point_of_sale", color = 0xFFF44336, isExpense = false),
                Category(id = 34, name = "Other Income", iconName = "attach_money", color = 0xFF607D8B, isExpense = false)
            )
    }
}
