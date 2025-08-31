package com.nftpro

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
// RevenueCat removed - using only crypto payments + Google Play backup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingManager @Inject constructor(
    private val context: Context
) : PurchasesUpdatedListener {
    
    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()
    
    private val _proStatus = MutableStateFlow(false)
    val proStatus: StateFlow<Boolean> = _proStatus
    
    init {
        initializeBilling()
        // RevenueCat removed - crypto payments managed by Web3PaymentManager
    }
    
    private fun initializeBilling() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryPurchases()
                }
            }
            
            override fun onBillingServiceDisconnected() {
                // Retry connection
            }
        })
    }
    
    // RevenueCat completely removed - using Web3PaymentManager for crypto payments
    
    suspend fun purchaseProSubscription(): Boolean {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("nft_pro_annual")
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()
        
        val productDetailsResult = billingClient.queryProductDetails(params)
        
        productDetailsResult.productDetailsList?.firstOrNull()?.let { productDetails ->
            val productDetailsParamsList = listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .build()
            )
            
            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()
            
            val billingResult = billingClient.launchBillingFlow(
                context as Activity,
                billingFlowParams
            )
            
            return billingResult.responseCode == BillingClient.BillingResponseCode.OK
        }
        
        return false
    }
    
    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            purchases?.forEach { purchase ->
                handlePurchase(purchase)
            }
        }
    }
    
    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        _proStatus.value = true
                    }
                }
            } else {
                _proStatus.value = true
            }
        }
    }
    
    private fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        
        billingClient.queryPurchasesAsync(params) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _proStatus.value = purchasesList.any { 
                    it.products.contains("nft_pro_annual") && 
                    it.purchaseState == Purchase.PurchaseState.PURCHASED 
                }
            }
        }
    }
    
    suspend fun isProUser(): Boolean = _proStatus.value
}