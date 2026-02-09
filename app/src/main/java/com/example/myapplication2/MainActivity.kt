package com.example.myapplication2

import android.os.Bundle
import com.example.myapplication2.R
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.myapplication2.ui.theme.MyApplication2Theme
import com.dynamic.sdk.android.DynamicSDK
import com.dynamic.sdk.android.core.ClientProps
import com.dynamic.sdk.android.core.LoggerLevel
import com.dynamic.sdk.android.UI.DynamicUI
import com.example.myapplication2.common.AppNavigation
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val props = ClientProps(
            environmentId = "86939ac6-3854-471b-a95c-2a453e1481ee",
            appLogoUrl = "https://demo.dynamic.xyz/favicon-32x32.png",
            appName = getString(R.string.app_name),
            redirectUrl = getString(R.string.redirect_url),
            appOrigin = getString(R.string.app_origin),
            logLevel = LoggerLevel.DEBUG
        )
        DynamicSDK.initialize(props, applicationContext, this)

        setContent {
            MyApplication2Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DynamicUI()
                    AppNavigation()
                }
            }
        }
    }
}
