package com.nftpro

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.protocol.http.HttpService
import org.web3j.tx.gas.DefaultGasProvider
import org.web3j.utils.Convert
import java.math.BigDecimal
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Web3PaymentManager @Inject constructor(
    private val context: Context
) {
    
    companion object {
        // IL TUO WALLET ADDRESS 
        const val PAYMENT_WALLET = "0xC69088eB5F015Fca5B385b8E3A0463749813093e"
        
        // PREZZI IN USD (convertiti in crypto)
        const val BASIC_PRICE_USD = 49.90
        const val PRO_PRICE_USD = 99.90
        
        // Contract addresses per USDC/USDT
        const val USDC_ETH = "0xA0b86a33E6441Db14F41c40Acd6FB79A8d15E23C"
        const val USDT_ETH = "0xdAC17F958D2ee523a2206206994597C13D831ec7"
        
        const val USDC_POLYGON = "0x2791Bca1f2de4661ED88A30C99A7a9449Aa84174"  
        const val USDT_POLYGON = "0xc2132D05D31c914a87C6611C10748AEb04B58e8F"
        
        const val USDC_BSC = "0x8AC76a51cc950d9822D68b83fE1Ad97B32Cd580d"
        const val USDT_BSC = "0x55d398326f99059fF775485246999027B3197955"
    }
    
    private val _paymentState = MutableStateFlow(PaymentState())
    val paymentState: StateFlow<PaymentState> = _paymentState
    
    // Web3 instances per ogni chain
    private val ethereumWeb3j = Web3j.build(HttpService("https://mainnet.infura.io/v3/YOUR_INFURA_KEY"))
    private val polygonWeb3j = Web3j.build(HttpService("https://polygon-rpc.com"))
    private val bscWeb3j = Web3j.build(HttpService("https://bsc-dataseed.binance.org"))
    
    /**
     * Genera payment request per Basic tier (€49.90)
     */
    fun createBasicPaymentRequest(chain: String, token: String): PaymentRequest {
        return PaymentRequest(
            tier = "Basic",
            priceUSD = BASIC_PRICE_USD,
            recipientAddress = PAYMENT_WALLET,
            chain = chain,
            tokenAddress = getTokenAddress(chain, token),
            tokenSymbol = token,
            amount = convertUSDToTokenAmount(BASIC_PRICE_USD, token),
            validUntil = System.currentTimeMillis() + (15 * 60 * 1000) // 15 minuti
        )
    }
    
    /**
     * Genera payment request per Pro tier (€99.90)
     */  
    fun createProPaymentRequest(chain: String, token: String): PaymentRequest {
        return PaymentRequest(
            tier = "Pro", 
            priceUSD = PRO_PRICE_USD,
            recipientAddress = PAYMENT_WALLET,
            chain = chain,
            tokenAddress = getTokenAddress(chain, token),
            tokenSymbol = token,
            amount = convertUSDToTokenAmount(PRO_PRICE_USD, token),
            validUntil = System.currentTimeMillis() + (15 * 60 * 1000) // 15 minuti
        )
    }
    
    /**
     * Verifica se pagamento è stato completato on-chain
     */
    suspend fun verifyPayment(paymentRequest: PaymentRequest, txHash: String): Boolean {
        try {
            val web3j = getWeb3jForChain(paymentRequest.chain)
            val receipt = web3j.ethGetTransactionReceipt(txHash).send()
            
            if (receipt.transactionReceipt.isPresent) {
                val tx = receipt.transactionReceipt.get()
                
                // Verifica che la transazione sia succeeded
                val success = tx.status == "0x1"
                
                // Verifica recipient address
                val correctRecipient = tx.to.equals(PAYMENT_WALLET, ignoreCase = true)
                
                // Per token ERC-20, verifica nel transfer event
                val correctAmount = verifyTokenTransferAmount(tx, paymentRequest)
                
                return success && correctRecipient && correctAmount
            }
            
            return false
        } catch (e: Exception) {
            return false
        }
    }
    
    /**
     * Monitora wallet per incoming payments
     */
    suspend fun monitorIncomingPayments() {
        // Ethereum monitoring
        monitorChainPayments("Ethereum", ethereumWeb3j)
        
        // Polygon monitoring  
        monitorChainPayments("Polygon", polygonWeb3j)
        
        // BSC monitoring
        monitorChainPayments("BSC", bscWeb3j)
    }
    
    private suspend fun monitorChainPayments(chain: String, web3j: Web3j) {
        try {
            val latestBlock = web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, true).send()
            
            latestBlock.block.transactions.forEach { txResult ->
                val tx = txResult.get() as org.web3j.protocol.core.methods.response.Transaction
                
                // Controlla se è pagamento al nostro wallet
                if (tx.to.equals(PAYMENT_WALLET, ignoreCase = true)) {
                    val amount = Convert.fromWei(tx.value.toBigDecimal(), Convert.Unit.ETHER)
                    
                    // Verifica se l'importo corrisponde ai nostri prezzi
                    when {
                        isBasicPayment(amount.toDouble()) -> {
                            processSuccessfulPayment("Basic", chain, tx.hash)
                        }
                        isProPayment(amount.toDouble()) -> {
                            processSuccessfulPayment("Pro", chain, tx.hash)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Log error
        }
    }
    
    private fun processSuccessfulPayment(tier: String, chain: String, txHash: String) {
        _paymentState.value = _paymentState.value.copy(
            lastPayment = SuccessfulPayment(
                tier = tier,
                chain = chain,
                txHash = txHash,
                timestamp = System.currentTimeMillis()
            ),
            isPaidUser = true,
            currentTier = tier
        )
        
        // Save to local storage
        savePaymentToLocal(tier, chain, txHash)
    }
    
    private fun getTokenAddress(chain: String, token: String): String {
        return when (chain to token) {
            "Ethereum" to "USDC" -> USDC_ETH
            "Ethereum" to "USDT" -> USDT_ETH
            "Polygon" to "USDC" -> USDC_POLYGON
            "Polygon" to "USDT" -> USDT_POLYGON
            "BSC" to "USDC" -> USDC_BSC
            "BSC" to "USDT" -> USDT_BSC
            else -> ""
        }
    }
    
    private fun convertUSDToTokenAmount(usdPrice: Double, token: String): Double {
        // USDC/USDT hanno 6 decimali e sono pegged a USD
        // Quindi €49.90 = 49.90 USDC/USDT (assumendo parità EUR/USD)
        return usdPrice
    }
    
    private fun getWeb3jForChain(chain: String): Web3j {
        return when (chain) {
            "Ethereum" -> ethereumWeb3j
            "Polygon" -> polygonWeb3j
            "BSC" -> bscWeb3j
            else -> ethereumWeb3j
        }
    }
    
    private fun verifyTokenTransferAmount(
        receipt: TransactionReceipt, 
        paymentRequest: PaymentRequest
    ): Boolean {
        // Verifica i logs della transazione per Transfer events
        receipt.logs.forEach { log ->
            // Transfer event signature: Transfer(address,address,uint256)
            if (log.topics.size >= 3) {
                val to = "0x" + log.topics[2].substring(26) // Remove padding
                val amount = BigInteger(log.data.substring(2), 16)
                
                if (to.equals(PAYMENT_WALLET, ignoreCase = true)) {
                    val tokenAmount = amount.toDouble() / Math.pow(10.0, 6.0) // USDC/USDT have 6 decimals
                    return Math.abs(tokenAmount - paymentRequest.amount) < 0.01 // 1 cent tolerance
                }
            }
        }
        return false
    }
    
    private fun isBasicPayment(amount: Double): Boolean {
        return Math.abs(amount - BASIC_PRICE_USD) < 1.0 // €1 tolerance
    }
    
    private fun isProPayment(amount: Double): Boolean {
        return Math.abs(amount - PRO_PRICE_USD) < 2.0 // €2 tolerance
    }
    
    private fun savePaymentToLocal(tier: String, chain: String, txHash: String) {
        val prefs = context.getSharedPreferences("nft_pro_payments", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("payment_tier", tier)
            .putString("payment_chain", chain)
            .putString("payment_tx", txHash)
            .putLong("payment_time", System.currentTimeMillis())
            .putBoolean("is_paid", true)
            .apply()
    }
    
    fun loadPaymentStatus(): Boolean {
        val prefs = context.getSharedPreferences("nft_pro_payments", Context.MODE_PRIVATE)
        val isPaid = prefs.getBoolean("is_paid", false)
        val paymentTime = prefs.getLong("payment_time", 0)
        
        // Payment valid for 1 year
        val oneYear = 365L * 24 * 60 * 60 * 1000
        val isStillValid = System.currentTimeMillis() - paymentTime < oneYear
        
        val currentlyPaid = isPaid && isStillValid
        
        _paymentState.value = _paymentState.value.copy(
            isPaidUser = currentlyPaid,
            currentTier = if (currentlyPaid) prefs.getString("payment_tier", "Basic") ?: "Basic" else null
        )
        
        return currentlyPaid
    }
}

data class PaymentState(
    val isPaidUser: Boolean = false,
    val currentTier: String? = null,
    val isProcessingPayment: Boolean = false,
    val lastPayment: SuccessfulPayment? = null,
    val error: String? = null
)

data class PaymentRequest(
    val tier: String,
    val priceUSD: Double,
    val recipientAddress: String,
    val chain: String,
    val tokenAddress: String,
    val tokenSymbol: String,
    val amount: Double,
    val validUntil: Long
)

data class SuccessfulPayment(
    val tier: String,
    val chain: String, 
    val txHash: String,
    val timestamp: Long
)