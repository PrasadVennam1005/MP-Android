package prasad.vennam.moneypilot.data.repository

import prasad.vennam.moneypilot.data.model.FinanceArticle
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LearnFinanceRepository @Inject constructor() {

    fun getArticles(): List<FinanceArticle> = articles

    fun getArticleById(id: String): FinanceArticle? = articles.find { it.id == id }

    fun getCategories(): List<String> = listOf("Budgeting", "Loans", "Investments", "Savings")

    companion object {
        private val articles = listOf(
            // Budgeting
            FinanceArticle(
                id = "budget_1",
                title = "50/30/20 Rule",
                category = "Budgeting",
                description = "The classic rule for simple budgeting.",
                content = """
                    The 50/30/20 rule is a simple way to budget that helps you manage your money effectively, easily and sustainably.
                    
                    The basic rule is to divide your after-tax income into three spending categories:
                    - 50% for Needs: Essential expenses like rent, groceries, and utilities.
                    - 30% for Wants: Non-essential spending like dining out, hobbies, and entertainment.
                    - 20% for Savings and Debt: Putting money toward your future or paying off debt.
                """.trimIndent()
            ),
            FinanceArticle(
                id = "budget_2",
                title = "How To Create Monthly Budget",
                category = "Budgeting",
                description = "Step-by-step guide to monthly budgeting.",
                content = """
                    Creating a monthly budget is the first step toward financial freedom.
                    
                    Steps:
                    1. Calculate your Net Income: Know how much you take home after taxes.
                    2. List Monthly Expenses: Track everything from rent to that daily coffee.
                    3. Categorize Fixed vs Variable: Fixed costs (rent) stay the same; variable (groceries) change.
                    4. Set Realistic Goals: Decide how much you want to save each month.
                    5. Adjust as Needed: If you're overspending, find areas to cut back.
                """.trimIndent()
            ),
            FinanceArticle(
                id = "budget_3",
                title = "How To Reduce Expenses",
                category = "Budgeting",
                description = "Practical tips to save more every month.",
                content = """
                    Reducing expenses doesn't mean stopping your life; it means spending smarter.
                    
                    Tips:
                    - Audit Subscriptions: Cancel services you don't use regularly.
                    - Cook at Home: Meal prepping can save thousands over time.
                    - Comparison Shopping: Always look for better deals on insurance or utilities.
                    - The 30-Day Rule: Wait 30 days before making a non-essential purchase to avoid impulse buying.
                """.trimIndent()
            ),
            FinanceArticle(
                id = "budget_4",
                title = "Emergency Fund Guide",
                category = "Budgeting",
                description = "Why and how to build your safety net.",
                content = """
                    An emergency fund is money set aside to cover life's unexpected expenses.
                    
                    - Aim for 3-6 months of essential expenses.
                    - Keep it in a high-yield savings account for liquidity and some growth.
                    - Use it only for real emergencies (medical, job loss, urgent repairs).
                    - Start small: even a small fund is better than none.
                """.trimIndent()
            ),
            
            // Loans
            FinanceArticle(
                id = "loan_1",
                title = "What Is EMI",
                category = "Loans",
                description = "Understanding Equated Monthly Installments.",
                content = """
                    EMI stands for Equated Monthly Installment. It is a fixed payment amount made by a borrower to a lender at a specified date each calendar month.
                    
                    EMIs are applied to both interest and principal each month so that over a specified number of years, the loan is paid off in full.
                    
                    Formula: EMI = [P x R x (1+R)^N] / [(1+R)^N-1]
                    Where P = Principal, R = Monthly Interest Rate, N = Number of Months.
                """.trimIndent()
            ),
            FinanceArticle(
                id = "loan_2",
                title = "Home Loan vs Personal Loan",
                category = "Loans",
                description = "Choosing the right loan for your needs.",
                content = """
                    - Home Loans: Usually have lower interest rates and longer tenures. They are secured against the property. Tax benefits are often available.
                    - Personal Loans: Higher interest rates and shorter tenures. They are unsecured (no collateral). Best for short-term needs or emergencies.
                    
                    Always compare processing fees and prepayment charges before deciding.
                """.trimIndent()
            ),
            FinanceArticle(
                id = "loan_3",
                title = "How Interest Is Calculated",
                category = "Loans",
                description = "Simple vs Compound interest on loans.",
                content = """
                    Lenders generally use reducing balance methods for interest calculation. 
                    - Flat Rate: Interest is calculated on the full principal throughout the tenure. (More expensive)
                    - Reducing Balance: Interest is calculated on the remaining principal amount after each EMI. (Standard and cheaper)
                    
                    Check your loan agreement to see which method is applied.
                """.trimIndent()
            ),
            FinanceArticle(
                id = "loan_4",
                title = "How To Close Loans Faster",
                category = "Loans",
                description = "Strategies to get debt-free sooner.",
                content = """
                    - Increase EMI: Even a 5% increase in your monthly EMI can significantly reduce your tenure.
                    - Annual Prepayments: Use bonuses or extra income to pay down the principal.
                    - Debt Snowball: Pay off the smallest loans first for psychological wins.
                    - Debt Avalanche: Pay off loans with the highest interest rates first to save the most money.
                """.trimIndent()
            ),
            FinanceArticle(
                id = "loan_5",
                title = "Prepayment Benefits",
                category = "Loans",
                description = "The power of paying early.",
                content = """
                    Prepaying a portion of your loan principal reduces the outstanding amount, which in turn reduces the interest calculated for the remaining months.
                    
                    Early prepayments (in the first half of the loan tenure) are most beneficial because interest components are higher during this period.
                """.trimIndent()
            ),

            // Investments
            FinanceArticle(
                id = "inv_1",
                title = "SIP Basics",
                category = "Investments",
                description = "Systematic Investment Plan explained.",
                content = """
                    A SIP allows you to invest a small, fixed amount regularly in a mutual fund scheme.
                    
                    Benefits:
                    - Disciplined Investing: Automates your savings.
                    - Rupee Cost Averaging: You buy more units when prices are low and fewer when prices are high.
                    - Power of Compounding: Investing early and regularly yields massive returns over time.
                """.trimIndent()
            ),
            FinanceArticle(
                id = "inv_2",
                title = "Mutual Funds",
                category = "Investments",
                description = "A diversified pool of assets.",
                content = """
                    A mutual fund is a company that pools money from many investors and invests it in securities such as stocks, bonds, and short-term debt.
                    
                    Types:
                    - Equity Funds: Invest in stocks (Higher risk, higher return).
                    - Debt Funds: Invest in fixed-income securities (Lower risk, stable return).
                    - Hybrid Funds: Mix of both.
                """.trimIndent()
            ),
            FinanceArticle(
                id = "inv_3",
                title = "Stocks For Beginners",
                category = "Investments",
                description = "Owning a piece of a company.",
                content = """
                    Buying a stock means you own a small part of a public company. 
                    
                    - Research: Understand the company's business model and earnings.
                    - Diversification: Don't put all your money in one stock.
                    - Long-term view: Stock markets are volatile in the short term but tend to grow over decades.
                    - Dividends: Some companies share a part of their profits with shareholders.
                """.trimIndent()
            ),
            FinanceArticle(
                id = "inv_4",
                title = "Gold Investment",
                category = "Investments",
                description = "The traditional safe haven.",
                content = """
                    Gold is often used as a hedge against inflation and currency fluctuations.
                    
                    Ways to invest:
                    - Physical Gold: Jewelry or coins (Storage and purity risks).
                    - Digital Gold: Buy gold through apps (Convenient).
                    - Gold ETFs: Trade like stocks on the exchange.
                    - Sovereign Gold Bonds (SGBs): Government-backed, pays interest plus price appreciation.
                """.trimIndent()
            ),
            FinanceArticle(
                id = "inv_5",
                title = "PPF vs EPF",
                category = "Investments",
                description = "Comparing retirement savings options.",
                content = """
                    - EPF (Employee Provident Fund): For salaried employees. Both employee and employer contribute. Mandatory for most.
                    - PPF (Public Provident Fund): Available to all citizens. 15-year lock-in. Tax-exempt at all stages.
                    
                    Both offer attractive interest rates and are considered very safe (Government-backed).
                """.trimIndent()
            ),
            FinanceArticle(
                id = "inv_6",
                title = "FD vs SIP",
                category = "Investments",
                description = "Fixed Deposit vs Systematic Investment Plan.",
                content = """
                    - Fixed Deposit (FD): Guaranteed returns, no market risk. Best for short-term goals or very conservative investors.
                    - SIP (Mutual Funds): Variable returns based on market performance. Generally outperforms FD over the long term.
                    
                    A healthy portfolio usually has a mix of both.
                """.trimIndent()
            ),

            // Savings
            FinanceArticle(
                id = "save_1",
                title = "Save First Spend Later",
                category = "Savings",
                description = "The most important financial habit.",
                content = """
                    Most people spend what they earn and save what is left. Successful savers do the opposite: they save first and spend what is left.
                    
                    Warren Buffett famously said: "Do not save what is left after spending, but spend what is left after saving."
                    
                    Automate your savings to go to a separate account the day your salary arrives.
                """.trimIndent()
            ),
            FinanceArticle(
                id = "save_2",
                title = "How Much Emergency Fund Needed",
                category = "Savings",
                description = "Calculating your unique safety net.",
                content = """
                    Your emergency fund size depends on:
                    - Monthly Expenses: Fixed costs (rent, food, insurance).
                    - Job Stability: If your income is irregular, aim for 9-12 months.
                    - Family Size: More dependents require a larger cushion.
                    
                    Standard advice is 3-6 months, but adjust for your personal comfort level.
                """.trimIndent()
            ),
            FinanceArticle(
                id = "save_3",
                title = "Building Wealth Slowly",
                category = "Savings",
                description = "Consistency over intensity.",
                content = """
                    Building wealth is a marathon, not a sprint.
                    
                    Key factors:
                    1. Savings Rate: How much you save relative to your income.
                    2. Time: Starting early is your biggest advantage.
                    3. Consistency: Staying invested even when markets are down.
                    4. Avoiding Lifestyle Inflation: When your income increases, don't increase your spending by the same amount.
                """.trimIndent()
            ),
            FinanceArticle(
                id = "gen_1",
                title = "Power of Compounding",
                category = "Investments",
                description = "Einstein's eighth wonder of the world.",
                content = """
                    Compounding is the process where the value of an investment increases because the earnings on an investment, both principal and interest, earn interest as time passes.
                    
                    Example: Invest $1,000 at 10% annually. Year 1 you earn $100. Year 2 you earn 10% on $1,100 ($110).
                    Over 30 years, this effect becomes massive.
                """.trimIndent()
            ),
            FinanceArticle(
                id = "gen_2",
                title = "Credit Score Basics",
                category = "Loans",
                description = "Why your financial reputation matters.",
                content = """
                    A credit score is a numerical representation of your creditworthiness.
                    
                    - Factors: Payment history, credit utilization, length of credit history.
                    - Benefits: Lower interest rates on loans, easier approval for credit cards and rentals.
                    - Maintenance: Pay bills on time and keep your credit usage low.
                """.trimIndent()
            )
        )
    }
}
