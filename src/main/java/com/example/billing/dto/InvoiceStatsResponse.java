package com.example.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.Map;

@Schema(name = "InvoiceStatsResponse", description = "Invoice statistics")
@Data
public class InvoiceStatsResponse {
    
    @Schema(description = "Total number of invoices", example = "2")
    private int totalInvoices;
    
    @Schema(description = "Sum of all amounts", example = "2533.5")
    private double totalAmount;
    
    @Schema(description = "Average amount", example = "1266.75")
    private double averageAmount;
    
    @Schema(description = "Maximum amount", example = "1333.0")
    private double maxAmount;
    
    @Schema(description = "Minimum amount", example = "1200.5")
    private double minAmount;
    
    @Schema(description = "Number of unique customers", example = "1")
    private int uniqueCustomers;
    
    @Schema(description = "Invoice count per customer")
    private Map<String, Long> customerInvoiceCounts;
    
    @Schema(description = "Additional metadata")
    private Map<String, Object> metadata;
    
    @Schema(description = "Generation timestamp")
    private String generatedAt;
}