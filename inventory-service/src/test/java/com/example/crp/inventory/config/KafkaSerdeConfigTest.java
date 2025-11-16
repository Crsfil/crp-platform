package com.example.crp.inventory.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaConsumerFactoryCustomizer;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaSerdeConfigTest {

    private final KafkaSerdeConfig kafkaSerdeConfig = new KafkaSerdeConfig();

    @Test
    void producerCustomizerSetsJsonSerializers() {
        Map<String, Object> props = new HashMap<>();
        DefaultKafkaProducerFactoryCustomizer customizer = kafkaSerdeConfig.producerCustomizer();
        DefaultKafkaProducerFactory<Object, Object> factory = new DefaultKafkaProducerFactory<>(props);

        customizer.customize(factory);

        Map<String, Object> configured = factory.getConfigurationProperties();
        assertThat(configured.get(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG)).isEqualTo(StringSerializer.class);
        assertThat(configured.get(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG)).isEqualTo(JsonSerializer.class);
    }

    @Test
    void consumerCustomizerSetsJsonDeserializers() {
        Map<String, Object> props = new HashMap<>();
        DefaultKafkaConsumerFactoryCustomizer customizer = kafkaSerdeConfig.consumerCustomizer();
        DefaultKafkaConsumerFactory<Object, Object> factory = new DefaultKafkaConsumerFactory<>(props);

        customizer.customize(factory);

        Map<String, Object> configured = factory.getConfigurationProperties();
        assertThat(configured.get(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG)).isEqualTo(StringDeserializer.class);
        assertThat(configured.get(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG)).isEqualTo(JsonDeserializer.class);
        assertThat(configured.get(JsonDeserializer.TRUSTED_PACKAGES)).isEqualTo("*");
    }
}
