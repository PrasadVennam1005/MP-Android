package prasad.vennam.moneypilot.util

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import prasad.vennam.moneypilot.data.entity.Category
import prasad.vennam.moneypilot.data.entity.Transaction
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportHelper {
    fun exportToCsv(
        transactions: List<Transaction>,
        categories: List<Category>,
        outputStream: OutputStream,
        currencyCode: String,
    ) {
        val writer = outputStream.bufferedWriter()
        writer.write("Date,Type,Category,Payment Mode,Amount,Note\n")

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        for (transaction in transactions) {
            val dateStr = dateFormat.format(Date(transaction.timestamp))
            val categoryName = categories.find { it.id == transaction.categoryId }?.name ?: "Other"

            // Escape notes containing double quotes, commas or newlines
            val escapedNote = transaction.note.replace("\"", "\"\"")
            val formattedNote =
                if (escapedNote.contains(",") || escapedNote.contains("\n") || escapedNote.contains("\"")) {
                    "\"$escapedNote\""
                } else {
                    escapedNote
                }

            writer.write(
                "$dateStr,${transaction.type.name},$categoryName,${transaction.paymentMode},${transaction.amount.inRupees},$formattedNote\n",
            )
        }
        writer.flush()
    }

    fun exportToPdf(
        transactions: List<Transaction>,
        categories: List<Category>,
        outputStream: OutputStream,
        currencyCode: String,
    ) {
        val document = PdfDocument()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        var pageNum = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
        var page = document.startPage(pageInfo)
        var canvas: Canvas = page.canvas

        val textPaint =
            Paint().apply {
                color = Color.DKGRAY
                textSize = 10f
                isAntiAlias = true
            }

        val headerPaint =
            Paint().apply {
                color = Color.BLACK
                textSize = 11f
                isFakeBoldText = true
                isAntiAlias = true
            }

        val titlePaint =
            Paint().apply {
                color = Color.rgb(63, 81, 181) // Deep Blue primary color
                textSize = 18f
                isFakeBoldText = true
                isAntiAlias = true
            }

        // Draw Cover / Page Title
        canvas.drawText("MoneyPilot Transaction Report", 36f, 50f, titlePaint)
        canvas.drawText("Generated on: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}", 36f, 70f, textPaint)

        // Draw Line under title
        val linePaint =
            Paint().apply {
                color = Color.LTGRAY
                strokeWidth = 1f
                isAntiAlias = true
            }
        canvas.drawLine(36f, 85f, 559f, 85f, linePaint)

        // Draw Table Header
        var y = 110f
        canvas.drawText("Date", 36f, y, headerPaint)
        canvas.drawText("Type", 120f, y, headerPaint)
        canvas.drawText("Category", 180f, y, headerPaint)
        canvas.drawText("Payment", 280f, y, headerPaint)
        canvas.drawText("Amount", 370f, y, headerPaint)
        canvas.drawText("Note", 450f, y, headerPaint)

        canvas.drawLine(36f, y + 6f, 559f, y + 6f, linePaint)
        y += 24f

        for (transaction in transactions) {
            // Check if we need to paginate to a new page
            if (y > 780f) {
                // Draw footer with page number
                canvas.drawText("Page $pageNum", 280f, 815f, textPaint)
                document.finishPage(page)

                pageNum++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas

                // Re-draw small header on subsequent pages
                canvas.drawText("MoneyPilot Transaction Report - Continued", 36f, 40f, headerPaint)
                canvas.drawLine(36f, 48f, 559f, 48f, linePaint)
                y = 70f
            }

            val dateStr = dateFormat.format(Date(transaction.timestamp))
            val categoryName = categories.find { it.id == transaction.categoryId }?.name ?: "Other"
            val amountStr = CurrencyFormatter.format(transaction.amount.inRupees, currencyCode)

            // Adjust amount color based on transaction type (Green for Income, Red for Expense)
            val amountPaint =
                Paint(textPaint).apply {
                    color =
                        if (transaction.type.name == "INCOME") {
                            Color.rgb(46, 125, 50) // Premium Green
                        } else {
                            Color.rgb(198, 40, 40) // Premium Red
                        }
                    isFakeBoldText = true
                }

            canvas.drawText(dateStr, 36f, y, textPaint)
            canvas.drawText(transaction.type.name, 120f, y, textPaint)
            canvas.drawText(categoryName, 180f, y, textPaint)
            canvas.drawText(transaction.paymentMode, 280f, y, textPaint)
            canvas.drawText(amountStr, 370f, y, amountPaint)

            // Truncate note if too long
            val maxNoteWidth = 100f
            val noteText =
                textPaint.let {
                    var temp = transaction.note
                    if (it.measureText(temp) > maxNoteWidth) {
                        while (temp.isNotEmpty() && it.measureText("$temp...") > maxNoteWidth) {
                            temp = temp.dropLast(1)
                        }
                        "$temp..."
                    } else {
                        temp
                    }
                }
            canvas.drawText(noteText, 450f, y, textPaint)

            y += 20f
        }

        // Draw footer on last page
        canvas.drawText("Page $pageNum", 280f, 815f, textPaint)
        document.finishPage(page)

        document.writeTo(outputStream)
        document.close()
    }
}
