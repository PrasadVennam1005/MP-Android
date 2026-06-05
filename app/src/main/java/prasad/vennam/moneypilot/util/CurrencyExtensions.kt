package prasad.vennam.moneypilot.util

/**
 * Converts a Long representing the smallest currency unit (e.g., paisa, cents)
 * back to a Double representation for UI display (e.g., Rupees, Dollars).
 */
val Long.inRupees: Double get() = this / 100.0

/**
 * Converts a Double representing a major currency unit (e.g., Rupees, Dollars)
 * into a Long representing the smallest currency unit (e.g., paisa, cents) for precise storage.
 */
val Double.inPaisa: Long get() = (this * 100).toLong()
