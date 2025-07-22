package com.example.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;
import java.time.LocalDateTime;

@Schema(name = "BulkOperationResponse", description = "Bulk operation result")
@Data
public class BulkOperationResponse {
    
    @Schema(description = "Number of successful operations", example = "3")
    private int successCount;
    
    @Schema(description = "Number of failed operations", example = "1")
    private int errorCount;
    
    @Schema(description = "Successfully created invoices")
    private List<InvoiceResponse> createdInvoices;
    
    @Schema(description = "Successfully deleted IDs")
    private List<String> deletedIds;
    
    @Schema(description = "Error messages")
    private List<String> errors;
    
    @Schema(description = "Operation result message")
    private String message;
    
    @Schema(description = "Completion timestamp")
    private LocalDateTime completedAt;
    
    @Schema(description = "Operation duration in milliseconds")
    private Long durationMs;
    
    @Schema(description = "Operation type")
    private String operationType;
    
    // Convenience methods
    public int getTotalProcessed() {
        return successCount + errorCount;
    }
    
    public double getSuccessRate() {
        if (getTotalProcessed() == 0) return 0.0;
        return (double) successCount / getTotalProcessed() * 100.0;
    }
}