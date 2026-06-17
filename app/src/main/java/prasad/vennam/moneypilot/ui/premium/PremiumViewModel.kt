package prasad.vennam.moneypilot.ui.premium

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import prasad.vennam.moneypilot.billing.BillingManager
import prasad.vennam.moneypilot.data.UserPreferences
import javax.inject.Inject

@HiltViewModel
class PremiumViewModel
    @Inject
    constructor(
        private val billingManager: BillingManager,
        userPreferences: UserPreferences,
    ) : ViewModel() {
        val products: StateFlow<List<ProductDetails>> = billingManager.products
        val isPremium: StateFlow<Boolean> =
            userPreferences.isPremium.stateIn(
                scope = viewModelScope,
                started =
                    kotlinx.coroutines.flow.SharingStarted
                        .WhileSubscribed(5000),
                initialValue = false,
            )

        fun launchBillingFlow(
            activity: Activity,
            productDetails: ProductDetails,
        ) {
            billingManager.launchBillingFlow(activity, productDetails)
        }
    }
