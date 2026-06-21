package prasad.vennam.moneypilot

import android.util.Log
import com.google.mlkit.vision.text.Text
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import prasad.vennam.moneypilot.data.entity.TransactionType
import prasad.vennam.moneypilot.feature.ai.domain.AiActionParser
import prasad.vennam.moneypilot.feature.ai.model.AiAction
import prasad.vennam.moneypilot.util.ReceiptParser
import org.mockito.Mockito.`when` as whenever
import kotlinx.coroutines.runBlocking
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.domain.usecase.GetTransactionByIdUseCase
import prasad.vennam.moneypilot.domain.usecase.SaveTransactionUseCase
import prasad.vennam.moneypilot.domain.usecase.DeleteTransactionUseCase
import prasad.vennam.moneypilot.domain.usecase.GetTransactionsUseCase
import prasad.vennam.moneypilot.domain.usecase.GetCategoriesUseCase
import prasad.vennam.moneypilot.domain.usecase.SaveCategoryUseCase
import prasad.vennam.moneypilot.domain.usecase.DeleteCategoryUseCase
import prasad.vennam.moneypilot.domain.usecase.RestoreBackupUseCase
import prasad.vennam.moneypilot.feature.ai.domain.AiRepository
import prasad.vennam.moneypilot.ui.viewmodel.TransactionViewModel
import prasad.vennam.moneypilot.util.ParsedReceipt

class ParserTests {



    private fun createMockVisionText(textLines: List<String>): Text {
        val mockText = mock(Text::class.java)
        val mockBlock = mock(Text.TextBlock::class.java)
        val lineMocks = textLines.map { lineText ->
            val mockLine = mock(Text.Line::class.java)
            whenever(mockLine.text).thenReturn(lineText)
            mockLine
        }
        whenever(mockBlock.lines).thenReturn(lineMocks)
        whenever(mockText.textBlocks).thenReturn(listOf(mockBlock))
        return mockText
    }

    // ── RECEIPT PARSER TESTS ─────────────────────────────────────────────────

    @Test
    fun testReceiptParser_extractsValidReceiptDetails() {
        val ocrLines = listOf(
            "Welcome to Starbucks",
            "Bill No: 98765",
            "Date: 20/06/2026",
            "Cappuccino  Rs 220.00",
            "Subtotal: Rs 220.00",
            "Grand Total: 220.00",
            "Thank you!"
        )
        val mockText = createMockVisionText(ocrLines)
        val parsed = ReceiptParser.parse(mockText)

        assertEquals("Starbucks", parsed.merchant)
        assertEquals(220.0, parsed.amount ?: 0.0, 0.0)
        assertNotNull(parsed.date)
    }

    @Test
    fun testReceiptParser_returnsNullAmount_whenNoDigitsFound() {
        val ocrLines = listOf(
            "Starbucks Coffee Shop",
            "No visible items here",
            "Temporary Closed"
        )
        val mockText = createMockVisionText(ocrLines)
        val parsed = ReceiptParser.parse(mockText)

        assertNull(parsed.amount)
        assertEquals("Starbucks Coffee Shop", parsed.merchant)
    }

    @Test
    fun testReceiptParser_scoresCorrectTotal_whenMultipleAmountsExist() {
        val ocrLines = listOf(
            "Dominos Pizza",
            "Paneer Pizza Rs 399",
            "Taxes: Rs 18",
            "Total Amount: Rs 417"
        )
        val mockText = createMockVisionText(ocrLines)
        val parsed = ReceiptParser.parse(mockText)

        // It should favor "Total Amount" (417) over item price (399) or tax (18) due to scoring heuristics
        assertEquals(417.0, parsed.amount ?: 0.0, 0.0)
    }

    @Test
    fun testReceiptParser_filtersOutPhoneAndInvoiceNumbersInFallback() {
        val ocrLines = listOf(
            "KFC Store",
            "Phone: 9876543210",
            "Invoice No: 20260620",
            "GSTIN: 29AAAAA1111A1Z1",
            "Items: Burger Rs 150",
            "Large Fries Rs 120"
        )
        val mockText = createMockVisionText(ocrLines)
        val parsed = ReceiptParser.parse(mockText)

        // The fallback logic should filter out lines containing metadata, avoiding phone/invoice numbers.
        // It should correctly extract 150.0 (the max amount from clean lines).
        assertEquals(150.0, parsed.amount ?: 0.0, 0.0)
    }

