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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
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
    var editingInvoice by remember { mutableStateOf<Invoice?>(null) }
    
    var invoices by remember { mutableStateOf<List<Invoice>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val userId = FirebaseManager.auth.currentUser?.uid ?: ""
    val context = LocalContext.current

    LaunchedEffect(userId) {
        if (userId.isEmpty()) {
            isLoading = false
            return@LaunchedEffect
        }
        
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

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            imageVector = it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { 
                        currentDestination = it
                        isAddingInvoice = false 
                        editingInvoice = null
                    }
                )
            }
        }
    ) {
        Scaffold(
            floatingActionButton = {
                if (currentDestination == AppDestinations.INVOICES && !isAddingInvoice && editingInvoice == null) {
                    FloatingActionButton(onClick = { isAddingInvoice = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Invoice")
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                when (currentDestination) {
                    AppDestinations.INVOICES -> {
                        if (isAddingInvoice || editingInvoice != null) {
                            AddInvoiceScreen(
                                invoiceToEdit = editingInvoice,
                                onInvoiceSaved = { 
                                    isAddingInvoice = false
                                    editingInvoice = null
                                },
                                onCancel = { 
                                    isAddingInvoice = false
                                    editingInvoice = null
                                }
                            )
                        } else {
                            InvoiceListScreen(
                                invoices = invoices,
                                isLoading = isLoading,
                                onEditInvoice = { editingInvoice = it }
                            )
                        }
                    }
                    AppDestinations.PROFILE -> {
                        ProfileScreen(
                            invoiceCount = invoices.size,
                            onLogout = onLogout
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InvoiceListScreen(
    invoices: List<Invoice>,
    isLoading: Boolean,
    onEditInvoice: (Invoice) -> Unit
) {
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
                    InvoiceItemCard(invoice, onEditClick = { onEditInvoice(invoice) })
                }
            }
        }
    }
}

@Composable
fun InvoiceItemCard(invoice: Invoice, onEditClick: () -> Unit) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Invoice") },
            text = { Text("Are you sure you want to delete invoice ${invoice.invoiceNumber}?") },
            confirmButton = {
                TextButton(onClick = {
                    FirebaseManager.db.collection("invoices").document(invoice.id).delete()
                    showDeleteDialog = false
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(invoice.customerName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(invoice.invoiceNumber, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    Text("From: ${invoice.sellerName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${String.format(Locale.US, "%,.2f", invoice.totalGross)} ${invoice.currency}", 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row {
                        IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(
                        SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date(invoice.date)),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        "Pay via: ${invoice.paymentMethod}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
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
fun AddInvoiceScreen(invoiceToEdit: Invoice? = null, onInvoiceSaved: () -> Unit, onCancel: () -> Unit) {
    var customerName by remember { mutableStateOf(invoiceToEdit?.customerName ?: "") }
    var customerAddress by remember { mutableStateOf(invoiceToEdit?.customerAddress ?: "") }
    var customerTaxId by remember { mutableStateOf(invoiceToEdit?.customerTaxId ?: "") }
    
    var sellerName by remember { mutableStateOf(invoiceToEdit?.sellerName ?: "My Business Ltd.") }
    var sellerAddress by remember { mutableStateOf(invoiceToEdit?.sellerAddress ?: "123 Business St, City") }
    var sellerTaxId by remember { mutableStateOf(invoiceToEdit?.sellerTaxId ?: "") }
    
    var invoiceNumber by remember { mutableStateOf(invoiceToEdit?.invoiceNumber ?: "INV-${System.currentTimeMillis() % 10000}") }
    var currency by remember { mutableStateOf(invoiceToEdit?.currency ?: "HUF") }
    var paymentMethod by remember { mutableStateOf(invoiceToEdit?.paymentMethod ?: "Átutalás") }
    
    var itemDescription by remember { mutableStateOf("") }
    var itemPrice by remember { mutableStateOf("") }
    val itemsList = remember { 
        mutableStateListOf<InvoiceLineItem>().apply {
            if (invoiceToEdit != null) addAll(invoiceToEdit.items)
        }
    }

    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf(invoiceToEdit?.attachmentName ?: "") }
    var existingAttachmentUrl by remember { mutableStateOf(invoiceToEdit?.attachmentUrl ?: "") }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var isSaving by remember { mutableStateOf(false) }
    
    // Dropdown states
    var currencyExpanded by remember { mutableStateOf(false) }
    val currencies = listOf("HUF", "EUR", "USD", "GBP")
    
    var paymentExpanded by remember { mutableStateOf(false) }
    val paymentMethods = listOf("Átutalás", "Készpénz", "Bankkártya", "Utánvét")

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
        Text(if (invoiceToEdit != null) "Edit Invoice" else "New Detailed Invoice", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Invoice Header", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = invoiceNumber,
                    onValueChange = { invoiceNumber = it },
                    label = { Text("Invoice Number") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Currency Dropdown
                    @OptIn(ExperimentalMaterial3Api::class)
                    ExposedDropdownMenuBox(
                        expanded = currencyExpanded,
                        onExpandedChange = { currencyExpanded = !currencyExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = currency,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Currency") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                        )
                        ExposedDropdownMenu(
                            expanded = currencyExpanded,
                            onDismissRequest = { currencyExpanded = false }
                        ) {
                            currencies.forEach { curr ->
                                DropdownMenuItem(
                                    text = { Text(curr) },
                                    onClick = {
                                        currency = curr
                                        currencyExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // Payment Method Dropdown
                    @OptIn(ExperimentalMaterial3Api::class)
                    ExposedDropdownMenuBox(
                        expanded = paymentExpanded,
                        onExpandedChange = { paymentExpanded = !paymentExpanded },
                        modifier = Modifier.weight(1.2f)
                    ) {
                        OutlinedTextField(
                            value = paymentMethod,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Payment Method") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentExpanded) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                        )
                        ExposedDropdownMenu(
                            expanded = paymentExpanded,
                            onDismissRequest = { paymentExpanded = false }
                        ) {
                            paymentMethods.forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method) },
                                    onClick = {
                                        paymentMethod = method
                                        paymentExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Seller Information", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = sellerName,
            onValueChange = { sellerName = it },
            label = { Text("Seller Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = sellerAddress,
            onValueChange = { sellerAddress = it },
            label = { Text("Seller Address") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = sellerTaxId,
            onValueChange = { sellerTaxId = it },
            label = { Text("Seller Tax ID") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text("Buyer Information (VEVŐ)", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = customerName,
            onValueChange = { customerName = it },
            label = { Text("Buyer Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = customerAddress,
            onValueChange = { customerAddress = it },
            label = { Text("Buyer Address") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = customerTaxId,
            onValueChange = { customerTaxId = it },
            label = { Text("Buyer Tax ID") },
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
        
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = itemDescription,
                onValueChange = { itemDescription = it },
                label = { Text("Description") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = itemPrice,
                onValueChange = { itemPrice = it },
                label = { Text("Price") },
                modifier = Modifier.width(110.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
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
                Icon(Icons.Default.AddCircle, "Add", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
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
                    Text("${String.format(Locale.US, "%,.2f", totalNet)} $currency")
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total VAT (27%):")
                    Text("${String.format(Locale.US, "%,.2f", totalVat)} $currency")
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Gross:", fontWeight = FontWeight.Bold)
                    Text("${String.format(Locale.US, "%,.2f", totalGross)} $currency", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
                        var attachmentUrl = existingAttachmentUrl
                        
                        // Upload file if selected
                        selectedFileUri?.let { uri ->
                            // Use MinIO for storage instead of Firebase
                            attachmentUrl = MinioManager.uploadFile(uri, context)
                        }

                        val invoice = Invoice(
                            id = invoiceToEdit?.id ?: "",
                            userId = userId,
                            invoiceNumber = invoiceNumber,
                            customerName = customerName,
                            customerAddress = customerAddress,
                            customerTaxId = customerTaxId,
                            sellerName = sellerName,
                            sellerAddress = sellerAddress,
                            sellerTaxId = sellerTaxId,
                            paymentMethod = paymentMethod,
                            currency = currency,
                            items = itemsList.toList(),
                            totalNet = totalNet,
                            totalVat = totalVat,
                            totalGross = totalGross,
                            attachmentUrl = attachmentUrl,
                            attachmentName = selectedFileName
                        )
                        
                        val collection = FirebaseManager.db.collection("invoices")
                        val task = if (invoiceToEdit != null) {
                            collection.document(invoiceToEdit.id).set(invoice.toMap())
                        } else {
                            collection.add(invoice.toMap())
                        }
                        
                        task.addOnSuccessListener {
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
fun ProfileScreen(invoiceCount: Int, onLogout: () -> Unit) {
    val user = FirebaseManager.auth.currentUser
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = user?.displayName ?: "User",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = user?.email ?: "Not logged in",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Total Invoices",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "$invoiceCount",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Logout")
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    INVOICES("Invoices", Icons.AutoMirrored.Filled.ReceiptLong),
    PROFILE("Profile", Icons.Default.AccountCircle),
}
