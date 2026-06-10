package prasad.vennam.moneypilot.data

import androidx.room.TypeConverter
import prasad.vennam.moneypilot.data.entity.TransactionType

class Converters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)
}
