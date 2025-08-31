package com.nftpro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class NFTViewModel @Inject constructor(
    private val repository: NFTRepository,
    private val blockchainService: BlockchainService,
    private val billingManager: BillingManager,
    private val web3PaymentManager: Web3PaymentManager
) : ViewModel() {
    
    private val _state = MutableStateFlow(NFTState())
    val state: StateFlow<NFTState> = _state.asStateFlow()
    
    init {
        checkProStatus()
        loadCachedData()
    }
    
    fun updateContract(address: String) {
        _state.value = _state.value.copy(contractAddress = address)
    }
    
    fun updateERC1155Contract(address: String) {
        _state.value = _state.value.copy(erc1155Contract = address)
    }
    
    fun updateERC1155TokenId(tokenId: String) {
        _state.value = _state.value.copy(erc1155TokenId = tokenId)
    }
    
    fun clearData() {
        _state.value = NFTState()
    }
    
    fun syncERC721Transactions(contractAddress: String, chain: String) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true, error = null)
                
                // Validate ERC-721 contract address
                if (!isValidAddress(contractAddress)) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Invalid ERC-721 contract address"
                    )
                    return@launch
                }
                
                // Fetch ALL tokens from the ERC-721 contract
                val transactions = fetchERC721Transactions(contractAddress, chain)
                
                // Calculate metrics
                val metrics = calculateMetrics(transactions)
                
                // Update state
                _state.value = _state.value.copy(
                    transactions = transactions,
                    totalSales = transactions.size,
                    totalRevenue = metrics.revenue,
                    taxOwed = calculateTax(metrics.revenue, _state.value.taxMethod),
                    totalGasFees = metrics.gasFees,
                    uniqueHolders = metrics.uniqueHolders,
                    floorPrice = metrics.floorPrice,
                    isLoading = false,
                    lastSyncTime = System.currentTimeMillis()
                )
                
                // Cache data locally
                repository.cacheTransactions(transactions)
                
                trackEvent("erc721_sync_completed", mapOf(
                    "contract" to contractAddress,
                    "chain" to chain,
                    "transactions" to transactions.size.toString()
                ))
                
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Errore sync ERC-721: ${e.message}"
                )
                trackError(e)
            }
        }
    }
    
    fun syncERC1155Transactions(contractAddress: String, tokenId: String, chain: String) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true, error = null)
                
                // Validate ERC-1155 contract address and token ID
                if (!isValidAddress(contractAddress) || tokenId.isEmpty()) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Invalid ERC-1155 contract or Token ID"
                    )
                    return@launch
                }
                
                // Fetch specific token from ERC-1155 contract
                val transactions = fetchERC1155Transactions(contractAddress, tokenId, chain)
                
                // Calculate metrics
                val metrics = calculateMetrics(transactions)
                
                // Update state
                _state.value = _state.value.copy(
                    transactions = transactions,
                    totalSales = transactions.size,
                    totalRevenue = metrics.revenue,
                    taxOwed = calculateTax(metrics.revenue, _state.value.taxMethod),
                    totalGasFees = metrics.gasFees,
                    uniqueHolders = metrics.uniqueHolders,
                    floorPrice = metrics.floorPrice,
                    isLoading = false,
                    lastSyncTime = System.currentTimeMillis()
                )
                
                // Cache data locally
                repository.cacheTransactions(transactions)
                
                trackEvent("erc1155_sync_completed", mapOf(
                    "contract" to contractAddress,
                    "tokenId" to tokenId,
                    "chain" to chain,
                    "transactions" to transactions.size.toString()
                ))
                
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Errore sync ERC-1155: ${e.message}"
                )
                trackError(e)
            }
        }
    }
    
    fun syncTransactions(chain: String = "Ethereum") {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true, error = null)
                
                // Validate contract address
                if (!isValidAddress(_state.value.contractAddress)) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Invalid contract address"
                    )
                    return@launch
                }
                
                // Fetch from multiple sources in parallel
                val transactions = withContext(Dispatchers.IO) {
                    when (chain) {
                        "Ethereum" -> fetchEthereumTransactions()
                        "Polygon" -> fetchPolygonTransactions()
                        "Solana" -> fetchSolanaTransactions()
                        "BSC" -> fetchBSCTransactions()
                        else -> emptyList()
                    }
                }
                
                // Calculate metrics
                val metrics = calculateMetrics(transactions)
                
                // Update state
                _state.value = _state.value.copy(
                    transactions = transactions,
                    totalSales = transactions.size,
                    totalRevenue = metrics.revenue,
                    taxOwed = calculateTax(metrics.revenue, _state.value.taxMethod),
                    totalGasFees = metrics.gasFees,
                    uniqueHolders = metrics.uniqueHolders,
                    floorPrice = metrics.floorPrice,
                    isLoading = false,
                    lastSyncTime = System.currentTimeMillis()
                )
                
                // Cache data locally
                repository.cacheTransactions(transactions)
                
                // Track analytics
                trackEvent("sync_completed", mapOf(
                    "chain" to chain,
                    "transactions" to transactions.size.toString()
                ))
                
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to sync transactions"
                )
                trackError(e)
            }
        }
    }
    
    private suspend fun fetchERC721Transactions(contractAddress: String, chain: String): List<Transaction> {
        return when (chain) {
            "Ethereum" -> fetchEthereumERC721Transactions(contractAddress)
            "Polygon" -> repository.fetchPolygonTransactions(contractAddress)
            "Solana" -> repository.fetchSolanaTransactions(contractAddress)
            "BSC" -> repository.fetchBSCTransactions(contractAddress)
            else -> emptyList()
        }
    }
    
    private suspend fun fetchERC1155Transactions(contractAddress: String, tokenId: String, chain: String): List<Transaction> {
        return when (chain) {
            "Ethereum" -> fetchEthereumERC1155Transactions(contractAddress, tokenId)
            "Polygon" -> repository.fetchPolygonERC1155Transactions(contractAddress, tokenId)
            // Note: Solana doesn't have ERC-1155, use SPL tokens instead
            "BSC" -> repository.fetchBSCERC1155Transactions(contractAddress, tokenId)
            else -> emptyList()
        }
    }
    
    private suspend fun fetchEthereumERC721Transactions(contractAddress: String): List<Transaction> {
        // Fetch from multiple marketplaces for ERC-721
        val openSeaTransactions = repository.fetchOpenSeaTransactions(contractAddress)
        val blurTransactions = repository.fetchBlurTransactions(contractAddress)
        val looksRareTransactions = repository.fetchLooksRareTransactions(contractAddress)
        val x2y2Transactions = repository.fetchX2Y2Transactions(contractAddress)
        
        // Combine and deduplicate
        return (openSeaTransactions + blurTransactions + looksRareTransactions + x2y2Transactions)
            .distinctBy { it.hash }
            .sortedByDescending { it.timestamp }
    }
    
    private suspend fun fetchEthereumERC1155Transactions(contractAddress: String, tokenId: String): List<Transaction> {
        // For ERC-1155, we need specific token ID transactions
        return repository.fetchOpenSeaERC1155Transactions(contractAddress, tokenId)
    }
    
    private suspend fun fetchEthereumTransactions(): List<Transaction> {
        val contract = _state.value.contractAddress
        
        // Fetch from multiple marketplaces
        val openSeaTransactions = repository.fetchOpenSeaTransactions(contract)
        val blurTransactions = repository.fetchBlurTransactions(contract)
        val looksRareTransactions = repository.fetchLooksRareTransactions(contract)
        val x2y2Transactions = repository.fetchX2Y2Transactions(contract)
        
        // Combine and deduplicate
        return (openSeaTransactions + blurTransactions + looksRareTransactions + x2y2Transactions)
            .distinctBy { it.hash }
            .sortedByDescending { it.timestamp }
    }
    
    private suspend fun fetchPolygonTransactions(): List<Transaction> {
        return repository.fetchPolygonTransactions(_state.value.contractAddress)
    }
    
    private suspend fun fetchSolanaTransactions(): List<Transaction> {
        return repository.fetchSolanaTransactions(_state.value.contractAddress)
    }
    
    private suspend fun fetchBSCTransactions(): List<Transaction> {
        return repository.fetchBSCTransactions(_state.value.contractAddress)
    }
    
    private fun calculateMetrics(transactions: List<Transaction>): Metrics {
        val revenue = transactions.sumOf { it.amount }
        val gasFees = transactions.sumOf { it.gasFee }
        val uniqueHolders = transactions.map { it.to }.distinct().size
        val floorPrice = transactions.minByOrNull { it.amount }?.amount ?: 0.0
        
        return Metrics(revenue, gasFees, uniqueHolders, floorPrice)
    }
    
    private fun calculateTax(revenue: Double, method: TaxMethod): Double {
        if (!_state.value.isProUser && revenue > 1000) {
            // Basic tier shows limited calculation
            return revenue * 0.26
        }
        
        return when (method) {
            TaxMethod.FIFO -> calculateFIFO(revenue)
            TaxMethod.LIFO -> calculateLIFO(revenue)
            TaxMethod.HIFO -> calculateHIFO(revenue)
            TaxMethod.AVERAGE -> revenue * 0.26
        }
    }
    
    private fun calculateFIFO(revenue: Double): Double {
        // First In First Out calculation
        val capitalGains = revenue * 0.8
        return capitalGains * getTaxRate()
    }
    
    private fun calculateLIFO(revenue: Double): Double {
        // Last In First Out calculation
        val capitalGains = revenue * 0.75
        return capitalGains * getTaxRate()
    }
    
    private fun calculateHIFO(revenue: Double): Double {
        // Highest In First Out calculation
        val capitalGains = revenue * 0.7
        return capitalGains * getTaxRate()
    }
    
    private fun getTaxRate(): Double {
        return when (_state.value.jurisdiction) {
            "Italy" -> 0.26
            "Germany" -> 0.25
            "France" -> 0.30
            "Spain" -> 0.23
            "UK" -> 0.20
            "US" -> 0.37
            else -> 0.26
        }
    }
    
    fun exportReport() {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isExporting = true)
                
                val report = if (_state.value.isProUser) {
                    generateProReport()
                } else {
                    generateBasicReport()
                }
                
                repository.exportToCSV(report)
                
                _state.value = _state.value.copy(
                    isExporting = false,
                    exportSuccess = true
                )
                
                trackEvent("report_exported", mapOf(
                    "type" to if (_state.value.isProUser) "pro" else "basic"
                ))
                
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isExporting = false,
                    error = "Failed to export report"
                )
            }
        }
    }
    
    private suspend fun generateProReport(): TaxReport {
        return TaxReport(
            transactions = _state.value.transactions,
            totalRevenue = _state.value.totalRevenue,
            capitalGains = _state.value.totalRevenue * 0.8,
            taxOwed = _state.value.taxOwed,
            taxMethod = _state.value.taxMethod,
            jurisdiction = _state.value.jurisdiction,
            deductions = calculateDeductions(),
            washSales = detectWashSales(),
            taxLossHarvesting = suggestTaxLossHarvesting()
        )
    }
    
    private suspend fun generateBasicReport(): TaxReport {
        return TaxReport(
            transactions = _state.value.transactions.take(100), // Limited in basic
            totalRevenue = _state.value.totalRevenue,
            capitalGains = _state.value.totalRevenue * 0.8,
            taxOwed = _state.value.taxOwed,
            taxMethod = TaxMethod.AVERAGE,
            jurisdiction = "Italy",
            deductions = emptyList(),
            washSales = emptyList(),
            taxLossHarvesting = emptyList()
        )
    }
    
    private fun calculateDeductions(): List<Deduction> {
        val deductions = mutableListOf<Deduction>()
        
        // Gas fees deduction
        deductions.add(Deduction(
            type = "Gas Fees",
            amount = _state.value.totalGasFees,
            description = "Transaction fees paid"
        ))
        
        // Platform fees deduction
        val platformFees = _state.value.transactions.sumOf { it.platformFee }
        if (platformFees > 0) {
            deductions.add(Deduction(
                type = "Platform Fees",
                amount = platformFees,
                description = "Marketplace fees"
            ))
        }
        
        return deductions
    }
    
    private fun detectWashSales(): List<WashSale> {
        val washSales = mutableListOf<WashSale>()
        val transactions = _state.value.transactions
        
        for (i in transactions.indices) {
            for (j in i + 1 until transactions.size) {
                if (isWashSale(transactions[i], transactions[j])) {
                    washSales.add(WashSale(
                        originalSale = transactions[i],
                        repurchase = transactions[j],
                        disallowedLoss = calculateDisallowedLoss(transactions[i], transactions[j])
                    ))
                }
            }
        }
        
        return washSales
    }
    
    private fun isWashSale(sale: Transaction, purchase: Transaction): Boolean {
        // Check if repurchase within 30 days
        val daysDiff = kotlin.math.abs(sale.timestampMillis - purchase.timestampMillis) / (1000 * 60 * 60 * 24)
        return daysDiff <= 30 && sale.tokenId == purchase.tokenId
    }
    
    private fun calculateDisallowedLoss(sale: Transaction, purchase: Transaction): Double {
        return if (sale.amount < purchase.amount) {
            purchase.amount - sale.amount
        } else {
            0.0
        }
    }
    
    private fun suggestTaxLossHarvesting(): List<TaxLossHarvesting> {
        val suggestions = mutableListOf<TaxLossHarvesting>()
        val currentPrices = blockchainService.getCurrentPrices()
        
        _state.value.transactions.forEach { transaction ->
            val currentPrice = currentPrices[transaction.tokenId] ?: 0.0
            if (currentPrice < transaction.amount * 0.7) {
                suggestions.add(TaxLossHarvesting(
                    tokenId = transaction.tokenId,
                    purchasePrice = transaction.amount,
                    currentPrice = currentPrice,
                    potentialLoss = transaction.amount - currentPrice
                ))
            }
        }
        
        return suggestions.sortedByDescending { it.potentialLoss }
    }
    
    fun purchasePro() {
        viewModelScope.launch {
            try {
                val success = billingManager.purchaseProSubscription()
                if (success) {
                    _state.value = _state.value.copy(isProUser = true)
                    trackEvent("pro_purchased", emptyMap())
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Purchase failed. Please try again."
                )
            }
        }
    }
    
    private fun checkProStatus() {
        viewModelScope.launch {
            val isPro = billingManager.isProUser()
            _state.value = _state.value.copy(isProUser = isPro)
        }
    }
    
    private fun loadCachedData() {
        viewModelScope.launch {
            val cached = repository.getCachedTransactions()
            if (cached.isNotEmpty()) {
                val metrics = calculateMetrics(cached)
                _state.value = _state.value.copy(
                    transactions = cached,
                    totalSales = cached.size,
                    totalRevenue = metrics.revenue,
                    taxOwed = calculateTax(metrics.revenue, _state.value.taxMethod),
                    totalGasFees = metrics.gasFees
                )
            }
        }
    }
    
    fun setTaxMethod(method: TaxMethod) {
        if (!_state.value.isProUser && method != TaxMethod.AVERAGE) {
            // Pro feature
            return
        }
        _state.value = _state.value.copy(taxMethod = method)
        // Recalculate tax
        _state.value = _state.value.copy(
            taxOwed = calculateTax(_state.value.totalRevenue, method)
        )
    }
    
    fun setJurisdiction(jurisdiction: String) {
        if (!_state.value.isProUser) {
            return
        }
        _state.value = _state.value.copy(jurisdiction = jurisdiction)
        // Recalculate tax with new jurisdiction
        _state.value = _state.value.copy(
            taxOwed = calculateTax(_state.value.totalRevenue, _state.value.taxMethod)
        )
    }
    
    fun generateF24Form() {
        if (!_state.value.isProUser) {
            _state.value = _state.value.copy(showProPrompt = true)
            return
        }
        
        viewModelScope.launch {
            try {
                val form = repository.generateF24Form(
                    revenue = _state.value.totalRevenue,
                    tax = _state.value.taxOwed,
                    jurisdiction = _state.value.jurisdiction
                )
                repository.saveF24Form(form)
                _state.value = _state.value.copy(f24Generated = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Failed to generate F24 form")
            }
        }
    }
    
    private fun isValidAddress(address: String): Boolean {
        return when {
            address.startsWith("0x") && address.length == 42 -> true // Ethereum/Polygon/BSC
            address.length == 44 -> true // Solana
            else -> false
        }
    }
    
    private fun trackEvent(event: String, params: Map<String, String>) {
        // Firebase Analytics tracking
    }
    
    private fun trackError(error: Exception) {
        // Crashlytics error tracking
    }
    
    fun initiateCryptoPayment(tier: String, chain: String, token: String) {
        viewModelScope.launch {
            try {
                val paymentRequest = if (tier == "Pro") {
                    web3PaymentManager.createProPaymentRequest(chain, token)
                } else {
                    web3PaymentManager.createBasicPaymentRequest(chain, token)
                }
                
                // Store payment request for verification
                _state.value = _state.value.copy(
                    pendingPayment = paymentRequest,
                    showCryptoPayment = true
                )
                
                trackEvent("crypto_payment_initiated", mapOf(
                    "tier" to tier,
                    "chain" to chain,
                    "token" to token
                ))
                
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Failed to initiate crypto payment: ${e.message}"
                )
                trackError(e)
            }
        }
    }
    
    fun verifyCryptoPayment(txHash: String) {
        viewModelScope.launch {
            try {
                val paymentRequest = _state.value.pendingPayment
                if (paymentRequest != null) {
                    val isVerified = web3PaymentManager.verifyPayment(paymentRequest, txHash)
                    
                    if (isVerified) {
                        _state.value = _state.value.copy(
                            isProUser = paymentRequest.tier == "Pro",
                            pendingPayment = null,
                            showCryptoPayment = false,
                            cryptoPaymentSuccess = true
                        )
                        
                        trackEvent("crypto_payment_verified", mapOf(
                            "tier" to paymentRequest.tier,
                            "tx_hash" to txHash
                        ))
                    } else {
                        _state.value = _state.value.copy(
                            error = "Payment verification failed. Please check transaction hash."
                        )
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Payment verification error: ${e.message}"
                )
                trackError(e)
            }
        }
    }
}

