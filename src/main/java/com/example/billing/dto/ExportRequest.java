package com.example.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Schema(name = "ExportRequest", description = "Export request with filtering options")
@Data
public class ExportRequest {
    
    @Schema(description = "Export format: summary, detailed, analytics", example = "detailed")
    private String format = "summary";
    
    @Schema(description = "List of customer IDs to include in export")
    private List<String> customerIds;
    
    @Schema(description = "Minimum amount filter", example = "100.0")
    private Double minAmount;
    
    @Schema(description = "Maximum amount filter", example = "2000.0")
    private Double maxAmount;
    
    @Schema(description = "Include invoices with zero amount", example = "true")
    private boolean includeZeroAmount = true;
    
    @Schema(description = "Report title", example = "Monthly Invoice Report")
    private String reportTitle;
    
    @Schema(description = "Additional metadata to include")
    private String metadata;
}