    @Test
    fun testReceiptParser_stripsWelcomePrefixFromMerchant() {
        val ocrLines = listOf(
            "Welcome to Starbucks Coffee",
            "Total: Rs 200"
        )
        val mockText = createMockVisionText(ocrLines)
        val parsed = ReceiptParser.parse(mockText)

        assertEquals("Starbucks Coffee", parsed.merchant)
    }

    @Test
    fun testReceiptParser_ignoresUrlsAndClosingPhrases() {
        val ocrLines = listOf(
            "www.starbucks.in",
            "Starbucks Coffee",
            "Total: Rs 200",
            "Thank you for visiting!"
        )
        val mockText = createMockVisionText(ocrLines)
        val parsed = ReceiptParser.parse(mockText)

        // www.starbucks.in should be ignored as a merchant name, so "Starbucks Coffee" is matched.
        // "Thank you for visiting!" contains closing phrase, so it is ignored.
        assertEquals("Starbucks Coffee", parsed.merchant)
    }

    // ── AI ACTION PARSER TESTS ────────────────────────────────────────────────

    @Test
    fun testAiActionParser_parsesAddExpenseAction() {
        val rawResponse = "Sure, I can log that for you. [ACTION:ADD_EXPENSE|amount=500|category=Food|note=Swiggy|date=today]"
        val (action, cleanedText) = AiActionParser.parse(rawResponse)

        assertEquals("Sure, I can log that for you.", cleanedText)
        assertNotNull(action)
        val addExpense = action as AiAction.AddTransaction
        assertEquals(TransactionType.EXPENSE, addExpense.type)
        assertEquals(500L, addExpense.amount)
        assertEquals("Food", addExpense.categoryName)
        assertEquals("Swiggy", addExpense.note)
        assertEquals(0, addExpense.dateOffset)
    }

    @Test
    fun testAiActionParser_parsesAddIncomeAction() {
        val rawResponse = "[ACTION:ADD_INCOME|amount=2.5k|category=Salary|note=Internship|date=yesterday] Done!"
        val (action, cleanedText) = AiActionParser.parse(rawResponse)

        assertEquals("Done!", cleanedText)
        assertNotNull(action)
        val addIncome = action as AiAction.AddTransaction
        assertEquals(TransactionType.INCOME, addIncome.type)
        assertEquals(2500L, addIncome.amount) // 2.5k = 2500
        assertEquals("Salary", addIncome.categoryName)
        assertEquals("Internship", addIncome.note)
        assertEquals(-1, addIncome.dateOffset)
    }

    @Test
    fun testAiActionParser_parsesAddInvestmentAction() {
        val rawResponse = "Added. [ACTION:ADD_INVESTMENT|name=Nifty 50|type=Mutual Fund|amount=1.5L|current_value=1.6L]"
        val (action, cleanedText) = AiActionParser.parse(rawResponse)

        assertEquals("Added.", cleanedText)
        assertNotNull(action)
        val addInvestment = action as AiAction.AddInvestment
        assertEquals("Nifty 50", addInvestment.name)
        assertEquals("Mutual Fund", addInvestment.type)
        assertEquals(150000L, addInvestment.investedAmount) // 1.5L = 150000
        assertEquals(160000L, addInvestment.currentValue) // 1.6L = 160000
    }

    @Test
    fun testAiActionParser_parsesAddLoanAction() {
        val rawResponse = "[ACTION:ADD_LOAN|name=HDFC Home Loan|amount=50L|emi=45k|next_emi_days=15]"
        val (action, cleanedText) = AiActionParser.parse(rawResponse)

        assertEquals("", cleanedText)
        assertNotNull(action)
        val addLoan = action as AiAction.AddLoan
        assertEquals("HDFC Home Loan", addLoan.name)
        assertEquals(5000000L, addLoan.totalAmount)
        assertEquals(45000L, addLoan.emiAmount)
        assertEquals(15, addLoan.nextEmiDays)
    }

    @Test
    fun testAiActionParser_returnsNullAction_whenNoTagIsPresent() {
        val rawResponse = "Here are your recent expenses."
        val (action, cleanedText) = AiActionParser.parse(rawResponse)

        assertNull(action)
        assertEquals("Here are your recent expenses.", cleanedText)
    }

