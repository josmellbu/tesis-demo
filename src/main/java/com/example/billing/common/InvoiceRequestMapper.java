package com.example.billing.common;

import java.util.List;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import com.example.billing.dto.InvoiceRequest;
import com.example.billing.entities.Invoice;

@Mapper(componentModel = "spring")
public interface InvoiceRequestMapper {

    @Mappings({
        @Mapping(source = "customer", target = "customerId"), // Mapea `customer` de InvoiceRequest a `customerId` en Invoice
        @Mapping(target = "id", ignore = true) // Ignora `id` en Invoice
    })
    Invoice InvoiceRequestToInvoice(InvoiceRequest source);

    @InheritInverseConfiguration
    InvoiceRequest InvoiceToInvoiceRequest(Invoice source);

    List<Invoice> InvoiceRequestListToInvoiceList(List<InvoiceRequest> source);

    @InheritInverseConfiguration
    List<InvoiceRequest> InvoiceListToInvoiceRequestList(List<Invoice> source);
}
