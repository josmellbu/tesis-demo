package com.example.billing.controller;

import com.example.billing.common.InvoiceRequestMapper;
import com.example.billing.common.InvoiceResponseMapper;
import com.example.billing.dto.*;
import com.example.billing.entities.Invoice;
import com.example.billing.respository.InvoiceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
}