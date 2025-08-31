package com.nftpro

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenSeaApi {
    @GET("api/v1/events")
    suspend fun getEvents(
        @Query("asset_contract_address") assetContractAddress: String,
        @Query("event_type") eventType: String,
        @Query("limit") limit: Int = 300,
        @Query("offset") offset: Int = 0
    ): OpenSeaResponse
}

interface BlockchainApiService {
    @GET("v1/nft/{address}/transactions")
    suspend fun getTransactions(
        @Path("address") address: String,
        @Query("chain") chain: String
    ): List<Transaction>
}

data class OpenSeaResponse(
    val assetEvents: List<AssetEvent>
)

data class AssetEvent(
    val asset: Asset?,
    val transaction: TransactionInfo?,
    val seller: Account?,
    val winner: Account?,
    val totalPrice: String?,
    val createdDate: String
)

data class Asset(
    val tokenId: String,
    val name: String?,
    val description: String?
)

data class TransactionInfo(
    val transactionHash: String,
    val gasUsed: String?,
    val gasPrice: String?
)

data class Account(
    val address: String,
    val username: String?
)