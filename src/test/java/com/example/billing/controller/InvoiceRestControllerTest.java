package com.example.billing.controller;

import com.example.billing.common.InvoiceRequestMapper;
import com.example.billing.common.InvoiceResponseMapper;
import com.example.billing.dto.InvoiceRequest;
import com.example.billing.dto.InvoiceResponse;
import com.example.billing.entities.Invoice;
import com.example.billing.respository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InvoiceRestController.class)
@WithMockUser(username = "admin", roles = {"ADMIN"})
class InvoiceRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InvoiceRepository billingRepository;

    @MockBean
    private InvoiceRequestMapper invoiceRequestMapper;

    @MockBean
    private InvoiceResponseMapper invoiceResponseMapper;

    private Invoice sampleInvoice;
    private InvoiceRequest sampleInvoiceRequest;
    private InvoiceResponse sampleInvoiceResponse;

    @BeforeEach
    void setUp() {
        sampleInvoice = new Invoice("1", "customer1", "INV-001", "Test invoice", 100.0);
        
        sampleInvoiceRequest = new InvoiceRequest();
        sampleInvoiceRequest.setCustomer("customer1");
        sampleInvoiceRequest.setNumber("INV-001");
        sampleInvoiceRequest.setDetail("Test invoice");
        sampleInvoiceRequest.setAmount(100.0);

        sampleInvoiceResponse = new InvoiceResponse();
        sampleInvoiceResponse.setInvoiceId("1");
        sampleInvoiceResponse.setCustomer("customer1");
        sampleInvoiceResponse.setNumber("INV-001");
        sampleInvoiceResponse.setDetail("Test invoice");
        sampleInvoiceResponse.setAmount(100.0);
    }

    @Test
    @DisplayName("Should return all invoices when invoices exist")
    void shouldReturnAllInvoicesWhenInvoicesExist() throws Exception {
        // Given
        List<Invoice> invoices = Arrays.asList(sampleInvoice);
        List<InvoiceResponse> invoiceResponses = Arrays.asList(sampleInvoiceResponse);
        
        given(billingRepository.findAll()).willReturn(invoices);
        given(invoiceResponseMapper.InvoiceListToInvoiceResponseList(invoices)).willReturn(invoiceResponses);

        // When & Then
        mockMvc.perform(get("/billing/v1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].invoiceId").value("1"))
                .andExpect(jsonPath("$[0].customer").value("customer1"))
                .andExpect(jsonPath("$[0].number").value("INV-001"));
    }
}