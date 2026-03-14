package com.himanshoe.tracey.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.himanshoe.tracey.Tracey
import kotlinx.coroutines.launch

/**
 * Sample app UI demonstrating Tracey recording. Every interaction in this
 * screen is captured and can be replayed via the overlay capture button or
 * [Tracey.capture].
 *
 * The `testTag` modifiers on key composables feed directly into the semantic
 * path that Tracey resolves per event — e.g. `"ProductList > ProductCard[2]"`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleApp() {
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text("Tracey Sample") })
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Button(
                onClick = {
                    scope.launch { Tracey.capture() }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("CaptureButton"),
            ) {
                Text("Manual Capture")
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("ProductList"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(sampleProducts, key = { it.id }) { product ->
                    ProductCard(product = product)
                }
            }
        }
    }
}

@Composable
private fun ProductCard(product: Product) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ProductCard[${product.id}]"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = product.name)
                Text(text = product.price)
            }
            Button(
                onClick = {},
                modifier = Modifier.testTag("AddToCartButton"),
            ) {
                Text("Add")
            }
        }
    }
}

private data class Product(val id: Int, val name: String, val price: String)

private val sampleProducts = List(20) { i ->
    Product(id = i, name = "Product ${i + 1}", price = "$${(i + 1) * 10}.99")
}
