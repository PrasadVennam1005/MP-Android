package prasad.vennam.moneypilot.util

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.IOException

object QuotesManager {
    data class Quote(
        val quote: String,
        val author: String
    )

    private var quotes: List<Quote> = emptyList()

    private fun loadQuotes(context: Context) {
        if (quotes.isNotEmpty()) return
        try {
            val jsonString = context.assets.open("quotes.json").bufferedReader().use { it.readText() }
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val type = Types.newParameterizedType(List::class.java, Quote::class.java)
            val adapter = moshi.adapter<List<Quote>>(type)
            quotes = adapter.fromJson(jsonString) ?: emptyList()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun getQuoteOfTheDay(context: Context): Quote? {
        loadQuotes(context)
        if (quotes.isEmpty()) return null
        val calendar = java.util.Calendar.getInstance()
        val dayOfYear = calendar.get(java.util.Calendar.DAY_OF_YEAR)
        return quotes[dayOfYear % quotes.size]
    }

    fun getRandomQuote(context: Context): Quote? {
        loadQuotes(context)
        if (quotes.isEmpty()) return null
        return quotes.random()
    }
}
