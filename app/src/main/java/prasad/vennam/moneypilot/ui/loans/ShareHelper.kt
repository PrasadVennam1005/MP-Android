package prasad.vennam.moneypilot.ui.loans

import android.content.Context
import android.content.Intent

object ShareHelper {

    fun shareText(
        context: Context,
        subject: String,
        text: String,
    ) {
        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, text)
            }

        context.startActivity(
            Intent.createChooser(
                intent,
                "Share Loan Report"
            )
        )
    }
}