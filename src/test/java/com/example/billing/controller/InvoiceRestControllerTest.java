package com.example.billing.controller;

import com.example.billing.common.InvoiceRequestMapper;
import com.example.billing.common.InvoiceResponseMapper;
import com.example.billing.dto.InvoiceRequest;
import com.example.billing.dto.InvoiceResponse;
import com.example.billing.entities.Invoice;
import com.example.billing.respository.InvoiceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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

    @Autowired
    private ObjectMapper objectMapper;

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

    @Test
    @DisplayName("Should throw BusinessRuleException when no invoices found")
    void shouldThrowBusinessRuleExceptionWhenNoInvoicesFound() throws Exception {
        // Given
        given(billingRepository.findAll()).willReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/billing/v1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return invoice by ID when invoice exists")
    void shouldReturnInvoiceByIdWhenInvoiceExists() throws Exception {
        // Given
        String invoiceId = "1";
        given(billingRepository.findById(invoiceId)).willReturn(Optional.of(sampleInvoice));
        given(invoiceResponseMapper.InvoiceToInvoiceResponse(sampleInvoice)).willReturn(sampleInvoiceResponse);

        // When & Then
        mockMvc.perform(get("/billing/v1/{id}", invoiceId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.invoiceId").value("1"))
                .andExpect(jsonPath("$.customer").value("customer1"));
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when invoice not found by ID")
    void shouldThrowBusinessRuleExceptionWhenInvoiceNotFoundById() throws Exception {
        // Given
        String invoiceId = "nonexistent";
        given(billingRepository.findById(invoiceId)).willReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/billing/v1/{id}", invoiceId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return paginated invoices")
    void shouldReturnPaginatedInvoices() throws Exception {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Invoice> invoices = Arrays.asList(sampleInvoice);
        Page<Invoice> invoicePage = new PageImpl<>(invoices, pageable, 1);
        
        given(billingRepository.findAll(any(Pageable.class))).willReturn(invoicePage);
        given(invoiceResponseMapper.InvoiceToInvoiceResponse(sampleInvoice)).willReturn(sampleInvoiceResponse);

        // When & Then
        mockMvc.perform(get("/billing/v1/pageable")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].invoiceId").value("1"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("Should create new invoice successfully")
    void shouldCreateNewInvoiceSuccessfully() throws Exception {
        // Given
        Invoice invoiceToSave = new Invoice(null, "customer1", "INV-001", "Test invoice", 100.0);
        Invoice savedInvoice = new Invoice("generated-id", "customer1", "INV-001", "Test invoice", 100.0);
        
        given(invoiceRequestMapper.InvoiceRequestToInvoice(any(InvoiceRequest.class))).willReturn(invoiceToSave);
        given(billingRepository.save(any(Invoice.class))).willReturn(savedInvoice);
        given(invoiceResponseMapper.InvoiceToInvoiceResponse(savedInvoice)).willReturn(sampleInvoiceResponse);

        // When & Then
        mockMvc.perform(post("/billing/v1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleInvoiceRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.customer").value("customer1"))
                .andExpect(jsonPath("$.number").value("INV-001"));

        verify(billingRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should update existing invoice successfully")
    void shouldUpdateExistingInvoiceSuccessfully() throws Exception {
        // Given
        String invoiceId = "1";
        Invoice updatedInvoice = new Invoice(invoiceId, "customer1", "INV-001-UPDATED", "Updated invoice", 150.0);
        
        given(billingRepository.findById(invoiceId)).willReturn(Optional.of(sampleInvoice));
        given(invoiceRequestMapper.InvoiceRequestToInvoice(any(InvoiceRequest.class))).willReturn(updatedInvoice);
        given(billingRepository.save(any(Invoice.class))).willReturn(updatedInvoice);
        given(invoiceResponseMapper.InvoiceToInvoiceResponse(updatedInvoice)).willReturn(sampleInvoiceResponse);

        // When & Then
        mockMvc.perform(put("/billing/v1/{id}", invoiceId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleInvoiceRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(billingRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should return not found when updating non-existent invoice")
    void shouldReturnNotFoundWhenUpdatingNonExistentInvoice() throws Exception {
        // Given
        String invoiceId = "nonexistent";
        given(billingRepository.findById(invoiceId)).willReturn(Optional.empty());

        // When & Then
        mockMvc.perform(put("/billing/v1/{id}", invoiceId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleInvoiceRequest)))
                .andExpect(status().isNotFound());

        verify(billingRepository, never()).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should delete existing invoice successfully")
    void shouldDeleteExistingInvoiceSuccessfully() throws Exception {
        // Given
        String invoiceId = "1";
        given(billingRepository.findById(invoiceId)).willReturn(Optional.of(sampleInvoice));

        // When & Then
        mockMvc.perform(delete("/billing/v1/{id}", invoiceId)
                .with(csrf()))
                .andExpect(status().isOk());

        verify(billingRepository).delete(sampleInvoice);
    }

    @Test
    @DisplayName("Should return not found when deleting non-existent invoice")
    void shouldReturnNotFoundWhenDeletingNonExistentInvoice() throws Exception {
        // Given
        String invoiceId = "nonexistent";
        given(billingRepository.findById(invoiceId)).willReturn(Optional.empty());

        // When & Then
        mockMvc.perform(delete("/billing/v1/{id}", invoiceId)
                .with(csrf()))
                .andExpect(status().isNotFound());

        verify(billingRepository, never()).delete(any(Invoice.class));
    }

    @Test
    @DisplayName("Should handle missing required fields gracefully")
    void shouldHandleMissingRequiredFieldsGracefully() throws Exception {
        // Given - Request with only partial data
        InvoiceRequest partialRequest = new InvoiceRequest();
        partialRequest.setCustomer("test-customer");
        // number, detail, and amount are missing/default
        
        given(invoiceRequestMapper.InvoiceRequestToInvoice(any(InvoiceRequest.class)))
                .willReturn(new Invoice(null, "test-customer", null, null, 0.0));
        given(billingRepository.save(any(Invoice.class)))
                .willReturn(new Invoice("generated-id", "test-customer", null, null, 0.0));
        given(invoiceResponseMapper.InvoiceToInvoiceResponse(any(Invoice.class)))
                .willReturn(sampleInvoiceResponse);

        // When & Then
        mockMvc.perform(post("/billing/v1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(partialRequest)))
                .andExpect(status().isCreated());
    }
}