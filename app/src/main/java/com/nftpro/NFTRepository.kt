package com.nftpro

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NFTRepository @Inject constructor(
    private val context: Context,
    private val database: NFTDatabase,
    private val apiService: BlockchainApiService
) {
    
    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("X-API-Key", getApiKey())
                .build()
            chain.proceed(request)
        }
        .build()
    
    private val openSeaApi = Retrofit.Builder()
        .baseUrl("https://api.opensea.io/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OpenSeaApi::class.java)
    
    suspend fun fetchOpenSeaTransactions(contractAddress: String): List<Transaction> {
        return withContext(Dispatchers.IO) {
            try {
                val response = openSeaApi.getEvents(
                    assetContractAddress = contractAddress,
                    eventType = "successful",
                    limit = 300
                )
                
                response.assetEvents.map { event ->
                    Transaction(
                        hash = event.transaction?.transactionHash ?: "",
                        tokenId = event.asset?.tokenId ?: "",
                        from = event.seller?.address ?: "",
                        to = event.winner?.address ?: "",
                        amount = event.totalPrice?.toDoubleOrNull() ?: 0.0 / 1e18,
                        gasFee = calculateGasFee(event.transaction),
                        platformFee = event.totalPrice?.toDoubleOrNull() ?: 0.0 * 0.025,
                        timestamp = formatTimestamp(event.createdDate),
                        timestampMillis = parseTimestamp(event.createdDate),
                        marketplace = "OpenSea",
                        chain = "Ethereum"
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    suspend fun fetchBlurTransactions(contractAddress: String): List<Transaction> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://api.blur.io/v1/collections/$contractAddress/activity"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Accept", "application/json")
                    .build()
                
                val response = client.newCall(request).execute()
                val json = JSONObject(response.body?.string() ?: "{}")
                val activities = json.getJSONArray("activities")
                
                val transactions = mutableListOf<Transaction>()
                for (i in 0 until activities.length()) {
                    val activity = activities.getJSONObject(i)
                    if (activity.getString("type") == "sale") {
                        transactions.add(Transaction(
                            hash = activity.getString("txHash"),
                            tokenId = activity.getString("tokenId"),
                            from = activity.getString("fromAddress"),
                            to = activity.getString("toAddress"),
                            amount = activity.getDouble("price"),
                            gasFee = activity.optDouble("gasFee", 0.0),
                            platformFee = activity.getDouble("price") * 0.005,
                            timestamp = formatTimestamp(activity.getLong("timestamp")),
                            timestampMillis = activity.getLong("timestamp"),
                            marketplace = "Blur",
                            chain = "Ethereum"
                        ))
                    }
                }
                transactions
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    suspend fun fetchLooksRareTransactions(contractAddress: String): List<Transaction> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://api.looksrare.org/api/v1/events?collection=$contractAddress&type=SALE"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Accept", "application/json")
                    .build()
                
                val response = client.newCall(request).execute()
                val json = JSONObject(response.body?.string() ?: "{}")
                val data = json.getJSONArray("data")
                
                val transactions = mutableListOf<Transaction>()
                for (i in 0 until data.length()) {
                    val event = data.getJSONObject(i)
                    transactions.add(Transaction(
                        hash = event.getString("hash"),
                        tokenId = event.getString("tokenId"),
                        from = event.getJSONObject("from").getString("address"),
                        to = event.getJSONObject("to").getString("address"),
                        amount = event.getJSONObject("order").getDouble("price") / 1e18,
                        gasFee = event.optDouble("gasUsed", 0.0) * event.optDouble("gasPrice", 0.0) / 1e18,
                        platformFee = event.getJSONObject("order").getDouble("price") / 1e18 * 0.02,
                        timestamp = formatTimestamp(event.getLong("createdAt")),
                        timestampMillis = event.getLong("createdAt"),
                        marketplace = "LooksRare",
                        chain = "Ethereum"
                    ))
                }
                transactions
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    suspend fun fetchX2Y2Transactions(contractAddress: String): List<Transaction> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://api.x2y2.org/v1/events?contract=$contractAddress&type=sale"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("X-API-KEY", "x2y2_api_key")
                    .build()
                
                val response = client.newCall(request).execute()
                val json = JSONObject(response.body?.string() ?: "{}")
                val events = json.getJSONArray("data")
                
                val transactions = mutableListOf<Transaction>()
                for (i in 0 until events.length()) {
                    val event = events.getJSONObject(i)
                    transactions.add(Transaction(
                        hash = event.getString("tx_hash"),
                        tokenId = event.getString("token_id"),
                        from = event.getString("seller"),
                        to = event.getString("buyer"),
                        amount = event.getDouble("price") / 1e18,
                        gasFee = event.optDouble("gas_fee", 0.0) / 1e18,
                        platformFee = event.getDouble("price") / 1e18 * 0.005,
                        timestamp = formatTimestamp(event.getLong("block_time")),
                        timestampMillis = event.getLong("block_time") * 1000,
                        marketplace = "X2Y2",
                        chain = "Ethereum"
                    ))
                }
                transactions
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    suspend fun fetchPolygonTransactions(contractAddress: String): List<Transaction> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://api.polygonscan.com/api?module=account&action=tokennfttx&contractaddress=$contractAddress&apikey=${getPolygonApiKey()}"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val json = JSONObject(response.body?.string() ?: "{}")
                val result = json.getJSONArray("result")
                
                val transactions = mutableListOf<Transaction>()
                for (i in 0 until result.length()) {
                    val tx = result.getJSONObject(i)
                    transactions.add(Transaction(
                        hash = tx.getString("hash"),
                        tokenId = tx.getString("tokenID"),
                        from = tx.getString("from"),
                        to = tx.getString("to"),
                        amount = tx.optDouble("value", 0.0) / 1e18,
                        gasFee = tx.optDouble("gasUsed", 0.0) * tx.optDouble("gasPrice", 0.0) / 1e18,
                        platformFee = 0.0,
                        timestamp = formatTimestamp(tx.getLong("timeStamp")),
                        timestampMillis = tx.getLong("timeStamp") * 1000,
                        marketplace = "Polygon",
                        chain = "Polygon"
                    ))
                }
                transactions
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    suspend fun fetchSolanaTransactions(contractAddress: String): List<Transaction> {
        return withContext(Dispatchers.IO) {
            try {
                // MagicEden Solana API
                val url = "https://api-mainnet.magiceden.dev/v2/collections/$contractAddress/activities"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Accept", "application/json")
                    .build()
                
                val response = client.newCall(request).execute()
                val json = JSONObject(response.body?.string() ?: "{}")
                val activities = json.getJSONArray("activities")
                
                val transactions = mutableListOf<Transaction>()
                for (i in 0 until activities.length()) {
                    val activity = activities.getJSONObject(i)
                    if (activity.getString("type") == "buyNow") {
                        transactions.add(Transaction(
                            hash = activity.getString("signature"),
                            tokenId = activity.getString("tokenMint"),
                            from = activity.getString("seller"),
                            to = activity.getString("buyer"),
                            amount = activity.getDouble("price"),
                            gasFee = 0.00025, // Solana typical fee
                            platformFee = activity.getDouble("price") * 0.02,
                            timestamp = formatTimestamp(activity.getLong("blockTime")),
                            timestampMillis = activity.getLong("blockTime") * 1000,
                            marketplace = "MagicEden",
                            chain = "Solana"
                        ))
                    }
                }
                transactions
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    suspend fun fetchBSCTransactions(contractAddress: String): List<Transaction> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://api.bscscan.com/api?module=account&action=tokennfttx&contractaddress=$contractAddress&apikey=${getBSCApiKey()}"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val json = JSONObject(response.body?.string() ?: "{}")
                val result = json.getJSONArray("result")
                
                val transactions = mutableListOf<Transaction>()
                for (i in 0 until result.length()) {
                    val tx = result.getJSONObject(i)
                    transactions.add(Transaction(
                        hash = tx.getString("hash"),
                        tokenId = tx.getString("tokenID"),
                        from = tx.getString("from"),
                        to = tx.getString("to"),
                        amount = tx.optDouble("value", 0.0) / 1e18,
                        gasFee = tx.optDouble("gasUsed", 0.0) * tx.optDouble("gasPrice", 0.0) / 1e18,
                        platformFee = 0.0,
                        timestamp = formatTimestamp(tx.getLong("timeStamp")),
                        timestampMillis = tx.getLong("timeStamp") * 1000,
                        marketplace = "BSC",
                        chain = "BSC"
                    ))
                }
                transactions
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    suspend fun cacheTransactions(transactions: List<Transaction>) {
        withContext(Dispatchers.IO) {
            database.transactionDao().insertAll(
                transactions.map { it.toEntity() }
            )
        }
    }
    
    suspend fun getCachedTransactions(): List<Transaction> {
        return withContext(Dispatchers.IO) {
            database.transactionDao().getAllTransactions().map { it.toTransaction() }
        }
    }
    
    suspend fun exportToCSV(report: TaxReport) {
        withContext(Dispatchers.IO) {
            val csvContent = buildString {
                // Header
                appendLine("Date,Token ID,From,To,Amount,Gas Fee,Platform Fee,Marketplace,Chain,Tax Owed")
                
                // Transactions
                report.transactions.forEach { tx ->
                    appendLine("${tx.timestamp},${tx.tokenId},${tx.from},${tx.to},${tx.amount},${tx.gasFee},${tx.platformFee},${tx.marketplace},${tx.chain},${report.taxOwed}")
                }
                
                // Summary
                appendLine()
                appendLine("Summary")
                appendLine("Total Revenue,${report.totalRevenue}")
                appendLine("Capital Gains,${report.capitalGains}")
                appendLine("Tax Owed,${report.taxOwed}")
                appendLine("Tax Method,${report.taxMethod}")
                appendLine("Jurisdiction,${report.jurisdiction}")
                
                // Deductions
                if (report.deductions.isNotEmpty()) {
                    appendLine()
                    appendLine("Deductions")
                    report.deductions.forEach { deduction ->
                        appendLine("${deduction.type},${deduction.amount},${deduction.description}")
                    }
                }
                
                // Wash Sales
                if (report.washSales.isNotEmpty()) {
                    appendLine()
                    appendLine("Wash Sales Detected")
                    report.washSales.forEach { wash ->
                        appendLine("Original: ${wash.originalSale.tokenId},Repurchase: ${wash.repurchase.tokenId},Disallowed Loss: ${wash.disallowedLoss}")
                    }
                }
            }
            
            val fileName = "nft_tax_report_${System.currentTimeMillis()}.csv"
            val file = File(context.getExternalFilesDir(null), fileName)
            file.writeText(csvContent)
        }
    }
    
    suspend fun generateF24Form(revenue: Double, tax: Double, jurisdiction: String): F24Form {
        return withContext(Dispatchers.IO) {
            F24Form(
                codiceFiscale = "", // User needs to input
                periodo = getCurrentTaxPeriod(),
                imponibile = revenue,
                imposta = tax,
                codiceEntrata = "1040", // Capital gains code
                dataVersamento = getPaymentDeadline()
            )
        }
    }
    
    suspend fun saveF24Form(form: F24Form) {
        withContext(Dispatchers.IO) {
            val content = generateF24Content(form)
            val file = File(context.getExternalFilesDir(null), "F24_${System.currentTimeMillis()}.pdf")
            // Generate PDF using iText or similar library
        }
    }
    
    private fun calculateGasFee(transaction: TransactionInfo?): Double {
        return transaction?.gasUsed?.toDoubleOrNull() ?: 0.0 * 
               transaction?.gasPrice?.toDoubleOrNull() ?: 0.0 / 1e18
    }
    
    private fun formatTimestamp(timestamp: Any): String {
        val date = when (timestamp) {
            is Long -> Date(timestamp * 1000)
            is String -> SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(timestamp)
            else -> Date()
        }
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(date ?: Date())
    }
    
    private fun parseTimestamp(timestamp: Any): Long {
        return when (timestamp) {
            is Long -> timestamp * 1000
            is String -> SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(timestamp)?.time ?: 0
            else -> 0
        }
    }
    
    private fun getApiKey(): String = "your_api_key"
    private fun getPolygonApiKey(): String = "polygon_api_key"
    private fun getBSCApiKey(): String = "bsc_api_key"
    
    private fun getCurrentTaxPeriod(): String {
        val year = Calendar.getInstance().get(Calendar.YEAR)
        return "$year"
    }
    
    private fun getPaymentDeadline(): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, Calendar.JUNE)
        cal.set(Calendar.DAY_OF_MONTH, 30)
        return SimpleDateFormat("dd/MM/yyyy", Locale.ITALY).format(cal.time)
    }
    
    private fun generateF24Content(form: F24Form): String {
        return """
            MODELLO F24
            Codice Fiscale: ${form.codiceFiscale}
            Periodo: ${form.periodo}
            Imponibile: €${form.imponibile}
            Imposta: €${form.imposta}
            Codice Entrata: ${form.codiceEntrata}
            Data Versamento: ${form.dataVersamento}
        """.trimIndent()
    }
}

data class F24Form(
    val codiceFiscale: String,
    val periodo: String,
    val imponibile: Double,
    val imposta: Double,
    val codiceEntrata: String,
    val dataVersamento: String
)

// Extension functions
fun Transaction.toEntity(): TransactionEntity {
    return TransactionEntity(
        hash = hash,
        tokenId = tokenId,
        from = from,
        to = to,
        amount = amount,
        gasFee = gasFee,
        platformFee = platformFee,
        timestamp = timestamp,
        timestampMillis = timestampMillis,
        marketplace = marketplace,
        chain = chain
    )
}

fun TransactionEntity.toTransaction(): Transaction {
    return Transaction(
        hash = hash,
        tokenId = tokenId,
        from = from,
        to = to,
        amount = amount,
        gasFee = gasFee,
        platformFee = platformFee,
        timestamp = timestamp,
        timestampMillis = timestampMillis,
        marketplace = marketplace,
        chain = chain
    )
}