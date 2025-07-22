package com.example.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.Map;

@Schema(name = "BatchUpdateRequest", description = "Batch update request with criteria and updates")
@Data
public class BatchUpdateRequest {
    
    @Schema(description = "Criteria to match invoices for update")
    private Map<String, Object> criteria;
    
    @Schema(description = "Fields to update with new values")
    private Map<String, Object> updates;
    
    @Schema(description = "Dry run mode - validate without updating", example = "false")
    private boolean dryRun = false;
    
    @Schema(description = "Operation description", example = "Update all invoices for customer 12")
    private String description;
    
    @Schema(description = "Stop on first error", example = "false")
    private boolean stopOnFirstError = false;
    
    // Validation methods
    public boolean hasCriteria() {
        return criteria != null && !criteria.isEmpty();
    }
    
    public boolean hasUpdates() {
        return updates != null && !updates.isEmpty();
    }
}