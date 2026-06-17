package prasad.vennam.moneypilot.util

import prasad.vennam.moneypilot.data.entity.*
import prasad.vennam.moneypilot.data.repository.MoneyPilotRepository
import java.util.Calendar

object DemoDataSeeder {
    suspend fun seed(repository: MoneyPilotRepository) {
        val now = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L

        // Categories: We use Category.DEFAULT_CATEGORIES
        val categories = Category.DEFAULT_CATEGORIES

        // Transactions
        val transactions = listOf(
            // Income
            Transaction(
                amount = 15000000L, // ₹1,50,000
                timestamp = now - 15 * oneDayMs,
                categoryId = 13, // Salary
                subCategory = "Monthly Pay",
                paymentMode = "Bank Transfer",
                note = "TechCorp June Salary",
                type = TransactionType.INCOME,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 4500000L, // ₹45,000
                timestamp = now - 5 * oneDayMs,
                categoryId = 14, // Freelance
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
                categoryId = 7, // Housing
                subCategory = "Rent",
                paymentMode = "Bank Transfer",
                note = "2BHK Apartment Rent",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 450000L, // ₹4,500
                timestamp = now - 8 * oneDayMs,
                categoryId = 1, // Food
                subCategory = "Groceries",
                paymentMode = "Card",
                note = "BigBasket monthly groceries",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 180000L, // ₹1,800
                timestamp = now - 1 * oneDayMs,
                categoryId = 1, // Food
                subCategory = "Dining Out",
                paymentMode = "UPI",
                note = "Dinner at Punjab Grill",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 350000L, // ₹3,500
                timestamp = now - 6 * oneDayMs,
                categoryId = 2, // Transport
                subCategory = "Fuel",
                paymentMode = "Card",
                note = "Car petrol refill",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 85000L, // ₹850
                timestamp = now - 2 * oneDayMs,
                categoryId = 2, // Transport
                subCategory = "Cab",
                paymentMode = "UPI",
                note = "Uber ride to office",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 1250000L, // ₹12,500
                timestamp = now - 4 * oneDayMs,
                categoryId = 3, // Shopping
                subCategory = "Clothing",
                paymentMode = "Card",
                note = "Zara weekend shopping",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 240000L, // ₹2,400
                timestamp = now - 12 * oneDayMs,
                categoryId = 6, // Utilities
                subCategory = "Electricity",
                paymentMode = "UPI",
                note = "BESCOM electric bill",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 99900L, // ₹999
                timestamp = now - 12 * oneDayMs,
                categoryId = 6, // Utilities
                subCategory = "Internet",
                paymentMode = "UPI",
                note = "Airtel Fiber Broadband",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 149900L, // ₹1,499
                timestamp = now - 14 * oneDayMs,
                categoryId = 4, // Entertainment
                subCategory = "Subscriptions",
                paymentMode = "UPI",
                note = "Netflix Premium Annual",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 250000L, // ₹2,500
                timestamp = now - 11 * oneDayMs,
                categoryId = 5, // Health
                subCategory = "Fitness",
                paymentMode = "Card",
                note = "Gym monthly membership",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 650000L, // ₹6,500
                timestamp = now - 9 * oneDayMs,
                categoryId = 11, // Insurance
                subCategory = "Medical",
                paymentMode = "UPI",
                note = "Star Health Premium",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 300000L, // ₹3,000
                timestamp = now - 3 * oneDayMs,
                categoryId = 9, // Gifts
                subCategory = "Birthday",
                paymentMode = "UPI",
                note = "Birthday gift for Rohit",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            ),
            Transaction(
                amount = 850000L, // ₹8,500
                timestamp = now - 7 * oneDayMs,
                categoryId = 10, // Travel
                subCategory = "Flights",
                paymentMode = "Card",
                note = "IndiGo Bangalore to Mumbai",
                type = TransactionType.EXPENSE,
                currencyCode = "INR"
            )
        )

        // Budgets
        val budgets = listOf(
            Budget(
                categoryId = 1, // Food
                amount = 1500000L, // ₹15,000
                period = "Monthly",
                currencyCode = "INR"
            ),
            Budget(
                categoryId = 2, // Transport
                amount = 600000L, // ₹6,000
                period = "Monthly",
                currencyCode = "INR"
            ),
            Budget(
                categoryId = 3, // Shopping
                amount = 1500000L, // ₹15,000
                period = "Monthly",
                currencyCode = "INR"
            ),
            Budget(
                categoryId = 6, // Utilities
                amount = 800000L, // ₹8,000
                period = "Monthly",
                currencyCode = "INR"
            )
        )

        // Investments
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

        // Loans
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

        // Emergency Fund
        val emergencyFund = EmergencyFund(
            monthlyExpenses = 45000.0,
            targetMonths = 6,
            currentSaved = 180000.0
        )

        // Perform atomic restore
        repository.restoreBackup(
            categories = categories,
            transactions = transactions,
            budgets = budgets,
            investments = investments,
            loans = loans,
            emergencyFund = emergencyFund
        )
    }
}
