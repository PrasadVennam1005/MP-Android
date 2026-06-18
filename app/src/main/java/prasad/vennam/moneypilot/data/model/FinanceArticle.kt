package prasad.vennam.moneypilot.data.model

data class FinanceArticle(
    val id: String,
    val title: String,
    val category: String,
    val content: String,
    val description: String = ""
)
