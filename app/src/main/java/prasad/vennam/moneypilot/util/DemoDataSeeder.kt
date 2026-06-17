package prasad.vennam.moneypilot.util

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

        // 3. Query all categories to get their newly generated auto-increment IDs
        val insertedCategories = repository.categoryDao.getAllCategoriesSync()

        // Helper maps/IDs
        val foodId = insertedCategories.find { it.name == "Food" && it.isExpense }?.id
        val transportId = insertedCategories.find { it.name == "Transport" && it.isExpense }?.id
        val shoppingId = insertedCategories.find { it.name == "Shopping" && it.isExpense }?.id
        val entertainmentId = insertedCategories.find { it.name == "Entertainment" && it.isExpense }?.id
        val healthId = insertedCategories.find { it.name == "Health" && it.isExpense }?.id
        val utilitiesId = insertedCategories.find { it.name == "Utilities" && it.isExpense }?.id
        val housingId = insertedCategories.find { it.name == "Housing" && it.isExpense }?.id
        val giftsExpenseId = insertedCategories.find { it.name == "Gifts" && it.isExpense }?.id
        val travelId = insertedCategories.find { it.name == "Travel" && it.isExpense }?.id
        val insuranceId = insertedCategories.find { it.name == "Insurance" && it.isExpense }?.id

        val salaryId = insertedCategories.find { it.name == "Salary" && !it.isExpense }?.id
        val freelanceId = insertedCategories.find { it.name == "Freelance" && !it.isExpense }?.id

        // 4. Create and insert Transactions
        val transactions = listOf(
            // Income
            Transaction(
                amount = 15000000L, // ₹1,50,000
                timestamp = now - 15 * oneDayMs,
                categoryId = salaryId,
                subCategory = "Monthly Pay",
                paymentMode = "Bank Transfer",
                note = "TechCorp June Salary",
                type = TransactionType.INCOME,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 4500000L, // ₹45,000
                timestamp = now - 5 * oneDayMs,
                categoryId = freelanceId,
                subCategory = "Consulting",
                paymentMode = "Bank Transfer",
                note = "UI Design Consultation",
                type = TransactionType.INCOME,
                currencyCode = "INR"
            ),
            // Expenses
            Transaction(
                amount = 2500000L, // ₹25,000
                timestamp = now - 10 * oneDayMs,
                categoryId = housingId,
                subCategory = "Rent",
                paymentMode = "Bank Transfer",
                note = "2BHK Apartment Rent",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 450000L, // ₹4,500
                timestamp = now - 8 * oneDayMs,
                categoryId = foodId,
                subCategory = "Groceries",
                paymentMode = "Card",
                note = "BigBasket monthly groceries",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 180000L, // ₹1,800
                timestamp = now - 1 * oneDayMs,
                categoryId = foodId,
                subCategory = "Dining Out",
                paymentMode = "UPI",
                note = "Dinner at Punjab Grill",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 350000L, // ₹3,500
                timestamp = now - 6 * oneDayMs,
                categoryId = transportId,
                subCategory = "Fuel",
                paymentMode = "Card",
                note = "Car petrol refill",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 85000L, // ₹850
                timestamp = now - 2 * oneDayMs,
                categoryId = transportId,
                subCategory = "Cab",
                paymentMode = "UPI",
                note = "Uber ride to office",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 1250000L, // ₹12,500
                timestamp = now - 4 * oneDayMs,
                categoryId = shoppingId,
                subCategory = "Clothing",
                paymentMode = "Card",
                note = "Zara weekend shopping",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 240000L, // ₹2,400
                timestamp = now - 12 * oneDayMs,
                categoryId = utilitiesId,
                subCategory = "Electricity",
                paymentMode = "UPI",
                note = "BESCOM electric bill",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 99900L, // ₹999
                timestamp = now - 12 * oneDayMs,
                categoryId = utilitiesId,
                subCategory = "Internet",
                paymentMode = "UPI",
                note = "Airtel Fiber Broadband",
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
                amount = 250000L, // ₹2,500
                timestamp = now - 11 * oneDayMs,
                categoryId = healthId,
                subCategory = "Fitness",
                paymentMode = "Card",
                note = "Gym monthly membership",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 650000L, // ₹6,500
                timestamp = now - 9 * oneDayMs,
                categoryId = insuranceId,
                subCategory = "Medical",
                paymentMode = "UPI",
                note = "Star Health Premium",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 300000L, // ₹3,000
                timestamp = now - 3 * oneDayMs,
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
                note = "IndiGo Bangalore to Mumbai",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            )
        )
        transactions.forEach { repository.transactionDao.insertTransaction(it) }

        // 5. Create and insert Budgets (filtering nulls to be safe)
        val budgets = mutableListOf<Budget>()
        foodId?.let { budgets.add(Budget(categoryId = it, amount = 1500000L, period = "Monthly", currencyCode = "INR")) }
        transportId?.let { budgets.add(Budget(categoryId = it, amount = 600000L, period = "Monthly", currencyCode = "INR")) }
        shoppingId?.let { budgets.add(Budget(categoryId = it, amount = 1500000L, period = "Monthly", currencyCode = "INR")) }
        utilitiesId?.let { budgets.add(Budget(categoryId = it, amount = 800000L, period = "Monthly", currencyCode = "INR")) }
        budgets.forEach { repository.budgetDao.insertBudget(it) }

        // 6. Create and insert Investments
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
                name = "HDFC Index Nifty 50 Fund",
                type = "Mutual Fund",
                investedAmount = 12000000L, // ₹1,20,000
                currentValue = 14850000L, // ₹1,48,500
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

        // 7. Create and insert Loans
        val nextMonthCal = Calendar.getInstance().apply {
            add(Calendar.MONTH, 1)
            set(Calendar.DAY_OF_MONTH, 5)
        }
        val loans = listOf(
            Loan(
                name = "Home Loan",
                totalAmount = 450000000L, // ₹45,00,000
                outstandingAmount = 412050000L, // ₹41,20,500
                emiAmount = 3908200L, // ₹39,082
                nextEmiDate = nextMonthCal.timeInMillis,
                currencyCode = "INR",
                lenderName = "HDFC Bank",
                interestRate = 8.5,
                tenureMonths = 240,
                dueDayOfMonth = 5,
                isNotificationEnabled = true,
                startDate = System.currentTimeMillis() - 365 * 24 * 60 * 60 * 1000L
            )
        )
        loans.forEach { repository.loanDao.insertLoan(it) }

        // 8. Create and insert Emergency Fund
        val emergencyFund = EmergencyFund(
            monthlyExpenses = 45000.0,
            targetMonths = 6,
            currentSaved = 180000.0
        )
        repository.emergencyFundDao.insertEmergencyFund(emergencyFund)
    }
}
