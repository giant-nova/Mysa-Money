package com.giantnovadevs.mysamoney.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.giantnovadevs.mysamoney.billing.EntitlementRepository
import com.giantnovadevs.mysamoney.data.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = EntitlementRepository.getInstance(app)
    private val preferencesManager = PreferencesManager(app)

    /** Combined DataStore + billing Pro status. Eagerly started — .value is always current. */
    val isProUser: StateFlow<Boolean> = repo.isProUser

    val proProductPrice = repo.proProductPrice
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "...")

    val freeScansRemaining = preferencesManager.freeScansRemaining
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)

    fun launchPurchase(activity: Activity) = repo.launchPurchase(activity)

    fun useFreeScan() = viewModelScope.launch(Dispatchers.IO) {
        preferencesManager.decrementFreeScans()
    }
}
