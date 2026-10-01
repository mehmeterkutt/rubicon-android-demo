package com.rubicon.demo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rubicon.analytics.ScreenName
import com.rubicon.demo.ui.RubiconScaffold
import com.rubicon.demo.ui.theme.RubiconTheme

/**
 * The home screen, named by annotation (screen view `home`). `singleTop` with the deep link
 * `rubicon://open`: a link while it is open arrives through onNewIntent, which the SDK hears through
 * ComponentActivity (D13). The app itself does nothing with the link: the SDK captures it.
 */
@ScreenName("home")
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RubiconTheme {
                RubiconScaffold(onOpenSettings = { startActivity(Intent(this, SettingsActivity::class.java)) })
            }
        }
    }
}
