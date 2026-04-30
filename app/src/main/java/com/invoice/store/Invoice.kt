package com.invoice.store

data class Invoice(
    val id: String = "",
    val userId: String = "",
    val invoiceNumber: String = "",
    val date: Long = System.currentTimeMillis(),
    val dueDate: Long = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000), // Default 7 days
    
    // Seller Info
    val sellerName: String = "",
    val sellerAddress: String = "",
    val sellerTaxId: String = "",
    val sellerBankName: String = "",
    val sellerBankAccount: String = "",
    
    // Buyer Info (VEVŐ)
    val customerName: String = "", // Same as buyerName
    val customerAddress: String = "",
    val customerTaxId: String = "",
    
    // Payment Details
    val paymentMethod: String = "Atutalás", // Default: Bank Transfer
    val currency: String = "HUF",
    
    // Items
    val items: List<InvoiceLineItem> = emptyList(),
    
    // Totals
    val totalNet: Double = 0.0,
    val totalVat: Double = 0.0,
    val totalGross: Double = 0.0,
    val attachmentUrl: String = "",
    val attachmentName: String = ""
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "userId" to userId,
            "invoiceNumber" to invoiceNumber,
            "date" to date,
            "dueDate" to dueDate,
            "sellerName" to sellerName,
            "sellerAddress" to sellerAddress,
            "sellerTaxId" to sellerTaxId,
            "sellerBankName" to sellerBankName,
            "sellerBankAccount" to sellerBankAccount,
            "customerName" to customerName,
            "customerAddress" to customerAddress,
            "customerTaxId" to customerTaxId,
            "paymentMethod" to paymentMethod,
            "currency" to currency,
            "items" to items.map { it.toMap() },
            "totalNet" to totalNet,
            "totalVat" to totalVat,
            "totalGross" to totalGross,
            "attachmentUrl" to attachmentUrl,
            "attachmentName" to attachmentName
        )
    }
}

data class InvoiceLineItem(
    val description: String = "",
    val quantity: Double = 0.0,
    val unit: String = "db",
    val unitPrice: Double = 0.0,
    val netPrice: Double = 0.0,
    val vatRate: Double = 27.0, // Default 27% (Hungarian VAT)
    val vatAmount: Double = 0.0,
    val grossPrice: Double = 0.0
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "description" to description,
            "quantity" to quantity,
            "unit" to unit,
            "unitPrice" to unitPrice,
            "netPrice" to netPrice,
            "vatRate" to vatRate,
            "vatAmount" to vatAmount,
            "grossPrice" to grossPrice
        )
    }
}
