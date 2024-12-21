package com.example.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
@Schema(name = "InvoiceResponse", description = "Modelo que representa una factura en la base de datos")
@Data
public class InvoiceResponse {
    
    @Schema(name = "invoiceId", required = true, example = "675cb6cdfd2c5b52e880d9ac", defaultValue = "1", description = "Identificador único de la factura en la base de datos")
    private String invoiceId;
    @Schema(name = "customer", required = true, example = "5d2f1f2e1c9d440000a19cac", defaultValue = "1", description = "Identificador único del cliente que posee la factura")
    private String customer;
    @Schema(name = "number", required = true, example = "3", defaultValue = "8", description = "Número dado en la factura física")
    private String number;
    private String detail;
    private double amount;  
}
