package com.himanshoe.tracey.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.himanshoe.tracey.Tracey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val ROUTE_HOME   = "HomeScreen"
private const val ROUTE_DETAIL = "DetailScreen/{productId}"

private fun detailRoute(productId: Int) = "DetailScreen/$productId"

@Composable
fun SampleApp() {
    val navController = rememberNavController()

    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            destination.route?.let { Tracey.route(it) }
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }

    NavHost(navController = navController, startDestination = ROUTE_HOME) {
        composable(ROUTE_HOME) {
            HomeScreen(navController = navController)
        }
        composable(ROUTE_DETAIL) { backStack ->
            val productId = backStack.arguments?.getString("productId")?.toIntOrNull() ?: 0
            DetailScreen(
                product = sampleProducts.getOrNull(productId) ?: sampleProducts.first(),
                onBack  = { navController.popBackStack() },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(navController: NavHostController) {
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Tracey Sample") }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            HomeActionButtons(scope = scope)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("ProductList"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(sampleProducts, key = { it.id }) { product ->
                    ProductCard(
                        product = product,
                        onClick = {
                            Tracey.log("Opened product: ${product.name}")
                            navController.navigate(detailRoute(product.id))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeActionButtons(scope: CoroutineScope) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = {
                Tracey.log("Manual capture triggered from HomeScreen")
                scope.launch { Tracey.capture() }
            },
            modifier = Modifier
                .weight(1f)
                .testTag("CaptureButton"),
        ) {
            Text("Capture")
        }

        Button(
            onClick = {
                Tracey.log("Crash button tapped — about to throw")
                @Suppress("TooGenericExceptionThrown")
                throw RuntimeException(
                    "Simulated crash — check LogcatReporter on next launch"
                )
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
            ),
            modifier = Modifier
                .weight(1f)
                .testTag("CrashButton"),
        ) {
            Text("Crash!")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailScreen(product: Product, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(product.name) },
                colors = TopAppBarDefaults.topAppBarColors(),
                navigationIcon = {
                    Button(onClick = onBack) { Text("←") }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = product.name, style = MaterialTheme.typography.headlineMedium)
            Text(text = product.price, style = MaterialTheme.typography.titleLarge)
            Text(text = product.description, style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    Tracey.log("Added to cart: ${product.name} @ ${product.price}")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("AddToCartButton"),
            ) {
                Text("Add to Cart")
            }

            Button(
                onClick = {
                    Tracey.log("Checkout initiated from DetailScreen")
                    scope.launch { Tracey.capture() }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("CheckoutButton"),
            ) {
                Text("Checkout")
            }
        }
    }
}

@Composable
private fun ProductCard(product: Product, onClick: () -> Unit) {
    Card(
        onClick  = onClick,
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
                Text(text = product.name, style = MaterialTheme.typography.titleMedium)
                Text(text = product.price, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                text = "›",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

private data class Product(
    val id: Int,
    val name: String,
    val price: String,
    val description: String,
)

private val sampleProducts = List(20) { i ->
    Product(
        id          = i,
        name        = "Product ${i + 1}",
        price       = "$${(i + 1) * 10}.99",
        description = "This is the full description for Product ${i + 1}. " +
            "Tap 'Add to Cart' to log a breadcrumb, or 'Checkout' to trigger a capture.",
    )
}
