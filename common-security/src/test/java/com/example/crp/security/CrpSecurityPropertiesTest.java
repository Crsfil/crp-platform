package com.example.crp.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CrpSecurityPropertiesTest {

    @Test
    void enforceAudienceIsTrueByDefault() {
        CrpSecurityProperties props = new CrpSecurityProperties();
        assertThat(props.isEnforceAudience()).isTrue();
    }
}

