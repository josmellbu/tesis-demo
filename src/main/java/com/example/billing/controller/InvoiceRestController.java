/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.example.billing.controller;

import org.springframework.web.bind.annotation.RestController;

import com.example.billing.common.InvoiceRequestMapper;
import com.example.billing.common.InvoiceResponseMapper;
import com.example.billing.dto.InvoiceRequest;
import com.example.billing.dto.InvoiceResponse;
import com.example.billing.dto.InvoiceSearchRequest;
import com.example.billing.dto.InvoiceStatsResponse;
import com.example.billing.entities.Invoice;
import com.example.billing.exception.BusinessRuleException;
import com.example.billing.respository.InvoiceRepository;

import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Billing API", description = "This API serves all functionality for management of Invoices")
@RestController
@RequestMapping("/billing/v1")
public class InvoiceRestController {

    @Autowired
    private InvoiceRepository billingRepository;

    @Autowired
    private InvoiceRequestMapper irm;

    @Autowired
    private InvoiceResponseMapper irspm;

    @Operation(description = "Return all invoices bundled into Response", summary = "Return 404 if no data found")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Success"),
        @ApiResponse(responseCode = "404", description = "No invoices found"),
        @ApiResponse(responseCode = "500", description = "Internal error")})
    @GetMapping()
    public List<InvoiceResponse> list() throws BusinessRuleException {
        List<Invoice> findAll = billingRepository.findAll();
        Supplier<BusinessRuleException> exceptionSupplier = () -> new BusinessRuleException("NOT_FOUND", "There are no invoices available", HttpStatus.NOT_FOUND);
        return Optional.ofNullable(findAll)
                .filter(list -> !list.isEmpty())
                .map(irspm::InvoiceListToInvoiceResponseList)
                .orElseThrow(exceptionSupplier);
    }

    @Operation(description = "Get invoice by ID", summary = "Return 404 if invoice not found")
    @GetMapping("/{id}")
    public InvoiceResponse get(@PathVariable("id") String id) throws BusinessRuleException {
        Optional<Invoice> findById = billingRepository.findById(id);
        return findById.map(irspm::InvoiceToInvoiceResponse)
                .orElseThrow(() -> new BusinessRuleException("NOT_FOUND", "Invoice with ID " + id + " not found", HttpStatus.NOT_FOUND));
    }

    @Operation(description = "Get paginated invoices", summary = "Return paginated results")
    @GetMapping("/pageable")
    public Page<InvoiceResponse> getAllPaged(
            @RequestParam("page") int page, 
            @RequestParam("size") int size) throws BusinessRuleException {
        Pageable pageable = PageRequest.of(page, size);
        Page<Invoice> findAll = billingRepository.findAll(pageable);
        if (findAll.isEmpty()) {
            throw new BusinessRuleException("NOT_FOUND", "No invoices found for the requested page", HttpStatus.NOT_FOUND);
        }
        return findAll.map(irspm::InvoiceToInvoiceResponse);
    }

    @Operation(description = "Update an existing invoice", summary = "Update invoice by ID")
    @PutMapping("/{id}")
    public ResponseEntity<?> put(
            @PathVariable("id") String id, 
            @RequestBody InvoiceRequest input) throws BusinessRuleException {
        Optional<Invoice> dtoOptional = billingRepository.findById(id);
        if (dtoOptional.isPresent()) {
            Invoice dtoTransformed = irm.InvoiceRequestToInvoice(input);
            dtoTransformed.setId(dtoOptional.get().getId());
            Invoice dto = billingRepository.save(dtoTransformed);
            return ResponseEntity.ok(irspm.InvoiceToInvoiceResponse(dto));
        } else {
            throw new BusinessRuleException("NOT_FOUND", "Invoice with ID " + id + " not found", HttpStatus.NOT_FOUND);
        }
    }

    @Operation(description = "Create a new invoice", summary = "Create new invoice")
    @PostMapping
    public ResponseEntity<?> post(@RequestBody InvoiceRequest input) {
        Invoice InvoiceRequestToInvoice = irm.InvoiceRequestToInvoice(input);
        String id = UUID.randomUUID().toString();
        InvoiceRequestToInvoice.setId(id);
        Invoice save = billingRepository.save(InvoiceRequestToInvoice);
        InvoiceResponse dto = irspm.InvoiceToInvoiceResponse(save);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @Operation(description = "Delete an invoice", summary = "Delete invoice by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") String id) throws BusinessRuleException {
        Optional<Invoice> dto = billingRepository.findById(id);
        if (!dto.isPresent()) {
            throw new BusinessRuleException("NOT_FOUND", "Invoice with ID " + id + " not found", HttpStatus.NOT_FOUND);
        }
        billingRepository.delete(dto.get());
        return ResponseEntity.ok().build();
    }

    @Operation(
        description = "Search invoices with advanced filtering capabilities",
        summary = "Advanced search with multiple criteria"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search results returned successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid search criteria"),
        @ApiResponse(responseCode = "404", description = "No results found")
    })
    @PostMapping("/search")
    public List<InvoiceResponse> searchInvoices(@RequestBody InvoiceSearchRequest searchRequest) throws BusinessRuleException {
        List<Invoice> allInvoices = billingRepository.findAll();
        List<Invoice> filteredInvoices = allInvoices.stream()
            .filter(invoice -> matchesSearchCriteria(invoice, searchRequest))
            .collect(Collectors.toList());
        
        if (searchRequest.getSortBy() != null) {
            filteredInvoices = applySorting(filteredInvoices, searchRequest);
        }
        
        if (searchRequest.getMaxResults() != null && searchRequest.getMaxResults() > 0) {
            filteredInvoices = filteredInvoices.stream()
                .limit(searchRequest.getMaxResults())
                .collect(Collectors.toList());
        }
        
        if (filteredInvoices.isEmpty()) {
            throw new BusinessRuleException("NOT_FOUND", "No invoices match the search criteria", HttpStatus.NOT_FOUND);
        }
        
        return irspm.InvoiceListToInvoiceResponseList(filteredInvoices);
    }

    @Operation(
        description = "Get invoices by customer ID with pagination",
        summary = "Retrieve all invoices for a specific customer"
    )
    @GetMapping("/customer/{customerId}")
    public Page<InvoiceResponse> getInvoicesByCustomer(
            @Parameter(description = "Customer ID") @PathVariable("customerId") String customerId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) throws BusinessRuleException {
        
        List<Invoice> allInvoices = billingRepository.findAll();
        List<Invoice> customerInvoices = allInvoices.stream()
            .filter(invoice -> customerId.equals(invoice.getCustomerId()))
            .collect(Collectors.toList());
        
        if (customerInvoices.isEmpty()) {
            throw new BusinessRuleException("NOT_FOUND", "No invoices found for customer: " + customerId, HttpStatus.NOT_FOUND);
        }
        
        Pageable pageable = PageRequest.of(page, size);
        int start = page * size;
        int end = Math.min(start + size, customerInvoices.size());
        List<Invoice> pagedInvoices = customerInvoices.subList(start, end);
        List<InvoiceResponse> responses = irspm.InvoiceListToInvoiceResponseList(pagedInvoices);
        
        return new PageImpl<>(responses, pageable, customerInvoices.size());
    }

    @Operation(
        description = "Get comprehensive statistics about invoices",
        summary = "Return analytics data including totals, averages, and counts"
    )
    @GetMapping("/statistics")
    public InvoiceStatsResponse getInvoiceStatistics() throws BusinessRuleException {
        List<Invoice> allInvoices = billingRepository.findAll();
        
        if (allInvoices.isEmpty()) {
            throw new BusinessRuleException("NOT_FOUND", "No invoices available for statistics", HttpStatus.NOT_FOUND);
        }
        
        InvoiceStatsResponse stats = new InvoiceStatsResponse();
        stats.setTotalInvoices(allInvoices.size());
        
        DoubleSummaryStatistics amountStats = allInvoices.stream()
            .mapToDouble(Invoice::getAmount)
            .summaryStatistics();
        
        stats.setTotalAmount(amountStats.getSum());
        stats.setAverageAmount(amountStats.getAverage());
        stats.setMaxAmount(amountStats.getMax());
        stats.setMinAmount(amountStats.getMin());
        
        Map<String, Long> customerCounts = allInvoices.stream()
            .collect(Collectors.groupingBy(Invoice::getCustomerId, Collectors.counting()));
        stats.setUniqueCustomers(customerCounts.size());
        stats.setCustomerInvoiceCounts(customerCounts);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("topCustomerByInvoiceCount", getTopCustomerByCount(customerCounts));
        metadata.put("topCustomerByAmount", getTopCustomerByAmount(allInvoices));
        metadata.put("averageInvoicesPerCustomer", (double) allInvoices.size() / customerCounts.size());
        
        stats.setMetadata(metadata);
        stats.setGeneratedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return stats;
    }

    @Operation(
        description = "Get invoices within a specific amount range",
        summary = "Filter invoices by minimum and maximum amount"
    )
    @GetMapping("/amount-range")
    public List<InvoiceResponse> getInvoicesByAmountRange(
            @Parameter(description = "Minimum amount") @RequestParam double minAmount,
            @Parameter(description = "Maximum amount") @RequestParam double maxAmount) throws BusinessRuleException {
        
        if (minAmount > maxAmount) {
            throw new BusinessRuleException("INVALID_RANGE", "Minimum amount cannot be greater than maximum amount", HttpStatus.BAD_REQUEST);
        }
        
        List<Invoice> allInvoices = billingRepository.findAll();
        List<Invoice> filteredInvoices = allInvoices.stream()
            .filter(invoice -> invoice.getAmount() >= minAmount && invoice.getAmount() <= maxAmount)
            .collect(Collectors.toList());
        
        if (filteredInvoices.isEmpty()) {
            throw new BusinessRuleException("NOT_FOUND", 
                String.format("No invoices found in amount range %.2f - %.2f", minAmount, maxAmount), 
                HttpStatus.NOT_FOUND);
        }
        
        return irspm.InvoiceListToInvoiceResponseList(filteredInvoices);
    }

    private boolean matchesSearchCriteria(Invoice invoice, InvoiceSearchRequest searchRequest) {
        if (searchRequest.getCustomerId() != null && !searchRequest.getCustomerId().isEmpty()) {
            if (!searchRequest.getCustomerId().equals(invoice.getCustomerId())) {
                return false;
            }
        } if (searchRequest.getInvoiceNumber() != null && !searchRequest.getInvoiceNumber().isEmpty()) {
            if (invoice.getNumber() == null || !invoice.getNumber().toLowerCase().contains(searchRequest.getInvoiceNumber().toLowerCase())) {
                return false;
            }
        } if (searchRequest.getMinAmount() != null && invoice.getAmount() < searchRequest.getMinAmount()) {
            return false;
        } if (searchRequest.getMaxAmount() != null && invoice.getAmount() > searchRequest.getMaxAmount()) {
            return false;
        } if (searchRequest.getDetailContains() != null && !searchRequest.getDetailContains().isEmpty()) {
            if (invoice.getDetail() == null || !invoice.getDetail().toLowerCase().contains(searchRequest.getDetailContains().toLowerCase())) {
                return false;
            }
        }
        return true;
    }

    private List<Invoice> applySorting(List<Invoice> invoices, InvoiceSearchRequest request) {
        if (request.getSortBy() == null) {
            return invoices;
        }
        
        java.util.Comparator<Invoice> comparator = getComparator(request.getSortBy());
        
        if ("DESC".equalsIgnoreCase(request.getSortDirection())) {
            comparator = comparator.reversed();
        }
        
        return invoices.stream()
            .sorted(comparator)
            .collect(Collectors.toList());
    }

    private java.util.Comparator<Invoice> getComparator(String sortBy) {
        switch (sortBy.toLowerCase()) {
            case "amount":
                return java.util.Comparator.comparing(Invoice::getAmount);
            case "number":
                return java.util.Comparator.comparing(Invoice::getNumber, java.util.Comparator.nullsLast(String::compareTo));
            case "customerid":
                return java.util.Comparator.comparing(Invoice::getCustomerId);
            case "detail":
                return java.util.Comparator.comparing(Invoice::getDetail, java.util.Comparator.nullsLast(String::compareTo));
            default:
                return java.util.Comparator.comparing(Invoice::getId);
        }
    }

    private String getTopCustomerByCount(Map<String, Long> customerCounts) {
        return customerCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("N/A");
    }

    private String getTopCustomerByAmount(List<Invoice> invoices) {
        Map<String, Double> customerAmounts = invoices.stream()
            .collect(Collectors.groupingBy(
                Invoice::getCustomerId,
                Collectors.summingDouble(Invoice::getAmount)
            ));
        
        return customerAmounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("N/A");
    }

}