package com.example.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Schema(name = "BulkInvoiceRequest", description = "Bulk invoice creation request")
@Data
public class BulkInvoiceRequest {
    
    @Schema(description = "List of invoices to create", required = true)
    private List<InvoiceRequest> invoices;
    
    @Schema(description = "Stop on first error", example = "false")
    private boolean stopOnFirstError = false;
    
    @Schema(description = "Operation description", example = "Monthly import")
    private String operationDescription;
    
    @Schema(description = "Validate before processing", example = "true")
    private boolean validateBeforeProcessing = true;
}
