package prasad.vennam.moneypilot.util

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

class SecureStorageHelper(
    context: Context,
) {
    private val masterKey =
        MasterKey
            .Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

    private val sharedPreferences =
        EncryptedSharedPreferences.create(
            context,
            "secure_money_pilot_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

    fun getOrGenerateDatabasePassphrase(): ByteArray {
        val key = "db_passphrase"
        val existingPassphrase = sharedPreferences.getString(key, null)
        return if (existingPassphrase != null) {
            Base64.decode(existingPassphrase, Base64.DEFAULT)
        } else {
            val newPassphrase = ByteArray(32)
            SecureRandom().nextBytes(newPassphrase)
            val encoded = Base64.encodeToString(newPassphrase, Base64.DEFAULT)
            sharedPreferences.edit().putString(key, encoded).apply()
            newPassphrase
        }
    }

    fun getOrGenerateDatabasePassphraseString(): String {
        val key = "db_passphrase"
        val existingPassphrase = sharedPreferences.getString(key, null)
        return if (existingPassphrase != null) {
            existingPassphrase
        } else {
            val newPassphrase = ByteArray(32)
            SecureRandom().nextBytes(newPassphrase)
            val encoded = Base64.encodeToString(newPassphrase, Base64.DEFAULT)
            sharedPreferences.edit().putString(key, encoded).apply()
            encoded
        }
    }
}
