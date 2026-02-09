package com.example.myapplication2.wallet

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dynamic.sdk.android.Chains.EVM.EthereumTransaction
import com.dynamic.sdk.android.Chains.EVM.convertEthToWei
import com.dynamic.sdk.android.DynamicSDK
import com.dynamic.sdk.android.Models.BaseWallet
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigInteger
import com.example.myapplication2.R
import javax.inject.Inject

@HiltViewModel
class SendTransactionViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    sealed interface SendState {
        data object Idle : SendState
        data object Loading : SendState
        data class Success(val txHash: String) : SendState
        data class Error(val message: String) : SendState
    }

    private val _sendState = MutableStateFlow<SendState>(SendState.Idle)
    val sendState: StateFlow<SendState> = _sendState.asStateFlow()

    private val _recipientAddress = MutableStateFlow("")
    val recipientAddress: StateFlow<String> = _recipientAddress.asStateFlow()

    private val _amount = MutableStateFlow("")
    val amount: StateFlow<String> = _amount.asStateFlow()

    private val dynamicSdk = DynamicSDK.getInstance()

    fun onRecipientChanged(value: String) {
        _recipientAddress.value = value.trim()
        if (_sendState.value is SendState.Error) {
            _sendState.value = SendState.Idle
        }
    }

    fun onAmountChanged(value: String) {
        _amount.value = value.filter { it.isDigit() || it == '.' }
        if (_sendState.value is SendState.Error) {
            _sendState.value = SendState.Idle
        }
    }

    fun sendTransaction() {
        val to = _recipientAddress.value
        val amountStr = _amount.value

        if (to.isBlank()) {
            _sendState.value = SendState.Error(context.getString(R.string.error_enter_recipient))
            return
        }
        if (!to.startsWith("0x") || to.length != 42) {
            _sendState.value = SendState.Error(context.getString(R.string.error_invalid_ethereum_address))
            return
        }
        if (amountStr.isBlank()) {
            _sendState.value = SendState.Error(context.getString(R.string.error_enter_amount))
            return
        }
        val amountEth = amountStr.toBigDecimalOrNull()
        if (amountEth == null || amountEth <= java.math.BigDecimal.ZERO) {
            _sendState.value = SendState.Error(context.getString(R.string.error_valid_amount))
            return
        }

        _sendState.value = SendState.Loading

        viewModelScope.launch {
            try {
                val wallet = dynamicSdk.wallets.userWallets?.firstOrNull { w ->
                    (w.chain ?: "").uppercase() == "EVM"
                } as? BaseWallet ?: run {
                    _sendState.value = SendState.Error(context.getString(R.string.error_no_evm_wallet_send))
                    return@launch
                }

                val chainId = SepoliaRepository.SEPOLIA_CHAIN_ID.toInt()
                val client = dynamicSdk.evm.createPublicClient(chainId)

                val gasPrice = client.getGasPrice()
                val maxFeePerGas = gasPrice * BigInteger.valueOf(2)
                val maxPriorityFeePerGas = gasPrice

                val weiAmount = convertEthToWei(amountStr)

                val transaction = EthereumTransaction(
                    from = wallet.address,
                    to = to,
                    value = weiAmount,
                    gas = BigInteger.valueOf(21000),
                    maxFeePerGas = maxFeePerGas,
                    maxPriorityFeePerGas = maxPriorityFeePerGas
                )

                val txHash = dynamicSdk.evm.sendTransaction(transaction, wallet)
                _sendState.value = SendState.Success(txHash ?: context.getString(R.string.tx_hash_unknown))
            } catch (e: Exception) {
                val errorMessage = parseErrorMessage(e)
                _sendState.value = SendState.Error(errorMessage)
            }
        }
    }

    fun resetState() {
        _sendState.value = SendState.Idle
    }

    private fun parseErrorMessage(e: Exception): String {
        val message = e.message ?: e.toString()
        
        if (message.contains("eth.llamarpc.com") || message.contains("Failed to fetch")) {
            return context.getString(R.string.error_network_mainnet)
        }
        if (message.contains("TransactionExecutionError") || message.contains("HTTP request failed")) {
            return context.getString(R.string.error_transaction_network)
        }
        if (message.contains("insufficient funds") || message.contains("balance")) {
            return context.getString(R.string.error_insufficient_balance)
        }
        if (message.contains("invalid address") || message.contains("address")) {
            return context.getString(R.string.error_invalid_recipient)
        }
        return if (message.length > 200) {
            context.getString(R.string.error_transaction_failed_generic)
        } else {
            message
        }
    }
}
