/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.example.billing.controller;

import org.springframework.web.bind.annotation.RestController;

import com.example.billing.common.InvoiceRequestMapper;
import com.example.billing.common.InvoiceResponseMapper;
import com.example.billing.dto.BulkInvoiceRequest;
import com.example.billing.dto.BulkOperationResponse;
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
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
            @Parameter(description = "Minimum amount") @RequestParam("minAmount") double minAmount,
            @Parameter(description = "Maximum amount") @RequestParam("maxAmount") double maxAmount) throws BusinessRuleException {
        
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

    @Operation(
        description = "Partial update of an invoice",
        summary = "Update specific fields of an invoice without affecting others"
    )
    @PatchMapping("/{id}")
    public ResponseEntity<InvoiceResponse> patchInvoice(
            @PathVariable("id") String id,
            @RequestBody Map<String, Object> updates) throws BusinessRuleException {
        
        Optional<Invoice> optionalInvoice = billingRepository.findById(id);
        if (!optionalInvoice.isPresent()) {
            throw new BusinessRuleException("NOT_FOUND", "Invoice with ID " + id + " not found", HttpStatus.NOT_FOUND);
        }
        
        Invoice invoice = optionalInvoice.get();
        
        updates.forEach((key, value) -> {
            switch (key) {
                case "detail":
                    invoice.setDetail((String) value);
                    break;
                case "amount":
                    invoice.setAmount(((Number) value).doubleValue());
                    break;
                case "number":
                    invoice.setNumber((String) value);
                    break;
                case "customerId":
                    invoice.setCustomerId((String) value);
                    break;
            }
        });
        
        Invoice savedInvoice = billingRepository.save(invoice);
        InvoiceResponse response = irspm.InvoiceToInvoiceResponse(savedInvoice);
        
        return ResponseEntity.ok(response);
    }

    @Operation(
        description = "Create multiple invoices in a single operation",
        summary = "Bulk create invoices with validation"
    )
    @PostMapping("/bulk")
    public ResponseEntity<BulkOperationResponse> createBulkInvoices(@RequestBody BulkInvoiceRequest bulkRequest) throws BusinessRuleException {
        List<InvoiceRequest> invoiceRequests = bulkRequest.getInvoices();
        
        if (invoiceRequests == null || invoiceRequests.isEmpty()) {
            throw new BusinessRuleException("INVALID_INPUT", "Bulk request must contain at least one invoice", HttpStatus.BAD_REQUEST);
        }
        
        long startTime = System.currentTimeMillis();
        BulkOperationResponse response = new BulkOperationResponse();
        response.setOperationType("BULK_CREATE");
        
        List<InvoiceResponse> createdInvoices = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        for (int i = 0; i < invoiceRequests.size(); i++) {
            try {
                InvoiceRequest request = invoiceRequests.get(i);
                if (request.getCustomer() == null || request.getCustomer().trim().isEmpty()) {
                    errors.add(String.format("Invoice %d: Customer ID is required", i + 1));
                    continue;
                }
                if (request.getAmount() <= 0) {
                    errors.add(String.format("Invoice %d: Amount must be greater than zero", i + 1));
                    continue;
                }
                Invoice invoice = irm.InvoiceRequestToInvoice(request);
                invoice.setId(UUID.randomUUID().toString());
                Invoice savedInvoice = billingRepository.save(invoice);
                createdInvoices.add(irspm.InvoiceToInvoiceResponse(savedInvoice));
            } catch (Exception e) {
                errors.add(String.format("Invoice %d: %s", i + 1, e.getMessage()));
            }
        }
        
        response.setSuccessCount(createdInvoices.size());
        response.setErrorCount(errors.size());
        response.setCreatedInvoices(createdInvoices);
        response.setErrors(errors);
        response.setCompletedAt(LocalDateTime.now());
        response.setDurationMs(System.currentTimeMillis() - startTime);
        response.setMessage(String.format("Processed %d invoices: %d successful, %d failed", 
            invoiceRequests.size(), createdInvoices.size(), errors.size()));
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
        description = "Delete multiple invoices by IDs",
        summary = "Bulk delete operation with detailed response"
    )
    @DeleteMapping("/bulk")
    public ResponseEntity<BulkOperationResponse> deleteBulkInvoices(@RequestBody List<String> invoiceIds) throws BusinessRuleException {
        if (invoiceIds == null || invoiceIds.isEmpty()) {
            throw new BusinessRuleException("INVALID_INPUT", "Must provide at least one invoice ID", HttpStatus.BAD_REQUEST);
        }
        
        long startTime = System.currentTimeMillis();
        BulkOperationResponse response = new BulkOperationResponse();
        response.setOperationType("BULK_DELETE");
        
        List<String> deletedIds = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        for (String id : invoiceIds) {
            try {
                Optional<Invoice> invoice = billingRepository.findById(id);
                if (invoice.isPresent()) {
                    billingRepository.delete(invoice.get());
                    deletedIds.add(id);
                } else {
                    errors.add(String.format("Invoice ID %s not found", id));
                }
            } catch (Exception e) {
                errors.add(String.format("Error deleting invoice %s: %s", id, e.getMessage()));
            }
        }
        
        response.setSuccessCount(deletedIds.size());
        response.setErrorCount(errors.size());
        response.setDeletedIds(deletedIds);
        response.setErrors(errors);
        response.setCompletedAt(LocalDateTime.now());
        response.setDurationMs(System.currentTimeMillis() - startTime);
        response.setMessage(String.format("Bulk deletion completed: %d successful, %d failed", 
            deletedIds.size(), errors.size()));
        
        return ResponseEntity.ok(response);
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