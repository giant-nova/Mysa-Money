package com.giantnovadevs.mysamoney.config

object ProUserGate {
    /**
     * Central testing switch for Pro status.
     *
     * - `null`  -> use real entitlement flow (billing + saved status)
     * - `true`  -> force Pro enabled everywhere
     * - `false` -> force Pro disabled everywhere
     *
     * Change this one value when testing.
     */
    val overrideForTesting: Boolean? = false

    fun resolve(savedStatus: Boolean, billingStatus: Boolean): Boolean {
        return overrideForTesting ?: (savedStatus || billingStatus)
    }
}
