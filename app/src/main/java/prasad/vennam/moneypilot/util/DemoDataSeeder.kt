package prasad.vennam.moneypilot.util

import kotlinx.coroutines.flow.first
import prasad.vennam.moneypilot.data.entity.*
import prasad.vennam.moneypilot.data.repository.*
import java.util.Calendar

object DemoDataSeeder {
    suspend fun seed(
        transactionRepository: TransactionRepository,
        categoryRepository: CategoryRepository,
        budgetRepository: BudgetRepository,
        investmentRepository: InvestmentRepository,
        loanRepository: LoanRepository,
        goalRepository: GoalRepository,
        dataManagementRepository: DataManagementRepository,
    ) {
        val now = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L

        // 1. Clear all data first
        dataManagementRepository.clearAllData()

        // 2. Insert default categories
        val categories = Category.DEFAULT_CATEGORIES
        categories.forEach { categoryRepository.insertCategory(it) }

        // 3. Query all categories to map their auto-increment IDs
        val insertedCategories = categoryRepository.allCategories.first()

        val foodId = insertedCategories.find { it.name == "Food" && it.isExpense }?.id
        val salaryId = insertedCategories.find { it.name == "Salary" && !it.isExpense }?.id
        val freelanceId = insertedCategories.find { it.name == "Freelance" && !it.isExpense }?.id
        val rentalIncomeId = insertedCategories.find { it.name == "Rental" && !it.isExpense }?.id
        val investmentsIncomeId = insertedCategories.find { it.name == "Investments" && !it.isExpense }?.id
        val housingId = insertedCategories.find { it.name == "Housing" && it.isExpense }?.id
        val transportId = insertedCategories.find { it.name == "Transport" && it.isExpense }?.id
        val shoppingId = insertedCategories.find { it.name == "Shopping" && it.isExpense }?.id
        val utilitiesId = insertedCategories.find { it.name == "Utilities" && it.isExpense }?.id
        val entertainmentId = insertedCategories.find { it.name == "Entertainment" && it.isExpense }?.id
        val healthId = insertedCategories.find { it.name == "Health" && it.isExpense }?.id
        val insuranceId = insertedCategories.find { it.name == "Insurance" && it.isExpense }?.id
        val giftsExpenseId = insertedCategories.find { it.name == "Gifts" && it.isExpense }?.id
        val travelId = insertedCategories.find { it.name == "Travel" && it.isExpense }?.id

        // 4. Create and insert Transactions (approx 30 entries)
        val transactions =
            listOf(
                Transaction(
                    amount = 15000000L,
                    timestamp = now - 15 * oneDayMs,
                    categoryId = salaryId,
                    subCategory = "Monthly Pay",
                    paymentMode = "Bank Transfer",
                    note = "TechCorp Monthly Salary",
                    type = TransactionType.INCOME,
                    currencyCode = "INR",
                ),
                Transaction(
                    amount = 4500000L,
                    timestamp = now - 5 * oneDayMs,
                    categoryId = freelanceId,
                    subCategory = "UI Design",
                    paymentMode = "Bank Transfer",
                    note = "Freelance Mobile App Design",
                    type = TransactionType.INCOME,
                    currencyCode = "INR",
                ),
                Transaction(
                    amount = 2500000L,
                    timestamp = now - 15 * oneDayMs,
                    categoryId = housingId,
                    subCategory = "Rent",
                    paymentMode = "Bank Transfer",
                    note = "2BHK Bangalore Apartment Rent",
                    type = TransactionType.EXPENSE,
                    currencyCode = "INR",
                ),
                Transaction(
                    amount = 550000L,
                    timestamp = now - 8 * oneDayMs,
                    categoryId = foodId,
                    subCategory = "Groceries",
                    paymentMode = "Card",
                    note = "Weekly groceries from BigBasket",
                    type = TransactionType.EXPENSE,
                    currencyCode = "INR",
                ),
                Transaction(
                    amount = 1500000L, // ₹15,000.00
                    timestamp = now - 30 * oneDayMs, // May 2026
                    categoryId = shoppingId,
                    subCategory = "Shopping",
                    paymentMode = "Credit Card",
                    note = "Amazon Premium Shopping",
                    type = TransactionType.EXPENSE,
                    currencyCode = "INR",
                ),
            )
        transactions.forEach { transactionRepository.insertTransaction(it) }

        // 5. Create and insert Budgets (6 categories)
        val budgets = mutableListOf<Budget>()
        foodId?.let { budgets.add(Budget(categoryId = it, amount = 1500000L, period = "Monthly", currencyCode = "INR")) }
        transportId?.let { budgets.add(Budget(categoryId = it, amount = 600000L, period = "Monthly", currencyCode = "INR")) }
        budgets.forEach { budgetRepository.insertBudget(it) }

        // 6. Create and insert Investments (5 entries)
        val investments =
            listOf(
                Investment(
                    name = "Reliance Industries",
                    type = "Stock",
                    investedAmount = 7500000L,
                    currentValue = 9240000L,
                    symbol = "RELIANCE",
                    quantity = 30.0,
                    currencyCode = "INR",
                ),
            )
        investments.forEach { investmentRepository.insertInvestment(it) }

        // 7. Create and insert Loans (2 entries)
        val nextMonthCalHome =
            Calendar.getInstance().apply {
                add(Calendar.MONTH, 1)
                set(Calendar.DAY_OF_MONTH, 5)
            }
        val loans =
            listOf(
                Loan(
                    name = "Home Loan",
                    totalAmount = 450000000L,
                    outstandingAmount = 412050000L,
                    emiAmount = 3908200L,
                    nextEmiDate = nextMonthCalHome.timeInMillis,
                    currencyCode = "INR",
                    lenderName = "HDFC Bank",
                    interestRate = 8.5,
                    tenureMonths = 240,
                    dueDayOfMonth = 5,
                    isNotificationEnabled = true,
                    startDate = System.currentTimeMillis() - 365 * 24 * 60 * 60 * 1000L,
                ),
            )
        loans.forEach { loanRepository.insertLoan(it) }

        // 8. Create and insert Loan Payments
        val insertedLoans = loanRepository.allLoans.first()
        val homeLoanId = insertedLoans.find { it.name == "Home Loan" }?.id

        homeLoanId?.let {
            loanRepository.insertLoanPayment(
                LoanPayment(
                    loanId = it,
                    amount = 3908200L,
                    date = now - 25 * oneDayMs,
                    note = "Monthly EMI Paid",
                ),
            )
        }

        // 9. Create and insert Emergency Fund
        val emergencyFund =
            EmergencyFund(
                monthlyExpenses = 45000.0,
                targetMonths = 6,
                currentSaved = 180000.0,
            )
        goalRepository.insertEmergencyFund(emergencyFund)

        // 10. Create and insert Pending SMS Transactions (2 entries)
        val pendingTransactions =
            listOf(
                PendingTransaction(
                    amount = 450.00,
                    type = "EXPENSE",
                    merchant = "Starbucks Coffee",
                    bankAccount = "HDFC Bank XX98",
                    rawMessage = "Alert: Rs. 450.00 spent at Starbucks Coffee on HDFC Bank Card XX98 on 17-06-26.",
                    timestamp = now - 2 * 60 * 60 * 1000L,
                ),
            )
        pendingTransactions.forEach { transactionRepository.insertPendingTransaction(it) }
    }
}
