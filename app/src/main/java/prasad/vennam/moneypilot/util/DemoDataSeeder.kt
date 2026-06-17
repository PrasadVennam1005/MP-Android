package prasad.vennam.moneypilot.util

import kotlinx.coroutines.flow.first
import prasad.vennam.moneypilot.data.entity.*
import prasad.vennam.moneypilot.data.repository.MoneyPilotRepository
import java.util.Calendar

object DemoDataSeeder {
    suspend fun seed(repository: MoneyPilotRepository) {
        val now = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L

        // 1. Clear all data first
        repository.clearAllData()

        // 2. Insert default categories
        val categories = Category.DEFAULT_CATEGORIES
        repository.categoryDao.insertCategories(categories)

        // 3. Query all categories to map their auto-increment IDs
        val insertedCategories = repository.categoryDao.getAllCategoriesSync()

        val foodId = insertedCategories.find { it.name == "Food" && it.isExpense }?.id
        val transportId = insertedCategories.find { it.name == "Transport" && it.isExpense }?.id
        val shoppingId = insertedCategories.find { it.name == "Shopping" && it.isExpense }?.id
        val entertainmentId = insertedCategories.find { it.name == "Entertainment" && it.isExpense }?.id
        val healthId = insertedCategories.find { it.name == "Health" && it.isExpense }?.id
        val utilitiesId = insertedCategories.find { it.name == "Utilities" && it.isExpense }?.id
        val housingId = insertedCategories.find { it.name == "Housing" && it.isExpense }?.id
        val educationId = insertedCategories.find { it.name == "Education" && it.isExpense }?.id
        val giftsExpenseId = insertedCategories.find { it.name == "Gifts" && it.isExpense }?.id
        val travelId = insertedCategories.find { it.name == "Travel" && it.isExpense }?.id
        val insuranceId = insertedCategories.find { it.name == "Insurance" && it.isExpense }?.id
        val billsId = insertedCategories.find { it.name == "Bills" && it.isExpense }?.id

        val salaryId = insertedCategories.find { it.name == "Salary" && !it.isExpense }?.id
        val freelanceId = insertedCategories.find { it.name == "Freelance" && !it.isExpense }?.id
        val rentalIncomeId = insertedCategories.find { it.name == "Rental" && !it.isExpense }?.id
        val investmentsIncomeId = insertedCategories.find { it.name == "Investments" && !it.isExpense }?.id

        // 4. Create and insert Transactions (approx 30 entries)
        val transactions = listOf(
            // --- Current Month Income ---
            Transaction(
                amount = 15000000L, // ₹1,50,000
                timestamp = now - 15 * oneDayMs,
                categoryId = salaryId,
                subCategory = "Monthly Pay",
                paymentMode = "Bank Transfer",
                note = "TechCorp Monthly Salary",
                type = TransactionType.INCOME,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 4500000L, // ₹45,000
                timestamp = now - 5 * oneDayMs,
                categoryId = freelanceId,
                subCategory = "UI Design",
                paymentMode = "Bank Transfer",
                note = "Freelance Mobile App Design",
                type = TransactionType.INCOME,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 1800000L, // ₹18,000
                timestamp = now - 10 * oneDayMs,
                categoryId = rentalIncomeId,
                subCategory = "Apartment rent",
                paymentMode = "Bank Transfer",
                note = "Rent from 1BHK property",
                type = TransactionType.INCOME,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 500000L, // ₹5,000
                timestamp = now - 12 * oneDayMs,
                categoryId = investmentsIncomeId,
                subCategory = "Dividends",
                paymentMode = "Bank Transfer",
                note = "TCS Stock Dividends",
                type = TransactionType.INCOME,
                currencyCode = "INR"
            ),

            // --- Previous Month Income ---
            Transaction(
                amount = 15000000L, // ₹1,50,000
                timestamp = now - 45 * oneDayMs,
                categoryId = salaryId,
                subCategory = "Monthly Pay",
                paymentMode = "Bank Transfer",
                note = "TechCorp Monthly Salary",
                type = TransactionType.INCOME,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 3500000L, // ₹35,000
                timestamp = now - 35 * oneDayMs,
                categoryId = freelanceId,
                subCategory = "Website development",
                paymentMode = "Bank Transfer",
                note = "Consulting Website Project",
                type = TransactionType.INCOME,
                currencyCode = "INR"
            ),

            // --- Current Month Expenses ---
            Transaction(
                amount = 2500000L, // ₹25,000
                timestamp = now - 15 * oneDayMs,
                categoryId = housingId,
                subCategory = "Rent",
                paymentMode = "Bank Transfer",
                note = "2BHK Bangalore Apartment Rent",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 550000L, // ₹5,500
                timestamp = now - 8 * oneDayMs,
                categoryId = foodId,
                subCategory = "Groceries",
                paymentMode = "Card",
                note = "Weekly groceries from BigBasket",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 240000L, // ₹2,400
                timestamp = now - 3 * oneDayMs,
                categoryId = foodId,
                subCategory = "Groceries",
                paymentMode = "UPI",
                note = "Fresh vegetables & milk",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 185000L, // ₹1,850
                timestamp = now - 1 * oneDayMs,
                categoryId = foodId,
                subCategory = "Dining Out",
                paymentMode = "UPI",
                note = "Dinner at Punjab Grill",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 120000L, // ₹1,200
                timestamp = now - 6 * oneDayMs,
                categoryId = foodId,
                subCategory = "Cafe",
                paymentMode = "UPI",
                note = "Third Wave Coffee weekend visit",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 350000L, // ₹3,500
                timestamp = now - 9 * oneDayMs,
                categoryId = transportId,
                subCategory = "Fuel",
                paymentMode = "Card",
                note = "Car petrol refill - Shell",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 89000L, // ₹890
                timestamp = now - 2 * oneDayMs,
                categoryId = transportId,
                subCategory = "Cab",
                paymentMode = "UPI",
                note = "Uber ride to airport",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 1250000L, // ₹12,500
                timestamp = now - 4 * oneDayMs,
                categoryId = shoppingId,
                subCategory = "Clothing",
                paymentMode = "Card",
                note = "Zara summer clothing collection",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 480000L, // ₹4,800
                timestamp = now - 13 * oneDayMs,
                categoryId = shoppingId,
                subCategory = "Electronics",
                paymentMode = "UPI",
                note = "Logitech Wireless Mouse",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 265000L, // ₹2,650
                timestamp = now - 12 * oneDayMs,
                categoryId = utilitiesId,
                subCategory = "Electricity",
                paymentMode = "UPI",
                note = "BESCOM electricity bill",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 99900L, // ₹999
                timestamp = now - 12 * oneDayMs,
                categoryId = utilitiesId,
                subCategory = "Internet",
                paymentMode = "UPI",
                note = "Airtel Fiber monthly bill",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 149900L, // ₹1,499
                timestamp = now - 14 * oneDayMs,
                categoryId = entertainmentId,
                subCategory = "Subscriptions",
                paymentMode = "UPI",
                note = "Netflix Premium Annual",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 120000L, // ₹1,200
                timestamp = now - 10 * oneDayMs,
                categoryId = entertainmentId,
                subCategory = "Movies",
                paymentMode = "UPI",
                note = "IMAX Tickets for Oppenheimer",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 250000L, // ₹2,500
                timestamp = now - 11 * oneDayMs,
                categoryId = healthId,
                subCategory = "Fitness",
                paymentMode = "Card",
                note = "Gym monthly membership fee",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 650000L, // ₹6,500
                timestamp = now - 9 * oneDayMs,
                categoryId = insuranceId,
                subCategory = "Medical",
                paymentMode = "UPI",
                note = "Star Health Insurance Premium",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 300000L, // ₹3,000
                timestamp = now - 5 * oneDayMs,
                categoryId = giftsExpenseId,
                subCategory = "Birthday",
                paymentMode = "UPI",
                note = "Birthday gift for Rohit",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 850000L, // ₹8,500
                timestamp = now - 7 * oneDayMs,
                categoryId = travelId,
                subCategory = "Flights",
                paymentMode = "Card",
                note = "IndiGo Flight Bangalore to Mumbai",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            ),

            // --- Previous Month Expenses ---
            Transaction(
                amount = 2500000L, // ₹25,000
                timestamp = now - 45 * oneDayMs,
                categoryId = housingId,
                subCategory = "Rent",
                paymentMode = "Bank Transfer",
                note = "2BHK Bangalore Apartment Rent",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 580000L, // ₹5,800
                timestamp = now - 38 * oneDayMs,
                categoryId = foodId,
                subCategory = "Groceries",
                paymentMode = "Card",
                note = "BigBasket monthly groceries",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 400000L, // ₹4,000
                timestamp = now - 40 * oneDayMs,
                categoryId = transportId,
                subCategory = "Fuel",
                paymentMode = "Card",
                note = "Car petrol refill",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 950000L, // ₹9,500
                timestamp = now - 34 * oneDayMs,
                categoryId = shoppingId,
                subCategory = "Electronics",
                paymentMode = "Card",
                note = "Mechanical Keyboard purchase",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 180000L, // ₹1,800
                timestamp = now - 42 * oneDayMs,
                categoryId = utilitiesId,
                subCategory = "Electricity",
                paymentMode = "UPI",
                note = "BESCOM electricity bill",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 220000L, // ₹2,200
                timestamp = now - 36 * oneDayMs,
                categoryId = healthId,
                subCategory = "Pharmacy",
                paymentMode = "UPI",
                note = "Vitamins & Medicines",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            )
        )
        transactions.forEach { repository.transactionDao.insertTransaction(it) }

        // 5. Create and insert Budgets (6 categories)
        val budgets = mutableListOf<Budget>()
        foodId?.let { budgets.add(Budget(categoryId = it, amount = 1500000L, period = "Monthly", currencyCode = "INR")) }
        transportId?.let { budgets.add(Budget(categoryId = it, amount = 600000L, period = "Monthly", currencyCode = "INR")) }
        shoppingId?.let { budgets.add(Budget(categoryId = it, amount = 1500000L, period = "Monthly", currencyCode = "INR")) }
        utilitiesId?.let { budgets.add(Budget(categoryId = it, amount = 800000L, period = "Monthly", currencyCode = "INR")) }
        entertainmentId?.let { budgets.add(Budget(categoryId = it, amount = 500000L, period = "Monthly", currencyCode = "INR")) }
        healthId?.let { budgets.add(Budget(categoryId = it, amount = 400000L, period = "Monthly", currencyCode = "INR")) }
        budgets.forEach { repository.budgetDao.insertBudget(it) }

        // 6. Create and insert Investments (5 entries)
        val investments = listOf(
            Investment(
                name = "Reliance Industries",
                type = "Stock",
                investedAmount = 7500000L, // ₹75,000
                currentValue = 9240000L, // ₹92,400
                symbol = "RELIANCE",
                quantity = 30.0,
                currencyCode = "INR"
            ),
            Investment(
                name = "HDFC Bank Ltd",
                type = "Stock",
                investedAmount = 5000000L, // ₹50,000
                currentValue = 5620000L, // ₹56,200
                symbol = "HDFCBANK",
                quantity = 35.0,
                currencyCode = "INR"
            ),
            Investment(
                name = "HDFC Index Nifty 50 Fund",
                type = "Mutual Fund",
                investedAmount = 12000000L, // ₹1,20,000
                currentValue = 14850000L, // ₹1,48,500
                currencyCode = "INR"
            ),
            Investment(
                name = "Sovereign Gold Bond",
                type = "Gold",
                investedAmount = 8000000L, // ₹80,000
                currentValue = 9850000L, // ₹98,500
                currencyCode = "INR"
            ),
            Investment(
                name = "Bitcoin",
                type = "Crypto",
                investedAmount = 5000000L, // ₹50,000
                currentValue = 4200000L, // ₹42,000
                symbol = "BTC",
                quantity = 0.015,
                currencyCode = "INR"
            )
        )
        investments.forEach { repository.investmentDao.insertInvestment(it) }

        // 7. Create and insert Loans (2 entries)
        val nextMonthCalHome = Calendar.getInstance().apply {
            add(Calendar.MONTH, 1)
            set(Calendar.DAY_OF_MONTH, 5)
        }
        val nextMonthCalCar = Calendar.getInstance().apply {
            add(Calendar.MONTH, 1)
            set(Calendar.DAY_OF_MONTH, 10)
        }
        val loans = listOf(
            Loan(
                name = "Home Loan",
                totalAmount = 450000000L, // ₹45,00,000
                outstandingAmount = 412050000L, // ₹41,20,500
                emiAmount = 3908200L, // ₹39,082
                nextEmiDate = nextMonthCalHome.timeInMillis,
                currencyCode = "INR",
                lenderName = "HDFC Bank",
                interestRate = 8.5,
                tenureMonths = 240,
                dueDayOfMonth = 5,
                isNotificationEnabled = true,
                startDate = System.currentTimeMillis() - 365 * 24 * 60 * 60 * 1000L
            ),
            Loan(
                name = "Car Loan",
                totalAmount = 120000000L, // ₹12,00,000
                outstandingAmount = 78000000L, // ₹7,80,000
                emiAmount = 2450000L, // ₹24,500
                nextEmiDate = nextMonthCalCar.timeInMillis,
                currencyCode = "INR",
                lenderName = "ICICI Bank",
                interestRate = 7.8,
                tenureMonths = 60,
                dueDayOfMonth = 10,
                isNotificationEnabled = true,
                startDate = System.currentTimeMillis() - 180 * 24 * 60 * 60 * 1000L
            )
        )
        loans.forEach { repository.loanDao.insertLoan(it) }

        // 8. Create and insert Loan Payments
        val insertedLoans = repository.loanDao.getAllLoans().first()
        val homeLoanId = insertedLoans.find { it.name == "Home Loan" }?.id
        val carLoanId = insertedLoans.find { it.name == "Car Loan" }?.id

        homeLoanId?.let {
            repository.loanPaymentDao.insertPayment(
                LoanPayment(
                    loanId = it,
                    amount = 3908200L,
                    date = now - 25 * oneDayMs,
                    note = "Monthly EMI Paid"
                )
            )
            repository.loanPaymentDao.insertPayment(
                LoanPayment(
                    loanId = it,
                    amount = 3908200L,
                    date = now - 55 * oneDayMs,
                    note = "Monthly EMI Paid"
                )
            )
        }

        carLoanId?.let {
            repository.loanPaymentDao.insertPayment(
                LoanPayment(
                    loanId = it,
                    amount = 2450000L,
                    date = now - 20 * oneDayMs,
                    note = "Monthly EMI Paid"
                )
            )
            repository.loanPaymentDao.insertPayment(
                LoanPayment(
                    loanId = it,
                    amount = 2450000L,
                    date = now - 50 * oneDayMs,
                    note = "Monthly EMI Paid"
                )
            )
        }

        // 9. Create and insert Emergency Fund
        val emergencyFund = EmergencyFund(
            monthlyExpenses = 45000.0,
            targetMonths = 6,
            currentSaved = 180000.0
        )
        repository.emergencyFundDao.insertEmergencyFund(emergencyFund)

        // 10. Create and insert Pending SMS Transactions (2 entries)
        val pendingTransactions = listOf(
            PendingTransaction(
                amount = 450.00,
                type = "EXPENSE",
                merchant = "Starbucks Coffee",
                bankAccount = "HDFC Bank XX98",
                rawMessage = "Alert: Rs. 450.00 spent at Starbucks Coffee on HDFC Bank Card XX98 on 17-06-26.",
                timestamp = now - 2 * 60 * 60 * 1000L // 2 hours ago
            ),
            PendingTransaction(
                amount = 1200.00,
                type = "EXPENSE",
                merchant = "Zomato",
                bankAccount = "ICICI Bank XX12",
                rawMessage = "Debited: Rs. 1,200.00 from ICICI Bank XX12 to Zomato on 17-06-26.",
                timestamp = now - 1 * 60 * 60 * 1000L // 1 hour ago
            )
        )
        pendingTransactions.forEach { repository.pendingTransactionDao.insertPendingTransaction(it) }
    }
}
