package com.example.crp.procurement.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@Configuration
@EnableKafka
public class KafkaConfig {
    @Bean public NewTopic topicRequested(){ return new NewTopic("procurement.requested",1,(short)1);} 
    @Bean public NewTopic topicApproved(){ return new NewTopic("procurement.approved",1,(short)1);} 
    @Bean public NewTopic topicRejected(){ return new NewTopic("procurement.rejected",1,(short)1);} 
    @Bean public NewTopic topicInvReserved(){ return new NewTopic("inventory.reserved",1,(short)1);} 
    @Bean public NewTopic topicInvReserveFailed(){ return new NewTopic("inventory.reserve_failed",1,(short)1);} 
    @Bean public NewTopic topicInvReleased(){ return new NewTopic("inventory.released",1,(short)1);} 
}

