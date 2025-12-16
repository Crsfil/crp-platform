package com.example.crp.inventory.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {
    @Bean
    public NewTopic topicProcurementApproved() { return new NewTopic("procurement.approved", 1, (short)1); }
    @Bean
    public NewTopic topicProcurementRejected() { return new NewTopic("procurement.rejected", 1, (short)1); }
    @Bean
    public NewTopic topicProcurementGoodsAccepted() { return new NewTopic("procurement.goods_accepted", 1, (short)1); }
    @Bean
    public NewTopic topicInventoryReserved() { return new NewTopic("inventory.reserved", 1, (short)1); }
    @Bean
    public NewTopic topicInventoryReleased() { return new NewTopic("inventory.released", 1, (short)1); }
    @Bean
    public NewTopic topicInventoryReserveFailed() { return new NewTopic("inventory.reserve_failed", 1, (short)1); }
}
