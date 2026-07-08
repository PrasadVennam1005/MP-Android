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
        val oneHourMs = 60 * 60 * 1000L
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
                    amount = 800000L, // ₹8,000.00
                    timestamp = now - 10 * oneHourMs, // Today
                    categoryId = freelanceId,
                    subCategory = "Consulting",
                    paymentMode = "Bank Transfer",
                    note = "Tech Consulting Session",
                    type = TransactionType.INCOME,
                    currencyCode = "INR",
                ),
                Transaction(
                    amount = 1800000L, // ₹18,000.00
                    timestamp = now - 10 * oneDayMs,
                    categoryId = rentalIncomeId,
                    subCategory = "Apartment Rent",
                    paymentMode = "Bank Transfer",
                    note = "Co-living Space Rent Income",
                    type = TransactionType.INCOME,
                    currencyCode = "INR",
                ),
                Transaction(
                    amount = 350000L, // ₹3,500.00
                    timestamp = now - 22 * oneDayMs,
                    categoryId = investmentsIncomeId,
                    subCategory = "Dividends",
                    paymentMode = "Bank Transfer",
                    note = "TCS Dividend Payout",
                    type = TransactionType.INCOME,
                    currencyCode = "INR",
                ),
                Transaction(
                    amount = 2000000L, // ₹20,000.00
                    timestamp = now - 25 * oneDayMs,
                    categoryId = freelanceId,
                    subCategory = "Logo Design",
                    paymentMode = "Bank Transfer",
                    note = "Brand Identity Freelance Work",
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
                    amount = 600000L, // ₹6,000.00
                    timestamp = now - 22 * oneDayMs,
                    categoryId = foodId,
                    subCategory = "Groceries",
                    paymentMode = "Card",
                    note = "Bulk grocery from Zepto",
                    type = TransactionType.EXPENSE,
                    currencyCode = "INR",
                ),
                Transaction(
                    amount = 120000L, // ₹1,200.00
                    timestamp = now - oneHourMs, // Today
                    categoryId = foodId,
                    subCategory = "Restaurant",
                    paymentMode = "UPI",
                    note = "Dinner with friends at Social",
                    type = TransactionType.EXPENSE,
                    currencyCode = "INR",
                ),
                Transaction(
                    amount = 42000L, // ₹420.00
                    timestamp = now - 8 * oneHourMs, // Today
                    categoryId = foodId,
                    subCategory = "Lunch Delivery",
                    paymentMode = "UPI",
                    note = "Swiggy Lunch Order",
                    type = TransactionType.EXPENSE,
                    currencyCode = "INR",
                ),
                Transaction(
                    amount = 85000L, // ₹850.00
                    timestamp = now - 11 * oneDayMs,
                    categoryId = foodId,
                    subCategory = "Dining out",
                    paymentMode = "UPI",
                    note = "Lunch buffet at Barbeque Nation",
                    type = TransactionType.EXPENSE,
                    currencyCode = "INR",
                ),
                Transaction(
                    amount = 140000L, // ₹1,400.00
                    timestamp = now - 20 * oneDayMs,
                    categoryId = foodId,
                    subCategory = "Dining out",
                    paymentMode = "Card",
                    note = "Family dinner at Mainland China",
                    type = TransactionType.EXPENSE,
                    currencyCode = "INR",
                ),
                Transaction(
                    amount = 35000L, // ₹350.00
                    timestamp = now - 3 * oneHourMs, // Today
                    categoryId = transportId,
                    subCategory = "Cab",
                    paymentMode = "UPI",
                    note = "Ola Cab Ride to office",
                    type = TransactionType.EXPENSE,
                    currencyCode = "INR",
                ),
                Transaction(
                    amount = 300000L, // ₹3,000.00
                    timestamp = now - 4 * oneDayMs,
                    categoryId = transportId,
                    subCategory = "Fuel",
                    paymentMode = "Card",
                    note = "HP Petrol Pump Refuel",
                    type = TransactionType.EXPENSE,
                    currencyCode = "INR",
                ),
                Transaction(
                    amount = 300000L, // ₹3,000.00
                    timestamp = now - 18 * oneDayMs,
                    categoryId = transportId,
                    subCategory = "Fuel",
                    paymentMode = "Card",
                    note = "Shell Fuel Pump Refuel",
                    type = TransactionType.EXPENSE,
                    currencyCode = "INR",
                ),
                Transaction(
                    amount = 85000L, // ₹850.00
                    timestamp = now - 4 * oneHourMs, // Today
                    categoryId = shoppingId,
                    subCategory = "Coffee",
                    paymentMode = "Card",
                    note = "Starbucks coffee beans and mug",
                    type = TransactionType.EXPENSE,
                    currencyCode = "INR",
                ),
                Transaction(
                    amount = 65000L, // ₹650.00
                    timestamp = now - 6 * oneHourMs, // Today
                    categoryId = entertainmentId,
                    subCategory = "Movies",
                    paymentMode = "UPI",
                    note = "PVR Movie Tickets",
                    type = TransactionType.EXPENSE,
                    currencyCode = "INR",
                ),
                Transaction(
                    amount = 5500000L, // ₹55,000.00
                    timestamp = now - 9 * oneDayMs,
                    categoryId = shoppingId,
                    subCategory = "Electronics",
                    paymentMode = "Credit Card",
                    note = "OnePlus Nord Tablet",
                    type = TransactionType.EXPENSE,
                    currencyCode = "INR",
                ),
                Transaction(
                    amount = 1500000L, // ₹15,000.00
                    timestamp = now - 30 * oneDayMs,
                    categoryId = shoppingId,
                    subCategory = "Clothing",
                    paymentMode = "Credit Card",
                    note = "Zara Autumn Jackets",
                    type = TransactionType.EXPENSE,
                    currencyCode = "INR",
                ),
                Transaction(
                    amount = 420000L, // ₹4,200.00
                    timestamp = now - 12 * oneDayMs,
                    categoryId = utilitiesId,
                    subCategory = "Electricity",
                    paymentMode = "Bank Transfer",
                    note = "BESCOM Electricity Bill",
                    type = TransactionType.EXPENSE,
                    currencyCode = "INR",
                ),
                Transaction(
                    amount = 150000L, // ₹1,500.00
                    timestamp = now - 14 * oneDayMs,
                    categoryId = utilitiesId,
                    subCategory = "Broadband",
                    paymentMode = "UPI",
                    note = "Airtel Xstream Fiber Bill",
                    type = TransactionType.EXPENSE,
                    currencyCode = "INR",
                ),
                Transaction(
                    amount = 180000L, // ₹1,800.00
                    timestamp = now - 6 * oneDayMs,
                    categoryId = healthId,
                    subCategory = "Pharmacy",
                    paymentMode = "UPI",
                    note = "Apollo Pharmacy Medicines",
                    type = TransactionType.EXPENSE,
                    currencyCode = "INR",
                ),
                Transaction(
                    amount = 89000L, // ₹890.00
                    timestamp = now - 27 * oneDayMs,
                    categoryId = entertainmentId,
                    subCategory = "Subscriptions",
                    paymentMode = "Card",
                    note = "Netflix Premium Plan",
                    type = TransactionType.EXPENSE,
                    currencyCode = "INR",
                ),
                Transaction(
                    amount = 12500000L, // ₹125,000.00
                    timestamp = now - 24 * oneDayMs,
                    categoryId = travelId,
                    subCategory = "Flight & Hotel",
                    paymentMode = "Credit Card",
                    note = "Weekend Trip to Goa",
                    type = TransactionType.EXPENSE,
                    currencyCode = "INR",
                ),
                Transaction(
                    amount = 250000L, // ₹2,500.00
                    timestamp = now - 28 * oneDayMs,
                    categoryId = healthId,
                    subCategory = "Gym",
                    paymentMode = "UPI",
                    note = "Gold's Gym Monthly Membership",
                    type = TransactionType.EXPENSE,
                    currencyCode = "INR",
                ),
            )
        transactions.forEach { transactionRepository.insertTransaction(it) }

        // 5. Create and insert Budgets (6 categories)
        val budgets = mutableListOf<Budget>()
        foodId?.let { budgets.add(Budget(categoryId = it, amount = 1500000L, period = "Monthly", currencyCode = "INR")) }
        transportId?.let { budgets.add(Budget(categoryId = it, amount = 800000L, period = "Monthly", currencyCode = "INR")) }
        shoppingId?.let { budgets.add(Budget(categoryId = it, amount = 2000000L, period = "Monthly", currencyCode = "INR")) }
        utilitiesId?.let { budgets.add(Budget(categoryId = it, amount = 1000000L, period = "Monthly", currencyCode = "INR")) }
        travelId?.let { budgets.add(Budget(categoryId = it, amount = 1500000L, period = "Monthly", currencyCode = "INR")) }
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
                Investment(
                    name = "HDFC Bank Ltd",
                    type = "Stock",
                    investedAmount = 5000000L,
                    currentValue = 5850000L,
                    symbol = "HDFCBANK",
                    quantity = 35.0,
                    currencyCode = "INR",
                ),
                Investment(
                    name = "Nifty 50 Index Fund",
                    type = "Mutual Fund",
                    investedAmount = 12000000L,
                    currentValue = 14350000L,
                    symbol = "NIFTY50",
                    quantity = 1000.0,
                    currencyCode = "INR",
                ),
                Investment(
                    name = "Physical Gold",
                    type = "Gold",
                    investedAmount = 8000000L,
                    currentValue = 9450000L,
                    symbol = "GOLD",
                    quantity = 15.0,
                    currencyCode = "INR",
                ),
                Investment(
                    name = "Bitcoin",
                    type = "Crypto",
                    investedAmount = 4000000L,
                    currentValue = 5120000L,
                    symbol = "BTC",
                    quantity = 0.01,
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
