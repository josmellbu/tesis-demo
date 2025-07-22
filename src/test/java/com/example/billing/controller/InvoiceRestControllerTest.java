package com.example.billing.controller;

import com.example.billing.common.InvoiceRequestMapper;
import com.example.billing.common.InvoiceResponseMapper;
import com.example.billing.dto.*;
import com.example.billing.entities.Invoice;
import com.example.billing.respository.InvoiceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

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

    // Sample data based on the user's actual database
    private Invoice sampleInvoice1;
    private Invoice sampleInvoice2;
    private InvoiceRequest sampleInvoiceRequest;
    private InvoiceResponse sampleInvoiceResponse1;
    private InvoiceResponse sampleInvoiceResponse2;
    private List<Invoice> sampleInvoiceList;
    private List<InvoiceResponse> sampleResponseList;

    @BeforeEach
    void setUp() {
        // Create sample data matching user's database
        sampleInvoice1 = new Invoice("3d646c6d-0ae1-453a-9203-227bb0acb7f7", "12", "12", 
                "Compra de equipos informáticos", 1200.5);
        sampleInvoice2 = new Invoice("1ca2a007-42ae-455d-aa1f-a9f21edc2a56", "12", "12", 
                "algo", 1333.0);
        
        sampleInvoiceRequest = new InvoiceRequest();
        sampleInvoiceRequest.setCustomer("12");
        sampleInvoiceRequest.setNumber("13");
        sampleInvoiceRequest.setDetail("Test invoice");
        sampleInvoiceRequest.setAmount(500.0);

        sampleInvoiceResponse1 = new InvoiceResponse();
        sampleInvoiceResponse1.setInvoiceId("3d646c6d-0ae1-453a-9203-227bb0acb7f7");
        sampleInvoiceResponse1.setCustomer("12");
        sampleInvoiceResponse1.setNumber("12");
        sampleInvoiceResponse1.setDetail("Compra de equipos informáticos");
        sampleInvoiceResponse1.setAmount(1200.5);

        sampleInvoiceResponse2 = new InvoiceResponse();
        sampleInvoiceResponse2.setInvoiceId("1ca2a007-42ae-455d-aa1f-a9f21edc2a56");
        sampleInvoiceResponse2.setCustomer("12");
        sampleInvoiceResponse2.setNumber("12");
        sampleInvoiceResponse2.setDetail("algo");
        sampleInvoiceResponse2.setAmount(1333.0);

        sampleInvoiceList = Arrays.asList(sampleInvoice1, sampleInvoice2);
        sampleResponseList = Arrays.asList(sampleInvoiceResponse1, sampleInvoiceResponse2);
    }
    
    @Test
    @DisplayName("Should return all invoices when invoices exist")
    void shouldReturnAllInvoicesWhenInvoicesExist() throws Exception {
        // Given
        given(billingRepository.findAll()).willReturn(sampleInvoiceList);
        given(invoiceResponseMapper.InvoiceListToInvoiceResponseList(sampleInvoiceList))
                .willReturn(sampleResponseList);

        // When & Then
        mockMvc.perform(get("/billing/v1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].invoiceId").value("3d646c6d-0ae1-453a-9203-227bb0acb7f7"))
                .andExpect(jsonPath("$[0].customer").value("12"))
                .andExpect(jsonPath("$[0].amount").value(1200.5))
                .andExpect(jsonPath("$[1].invoiceId").value("1ca2a007-42ae-455d-aa1f-a9f21edc2a56"))
                .andExpect(jsonPath("$[1].amount").value(1333.0));
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
        String invoiceId = "3d646c6d-0ae1-453a-9203-227bb0acb7f7";
        given(billingRepository.findById(invoiceId)).willReturn(Optional.of(sampleInvoice1));
        given(invoiceResponseMapper.InvoiceToInvoiceResponse(sampleInvoice1))
                .willReturn(sampleInvoiceResponse1);

        // When & Then
        mockMvc.perform(get("/billing/v1/{id}", invoiceId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.invoiceId").value(invoiceId))
                .andExpect(jsonPath("$.customer").value("12"))
                .andExpect(jsonPath("$.amount").value(1200.5));
    }

    @Test
    @DisplayName("Should create new invoice successfully")
    void shouldCreateNewInvoiceSuccessfully() throws Exception {
        // Given
        Invoice invoiceToSave = new Invoice(null, "12", "13", "Test invoice", 500.0);
        Invoice savedInvoice = new Invoice("new-uuid", "12", "13", "Test invoice", 500.0);
        InvoiceResponse expectedResponse = new InvoiceResponse();
        expectedResponse.setInvoiceId("new-uuid");
        expectedResponse.setCustomer("12");
        expectedResponse.setNumber("13");
        expectedResponse.setAmount(500.0);
        
        given(invoiceRequestMapper.InvoiceRequestToInvoice(any(InvoiceRequest.class)))
                .willReturn(invoiceToSave);
        given(billingRepository.save(any(Invoice.class))).willReturn(savedInvoice);
        given(invoiceResponseMapper.InvoiceToInvoiceResponse(savedInvoice))
                .willReturn(expectedResponse);

        // When & Then
        mockMvc.perform(post("/billing/v1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleInvoiceRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.customer").value("12"))
                .andExpect(jsonPath("$.number").value("13"));

        verify(billingRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should perform advanced search successfully")
    void shouldPerformAdvancedSearchSuccessfully() throws Exception {
        // Given
        InvoiceSearchRequest searchRequest = new InvoiceSearchRequest();
        searchRequest.setCustomerId("12");
        
        given(billingRepository.findAll()).willReturn(sampleInvoiceList);
        given(invoiceResponseMapper.InvoiceListToInvoiceResponseList(anyList()))
                .willReturn(sampleResponseList);

        // When & Then
        mockMvc.perform(post("/billing/v1/search")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(searchRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].customer").value("12"))
                .andExpect(jsonPath("$[1].customer").value("12"));
    }

    @Test
    @DisplayName("Should search by amount range successfully")
    void shouldSearchByAmountRangeSuccessfully() throws Exception {
        // Given
        InvoiceSearchRequest searchRequest = new InvoiceSearchRequest();
        searchRequest.setMinAmount(1300.0);
        searchRequest.setMaxAmount(1400.0);
        
        given(billingRepository.findAll()).willReturn(sampleInvoiceList);
        given(invoiceResponseMapper.InvoiceListToInvoiceResponseList(anyList()))
                .willReturn(Arrays.asList(sampleInvoiceResponse2)); // Only the 1333.0 amount invoice

        // When & Then
        mockMvc.perform(post("/billing/v1/search")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(searchRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].amount").value(1333.0));
    }

    @Test
    @DisplayName("Should search by detail text successfully")
    void shouldSearchByDetailTextSuccessfully() throws Exception {
        // Given
        InvoiceSearchRequest searchRequest = new InvoiceSearchRequest();
        searchRequest.setDetailContains("equipos");
        
        given(billingRepository.findAll()).willReturn(sampleInvoiceList);
        given(invoiceResponseMapper.InvoiceListToInvoiceResponseList(anyList()))
                .willReturn(Arrays.asList(sampleInvoiceResponse1)); // Only the "equipos" invoice

        // When & Then
        mockMvc.perform(post("/billing/v1/search")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(searchRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].detail").value("Compra de equipos informáticos"));
    }

    @Test
    @DisplayName("Should search with sorting successfully")
    void shouldSearchWithSortingSuccessfully() throws Exception {
        // Given
        InvoiceSearchRequest searchRequest = new InvoiceSearchRequest();
        searchRequest.setCustomerId("12");
        searchRequest.setSortBy("amount");
        searchRequest.setSortDirection("DESC");
        
        given(billingRepository.findAll()).willReturn(sampleInvoiceList);
        given(invoiceResponseMapper.InvoiceListToInvoiceResponseList(anyList()))
                .willReturn(Arrays.asList(sampleInvoiceResponse2, sampleInvoiceResponse1)); // Sorted by amount DESC

        // When & Then
        mockMvc.perform(post("/billing/v1/search")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(searchRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].amount").value(1333.0)) // Highest amount first
                .andExpect(jsonPath("$[1].amount").value(1200.5));
    }

    @Test
    @DisplayName("Should return empty search results with 404")
    void shouldReturnEmptySearchResultsWith404() throws Exception {
        // Given
        InvoiceSearchRequest searchRequest = new InvoiceSearchRequest();
        searchRequest.setCustomerId("999"); // Non-existent customer
        
        given(billingRepository.findAll()).willReturn(sampleInvoiceList);

        // When & Then
        mockMvc.perform(post("/billing/v1/search")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(searchRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should get invoices by customer successfully")
    void shouldGetInvoicesByCustomerSuccessfully() throws Exception {
        // Given
        String customerId = "12";
        given(billingRepository.findAll()).willReturn(sampleInvoiceList);
        given(invoiceResponseMapper.InvoiceListToInvoiceResponseList(anyList()))
                .willReturn(sampleResponseList);

        // When & Then
        mockMvc.perform(get("/billing/v1/customer/{customerId}", customerId)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].customer").value("12"))
                .andExpect(jsonPath("$.content[1].customer").value("12"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("Should return 404 for non-existent customer")
    void shouldReturn404ForNonExistentCustomer() throws Exception {
        // Given
        String customerId = "999";
        given(billingRepository.findAll()).willReturn(sampleInvoiceList);

        // When & Then
        mockMvc.perform(get("/billing/v1/customer/{customerId}", customerId)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should get invoice statistics successfully")
    void shouldGetInvoiceStatisticsSuccessfully() throws Exception {
        // Given
        given(billingRepository.findAll()).willReturn(sampleInvoiceList);

        // When & Then
        mockMvc.perform(get("/billing/v1/statistics"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalInvoices").value(2))
                .andExpect(jsonPath("$.totalAmount").value(2533.5)) // 1200.5 + 1333
                .andExpect(jsonPath("$.averageAmount").value(1266.75))
                .andExpect(jsonPath("$.maxAmount").value(1333.0))
                .andExpect(jsonPath("$.minAmount").value(1200.5))
                .andExpect(jsonPath("$.uniqueCustomers").value(1))
                .andExpect(jsonPath("$.customerInvoiceCounts.12").value(2));
    }

    @Test
    @DisplayName("Should return 404 for statistics when no invoices")
    void shouldReturn404ForStatisticsWhenNoInvoices() throws Exception {
        // Given
        given(billingRepository.findAll()).willReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/billing/v1/statistics"))
                .andExpect(status().isNotFound());
    }

    

    @Test
    @DisplayName("Should get invoices by amount range successfully")
    void shouldGetInvoicesByAmountRangeSuccessfully() throws Exception {
        // Given
        given(billingRepository.findAll()).willReturn(sampleInvoiceList);
        given(invoiceResponseMapper.InvoiceListToInvoiceResponseList(anyList()))
                .willReturn(sampleResponseList);

        // When & Then
        mockMvc.perform(get("/billing/v1/amount-range")
                .param("minAmount", "1200")
                .param("maxAmount", "1400"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].amount").value(1200.5))
                .andExpect(jsonPath("$[1].amount").value(1333.0));
    }

    @Test
    @DisplayName("Should return 400 for invalid amount range")
    void shouldReturn400ForInvalidAmountRange() throws Exception {
        // When & Then - minAmount > maxAmount
        mockMvc.perform(get("/billing/v1/amount-range")
                .param("minAmount", "1500")
                .param("maxAmount", "1000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should patch invoice successfully")
    void shouldPatchInvoiceSuccessfully() throws Exception {
        // Given
        String invoiceId = "3d646c6d-0ae1-453a-9203-227bb0acb7f7";
        Map<String, Object> updates = new HashMap<>();
        updates.put("amount", 1500.0);
        updates.put("detail", "Compra de equipos informáticos - ACTUALIZADO");

        Invoice updatedInvoice = new Invoice(invoiceId, "12", "12", 
                "Compra de equipos informáticos - ACTUALIZADO", 1500.0);
        InvoiceResponse updatedResponse = new InvoiceResponse();
        updatedResponse.setInvoiceId(invoiceId);
        updatedResponse.setAmount(1500.0);
        updatedResponse.setDetail("Compra de equipos informáticos - ACTUALIZADO");

        given(billingRepository.findById(invoiceId)).willReturn(Optional.of(sampleInvoice1));
        given(billingRepository.save(any(Invoice.class))).willReturn(updatedInvoice);
        given(invoiceResponseMapper.InvoiceToInvoiceResponse(updatedInvoice))
                .willReturn(updatedResponse);

        // When & Then
        mockMvc.perform(patch("/billing/v1/{id}", invoiceId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.amount").value(1500.0))
                .andExpect(jsonPath("$.detail").value("Compra de equipos informáticos - ACTUALIZADO"));
    }

    @Test
    @DisplayName("Should return 404 when patching non-existent invoice")
    void shouldReturn404WhenPatchingNonExistentInvoice() throws Exception {
        // Given
        String invoiceId = "non-existent";
        Map<String, Object> updates = new HashMap<>();
        updates.put("amount", 1500.0);

        given(billingRepository.findById(invoiceId)).willReturn(Optional.empty());

        // When & Then
        mockMvc.perform(patch("/billing/v1/{id}", invoiceId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should create bulk invoices successfully")
    void shouldCreateBulkInvoicesSuccessfully() throws Exception {
        // Given
        InvoiceRequest request1 = new InvoiceRequest();
        request1.setCustomer("13");
        request1.setNumber("INV-001");
        request1.setDetail("Bulk invoice 1");
        request1.setAmount(100.0);

        InvoiceRequest request2 = new InvoiceRequest();
        request2.setCustomer("14");
        request2.setNumber("INV-002");
        request2.setDetail("Bulk invoice 2");
        request2.setAmount(200.0);

        BulkInvoiceRequest bulkRequest = new BulkInvoiceRequest();
        bulkRequest.setInvoices(Arrays.asList(request1, request2));

        // Mock successful saves
        Invoice savedInvoice1 = new Invoice("uuid1", "13", "INV-001", "Bulk invoice 1", 100.0);
        Invoice savedInvoice2 = new Invoice("uuid2", "14", "INV-002", "Bulk invoice 2", 200.0);
        
        InvoiceResponse response1 = new InvoiceResponse();
        response1.setInvoiceId("uuid1");
        response1.setCustomer("13");
        
        InvoiceResponse response2 = new InvoiceResponse();
        response2.setInvoiceId("uuid2");
        response2.setCustomer("14");

        given(invoiceRequestMapper.InvoiceRequestToInvoice(any(InvoiceRequest.class)))
                .willReturn(savedInvoice1, savedInvoice2);
        given(billingRepository.save(any(Invoice.class)))
                .willReturn(savedInvoice1, savedInvoice2);
        given(invoiceResponseMapper.InvoiceToInvoiceResponse(any(Invoice.class)))
                .willReturn(response1, response2);

        // When & Then
        mockMvc.perform(post("/billing/v1/bulk")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bulkRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.successCount").value(2))
                .andExpect(jsonPath("$.errorCount").value(0))
                .andExpect(jsonPath("$.operationType").value("BULK_CREATE"));

        verify(billingRepository, times(2)).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should handle bulk creation with validation errors")
    void shouldHandleBulkCreationWithValidationErrors() throws Exception {
        // Given - One valid, one invalid invoice
        InvoiceRequest validRequest = new InvoiceRequest();
        validRequest.setCustomer("13");
        validRequest.setNumber("INV-001");
        validRequest.setAmount(100.0);

        InvoiceRequest invalidRequest = new InvoiceRequest();
        // Missing customer and invalid amount
        invalidRequest.setNumber("INV-002");
        invalidRequest.setAmount(-50.0);

        BulkInvoiceRequest bulkRequest = new BulkInvoiceRequest();
        bulkRequest.setInvoices(Arrays.asList(validRequest, invalidRequest));

        // Mock successful save for valid invoice
        Invoice savedInvoice = new Invoice("uuid1", "13", "INV-001", null, 100.0);
        InvoiceResponse response = new InvoiceResponse();
        response.setInvoiceId("uuid1");

        given(invoiceRequestMapper.InvoiceRequestToInvoice(any(InvoiceRequest.class)))
                .willReturn(savedInvoice);
        given(billingRepository.save(any(Invoice.class))).willReturn(savedInvoice);
        given(invoiceResponseMapper.InvoiceToInvoiceResponse(any(Invoice.class)))
                .willReturn(response);

        // When & Then
        mockMvc.perform(post("/billing/v1/bulk")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bulkRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.successCount").value(1))
                .andExpect(jsonPath("$.errorCount").value(1))
                .andExpect(jsonPath("$.errors[0]").value(Matchers.containsString("Customer ID is required")));
    }

    @Test
    @DisplayName("Should delete bulk invoices successfully")
    void shouldDeleteBulkInvoicesSuccessfully() throws Exception {
        // Given
        List<String> invoiceIds = Arrays.asList(
                "3d646c6d-0ae1-453a-9203-227bb0acb7f7", 
                "1ca2a007-42ae-455d-aa1f-a9f21edc2a56"
        );

        given(billingRepository.findById("3d646c6d-0ae1-453a-9203-227bb0acb7f7"))
                .willReturn(Optional.of(sampleInvoice1));
        given(billingRepository.findById("1ca2a007-42ae-455d-aa1f-a9f21edc2a56"))
                .willReturn(Optional.of(sampleInvoice2));

        // When & Then
        mockMvc.perform(delete("/billing/v1/bulk")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invoiceIds)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.successCount").value(2))
                .andExpect(jsonPath("$.errorCount").value(0))
                .andExpect(jsonPath("$.operationType").value("BULK_DELETE"));

        verify(billingRepository).delete(sampleInvoice1);
        verify(billingRepository).delete(sampleInvoice2);
    }

    @Test
    @DisplayName("Should validate invoice successfully")
    void shouldValidateInvoiceSuccessfully() throws Exception {
        // Given
        InvoiceRequest validRequest = new InvoiceRequest();
        validRequest.setCustomer("13");
        validRequest.setNumber("INV-NEW");
        validRequest.setAmount(100.0);

        given(billingRepository.findAll()).willReturn(sampleInvoiceList);

        // When & Then
        mockMvc.perform(post("/billing/v1/validate")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.errors").isEmpty())
                .andExpect(jsonPath("$.warnings").isEmpty());
    }

    @Test
    @DisplayName("Should validate invoice with errors")
    void shouldValidateInvoiceWithErrors() throws Exception {
        // Given
        InvoiceRequest invalidRequest = new InvoiceRequest();
        // Missing customer, invalid amount
        invalidRequest.setNumber("INV-NEW");
        invalidRequest.setAmount(-10.0);

        given(billingRepository.findAll()).willReturn(sampleInvoiceList);

        // When & Then
        mockMvc.perform(post("/billing/v1/validate")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.errors[0]").value("Customer ID is required"))
                .andExpect(jsonPath("$.errors[1]").value("Invoice amount must be greater than zero"));
    }

    @Test
    @DisplayName("Should validate invoice with duplicate number warning")
    void shouldValidateInvoiceWithDuplicateNumberWarning() throws Exception {
        // Given
        InvoiceRequest requestWithDuplicateNumber = new InvoiceRequest();
        requestWithDuplicateNumber.setCustomer("13");
        requestWithDuplicateNumber.setNumber("12"); // Duplicate number
        requestWithDuplicateNumber.setAmount(100.0);

        given(billingRepository.findAll()).willReturn(sampleInvoiceList);

        // When & Then
        mockMvc.perform(post("/billing/v1/validate")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(requestWithDuplicateNumber)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.valid").value(true))
            .andExpect(jsonPath("$.errors").isEmpty())
            .andExpect(jsonPath("$.warnings[0]").value("Invoice number already exists in the system"));
    }

    @Test
    @DisplayName("Should return top customers by total amount successfully")
    void shouldReturnTopCustomersByTotalAmountSuccessfully() throws Exception {
        // Given - usando los datos reales del usuario
        Invoice invoice1 = new Invoice("3d646c6d-0ae1-453a-9203-227bb0acb7f7", "12", "12", 
                "Compra de equipos informáticos - ACTUALIZADO", 1200.5);
        Invoice invoice2 = new Invoice("1ca2a007-42ae-455d-aa1f-a9f21edc2a56", "12", "12", 
                "algo", 1333.0);
        Invoice invoice3 = new Invoice("c3d311be-a08c-4f67-bf3e-fc7e1b8f12ca", "13", "INV-001", 
                "Factura de prueba 1", 500.0);
        Invoice invoice4 = new Invoice("67cc1d09-9e20-4441-918c-4c396d3aa382", "14", "INV-002", 
                "Factura de prueba 2", 750.0);
        Invoice invoice5 = new Invoice("6fc25ddf-c8c6-4afd-a1c5-8a989279a0cb", "15", "INV-003", 
                "Factura de prueba 3", 300.0);
        
        List<Invoice> invoices = Arrays.asList(invoice1, invoice2, invoice3, invoice4, invoice5);
        given(billingRepository.findAll()).willReturn(invoices);
    
        // When & Then
        mockMvc.perform(get("/billing/v1/top-customers")
            .param("metric", "total_amount")
            .param("limit", "3"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.topCustomers").isArray())
            .andExpect(jsonPath("$.topCustomers[0].customerId").value("12")) // Customer 12 has highest total (2533.5)
            .andExpect(jsonPath("$.topCustomers[0].totalAmount").value(2533.5))
            .andExpect(jsonPath("$.topCustomers[1].customerId").value("14")) // Customer 14 has 750.0
            .andExpect(jsonPath("$.topCustomers[1].totalAmount").value(750.0))
            .andExpect(jsonPath("$.metric").value("Total Amount"))
            .andExpect(jsonPath("$.totalCustomers").value(4));
    }
    
    @Test
    @DisplayName("Should return top customers by invoice count successfully")
    void shouldReturnTopCustomersByInvoiceCountSuccessfully() throws Exception {
        // Given
        given(billingRepository.findAll()).willReturn(createSampleInvoicesWithData());
    
        // When & Then
        mockMvc.perform(get("/billing/v1/top-customers")
            .param("metric", "count")
            .param("limit", "2"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.topCustomers").isArray())
            .andExpect(jsonPath("$.topCustomers[0].customerId").value("12")) // Customer 12 has 2 invoices
            .andExpect(jsonPath("$.topCustomers[0].invoiceCount").value(2))
            .andExpect(jsonPath("$.metric").value("Invoice Count"));
    }
    
    @Test
    @DisplayName("Should return top customers by average amount successfully")
    void shouldReturnTopCustomersByAverageAmountSuccessfully() throws Exception {
        // Given
        given(billingRepository.findAll()).willReturn(createSampleInvoicesWithData());
    
        // When & Then
        mockMvc.perform(get("/billing/v1/top-customers")
            .param("metric", "average_amount")
            .param("limit", "3"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.topCustomers").isArray())
            .andExpect(jsonPath("$.topCustomers[0].customerId").value("12")) // Customer 12 has highest average
            .andExpect(jsonPath("$.topCustomers[0].averageAmount").value(1266.75)) // (1200.5 + 1333) / 2
            .andExpect(jsonPath("$.metric").value("Average Amount"));
    }
    
    @Test
    @DisplayName("Should return 400 for invalid top customers metric")
    void shouldReturn400ForInvalidTopCustomersMetric() throws Exception {
        // Given
        given(billingRepository.findAll()).willReturn(createSampleInvoicesWithData());
    
        // When & Then
        mockMvc.perform(get("/billing/v1/top-customers")
                .param("metric", "invalid_metric"))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Should return 404 when no invoices for top customers")
    void shouldReturn404WhenNoInvoicesForTopCustomers() throws Exception {
        // Given
        given(billingRepository.findAll()).willReturn(Collections.emptyList());
    
        // When & Then
        mockMvc.perform(get("/billing/v1/top-customers"))
                .andExpect(status().isNotFound());
    }

    // Helper method to provide sample invoices for top customer tests
    private List<Invoice> createSampleInvoicesWithData() {
        Invoice invoice1 = new Invoice("3d646c6d-0ae1-453a-9203-227bb0acb7f7", "12", "12", 
                "Compra de equipos informáticos", 1200.5);
        Invoice invoice2 = new Invoice("1ca2a007-42ae-455d-aa1f-a9f21edc2a56", "12", "12", 
                "algo", 1333.0);
        Invoice invoice3 = new Invoice("c3d311be-a08c-4f67-bf3e-fc7e1b8f12ca", "13", "INV-001", 
                "Factura de prueba 1", 500.0);
        Invoice invoice4 = new Invoice("67cc1d09-9e20-4441-918c-4c396d3aa382", "14", "INV-002", 
                "Factura de prueba 2", 750.0);
        Invoice invoice5 = new Invoice("6fc25ddf-c8c6-4afd-a1c5-8a989279a0cb", "15", "INV-003", 
                "Factura de prueba 3", 300.0);
        return Arrays.asList(invoice1, invoice2, invoice3, invoice4, invoice5);
    }
}