package com.example.billing.common;

import java.util.List;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import com.example.billing.dto.InvoiceResponse;
import com.example.billing.entities.Invoice;

@Mapper(componentModel = "spring")
public interface InvoiceResponseMapper {

    @Mappings({
        @Mapping(source = "customerId", target = "customer"),
        @Mapping(source = "id", target = "invoiceId")
    })
    InvoiceResponse InvoiceToInvoiceResponse(Invoice source);

    @InheritInverseConfiguration
    Invoice InvoiceResponseToInvoice(InvoiceResponse source);

    List<InvoiceResponse> InvoiceListToInvoiceResponseList(List<Invoice> source);

    @InheritInverseConfiguration
    List<Invoice> InvoiceResponseListToInvoiceList(List<InvoiceResponse> source);
}

