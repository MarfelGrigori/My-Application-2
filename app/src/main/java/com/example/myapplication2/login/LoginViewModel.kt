package com.example.myapplication2.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dynamic.sdk.android.DynamicSDK
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.myapplication2.R
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val dynamicSdk = DynamicSDK.getInstance()

    sealed interface AuthState {
        data object Idle : AuthState
        data object SendingOtp : AuthState
        data object VerifyingOtp : AuthState
        data class OtpSent(val email: String) : AuthState
        data class Error(val message: String) : AuthState
        data object Authenticated : AuthState
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _otpCode = MutableStateFlow("")
    val otpCode: StateFlow<String> = _otpCode.asStateFlow()

    fun onEmailChanged(newEmail: String) {
        _email.value = newEmail.trim()
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Idle
        }
    }

    fun isEmailValid(): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(_email.value).matches()
    }

    fun sendOtp() {
        if (!isEmailValid()) {
            _authState.value = AuthState.Error(context.getString(R.string.error_invalid_email))
            return
        }

        _authState.value = AuthState.SendingOtp

        viewModelScope.launch {
            try {
                dynamicSdk.auth.email.sendOTP(_email.value)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(
                    when {
                        e.message?.contains("rate_limit", ignoreCase = true) == true ->
                            context.getString(R.string.error_rate_limit)
                        else -> context.getString(R.string.error_send_otp)
                    }
                )
            }
            if (_authState.value !is AuthState.Error) {
                _otpCode.value = ""
                _authState.value = AuthState.OtpSent(_email.value)
            }
        }
    }

    // === OTP Verification Screen ===
    fun onOtpChanged(newCode: String) {
        _otpCode.value = newCode.filter { it.isDigit() }.take(6)
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Idle
        }
    }

    fun verifyOtp() {
        if (_otpCode.value.length != 6) {
            _authState.value = AuthState.Error(context.getString(R.string.error_enter_6_digits))
            return
        }

        _authState.value = AuthState.VerifyingOtp

        viewModelScope.launch {
            try {
                dynamicSdk.auth.email.verifyOTP(_otpCode.value)
                // OTP verified successfully – mark as authenticated for this session
                _authState.value = AuthState.Authenticated
            } catch (e: Exception) {
                _otpCode.value = ""
                _authState.value = AuthState.Error(
                    when {
                        e.message?.contains("invalid_code", ignoreCase = true) == true ->
                            context.getString(R.string.error_invalid_code)
                        e.message?.contains("expired", ignoreCase = true) == true ->
                            context.getString(R.string.error_code_expired)
                        else -> context.getString(R.string.error_verification_failed)
                    }
                )
            }
        }
    }

    fun dismissOtpSheet() {
        _authState.value = AuthState.Idle
        _otpCode.value = ""
    }

    fun resendOtp() {
        _otpCode.value = ""
        _authState.value = AuthState.SendingOtp

        viewModelScope.launch {
            try {
                dynamicSdk.auth.email.resendOTP()
            } catch (e: Exception) {
                _authState.value = AuthState.Error(context.getString(R.string.error_resend_failed))
            } finally {
                if (_authState.value !is AuthState.Error) {
                    _authState.value = AuthState.Idle
                }
            }
        }
    }
}