package com.example.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(name = "InvoiceSearchRequest", description = "Search criteria for invoices")
@Data
public class InvoiceSearchRequest {
    
    @Schema(description = "Customer ID to filter by", example = "12")
    private String customerId;
    
    @Schema(description = "Invoice number to search for", example = "12")
    private String invoiceNumber;
    
    @Schema(description = "Minimum invoice amount", example = "100.0")
    private Double minAmount;
    
    @Schema(description = "Maximum invoice amount", example = "2000.0")
    private Double maxAmount;
    
    @Schema(description = "Text to search within invoice details", example = "equipos")
    private String detailContains;
    
    @Schema(description = "Sort field", example = "amount")
    private String sortBy;
    
    @Schema(description = "Sort direction", example = "DESC")
    private String sortDirection = "ASC";
    
    @Schema(description = "Maximum results to return", example = "100")
    private Integer maxResults;
    
    @Schema(description = "Include zero amount invoices", example = "true")
    private Boolean includeZeroAmount = true;
    
    @Schema(description = "Exact customer match", example = "true")
    private Boolean exactCustomerMatch = true;
}