    @Test
    fun testAiRepository_parseReceiptText_success() {
        runBlocking {
            val mockContext = mock(android.content.Context::class.java)
            whenever(mockContext.applicationContext).thenReturn(mockContext)

            val mockLlmService = mock(prasad.vennam.moneypilot.feature.ai.service.LlmService::class.java)
            val mockRepository = mock(prasad.vennam.moneypilot.data.repository.MoneyPilotRepository::class.java)
            val mockWorkManager = mock(androidx.work.impl.WorkManagerImpl::class.java)

            // Mock the partial responses flow to prevent NPE in repository init block
            whenever(mockLlmService.partialResponses).thenReturn(
                kotlinx.coroutines.flow.MutableSharedFlow<prasad.vennam.moneypilot.feature.ai.model.LlmResponse>()
            )

            // Set the test delegate for WorkManager
            androidx.work.impl.WorkManagerImpl.setDelegate(mockWorkManager)

            whenever(mockWorkManager.getWorkInfosForUniqueWorkFlow("llm_model_download_work")).thenReturn(
                kotlinx.coroutines.flow.emptyFlow()
            )

            val tempDir = java.nio.file.Files.createTempDirectory("temp_model_dir").toFile()
            whenever(mockContext.getExternalFilesDir(null)).thenReturn(tempDir)
            whenever(mockContext.filesDir).thenReturn(tempDir)

            // Create dummy model files with non-zero size
            val modelFile1 = java.io.File(tempDir, "gemma3-1b-it-int4.litertlm")
            modelFile1.writeText("dummy_model_content")
            val modelFile2 = java.io.File(tempDir, "gemma-3n-E4B-it-int4.litertlm")
            modelFile2.writeText("dummy_model_content")

            val repository = prasad.vennam.moneypilot.feature.ai.data.AiRepositoryImpl(
                context = mockContext,
                llmService = mockLlmService,
                moneyPilotRepository = mockRepository
            )

            // Initialize to transition state to LlmState.Ready
            repository.initialize()

            // Set up the LLM response mock
            val ocrText = "Starbucks\nTotal: 250"
            val expectedPrompt = buildString {
                append("<start_of_turn>user\n")
                append("Analyze the following OCR text from a transaction receipt and extract:\n")
                append("1. The merchant name (e.g. Starbucks, Walmart, Swiggy).\n")
                append("2. The total transaction amount paid as a numeric value.\n")
                append("Format your response as an action tag with NO other text or explanation:\n")
                append("[ACTION:ADD_EXPENSE|amount=VALUE|category=Other|note=MERCHANT_NAME|date=today]\n\n")
                append("OCR Text:\n")
                append(ocrText)
                append("<end_of_turn>\n<start_of_turn>model\n")
            }

            whenever(mockLlmService.generateResponse(expectedPrompt)).thenReturn(
                "[ACTION:ADD_EXPENSE|amount=250|category=Other|note=Starbucks|date=today]"
            )

            val result = repository.parseReceiptText(ocrText)

            assertNotNull(result)
            assertEquals("Starbucks", result?.merchant)
            assertEquals(250.0, result?.amount ?: 0.0, 0.0)

            // Cleanup temp files & reset delegate
            androidx.work.impl.WorkManagerImpl.setDelegate(null)
            modelFile1.delete()
            modelFile2.delete()
            tempDir.delete()
        }
    }

