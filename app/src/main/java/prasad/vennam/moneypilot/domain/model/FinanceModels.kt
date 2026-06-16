package prasad.vennam.moneypilot.domain.model

data class YahooPriceInfo(
    val price: Double,
    val currency: String,
)

data class SymbolResult(
    val symbol: String,
    val name: String,
    val exchange: String = "",
)
