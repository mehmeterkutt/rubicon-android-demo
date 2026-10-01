package com.rubicon.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rubicon.analytics.ScreenName
import com.rubicon.demo.sdk.Calls
import com.rubicon.demo.ui.SecondaryScaffold
import com.rubicon.demo.ui.components.Expect
import com.rubicon.demo.ui.components.SectionCard
import com.rubicon.demo.ui.theme.RubiconTheme

/** A screen named by `@ScreenName`: the screen view is `product_detail`, `name_source: annotation`. */
@ScreenName("product_detail")
class ProductActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val analytics = demo.analytics
        setContent {
            RubiconTheme {
                SecondaryScaffold("Product", onBack = { finish() }) { modifier ->
                    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        SectionCard("product_detail", "An activity with @ScreenName(\"product_detail\").") {
                            Button(onClick = {
                                Calls.run("Tracked Product Viewed") { analytics.track("Product Viewed", mapOf("sku" to "A1", "price" to 19.9)) }
                            }) { Text("Track Product Viewed") }
                            Expect("Rotate the device: no second screen view. Go back: home is sent again.")
                        }
                    }
                }
            }
        }
    }
}
