package com.example.crp.billing.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {
    @Bean public NewTopic topicPaymentReceived(){ return new NewTopic("payment.received",1,(short)1);} 
    @Bean public NewTopic topicInvoiceIssued(){ return new NewTopic("invoice.issued",1,(short)1);} 
    @Bean public NewTopic topicInvoicePaid(){ return new NewTopic("invoice.paid",1,(short)1);} 
}

