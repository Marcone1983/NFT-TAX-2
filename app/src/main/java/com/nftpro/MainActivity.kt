package com.nftpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nftpro.ui.theme.NFTProTheme
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NFTProTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NFTTaxApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NFTTaxApp() {
    val viewModel: NFTViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var erc721Contract by remember { mutableStateOf("") }
    var erc1155Contract by remember { mutableStateOf("") }
    var erc1155TokenId by remember { mutableStateOf("") }
    var selectedChain by remember { mutableStateOf("Ethereum") }
    var showUpgradeDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Diamond, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("NFT Tax Calculator Pro", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.exportReport() }) {
                        Icon(Icons.Default.Download, contentDescription = "Export")
                    }
                    IconButton(onClick = { /* Settings */ }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Chain Selection
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "🔗 Select Blockchain", 
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedChain == "Ethereum",
                                onClick = { selectedChain = "Ethereum" },
                                label = { Text("Ethereum") },
                                leadingIcon = { Icon(Icons.Default.Toll, contentDescription = null) }
                            )
                            FilterChip(
                                selected = selectedChain == "Polygon",
                                onClick = { selectedChain = "Polygon" },
                                label = { Text("Polygon") },
                                leadingIcon = { Icon(Icons.Default.Pentagon, contentDescription = null) }
                            )
                            FilterChip(
                                selected = selectedChain == "Solana",
                                onClick = { selectedChain = "Solana" },
                                label = { Text("Solana") },
                                leadingIcon = { Icon(Icons.Default.WbSunny, contentDescription = null) }
                            )
                        }
                    }
                }
            }
            
            // NFT Input Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "💎 I TUOI NFT - Inserisci Contract & Token ID",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        // ERC-721 Section
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Image, 
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "ERC-721 NFTs (1:1 unici)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                Text(
                                    "Es: Bored Ape, CryptoPunks, Art NFTs, PFP collections",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                OutlinedTextField(
                                    value = erc721Contract,
                                    onValueChange = { 
                                        erc721Contract = it
                                        viewModel.updateContract(it)
                                    },
                                    label = { Text("Contract Address ERC-721") },
                                    placeholder = { Text("0xBC4CA0EdA7647A8aB7C2061c2E118A18a936f13D") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    leadingIcon = {
                                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null)
                                    },
                                    supportingText = {
                                        Text("Tutti i token del contratto verranno sincronizzati")
                                    },
                                    isError = erc721Contract.isNotEmpty() && 
                                             !erc721Contract.startsWith("0x")
                                )
                            }
                        }
                        
                        // ERC-1155 Section  
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Apps, 
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "ERC-1155 NFTs (Multi-edition)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                
                                Text(
                                    "Es: Gaming items, Collectible cards, OpenSea Shared",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = erc1155Contract,
                                        onValueChange = { 
                                            erc1155Contract = it
                                            viewModel.updateERC1155Contract(it)
                                        },
                                        label = { Text("Contract Address") },
                                        placeholder = { Text("0x495f...") },
                                        modifier = Modifier.weight(2f),
                                        singleLine = true,
                                        leadingIcon = {
                                            Icon(Icons.Default.Inventory, contentDescription = null)
                                        }
                                    )
                                    
                                    OutlinedTextField(
                                        value = erc1155TokenId,
                                        onValueChange = { 
                                            erc1155TokenId = it
                                            viewModel.updateERC1155TokenId(it)
                                        },
                                        label = { Text("Token ID") },
                                        placeholder = { Text("12345") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        leadingIcon = {
                                            Icon(Icons.Default.Tag, contentDescription = null)
                                        }
                                    )
                                }
                                
                                Text(
                                    "Serve sia contract CHE token ID specifico",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        
                        // Sync Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        if (erc721Contract.isNotEmpty()) {
                                            viewModel.syncERC721Transactions(erc721Contract, selectedChain)
                                        }
                                        if (erc1155Contract.isNotEmpty() && erc1155TokenId.isNotEmpty()) {
                                            viewModel.syncERC1155Transactions(erc1155Contract, erc1155TokenId, selectedChain)
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !state.isLoading && 
                                         (erc721Contract.isNotEmpty() || 
                                          (erc1155Contract.isNotEmpty() && erc1155TokenId.isNotEmpty()))
                            ) {
                                if (state.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White
                                    )
                                } else {
                                    Icon(Icons.Default.Sync, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sync I Miei NFT")
                                }
                            }
                            
                            OutlinedButton(
                                onClick = { 
                                    erc721Contract = ""
                                    erc1155Contract = ""
                                    erc1155TokenId = ""
                                    viewModel.clearData() 
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Reset")
                            }
                        }
                    }
                }
            }
            
            // Statistics Dashboard
            if (state.totalSales > 0) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            title = "Vendite Totali",
                            value = state.totalSales.toString(),
                            icon = Icons.Default.ShoppingCart,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.primary
                        )
                        StatCard(
                            title = "Revenue €",
                            value = formatEuro(state.totalRevenue),
                            icon = Icons.Default.AttachMoney,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            title = "Tasse Dovute",
                            value = formatEuro(state.taxOwed),
                            icon = Icons.Default.Receipt,
                            modifier = Modifier.weight(1f),
                            isPro = !state.isProUser,
                            onProClick = { showUpgradeDialog = true },
                            color = MaterialTheme.colorScheme.error
                        )
                        StatCard(
                            title = "Gas Fees €",
                            value = formatEuro(state.totalGasFees),
                            icon = Icons.Default.LocalGasStation,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
            
            // Transaction List
            if (state.transactions.isNotEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "🧾 Le Tue Transazioni (${state.transactions.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Row {
                                    if (state.isProUser) {
                                        TextButton(onClick = { viewModel.generateF24Form() }) {
                                            Icon(Icons.Default.Description, contentDescription = null)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("F24")
                                        }
                                    }
                                    
                                    TextButton(onClick = { viewModel.exportReport() }) {
                                        Icon(Icons.Default.Download, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("CSV")
                                    }
                                }
                            }
                        }
                    }
                }
                
                items(state.transactions.take(10)) { transaction ->
                    TransactionCard(transaction)
                }
                
                if (state.transactions.size > 10) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { /* Show all transactions */ }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Vedi altre ${state.transactions.size - 10} transazioni...",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
            
            // Pro Features Upgrade
            if (!state.isProUser) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        onClick = { showUpgradeDialog = true }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "🚀 Upgrade to NFT Pro",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "FIFO/LIFO • F24 Italia • Wash Sale Detection",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Button(onClick = { showUpgradeDialog = true }) {
                                    Text("€99.90/anno")
                                }
                            }
                        }
                    }
                }
            }
            
            // Empty State
            if (!state.isLoading && state.totalSales == 0 && 
                (erc721Contract.isNotEmpty() || erc1155Contract.isNotEmpty())) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.SearchOff,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Nessuna transazione trovata",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Controlla contract address e blockchain selezionata",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Crypto Payment Dialog
    if (showUpgradeDialog) {
        CryptoPaymentDialog(
            onDismiss = { showUpgradeDialog = false },
            onPaymentInitiated = { tier, chain, token ->
                // Handle crypto payment
                viewModel.initiateCryptoPayment(tier, chain, token)
                showUpgradeDialog = false
            }
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    isPro: Boolean = false,
    onProClick: (() -> Unit)? = null,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = modifier.then(
            if (isPro && onProClick != null) Modifier.clickable { onProClick() }
            else Modifier
        ),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            if (isPro) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd),
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        "PRO",
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionCard(transaction: Transaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Token #${transaction.tokenId}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${transaction.marketplace} • ${transaction.chain}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    transaction.timestamp,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatEuro(transaction.amount),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (transaction.gasFee > 0) {
                    Text(
                        "Gas: ${formatEuro(transaction.gasFee)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun CryptoPaymentDialog(
    onDismiss: () -> Unit,
    onPaymentInitiated: (String, String, String) -> Unit
) {
    var selectedTier by remember { mutableStateOf("Pro") }
    var selectedChain by remember { mutableStateOf("Polygon") }
    var selectedToken by remember { mutableStateOf("USDC") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "💰 Paga con Crypto",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tier Selection
                Text("Scegli piano:", fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedTier == "Basic",
                        onClick = { selectedTier = "Basic" },
                        label = { Text("Basic €49.90") }
                    )
                    FilterChip(
                        selected = selectedTier == "Pro", 
                        onClick = { selectedTier = "Pro" },
                        label = { Text("Pro €99.90") }
                    )
                }
                
                if (selectedTier == "Pro") {
                    Column {
                        Text("Funzionalità Pro:", style = MaterialTheme.typography.bodySmall)
                        ProFeatureItem("📊 FIFO/LIFO/HIFO tax optimization")
                        ProFeatureItem("🇮🇹 F24 automatico Italia")
                        ProFeatureItem("⚠️ Wash sale detection")
                        ProFeatureItem("💰 Tax loss harvesting")
                        ProFeatureItem("🔗 DeFi yield tracking")
                    }
                }
                
                Divider()
                
                // Chain Selection
                Text("Blockchain:", fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedChain == "Ethereum",
                        onClick = { selectedChain = "Ethereum" },
                        label = { Text("ETH") }
                    )
                    FilterChip(
                        selected = selectedChain == "Polygon",
                        onClick = { selectedChain = "Polygon" },
                        label = { Text("Polygon") }
                    )
                    FilterChip(
                        selected = selectedChain == "BSC",
                        onClick = { selectedChain = "BSC" },
                        label = { Text("BSC") }
                    )
                }
                
                // Token Selection
                Text("Token:", fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedToken == "USDC",
                        onClick = { selectedToken = "USDC" },
                        label = { Text("USDC") }
                    )
                    FilterChip(
                        selected = selectedToken == "USDT",
                        onClick = { selectedToken = "USDT" },
                        label = { Text("USDT") }
                    )
                }
                
                // Payment Info
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("💳 Dettagli Pagamento:", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Piano:")
                            Text(selectedTier, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Importo:")
                            Text(
                                if (selectedTier == "Pro") "99.90 $selectedToken" else "49.90 $selectedToken",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Rete:")
                            Text(selectedChain, fontWeight = FontWeight.Bold)
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            "🔒 Wallet destinazione:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "0xC69088eB5F015Fca5B385b8E3A0463749813093e",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onPaymentInitiated(selectedTier, selectedChain, selectedToken)
                }
            ) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Paga con ${selectedChain}")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

@Composable
fun ProFeatureItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

fun formatEuro(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale.ITALY)
    return format.format(amount)
}