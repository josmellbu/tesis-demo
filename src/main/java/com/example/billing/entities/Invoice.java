package com.example.billing.entities;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

import org.springframework.data.annotation.Id;

@Data
@Document
public class Invoice {
   @Id
   private String id;
   private String customerId;
   private String number;
   private String detail;
   private double amount;  
}
