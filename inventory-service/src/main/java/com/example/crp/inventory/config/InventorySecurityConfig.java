package com.example.crp.inventory.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(InventoryLocationAbacProperties.class)
public class InventorySecurityConfig {
}

