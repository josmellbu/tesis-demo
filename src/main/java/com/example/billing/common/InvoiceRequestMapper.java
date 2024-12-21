package com.example.billing.common;

import java.util.List;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import com.example.billing.dto.InvoiceRequest;
import com.example.billing.entities.Invoice;

/**
 *
 * @author sotobotero
 */
@Mapper(componentModel = "spring")
public interface InvoiceRequestMapper {

    @Mappings({
        @Mapping(source = "customer", target = "customerId"),
        @Mapping(target = "id", ignore = true)}
    )
    Invoice InvoiceRequestToInvoice(InvoiceRequest source);

    List<Invoice> InvoiceRequestListToInvoiceList(List<InvoiceRequest> source);

    @InheritInverseConfiguration
    InvoiceRequest InvoiceToInvoiceRequest(Invoice source);

    @InheritInverseConfiguration
    List<InvoiceRequest> InvoiceListToInvoiceRequestList(List<Invoice> source);

}