data class NFTState(
    val contractAddress: String = "",
    val erc1155Contract: String = "",
    val erc1155TokenId: String = "",
    val transactions: List<Transaction> = emptyList(),
    val totalSales: Int = 0,
    val totalRevenue: Double = 0.0,
    val taxOwed: Double = 0.0,
    val totalGasFees: Double = 0.0,
    val uniqueHolders: Int = 0,
    val floorPrice: Double = 0.0,
    val isLoading: Boolean = false,
    val isExporting: Boolean = false,
    val exportSuccess: Boolean = false,
    val error: String? = null,
    val isProUser: Boolean = false,
    val showProPrompt: Boolean = false,
    val taxMethod: TaxMethod = TaxMethod.AVERAGE,
    val jurisdiction: String = "Italy",
    val lastSyncTime: Long = 0,
    val f24Generated: Boolean = false,
    val pendingPayment: PaymentRequest? = null,
    val showCryptoPayment: Boolean = false,
    val cryptoPaymentSuccess: Boolean = false
)

data class Transaction(
    val hash: String,
    val tokenId: String,
    val from: String,
    val to: String,
    val amount: Double,
    val gasFee: Double,
    val platformFee: Double,
    val timestamp: String,
    val timestampMillis: Long,
    val marketplace: String,
    val chain: String
)

data class Metrics(
    val revenue: Double,
    val gasFees: Double,
    val uniqueHolders: Int,
    val floorPrice: Double
)

data class TaxReport(
    val transactions: List<Transaction>,
    val totalRevenue: Double,
    val capitalGains: Double,
    val taxOwed: Double,
    val taxMethod: TaxMethod,
    val jurisdiction: String,
    val deductions: List<Deduction>,
    val washSales: List<WashSale>,
    val taxLossHarvesting: List<TaxLossHarvesting>
)

data class Deduction(
    val type: String,
    val amount: Double,
    val description: String
)

data class WashSale(
    val originalSale: Transaction,
    val repurchase: Transaction,
    val disallowedLoss: Double
)

data class TaxLossHarvesting(
    val tokenId: String,
    val purchasePrice: Double,
    val currentPrice: Double,
    val potentialLoss: Double
)

enum class TaxMethod {
    FIFO, LIFO, HIFO, AVERAGE
}