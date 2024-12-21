package com.example.billing.respository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.billing.entities.Invoice;

public interface InvoiceRepository extends MongoRepository<Invoice, String>  {
    
}
