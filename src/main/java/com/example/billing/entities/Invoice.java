package com.example.billing.entities;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.Id;

@Data
@Document
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {
   @Id
   private String id; 
   private String customerId; 
   private String number;
   private String detail;
   private double amount;  
}

