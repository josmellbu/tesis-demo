package com.example.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(name = "InvoiceRequest", description = "Model representing an invoice request")
@Data
public class InvoiceRequest {
   @Schema(name = "customer", requiredMode = Schema.RequiredMode.REQUIRED,example = "2", defaultValue = "1", description = "Unique Id of customer that represent the owner of invoice")
   private String customer;
   @Schema(name = "number", requiredMode = Schema.RequiredMode.REQUIRED,example = "3", defaultValue = "8", description = "Number given on fisical invoice")
   private String number;
   private String detail;
   private double amount;
}

