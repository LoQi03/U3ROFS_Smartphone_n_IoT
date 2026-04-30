package com.invoice.store

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.invoice.store.ui.theme.Invoice_storeTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Invoice_storeTheme {
                Invoice_storeApp()
            }
        }
    }
}

@Composable
fun Invoice_storeApp() {
    var currentUser by remember { mutableStateOf(FirebaseManager.auth.currentUser) }

    if (currentUser == null) {
        AuthScreen(onAuthSuccess = {
            currentUser = FirebaseManager.auth.currentUser
        })
    } else {
        MainScreen(onLogout = {
            FirebaseManager.auth.signOut()
            currentUser = null
        })
    }
}

@Composable
fun MainScreen(onLogout: () -> Unit) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.INVOICES) }
    var isAddingInvoice by rememberSaveable { mutableStateOf(false) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            painterResource(it.icon),
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { 
                        currentDestination = it
                        isAddingInvoice = false 
                    }
                )
            }
        }
    ) {
        Scaffold(
            floatingActionButton = {
                if (currentDestination == AppDestinations.INVOICES && !isAddingInvoice) {
                    FloatingActionButton(onClick = { isAddingInvoice = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Invoice")
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                when (currentDestination) {
                    AppDestinations.INVOICES -> {
                        if (isAddingInvoice) {
                            AddInvoiceScreen(
                                onInvoiceSaved = { isAddingInvoice = false },
                                onCancel = { isAddingInvoice = false }
                            )
                        } else {
                            InvoiceListScreen()
                        }
                    }
                    AppDestinations.PROFILE -> {
                        ProfileScreen(onLogout = onLogout)
                    }
                }
            }
        }
    }
}

@Composable
fun InvoiceListScreen() {
    val context = LocalContext.current
    var invoices by remember { mutableStateOf<List<Invoice>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val userId = FirebaseManager.auth.currentUser?.uid ?: ""

    LaunchedEffect(userId) {
        if (userId.isEmpty()) return@LaunchedEffect
        
        FirebaseManager.db.collection("invoices")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, e ->
                isLoading = false
                if (e != null) {
                    Toast.makeText(context, "Error loading: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    invoices = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Invoice::class.java)?.copy(id = doc.id)
                    }
                }
            }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("My Invoices", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (invoices.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No invoices found. Click + to add one.")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(invoices) { invoice ->
                    InvoiceItemCard(invoice)
                }
            }
        }
    }
}

@Composable
fun InvoiceItemCard(invoice: Invoice) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(invoice.customerName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(invoice.invoiceNumber, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                }
                Text(
                    "${String.format(Locale.US, "%,.0f", invoice.totalGross)} ${invoice.currency}", 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date(invoice.date)),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                if (invoice.attachmentName.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            if (invoice.attachmentUrl.isNotEmpty()) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(invoice.attachmentUrl))
                                context.startActivity(intent)
                            }
                        },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(invoice.attachmentName, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddInvoiceScreen(onInvoiceSaved: () -> Unit, onCancel: () -> Unit) {
    var customerName by remember { mutableStateOf("") }
    var customerAddress by remember { mutableStateOf("") }
    var invoiceNumber by remember { mutableStateOf("INV-${System.currentTimeMillis() % 10000}") }
    
    var itemDescription by remember { mutableStateOf("") }
    var itemPrice by remember { mutableStateOf("") }
    val itemsList = remember { mutableStateListOf<InvoiceLineItem>() }

    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var isSaving by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedFileUri = uri
        uri?.let {
            val cursor = context.contentResolver.query(it, null, null, null, null)
            cursor?.use { c ->
                val nameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (c.moveToFirst()) {
                    selectedFileName = c.getString(nameIndex)
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("New Detailed Invoice", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = invoiceNumber,
            onValueChange = { invoiceNumber = it },
            label = { Text("Invoice Number") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = customerName,
            onValueChange = { customerName = it },
            label = { Text("Buyer Name (VEVŐ)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = customerAddress,
            onValueChange = { customerAddress = it },
            label = { Text("Buyer Address") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("Attachment (Image or PDF)", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { filePickerLauncher.launch("*/*") }) {
                Icon(Icons.Default.AttachFile, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pick File")
            }
            if (selectedFileName.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(selectedFileName, modifier = Modifier.weight(1f), fontSize = 12.sp)
                IconButton(onClick = { 
                    selectedFileUri = null
                    selectedFileName = ""
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Items", style = MaterialTheme.typography.titleMedium)
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = itemDescription,
                onValueChange = { itemDescription = it },
                label = { Text("Description") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = itemPrice,
                onValueChange = { itemPrice = it },
                label = { Text("Net Price") },
                modifier = Modifier.width(100.dp)
            )
            IconButton(onClick = {
                val price = itemPrice.toDoubleOrNull()
                if (itemDescription.isNotBlank() && price != null) {
                    val vat = price * 0.27
                    itemsList.add(InvoiceLineItem(
                        description = itemDescription,
                        quantity = 1.0,
                        unitPrice = price,
                        netPrice = price,
                        vatAmount = vat,
                        grossPrice = price + vat
                    ))
                    itemDescription = ""
                    itemPrice = ""
                }
            }) {
                Icon(Icons.Default.Add, "Add")
            }
        }
        
        itemsList.forEachIndexed { index, item ->
            ListItem(
                headlineContent = { Text(item.description) },
                supportingContent = { Text("Net: ${item.netPrice} + VAT: ${item.vatAmount}") },
                trailingContent = {
                    IconButton(onClick = { itemsList.removeAt(index) }) {
                        Icon(Icons.Default.Delete, "Delete")
                    }
                }
            )
        }
        
        val totalNet = itemsList.sumOf { it.netPrice }
        val totalVat = itemsList.sumOf { it.vatAmount }
        val totalGross = totalNet + totalVat

        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Summary", fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Net:")
                    Text("${String.format(Locale.US, "%,.0f", totalNet)} HUF")
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total VAT (27%):")
                    Text("${String.format(Locale.US, "%,.0f", totalVat)} HUF")
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Gross:", fontWeight = FontWeight.Bold)
                    Text("${String.format(Locale.US, "%,.0f", totalGross)} HUF", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                if (customerName.isBlank() || itemsList.isEmpty()) {
                    Toast.makeText(context, "Please add customer and at least one item", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                
                isSaving = true
                scope.launch {
                    try {
                        val userId = FirebaseManager.auth.currentUser?.uid ?: ""
                        var attachmentUrl = ""
                        
                        // Upload file if selected
                        selectedFileUri?.let { uri ->
                            val ref = FirebaseManager.storage.reference.child("invoices/$userId/${System.currentTimeMillis()}_$selectedFileName")
                            ref.putFile(uri).await()
                            attachmentUrl = ref.downloadUrl.await().toString()
                        }

                        val invoice = Invoice(
                            userId = userId,
                            invoiceNumber = invoiceNumber,
                            customerName = customerName,
                            customerAddress = customerAddress,
                            items = itemsList.toList(),
                            totalNet = totalNet,
                            totalVat = totalVat,
                            totalGross = totalGross,
                            attachmentUrl = attachmentUrl,
                            attachmentName = selectedFileName
                        )
                        FirebaseManager.db.collection("invoices")
                            .add(invoice.toMap())
                            .addOnSuccessListener {
                                isSaving = false
                                onInvoiceSaved()
                            }
                            .addOnFailureListener {
                                isSaving = false
                                Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                    } catch (e: Exception) {
                        isSaving = false
                        Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving
        ) {
            if (isSaving) CircularProgressIndicator(modifier = Modifier.size(24.dp))
            else Text("Save Detailed Invoice")
        }
        
        TextButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving
        ) {
            Text("Cancel")
        }
    }
}

@Composable
fun ProfileScreen(onLogout: () -> Unit) {
    val user = FirebaseManager.auth.currentUser
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Profile", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Email: ${user?.email ?: "Not logged in"}")
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onLogout, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
            Text("Logout")
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    INVOICES("Invoices", R.drawable.ic_home),
    PROFILE("Profile", R.drawable.ic_account_box),
}
