package com.example.myapplication2.wallet

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Convert
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

class SepoliaRepository {

    companion object {
        const val SEPOLIA_CHAIN_ID = 11155111L
        private const val SEPOLIA_RPC_URL = "https://ethereum-sepolia-rpc.publicnode.com"
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val web3j: Web3j = Web3j.build(HttpService(SEPOLIA_RPC_URL, okHttpClient, false))

    suspend fun getBalance(address: String): BigDecimal {
        return withContext(Dispatchers.IO) {
            val balanceWei = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST)
                .send()
                .balance
            Convert.fromWei(balanceWei.toString(), Convert.Unit.ETHER)
        }
    }

    fun shutdown() {
        web3j.shutdown()
    }
}
