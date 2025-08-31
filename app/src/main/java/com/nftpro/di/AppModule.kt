package com.nftpro.di

import android.content.Context
import androidx.room.Room
import com.nftpro.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
        
        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(loggingInterceptor)
        }
        
        return builder.build()
    }
    
    @Provides
    @Singleton
    fun provideOpenSeaApi(okHttpClient: OkHttpClient): OpenSeaApi {
        return Retrofit.Builder()
            .baseUrl("https://api.opensea.io/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenSeaApi::class.java)
    }
    
    @Provides
    @Singleton
    fun provideBlockchainApiService(okHttpClient: OkHttpClient): BlockchainApiService {
        return Retrofit.Builder()
            .baseUrl("https://api.blockchain.io/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BlockchainApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NFTDatabase {
        // Encrypt the database with SQLCipher
        val passphrase = SQLiteDatabase.getBytes("nft_pro_secret_key".toCharArray())
        val factory = SupportFactory(passphrase)
        
        return Room.databaseBuilder(
            context,
            NFTDatabase::class.java,
            "nft_pro_database"
        )
        .openHelperFactory(factory)
        .fallbackToDestructiveMigration()
        .build()
    }
    
    @Provides
    @Singleton
    fun provideTransactionDao(database: NFTDatabase): TransactionDao {
        return database.transactionDao()
    }
    
    @Provides
    @Singleton
    fun provideCacheDao(database: NFTDatabase): CacheDao {
        return database.cacheDao()
    }
    
    @Provides
    @Singleton
    fun provideNFTRepository(
        @ApplicationContext context: Context,
        database: NFTDatabase,
        apiService: BlockchainApiService
    ): NFTRepository {
        return NFTRepository(context, database, apiService)
    }
    
    @Provides
    @Singleton
    fun provideBlockchainService(): BlockchainService {
        return BlockchainService()
    }
    
    @Provides
    @Singleton
    fun provideBillingManager(@ApplicationContext context: Context): BillingManager {
        return BillingManager(context)
    }
}