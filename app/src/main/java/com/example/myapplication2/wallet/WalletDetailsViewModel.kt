package com.example.myapplication2.wallet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dynamic.sdk.android.DynamicSDK
import com.dynamic.sdk.android.Models.BaseWallet
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.myapplication2.R
import javax.inject.Inject

@HiltViewModel
class WalletDetailsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sepoliaRepository: SepoliaRepository
) : ViewModel() {

    sealed interface WalletState {
        data object Loading : WalletState
        data class Loaded(
            val address: String,
            val network: String,
            val balanceEth: String
        ) : WalletState
        data class Error(val message: String) : WalletState
    }

    private val _walletState = MutableStateFlow<WalletState>(WalletState.Loading)
    val walletState: StateFlow<WalletState> = _walletState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val dynamicSdk = DynamicSDK.getInstance()
    private var creationTimeoutJob: Job? = null

    init {
        viewModelScope.launch {
            dynamicSdk.wallets.userWalletsChanges.collect { walletList ->
                val evmWallet = walletList?.firstOrNull { isEvmWallet(it.chain) }
                if (evmWallet != null && !evmWallet.address.isNullOrBlank()) {
                    creationTimeoutJob?.cancel()
                    loadWalletFrom(evmWallet)
                } else if (evmWallet != null) {
                    // Кошелёк существует, но адрес ещё не сгенерирован
                    creationTimeoutJob?.cancel()
                    if (!_isRefreshing.value && _walletState.value is WalletState.Loading) {
                        startCreationTimeout()
                    }
                } else if (!_isRefreshing.value && _walletState.value is WalletState.Loading) {
                    // Нет кошельков — ждём создания
                    startCreationTimeout()
                }
            }
        }
        // ❌ УБРАНО: loadWalletWithRetry() — полагаемся только на подписку на изменения
    }

    private fun startCreationTimeout() {
        creationTimeoutJob?.cancel()
        creationTimeoutJob = viewModelScope.launch {
            delay(30_000) // Увеличен до 30 секунд для надёжности
            if (_walletState.value is WalletState.Loading) {
                _walletState.value = WalletState.Error(
                    context.getString(R.string.error_wallet_creation_timeout)
                )
            }
        }
    }

    private fun loadWalletFrom(evmWallet: BaseWallet) {
        creationTimeoutJob?.cancel()
        viewModelScope.launch {
            // Защита от гонки: обрабатываем только если всё ещё в Loading/Error
            if (_walletState.value !is WalletState.Loading &&
                _walletState.value !is WalletState.Error) return@launch

            _walletState.value = WalletState.Loading
            try {
                val address = evmWallet.address ?: ""
                if (address.isEmpty()) {
                    _isRefreshing.value = false
                    return@launch
                }

                val balance = try {
                    sepoliaRepository.getBalance(address)
                } catch (e: Exception) {
                    java.math.BigDecimal.ZERO
                }

                // Устанавливаем результат только если всё ещё ожидаем данные
                if (_walletState.value is WalletState.Loading) {
                    _walletState.value = WalletState.Loaded(
                        address = address,
                        network = context.getString(
                            R.string.network_sepolia_with_chain,
                            SepoliaRepository.SEPOLIA_CHAIN_ID.toInt()
                        ),
                        balanceEth = balance.toPlainString()
                    )
                }
            } catch (e: Exception) {
                if (_walletState.value is WalletState.Loading) {
                    _walletState.value = WalletState.Error(
                        e.message ?: context.getString(R.string.error_failed_load_wallet)
                    )
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun loadWalletWithRetry(retryCount: Int = 0) {
        viewModelScope.launch {
            _walletState.value = WalletState.Loading
            _isRefreshing.value = true
            try {
                // Экспоненциальная задержка между попытками
                if (retryCount > 0) {
                    delay((500L * 2.coerceAtLeast(retryCount)).coerceAtMost(5000))
                }

                val wallets = dynamicSdk.wallets.userWallets
                val evmWallet = wallets?.firstOrNull { isEvmWallet(it.chain) }

                if (evmWallet == null || evmWallet.address.isNullOrBlank()) {
                    if (retryCount < 8) {
                        loadWalletWithRetry(retryCount + 1)
                    } else {
                        _walletState.value = WalletState.Error(
                            context.getString(R.string.error_failed_load_wallet)
                        )
                    }
                    return@launch
                }

                val address = evmWallet.address!!
                val balance = try {
                    sepoliaRepository.getBalance(address)
                } catch (e: Exception) {
                    java.math.BigDecimal.ZERO
                }

                _walletState.value = WalletState.Loaded(
                    address = address,
                    network = context.getString(
                        R.string.network_sepolia_with_chain,
                        SepoliaRepository.SEPOLIA_CHAIN_ID.toInt()
                    ),
                    balanceEth = balance.toPlainString()
                )
            } catch (e: Exception) {
                _walletState.value = WalletState.Error(
                    e.message ?: context.getString(R.string.error_failed_load_wallet)
                )
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun loadWallet() {
        loadWalletWithRetry(0)
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadWalletWithRetry(0)
        }
    }

    fun copyAddress() {
        val state = _walletState.value
        if (state is WalletState.Loaded) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(
                ClipData.newPlainText(
                    context.getString(R.string.wallet_address_clipboard_label),
                    state.address
                )
            )
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                dynamicSdk.auth.logout()
            } finally {
                onComplete()
            }
        }
    }

    fun getEvmWallet(): Any? {
        return dynamicSdk.wallets.userWallets?.firstOrNull { isEvmWallet(it.chain) }
    }

    override fun onCleared() {
        super.onCleared()
        sepoliaRepository.shutdown()
    }
}

private fun isEvmWallet(chain: String?): Boolean {
    val c = (chain ?: "").uppercase()
    return c == "EVM" || c == "ETH" || c == "ETHEREUM"
}
