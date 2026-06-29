package prasad.vennam.moneypilot.data.model

import com.squareup.moshi.Json

data class FrankfurterRateResponseItem(
    val date: String,
    val base: String,
    val quote: String,
    val rate: Double,
)

data class FrankfurterCurrencyItem(
    @Json(name = "iso_code") val isoCode: String,
    val name: String,
    val symbol: String? = null,
)

data class RateAlert(
    val from: String,
    val to: String,
    val targetRate: Double,
    val isAbove: Boolean,
)