    @Test
    fun testTransactionViewModel_parseReceiptText_decrementsQuotaForNonPremium() = runBlocking {
        val mockUserPreferences = mock(UserPreferences::class.java)
        val mockGetTransactionById = mock(GetTransactionByIdUseCase::class.java)
        val mockSaveTransaction = mock(SaveTransactionUseCase::class.java)
        val mockDeleteTransaction = mock(DeleteTransactionUseCase::class.java)
        val mockGetTransactions = mock(GetTransactionsUseCase::class.java)
        val mockGetCategories = mock(GetCategoriesUseCase::class.java)
        val mockSaveCategory = mock(SaveCategoryUseCase::class.java)
        val mockDeleteCategory = mock(DeleteCategoryUseCase::class.java)
        val mockRestoreBackup = mock(RestoreBackupUseCase::class.java)
        val mockAiRepository = mock(AiRepository::class.java)

        whenever(mockUserPreferences.isPremium).thenReturn(kotlinx.coroutines.flow.flowOf(false))
        whenever(mockUserPreferences.remainingAiScans).thenReturn(kotlinx.coroutines.flow.flowOf(3))
        whenever(mockGetTransactions()).thenReturn(kotlinx.coroutines.flow.flowOf(emptyList()))
        whenever(mockGetCategories()).thenReturn(kotlinx.coroutines.flow.flowOf(emptyList()))

        val viewModel = TransactionViewModel(
            userPreferences = mockUserPreferences,
            getTransactionByIdUseCase = mockGetTransactionById,
            saveTransactionUseCase = mockSaveTransaction,
            deleteTransactionUseCase = mockDeleteTransaction,
            getTransactionsUseCase = mockGetTransactions,
            getCategoriesUseCase = mockGetCategories,
            saveCategoryUseCase = mockSaveCategory,
            deleteCategoryUseCase = mockDeleteCategory,
            restoreBackupUseCase = mockRestoreBackup,
            aiRepository = mockAiRepository
        )

        val ocrText = "Receipt Text"
        val expectedResult = ParsedReceipt(merchant = "Test", amount = 100.0)
        whenever(mockAiRepository.parseReceiptText(ocrText)).thenReturn(expectedResult)

        val result = viewModel.parseReceiptText(ocrText)

        assertNotNull(result)
        assertEquals("Test", result?.merchant)
        assertEquals(100.0, result?.amount ?: 0.0, 0.0)

        // Verify decrement was called
        org.mockito.Mockito.verify(mockUserPreferences).decrementAiScans()
    }

    @Test
    fun testTransactionViewModel_parseReceiptText_doesNotDecrementQuotaForPremium() = runBlocking {
        val mockUserPreferences = mock(UserPreferences::class.java)
        val mockGetTransactionById = mock(GetTransactionByIdUseCase::class.java)
        val mockSaveTransaction = mock(SaveTransactionUseCase::class.java)
        val mockDeleteTransaction = mock(DeleteTransactionUseCase::class.java)
        val mockGetTransactions = mock(GetTransactionsUseCase::class.java)
        val mockGetCategories = mock(GetCategoriesUseCase::class.java)
        val mockSaveCategory = mock(SaveCategoryUseCase::class.java)
        val mockDeleteCategory = mock(DeleteCategoryUseCase::class.java)
        val mockRestoreBackup = mock(RestoreBackupUseCase::class.java)
        val mockAiRepository = mock(AiRepository::class.java)

        whenever(mockUserPreferences.isPremium).thenReturn(kotlinx.coroutines.flow.flowOf(true))
        whenever(mockUserPreferences.remainingAiScans).thenReturn(kotlinx.coroutines.flow.flowOf(3))
        whenever(mockGetTransactions()).thenReturn(kotlinx.coroutines.flow.flowOf(emptyList()))
        whenever(mockGetCategories()).thenReturn(kotlinx.coroutines.flow.flowOf(emptyList()))

        val viewModel = TransactionViewModel(
            userPreferences = mockUserPreferences,
            getTransactionByIdUseCase = mockGetTransactionById,
            saveTransactionUseCase = mockSaveTransaction,
            deleteTransactionUseCase = mockDeleteTransaction,
            getTransactionsUseCase = mockGetTransactions,
            getCategoriesUseCase = mockGetCategories,
            saveCategoryUseCase = mockSaveCategory,
            deleteCategoryUseCase = mockDeleteCategory,
            restoreBackupUseCase = mockRestoreBackup,
            aiRepository = mockAiRepository
        )

        val ocrText = "Receipt Text"
        val expectedResult = ParsedReceipt(merchant = "Test", amount = 100.0)
        whenever(mockAiRepository.parseReceiptText(ocrText)).thenReturn(expectedResult)

        val result = viewModel.parseReceiptText(ocrText)

        assertNotNull(result)
        assertEquals("Test", result?.merchant)
        assertEquals(100.0, result?.amount ?: 0.0, 0.0)

        // Verify decrement was NOT called
        org.mockito.Mockito.verify(mockUserPreferences, org.mockito.Mockito.never()).decrementAiScans()
    }
}
