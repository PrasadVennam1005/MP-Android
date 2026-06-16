# MoneyPilot Production Readiness Checklist

## 1. Security & Data Integrity
- [ ] **R8/ProGuard Verification**: Run `./gradlew assembleRelease` and verify the app doesn't crash. Ensure Room and Hilt rules are intact.
- [ ] **Database Hardening**: Verify `SecureStorageHelper` retrieves the SQLCipher key. Test that the database is unreadable without the Keystore entry.
- [ ] **Migration Integrity**: Test upgrade from schema version 9 to 10. Ensure no data loss in existing loans.
- [ ] **Secrets Management**: Verify `GOOGLE_CLIENT_ID` and API keys are not in version control.

## 2. Loan Intelligence Sanity Check
- [ ] **Boundary Calculations**: Test `LoanIntelligenceUtil` with 0% interest and high principal values.
- [ ] **Payment Logic**: Verify recording "Extra Payment" correctly updates the "Estimated Payoff Date".
- [ ] **Loan Completion**: Verify a loan marks as 100% paid when the balance reaches zero.

## 3. AI & Performance
- [ ] **Model Download**: Confirm the 1.5GB LLM download handles network interruptions gracefully.
- [ ] **Privacy Check**: Verify AI works in Flight Mode (offline processing).
- [ ] **Memory Usage**: Monitor for memory leaks or crashes during long AI chat sessions.

## 4. Infrastructure & Integration
- [ ] **Release SHA-1**: Ensure the production keystore SHA-1 is added to Firebase/Google Cloud Console.
- [ ] **Google OAuth**: Set the GCP project to "In Production" to prevent token expiry.
- [ ] **Google Sheets**: Verify sync works with large datasets (>1000 transactions).

## 5. Store Preparation
- [ ] **Versioning**: Update `versionCode` and `versionName` in `libs.versions.toml`.
- [ ] **Assets**: Review `playstore_assets/` for correct aspect ratios and safe-zones.
- [ ] **Privacy Policy**: Ensure the legal screen correctly states that data stays on-device or in the user's private Google Drive.

## 6. Final Smoke Test
- [ ] **Golden Path**: Fresh Install -> Google Sign-In -> Add Expense -> Add Loan -> Record EMI -> Verify Dashboard charts.
