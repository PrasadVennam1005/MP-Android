package prasad.vennam.moneypilot.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import prasad.vennam.moneypilot.R

val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold),
)

val Typography =
    Typography(
        displayLarge =
            TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 57.sp,
                lineHeight = 64.sp,
                letterSpacing = (-0.25).sp,
            ),
        displayMedium =
            TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 45.sp,
                lineHeight = 52.sp,
                letterSpacing = 0.sp,
            ),
        displaySmall =
            TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp,
                lineHeight = 44.sp,
                letterSpacing = 0.sp,
            ),
        headlineLarge =
            TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                lineHeight = 40.sp,
                letterSpacing = 0.sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                letterSpacing = 0.sp,
            ),
        headlineSmall =
            TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                letterSpacing = 0.sp,
            ),
        titleLarge =
            TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                letterSpacing = 0.sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.15.sp,
            ),
        titleSmall =
            TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.5.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.25.sp,
            ),
        bodySmall =
            TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.4.sp,
            ),
        labelLarge =
            TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
            ),
        labelMedium =
            TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp,
            ),
        labelSmall =
            TextStyle(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp,
            ),
    )

fun getScaledTypography(scaleFactor: Float): Typography {
    return Typography(
        displayLarge = Typography.displayLarge.copy(
            fontSize = (Typography.displayLarge.fontSize.value * scaleFactor).sp,
            lineHeight = (Typography.displayLarge.lineHeight.value * scaleFactor).sp
        ),
        displayMedium = Typography.displayMedium.copy(
            fontSize = (Typography.displayMedium.fontSize.value * scaleFactor).sp,
            lineHeight = (Typography.displayMedium.lineHeight.value * scaleFactor).sp
        ),
        displaySmall = Typography.displaySmall.copy(
            fontSize = (Typography.displaySmall.fontSize.value * scaleFactor).sp,
            lineHeight = (Typography.displaySmall.lineHeight.value * scaleFactor).sp
        ),
        headlineLarge = Typography.headlineLarge.copy(
            fontSize = (Typography.headlineLarge.fontSize.value * scaleFactor).sp,
            lineHeight = (Typography.headlineLarge.lineHeight.value * scaleFactor).sp
        ),
        headlineMedium = Typography.headlineMedium.copy(
            fontSize = (Typography.headlineMedium.fontSize.value * scaleFactor).sp,
            lineHeight = (Typography.headlineMedium.lineHeight.value * scaleFactor).sp
        ),
        headlineSmall = Typography.headlineSmall.copy(
            fontSize = (Typography.headlineSmall.fontSize.value * scaleFactor).sp,
            lineHeight = (Typography.headlineSmall.lineHeight.value * scaleFactor).sp
        ),
        titleLarge = Typography.titleLarge.copy(
            fontSize = (Typography.titleLarge.fontSize.value * scaleFactor).sp,
            lineHeight = (Typography.titleLarge.lineHeight.value * scaleFactor).sp
        ),
        titleMedium = Typography.titleMedium.copy(
            fontSize = (Typography.titleMedium.fontSize.value * scaleFactor).sp,
            lineHeight = (Typography.titleMedium.lineHeight.value * scaleFactor).sp
        ),
        titleSmall = Typography.titleSmall.copy(
            fontSize = (Typography.titleSmall.fontSize.value * scaleFactor).sp,
            lineHeight = (Typography.titleSmall.lineHeight.value * scaleFactor).sp
        ),
        bodyLarge = Typography.bodyLarge.copy(
            fontSize = (Typography.bodyLarge.fontSize.value * scaleFactor).sp,
            lineHeight = (Typography.bodyLarge.lineHeight.value * scaleFactor).sp
        ),
        bodyMedium = Typography.bodyMedium.copy(
            fontSize = (Typography.bodyMedium.fontSize.value * scaleFactor).sp,
            lineHeight = (Typography.bodyMedium.lineHeight.value * scaleFactor).sp
        ),
        bodySmall = Typography.bodySmall.copy(
            fontSize = (Typography.bodySmall.fontSize.value * scaleFactor).sp,
            lineHeight = (Typography.bodySmall.lineHeight.value * scaleFactor).sp
        ),
        labelLarge = Typography.labelLarge.copy(
            fontSize = (Typography.labelLarge.fontSize.value * scaleFactor).sp,
            lineHeight = (Typography.labelLarge.lineHeight.value * scaleFactor).sp
        ),
        labelMedium = Typography.labelMedium.copy(
            fontSize = (Typography.labelMedium.fontSize.value * scaleFactor).sp,
            lineHeight = (Typography.labelMedium.lineHeight.value * scaleFactor).sp
        ),
        labelSmall = Typography.labelSmall.copy(
            fontSize = (Typography.labelSmall.fontSize.value * scaleFactor).sp,
            lineHeight = (Typography.labelSmall.lineHeight.value * scaleFactor).sp
        )
    )
}

