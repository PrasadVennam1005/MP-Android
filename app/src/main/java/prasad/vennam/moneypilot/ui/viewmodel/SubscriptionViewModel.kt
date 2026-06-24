package prasad.vennam.moneypilot.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import prasad.vennam.moneypilot.data.entity.Category
import prasad.vennam.moneypilot.data.entity.Subscription
import prasad.vennam.moneypilot.data.repository.MoneyPilotRepository
import javax.inject.Inject

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val repository: MoneyPilotRepository
) : ViewModel() {

    val allSubscriptions: StateFlow<List<Subscription>> =
        repository.allSubscriptions
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<Category>> =
        repository.allCategories
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveSubscription(subscription: Subscription) {
        viewModelScope.launch {
            try {
                if (subscription.id == 0L) {
                    repository.insertSubscription(subscription)
                } else {
                    repository.updateSubscription(subscription)
                }
            } catch (e: Exception) {
                android.util.Log.e("SubscriptionViewModel", "Error saving subscription", e)
            }
        }
    }

    fun deleteSubscription(subscription: Subscription) {
        viewModelScope.launch {
            try {
                repository.deleteSubscription(subscription)
            } catch (e: Exception) {
                android.util.Log.e("SubscriptionViewModel", "Error deleting subscription", e)
            }
        }
    }
}
