package com.microservices.usersmanagement.config;

import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class KeycloakConfig {

    @Autowired
    private Environment env;

    @Bean
    public KeycloakSpringBootConfigResolver keycloakConfigResolver() {
        return new KeycloakSpringBootConfigResolver();
    }


    //TODO refactor
    @Bean
    public Keycloak keycloak() {
        return Keycloak.getInstance("https://127.0.0.1:8443", "microservices",
                "hbenyahia", "vnk9qws8", "microservices-client","4j4asrqMic9Nl6q47jgXw7WafGooAhUs");
    }

}
