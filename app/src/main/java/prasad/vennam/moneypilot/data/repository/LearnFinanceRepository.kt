package prasad.vennam.moneypilot.data.repository

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import prasad.vennam.moneypilot.data.model.ArticleQuiz
import prasad.vennam.moneypilot.data.model.FinanceArticle
import prasad.vennam.moneypilot.util.RemoteConfigHelper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LearnFinanceRepository @Inject constructor(
    private val remoteConfigHelper: RemoteConfigHelper,
    private val moshi: Moshi
) {

    private var cachedArticles: List<FinanceArticle> = articles

    fun getArticles(): Flow<List<FinanceArticle>> {
        return flow {
            val jsonString = remoteConfigHelper.getFinanceArticlesJson()
            val articlesList = if (jsonString.isNotBlank()) {
                try {
                    val type = Types.newParameterizedType(List::class.java, FinanceArticle::class.java)
                    val adapter = moshi.adapter<List<FinanceArticle>>(type)
                    adapter.fromJson(jsonString) ?: articles
                } catch (e: Exception) {
                    android.util.Log.e("LearnFinanceRepository", "Error parsing remote articles JSON", e)
                    articles
                }
            } else {
                articles
            }
            cachedArticles = articlesList
            emit(articlesList)
        }.flowOn(Dispatchers.IO)
    }

    fun getArticleById(id: String): FinanceArticle? {
        return cachedArticles.find { it.id == id }
    }

    fun getCategories(): List<String> = listOf(
        "Budgeting",
        "Savings",
        "Loans",
        "Investments",
        "Insurance",
        "Taxes",
        "Credit Score",
        "Retirement",
        "Agriculture",
        "Financial Planning"
    )

    companion object {
        private val articles = listOf(
            FinanceArticle(
                id = "gen_1",
                title = "Why Personal Finance Matters",
                category = "Financial Planning",
                subcategory = "Getting Started",
                description = "A quick primer on why managing money well changes your options in life.",
                content = "Personal finance is simply the set of decisions you make about earning, spending, saving, borrowing, and investing money, and how well you manage it directly shapes your options later in life, from buying a home to retiring comfortably. Unlike a fixed school subject, it's a practical skill best learned by doing: tracking your spending, automating savings, and gradually exploring concepts like investing as your confidence grows. This app is built to break those concepts into short, practical lessons you can apply immediately, regardless of where you're starting from.",
                level = "Beginner",
                readTimeMinutes = 3,
                featured = true,
                isPremium = false,
                tags = listOf("personal-finance", "basics"),
                relatedArticles = listOf("budget_1", "plan_1"),
                recommendedFor = listOf("new_user"),
                quiz = ArticleQuiz(enabled = false),
                publishedAt = "2026-06-16",
                lastUpdatedAt = "2026-06-16"
            ),
            FinanceArticle(
                id = "budget_1",
                title = "50/30/20 Rule",
                category = "Budgeting",
                subcategory = "Budget Basics",
                description = "The classic rule for simple budgeting.",
                content = "The 50/30/20 rule is a simple framework for dividing your monthly take-home income into three buckets: 50% for needs like rent, groceries, and utility bills, 30% for wants such as dining out, travel, and entertainment, and 20% for savings and debt repayment. It's not a strict law, but a starting point you can adjust based on your income level and financial goals. People with high fixed costs in expensive cities may need a different split, while those targeting early retirement might push more into the savings bucket.",
                level = "Beginner",
                readTimeMinutes = 4,
                featured = true,
                isPremium = false,
                tags = listOf("budget", "money-management"),
                relatedArticles = listOf("budget_2", "budget_3"),
                recommendedFor = listOf("new_user"),
                quiz = ArticleQuiz(enabled = false),
                publishedAt = "2026-06-16",
                lastUpdatedAt = "2026-06-16"
            ),
            FinanceArticle(
                id = "budget_2",
                title = "Zero-Based Budgeting",
                category = "Budgeting",
                subcategory = "Budgeting Methods",
                description = "A budgeting method where every rupee is given a job.",
                content = "Zero-based budgeting means every rupee of your income is assigned a job before the month begins, so income minus expenses, savings, and investments equals zero. Unlike the 50/30/20 rule, it doesn't use fixed percentages, instead you build your budget from scratch each month based on your actual bills and goals. This method works well if your income or expenses change often, since it forces you to plan deliberately rather than spend on autopilot.",
                level = "Intermediate",
                readTimeMinutes = 5,
                featured = false,
                isPremium = false,
                tags = listOf("budget", "zero-based", "planning"),
                relatedArticles = listOf("budget_1", "budget_3"),
                recommendedFor = listOf("new_user", "goal_setter"),
                quiz = ArticleQuiz(
                    enabled = true,
                    question = "What is the main idea behind zero-based budgeting?",
                    options = listOf(
                        "Spend first, save later",
                        "Every rupee is allocated a purpose",
                        "Save exactly 20% every month",
                        "Track expenses once a year"
                    ),
                    correctAnswer = 1,
                    explanation = "Zero-based budgeting assigns every rupee of income to a specific category until nothing is left unallocated."
                ),
                publishedAt = "2026-06-16",
                lastUpdatedAt = "2026-06-16"
            ),
            FinanceArticle(
                id = "budget_3",
                title = "Tracking Your Expenses",
                category = "Budgeting",
                subcategory = "Expense Tracking",
                description = "Why tracking spending is the foundation of every budget.",
                content = "You can't manage what you don't measure, tracking expenses is the foundation of every budgeting method. Start by recording every transaction for 30 days using a notebook, spreadsheet, or expense-tracking app, then group spends into categories like food, transport, and subscriptions. Reviewing this data monthly helps you spot leaks, such as forgotten subscriptions or impulse purchases, and gives you real numbers to plan future budgets around.",
                level = "Beginner",
                readTimeMinutes = 3,
                featured = false,
                isPremium = false,
                tags = listOf("expense-tracking", "budget"),
                relatedArticles = listOf("budget_1", "budget_2"),
                recommendedFor = listOf("new_user"),
                quiz = ArticleQuiz(enabled = false),
                publishedAt = "2026-06-16",
                lastUpdatedAt = "2026-06-16"
            ),
            FinanceArticle(
                id = "savings_1",
                title = "Emergency Fund Basics",
                category = "Savings",
                subcategory = "Emergency Fund",
                description = "Why you need a safety net before you start investing.",
                content = "An emergency fund is money set aside specifically for unexpected events like job loss, medical emergencies, or urgent repairs, so you don't have to rely on credit cards or loans. A common guideline is to save 3 to 6 months of essential expenses in an easily accessible account, such as a savings account or liquid mutual fund. Building this fund gradually, even with small monthly contributions, gives you a financial cushion and reduces stress when life throws a surprise at you.",
                level = "Beginner",
                readTimeMinutes = 4,
                featured = true,
                isPremium = false,
                tags = listOf("emergency-fund", "savings"),
                relatedArticles = listOf("savings_2"),
                recommendedFor = listOf("new_user"),
                quiz = ArticleQuiz(
                    enabled = true,
                    question = "How many months of expenses should an emergency fund typically cover?",
                    options = listOf(
                        "1 month",
                        "3 to 6 months",
                        "12 months",
                        "No fixed amount needed"
                    ),
                    correctAnswer = 1,
                    explanation = "Most financial planners recommend keeping 3 to 6 months of essential expenses as an emergency cushion."
                ),
                publishedAt = "2026-06-16",
                lastUpdatedAt = "2026-06-16"
            ),
            FinanceArticle(
                id = "savings_2",
                title = "High-Yield Savings Accounts",
                category = "Savings",
                subcategory = "Savings Accounts",
                description = "How to get more interest from your savings without losing liquidity.",
                content = "Not all savings accounts pay the same interest, high-yield savings accounts, often offered by small finance banks or digital-first banks, can pay noticeably more than traditional accounts while still keeping your money liquid and protected up to the insured limit. Before moving your emergency fund or short-term savings, compare interest rates, withdrawal limits, and the bank's deposit insurance coverage. Remember that interest earned is taxable, so factor that into your real returns.",
                level = "Intermediate",
                readTimeMinutes = 4,
                featured = false,
                isPremium = false,
                tags = listOf("savings", "interest-rate", "banking"),
                relatedArticles = listOf("savings_1"),
                recommendedFor = listOf("new_user"),
                quiz = ArticleQuiz(enabled = false),
                publishedAt = "2026-06-16",
                lastUpdatedAt = "2026-06-16"
            ),
            FinanceArticle(
                id = "loans_1",
                title = "Understanding Loan EMIs",
                category = "Loans",
                subcategory = "Loan Basics",
                description = "How EMIs are structured and why early payments are interest-heavy.",
                content = "An EMI, or Equated Monthly Installment, is the fixed payment you make each month toward a loan, made up of both principal and interest. In the early years of a long-term loan like a home loan, a larger portion of your EMI goes toward interest, and over time that shifts toward paying down the principal. Using an EMI calculator before borrowing helps you see the total interest you'll pay over the loan's life, not just the monthly amount.",
                level = "Beginner",
                readTimeMinutes = 4,
                featured = true,
                isPremium = false,
                tags = listOf("loans", "emi", "interest"),
                relatedArticles = listOf("loans_2"),
                recommendedFor = listOf("loan_seeker"),
                quiz = ArticleQuiz(
                    enabled = true,
                    question = "In the early years of a long-term loan, most of your EMI goes toward?",
                    options = listOf(
                        "Principal",
                        "Interest",
                        "Insurance",
                        "Processing fees"
                    ),
                    correctAnswer = 1,
                    explanation = "Early EMIs are interest-heavy; the principal portion grows as the loan matures."
                ),
                publishedAt = "2026-06-16",
                lastUpdatedAt = "2026-06-16"
            ),
            FinanceArticle(
                id = "loans_2",
                title = "Good Debt vs Bad Debt",
                category = "Loans",
                subcategory = "Debt Management",
                description = "Not all borrowing is equally harmful, here's how to tell the difference.",
                content = "Not all debt is equally harmful, good debt, like an education loan or home loan, typically funds something that can grow your income or net worth over time and usually carries a lower interest rate. Bad debt, like high-interest credit card balances used for discretionary spending, drains your finances without building any lasting value. The goal isn't to avoid debt entirely, but to borrow intentionally and pay off high-interest debt as quickly as possible.",
                level = "Beginner",
                readTimeMinutes = 3,
                featured = false,
                isPremium = false,
                tags = listOf("debt", "loans", "credit-card"),
                relatedArticles = listOf("loans_1", "credit_2"),
                recommendedFor = listOf("loan_seeker"),
                quiz = ArticleQuiz(enabled = false),
                publishedAt = "2026-06-16",
                lastUpdatedAt = "2026-06-16"
            ),
            FinanceArticle(
                id = "inv_1",
                title = "SIP Basics",
                category = "Investments",
                subcategory = "Mutual Funds",
                description = "Systematic Investment Plan explained.",
                content = "A Systematic Investment Plan, or SIP, allows you to invest a fixed amount regularly, typically monthly, into a mutual fund of your choice, rather than investing a lump sum all at once. This approach enforces saving discipline and takes advantage of rupee cost averaging, since you buy more units when prices are low and fewer when prices are high. Over long periods, SIPs also benefit from compounding, making them a popular starting point for new investors.",
                level = "Beginner",
                readTimeMinutes = 3,
                featured = true,
                isPremium = false,
                tags = listOf("sip", "mutual-fund", "investment", "compounding"),
                relatedArticles = listOf("gen_1", "inv_2"),
                recommendedFor = listOf("new_investor", "sip_user"),
                quiz = ArticleQuiz(
                    enabled = true,
                    question = "What is the biggest benefit of SIP investing?",
                    options = listOf(
                        "Tax saving",
                        "Rupee Cost Averaging",
                        "Loan eligibility",
                        "Fixed returns"
                    ),
                    correctAnswer = 1,
                    explanation = "SIP helps average purchase cost over time."
                ),
                publishedAt = "2026-06-16",
                lastUpdatedAt = "2026-06-16"
            ),
            FinanceArticle(
                id = "inv_2",
                title = "Asset Allocation Basics",
                category = "Investments",
                subcategory = "Portfolio Management",
                description = "How to divide your money across equity, debt, and gold.",
                content = "Asset allocation is how you divide your investments across categories like equity, debt, gold, and cash, based on your goals, time horizon, and risk tolerance. A young investor saving for a goal 20 years away can typically afford more equity exposure, while someone nearing retirement usually shifts toward safer debt instruments. Rebalancing your portfolio periodically, selling a bit of what's grown and buying more of what's lagged, keeps your risk level aligned with your original plan.",
                level = "Intermediate",
                readTimeMinutes = 5,
                featured = false,
                isPremium = true,
                tags = listOf("asset-allocation", "investment", "portfolio"),
                relatedArticles = listOf("inv_1", "plan_2"),
                recommendedFor = listOf("new_investor"),
                quiz = ArticleQuiz(enabled = false),
                publishedAt = "2026-06-16",
                lastUpdatedAt = "2026-06-16"
            ),
            FinanceArticle(
                id = "insurance_1",
                title = "Term vs Whole Life Insurance",
                category = "Insurance",
                subcategory = "Life Insurance",
                description = "Why most advisors recommend term insurance over endowment plans.",
                content = "Term insurance provides pure life cover for a fixed period at a low premium, paying out only if the policyholder dies during that term, with no maturity benefit otherwise. Whole life or endowment plans combine insurance with a savings component, costing significantly more but returning some money even if you survive the policy term. Most financial advisors recommend buying term insurance for protection and investing the premium difference separately, since mixing insurance and investment usually means weaker returns on the investment side.",
                level = "Beginner",
                readTimeMinutes = 4,
                featured = true,
                isPremium = false,
                tags = listOf("insurance", "term-plan", "life-insurance"),
                relatedArticles = listOf("insurance_2"),
                recommendedFor = listOf("new_user"),
                quiz = ArticleQuiz(
                    enabled = true,
                    question = "What is the main advantage of term insurance over whole life insurance?",
                    options = listOf(
                        "Higher returns",
                        "Lower premium for the same cover",
                        "Guaranteed maturity payout",
                        "Tax-free withdrawals"
                    ),
                    correctAnswer = 1,
                    explanation = "Term insurance offers pure life cover at a much lower premium since it has no investment component."
                ),
                publishedAt = "2026-06-16",
                lastUpdatedAt = "2026-06-16"
            ),
            FinanceArticle(
                id = "insurance_2",
                title = "Health Insurance Essentials",
                category = "Insurance",
                subcategory = "Health Insurance",
                description = "What to check before buying a health policy.",
                content = "A health insurance policy covers hospitalization and medical expenses, protecting your savings from being wiped out by a single medical emergency. When comparing policies, look beyond the premium at the sum insured, room rent limits, waiting periods for pre-existing conditions, and the insurer's claim settlement ratio. Even if your employer provides health cover, having an individual or family floater policy is wise, since employer cover usually ends the day you leave the job.",
                level = "Beginner",
                readTimeMinutes = 4,
                featured = false,
                isPremium = false,
                tags = listOf("insurance", "health-insurance"),
                relatedArticles = listOf("insurance_1"),
                recommendedFor = listOf("new_user"),
                quiz = ArticleQuiz(enabled = false),
                publishedAt = "2026-06-16",
                lastUpdatedAt = "2026-06-16"
            ),
            FinanceArticle(
                id = "tax_1",
                title = "Income Tax Basics",
                category = "Taxes",
                subcategory = "Tax Basics",
                description = "Old regime vs new regime, and how slab rates work.",
                content = "Income tax is calculated on your total taxable income after allowed deductions, using slab rates that increase as your income rises. Most taxpayers can choose between the old tax regime, which allows deductions like 80C and HRA, and the new tax regime, which offers lower slab rates but fewer deductions. Comparing both regimes using your actual income and eligible deductions each year helps you figure out which one results in lower tax outgo.",
                level = "Beginner",
                readTimeMinutes = 5,
                featured = true,
                isPremium = false,
                tags = listOf("tax", "income-tax"),
                relatedArticles = listOf("tax_2"),
                recommendedFor = listOf("tax_filer"),
                quiz = ArticleQuiz(
                    enabled = true,
                    question = "What mainly differentiates the old and new tax regimes in India?",
                    options = listOf(
                        "Old regime has lower rates but no deductions",
                        "New regime has lower rates but fewer deductions",
                        "Both offer identical deductions",
                        "New regime only applies to businesses"
                    ),
                    correctAnswer = 1,
                    explanation = "The new tax regime offers reduced slab rates in exchange for giving up most exemptions and deductions."
                ),
                publishedAt = "2026-06-16",
                lastUpdatedAt = "2026-06-16"
            ),
            FinanceArticle(
                id = "tax_2",
                title = "Tax-Saving Investments (80C)",
                category = "Taxes",
                subcategory = "Tax Saving",
                description = "Comparing ELSS, PPF, and other 80C options.",
                content = "Section 80C of the Income Tax Act lets you claim deductions up to a set annual limit by investing in instruments like ELSS mutual funds, PPF, life insurance premiums, and 5-year tax-saving fixed deposits. ELSS funds have the shortest lock-in among 80C options at three years and offer market-linked, potentially higher returns, while PPF is safer but locks your money for 15 years. Choosing where to invest under 80C should depend on your risk appetite and how soon you might need the money, not just the tax saving.",
                level = "Intermediate",
                readTimeMinutes = 5,
                featured = false,
                isPremium = true,
                tags = listOf("tax", "80c", "elss", "ppf"),
                relatedArticles = listOf("tax_1", "retire_2"),
                recommendedFor = listOf("tax_filer"),
                quiz = ArticleQuiz(enabled = false),
                publishedAt = "2026-06-16",
                lastUpdatedAt = "2026-06-16"
            ),
            FinanceArticle(
                id = "credit_1",
                title = "What is a Credit Score",
                category = "Credit Score",
                subcategory = "Credit Basics",
                description = "How the three-digit number that lenders care about is calculated.",
                content = "A credit score is a three-digit number, typically ranging from 300 to 900 in India, that reflects how reliably you've repaid debt in the past. Lenders use it to decide whether to approve loans and credit cards, and at what interest rate. Your score is influenced by factors like repayment history, credit utilization, length of credit history, and the number of recent loan inquiries.",
                level = "Beginner",
                readTimeMinutes = 3,
                featured = true,
                isPremium = false,
                tags = listOf("credit-score", "credit-report"),
                relatedArticles = listOf("credit_2"),
                recommendedFor = listOf("credit_builder"),
                quiz = ArticleQuiz(
                    enabled = true,
                    question = "What range does a typical credit score fall within in India?",
                    options = listOf(
                        "0 to 100",
                        "300 to 900",
                        "1 to 10",
                        "500 to 5000"
                    ),
                    correctAnswer = 1,
                    explanation = "Indian credit scores from bureaus like CIBIL typically range from 300 to 900."
                ),
                publishedAt = "2026-06-16",
                lastUpdatedAt = "2026-06-16"
            ),
            FinanceArticle(
                id = "credit_2",
                title = "How to Improve Your Credit Score",
                category = "Credit Score",
                subcategory = "Credit Improvement",
                description = "Practical habits that move your score in the right direction.",
                content = "Improving your credit score starts with paying every EMI and credit card bill on time, since payment history carries the most weight in most scoring models. Keeping your credit utilization, the percentage of your credit limit you actually use, below 30% also helps, as does avoiding multiple loan applications in a short span. Checking your credit report periodically for errors and disputing any incorrect entries can also prevent your score from being unfairly dragged down.",
                level = "Beginner",
                readTimeMinutes = 4,
                featured = false,
                isPremium = false,
                tags = listOf("credit-score", "credit-card"),
                relatedArticles = listOf("credit_1", "loans_2"),
                recommendedFor = listOf("credit_builder"),
                quiz = ArticleQuiz(enabled = false),
                publishedAt = "2026-06-16",
                lastUpdatedAt = "2026-06-16"
            ),
            FinanceArticle(
                id = "retire_1",
                title = "Retirement Planning Basics",
                category = "Retirement",
                subcategory = "Retirement Basics",
                description = "Why starting early matters more than the amount you invest.",
                content = "Retirement planning is about estimating how much money you'll need to maintain your lifestyle once your regular income stops, then working backward to figure out how much to save and invest today. Because inflation erodes purchasing power over decades, the amount you'll need at retirement is usually far higher than your current annual expenses suggest. Starting early matters more than the amount you invest, since compounding has more time to work in your favor.",
                level = "Beginner",
                readTimeMinutes = 4,
                featured = true,
                isPremium = false,
                tags = listOf("retirement", "planning"),
                relatedArticles = listOf("retire_2"),
                recommendedFor = listOf("retiree_planner"),
                quiz = ArticleQuiz(
                    enabled = true,
                    question = "Why does starting retirement savings early matter so much?",
                    options = listOf(
                        "Tax rates are lower for young people",
                        "Compounding has more time to grow your money",
                        "Inflation doesn't affect young savers",
                        "Retirement age changes for early savers"
                    ),
                    correctAnswer = 1,
                    explanation = "The earlier you start, the longer your money compounds, which has a bigger impact than the contribution amount alone."
                ),
                publishedAt = "2026-06-16",
                lastUpdatedAt = "2026-06-16"
            ),
            FinanceArticle(
                id = "retire_2",
                title = "NPS vs PPF",
                category = "Retirement",
                subcategory = "Retirement Schemes",
                description = "Comparing India's two popular long-term retirement savings options.",
                content = "The National Pension System (NPS) and Public Provident Fund (PPF) are both popular long-term retirement savings options, but they work differently. NPS invests in a mix of equity and debt, offers potentially higher returns, and requires you to use part of the corpus to buy an annuity at retirement, while PPF is a fully debt-based, government-backed scheme with a fixed 15-year tenure and tax-free returns. Many investors use both, PPF for guaranteed safety and NPS for growth and additional tax benefits under Section 80CCD.",
                level = "Intermediate",
                readTimeMinutes = 5,
                featured = false,
                isPremium = true,
                tags = listOf("nps", "ppf", "retirement"),
                relatedArticles = listOf("retire_1", "tax_2"),
                recommendedFor = listOf("retiree_planner"),
                quiz = ArticleQuiz(enabled = false),
                publishedAt = "2026-06-16",
                lastUpdatedAt = "2026-06-16"
            ),
            FinanceArticle(
                id = "agri_1",
                title = "Crop Insurance Basics",
                category = "Agriculture",
                subcategory = "Farm Insurance",
                description = "How crop insurance protects farmers from losses outside their control.",
                content = "Crop insurance protects farmers from financial loss due to crop failure caused by natural calamities, pests, or diseases, by compensating them based on the extent of the damage. In India, schemes like the Pradhan Mantri Fasal Bima Yojana subsidize premiums so that farmers pay only a small percentage of the total premium, with the government covering the rest. Enrolling before the crop season deadline and accurately reporting the area sown are essential for a claim to be processed smoothly.",
                level = "Beginner",
                readTimeMinutes = 4,
                featured = true,
                isPremium = false,
                tags = listOf("agriculture", "crop-insurance"),
                relatedArticles = listOf("agri_2"),
                recommendedFor = listOf("farmer"),
                quiz = ArticleQuiz(
                    enabled = true,
                    question = "What does crop insurance primarily protect against?",
                    options = listOf(
                        "Falling market prices only",
                        "Loss due to natural calamities and crop failure",
                        "Theft of farm equipment",
                        "Loan interest rate increases"
                    ),
                    correctAnswer = 1,
                    explanation = "Crop insurance compensates farmers for losses from events like floods, drought, pests, or disease."
                ),
                publishedAt = "2026-06-16",
                lastUpdatedAt = "2026-06-16"
            ),
            FinanceArticle(
                id = "agri_2",
                title = "Kisan Credit Card Explained",
                category = "Agriculture",
                subcategory = "Farm Credit",
                description = "How farmers can access flexible, subsidized credit for farming needs.",
                content = "The Kisan Credit Card (KCC) gives farmers easy access to short-term credit for crop production, equipment, and other farming needs, often at subsidized interest rates compared to regular loans. It works much like a credit card or overdraft, letting farmers withdraw funds as needed up to a sanctioned limit rather than taking a fixed lump-sum loan. Repaying within the stipulated period, usually tied to the harvest cycle, helps farmers retain the interest subsidy and avoid penal charges.",
                level = "Beginner",
                readTimeMinutes = 3,
                featured = false,
                isPremium = false,
                tags = listOf("agriculture", "kisan-credit-card", "loans"),
                relatedArticles = listOf("agri_1", "loans_1"),
                recommendedFor = listOf("farmer"),
                quiz = ArticleQuiz(enabled = false),
                publishedAt = "2026-06-16",
                lastUpdatedAt = "2026-06-16"
            ),
            FinanceArticle(
                id = "plan_1",
                title = "Setting Financial Goals",
                category = "Financial Planning",
                subcategory = "Goal Setting",
                description = "How to separate short-term, medium-term, and long-term goals.",
                content = "Good financial planning starts with clearly defined goals, separating short-term goals like a vacation, medium-term goals like a car or a child's school admission, and long-term goals like retirement or a child's higher education. Each goal should have a target amount, a timeline, and a dedicated investment or savings vehicle suited to that timeline, since money needed in two years shouldn't be invested the same way as money needed in twenty. Writing goals down and reviewing them yearly keeps your financial plan grounded in reality rather than vague intentions.",
                level = "Beginner",
                readTimeMinutes = 4,
                featured = true,
                isPremium = false,
                tags = listOf("financial-planning", "goals"),
                relatedArticles = listOf("plan_2"),
                recommendedFor = listOf("goal_setter"),
                quiz = ArticleQuiz(enabled = false),
                publishedAt = "2026-06-16",
                lastUpdatedAt = "2026-06-16"
            ),
            FinanceArticle(
                id = "plan_2",
                title = "Building a Financial Plan",
                category = "Financial Planning",
                subcategory = "Financial Strategy",
                description = "How budgeting, insurance, debt, and investing fit together.",
                content = "A complete financial plan ties together budgeting, an emergency fund, insurance, debt management, and investments into one coherent strategy aimed at your specific goals. The usual order of priority is to build a basic emergency fund first, get adequate insurance cover, pay down high-interest debt, and only then focus heavily on long-term investing, though in practice many of these happen in parallel. Revisiting your plan after major life events, like a new job, marriage, or having a child, keeps it relevant as your circumstances change.",
                level = "Intermediate",
                readTimeMinutes = 5,
                featured = false,
                isPremium = true,
                tags = listOf("financial-planning", "strategy"),
                relatedArticles = listOf("plan_1", "inv_2"),
                recommendedFor = listOf("goal_setter"),
                quiz = ArticleQuiz(
                    enabled = true,
                    question = "What's generally recommended before focusing heavily on long-term investing?",
                    options = listOf(
                        "Buying premium insurance only",
                        "Building an emergency fund and managing high-interest debt",
                        "Maximizing credit card limits",
                        "Investing only in gold"
                    ),
                    correctAnswer = 1,
                    explanation = "Most planners suggest securing a basic safety net and clearing high-interest debt before aggressive long-term investing."
                ),
                publishedAt = "2026-06-16",
                lastUpdatedAt = "2026-06-16"
            )
        )
    }
}
