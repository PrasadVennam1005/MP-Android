package prasad.vennam.moneypilot.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FrankfurterRateResponseItem(
    val date: String,
    val base: String,
    val quote: String,
    val rate: Double,
)

@JsonClass(generateAdapter = true)
data class FrankfurterCurrencyItem(
    @Json(name = "iso_code") val isoCode: String,
    val name: String,
    val symbol: String? = null,
)

@JsonClass(generateAdapter = true)
data class RateAlert(
    val from: String,
    val to: String,
    val targetRate: Double,
    val isAbove: Boolean,
)
