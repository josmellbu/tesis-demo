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
import com.example.billing.entities.Invoice;
import com.example.billing.exception.BusinessRuleException;
import com.example.billing.respository.InvoiceRepository;

import org.springframework.web.bind.annotation.RequestMapping;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
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
}