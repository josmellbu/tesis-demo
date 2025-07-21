package com.example.billing.common;

import com.example.billing.dto.InvoiceRequest;
import com.example.billing.dto.InvoiceResponse;
import com.example.billing.entities.Invoice;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class InvoiceMapperTest {

    @Autowired
    private InvoiceRequestMapper invoiceRequestMapper;

    @Autowired
    private InvoiceResponseMapper invoiceResponseMapper;

    @Test
    @DisplayName("Should map InvoiceRequest to Invoice correctly")
    void shouldMapInvoiceRequestToInvoiceCorrectly() {
        // Given
        InvoiceRequest request = new InvoiceRequest();
        request.setCustomer("customer123");
        request.setNumber("INV-001");
        request.setDetail("Test invoice detail");
        request.setAmount(250.75);

        // When
        Invoice invoice = invoiceRequestMapper.InvoiceRequestToInvoice(request);

        // Then
        assertThat(invoice).isNotNull();
        assertThat(invoice.getCustomerId()).isEqualTo("customer123");
        assertThat(invoice.getNumber()).isEqualTo("INV-001");
        assertThat(invoice.getDetail()).isEqualTo("Test invoice detail");
        assertThat(invoice.getAmount()).isEqualTo(250.75);
        assertThat(invoice.getId()).isNull(); // Should be ignored
    }

    @Test
    @DisplayName("Should map Invoice to InvoiceRequest correctly")
    void shouldMapInvoiceToInvoiceRequestCorrectly() {
        // Given
        Invoice invoice = new Invoice("inv-123", "customer456", "INV-002", "Another invoice", 500.0);

        // When
        InvoiceRequest request = invoiceRequestMapper.InvoiceToInvoiceRequest(invoice);

        // Then
        assertThat(request).isNotNull();
        assertThat(request.getCustomer()).isEqualTo("customer456");
        assertThat(request.getNumber()).isEqualTo("INV-002");
        assertThat(request.getDetail()).isEqualTo("Another invoice");
        assertThat(request.getAmount()).isEqualTo(500.0);
    }

    @Test
    @DisplayName("Should map Invoice to InvoiceResponse correctly")
    void shouldMapInvoiceToInvoiceResponseCorrectly() {
        // Given
        Invoice invoice = new Invoice("inv-789", "customer789", "INV-003", "Response test", 150.50);

        // When
        InvoiceResponse response = invoiceResponseMapper.InvoiceToInvoiceResponse(invoice);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getInvoiceId()).isEqualTo("inv-789");
        assertThat(response.getCustomer()).isEqualTo("customer789");
        assertThat(response.getNumber()).isEqualTo("INV-003");
        assertThat(response.getDetail()).isEqualTo("Response test");
        assertThat(response.getAmount()).isEqualTo(150.50);
    }

    @Test
    @DisplayName("Should map InvoiceResponse to Invoice correctly")
    void shouldMapInvoiceResponseToInvoiceCorrectly() {
        // Given
        InvoiceResponse response = new InvoiceResponse();
        response.setInvoiceId("resp-123");
        response.setCustomer("customer999");
        response.setNumber("INV-004");
        response.setDetail("Reverse mapping test");
        response.setAmount(75.25);

        // When
        Invoice invoice = invoiceResponseMapper.InvoiceResponseToInvoice(response);

        // Then
        assertThat(invoice).isNotNull();
        assertThat(invoice.getId()).isEqualTo("resp-123");
        assertThat(invoice.getCustomerId()).isEqualTo("customer999");
        assertThat(invoice.getNumber()).isEqualTo("INV-004");
        assertThat(invoice.getDetail()).isEqualTo("Reverse mapping test");
        assertThat(invoice.getAmount()).isEqualTo(75.25);
    }

    @Test
    @DisplayName("Should map list of Invoices to list of InvoiceResponses")
    void shouldMapListOfInvoicesToListOfInvoiceResponses() {
        // Given
        List<Invoice> invoices = Arrays.asList(
                new Invoice("1", "customer1", "INV-001", "First invoice", 100.0),
                new Invoice("2", "customer2", "INV-002", "Second invoice", 200.0),
                new Invoice("3", "customer3", "INV-003", "Third invoice", 300.0)
        );

        // When
        List<InvoiceResponse> responses = invoiceResponseMapper.InvoiceListToInvoiceResponseList(invoices);

        // Then
        assertThat(responses).hasSize(3);
        
        assertThat(responses.get(0).getInvoiceId()).isEqualTo("1");
        assertThat(responses.get(0).getCustomer()).isEqualTo("customer1");
        assertThat(responses.get(0).getAmount()).isEqualTo(100.0);
        
        assertThat(responses.get(1).getInvoiceId()).isEqualTo("2");
        assertThat(responses.get(1).getCustomer()).isEqualTo("customer2");
        assertThat(responses.get(1).getAmount()).isEqualTo(200.0);
        
        assertThat(responses.get(2).getInvoiceId()).isEqualTo("3");
        assertThat(responses.get(2).getCustomer()).isEqualTo("customer3");
        assertThat(responses.get(2).getAmount()).isEqualTo(300.0);
    }

    @Test
    @DisplayName("Should handle null values gracefully")
    void shouldHandleNullValuesGracefully() {
        // Given & When
        Invoice invoiceFromNull = invoiceRequestMapper.InvoiceRequestToInvoice(null);
        InvoiceResponse responseFromNull = invoiceResponseMapper.InvoiceToInvoiceResponse(null);

        // Then
        assertThat(invoiceFromNull).isNull();
        assertThat(responseFromNull).isNull();
    }

    @Test
    @DisplayName("Should handle empty lists correctly")
    void shouldHandleEmptyListsCorrectly() {
        // Given
        List<Invoice> emptyInvoiceList = Arrays.asList();

        // When
        List<InvoiceResponse> emptyResponseList = invoiceResponseMapper.InvoiceListToInvoiceResponseList(emptyInvoiceList);

        // Then
        assertThat(emptyResponseList).isEmpty();
    }

    @Test
    @DisplayName("Should handle partial data mapping")
    void shouldHandlePartialDataMapping() {
        // Given
        InvoiceRequest partialRequest = new InvoiceRequest();
        partialRequest.setCustomer("customer-partial");
        partialRequest.setNumber("INV-PARTIAL");
        // detail and amount are null/default

        // When
        Invoice invoice = invoiceRequestMapper.InvoiceRequestToInvoice(partialRequest);

        // Then
        assertThat(invoice).isNotNull();
        assertThat(invoice.getCustomerId()).isEqualTo("customer-partial");
        assertThat(invoice.getNumber()).isEqualTo("INV-PARTIAL");
        assertThat(invoice.getDetail()).isNull();
        assertThat(invoice.getAmount()).isEqualTo(0.0); // primitive default
    }
}