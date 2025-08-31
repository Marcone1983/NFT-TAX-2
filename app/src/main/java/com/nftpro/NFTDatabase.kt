package com.nftpro

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Database(
    entities = [TransactionEntity::class, CacheEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class NFTDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun cacheDao(): CacheDao
}

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val hash: String,
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

@Entity(tableName = "cache")
data class CacheEntity(
    @PrimaryKey val key: String,
    val value: String,
    val timestamp: Long
)

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestampMillis DESC")
    suspend fun getAllTransactions(): List<TransactionEntity>
    
    @Query("SELECT * FROM transactions WHERE chain = :chain ORDER BY timestampMillis DESC")
    suspend fun getTransactionsByChain(chain: String): List<TransactionEntity>
    
    @Query("SELECT * FROM transactions WHERE tokenId = :tokenId")
    suspend fun getTransactionsByToken(tokenId: String): List<TransactionEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)
    
    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTransactionCount(): Int
    
    @Query("SELECT SUM(amount) FROM transactions")
    suspend fun getTotalRevenue(): Double?
    
    @Query("SELECT SUM(gasFee) FROM transactions")
    suspend fun getTotalGasFees(): Double?
}

@Dao
interface CacheDao {
    @Query("SELECT * FROM cache WHERE key = :key")
    suspend fun getCache(key: String): CacheEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(cache: CacheEntity)
    
    @Query("DELETE FROM cache WHERE timestamp < :timestamp")
    suspend fun deleteOldCache(timestamp: Long)
}

class Converters {
    // Add any type converters if needed
}