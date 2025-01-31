/*
 * Copyright 2016 Danish Maritime Authority.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.niord.web.conf;

import io.quarkus.vertx.web.ReactiveRoutes;
import io.quarkus.vertx.web.Route;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import org.niord.core.keycloak.KeycloakIntegrationService;
import org.slf4j.Logger;

import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads the keycloak.json file and updates it with one constructed by the KeycloakIntegrationService
 */
@ApplicationScoped
public class KeycloakJsonServletFilter {

    /* A single keycloak deployment hashmap */
    final static Map<String, Object> KEYCLOAK_DEPLOYMENT = new HashMap<>();

    @Inject
    Logger log;

    @Inject
    KeycloakIntegrationService keycloakIntegrationService;

    /**
     * Updates the response with system properties
     */
    @Route(path = "/conf/keycloak.json", produces = ReactiveRoutes.APPLICATION_JSON)
    Uni<Map<String,Object>> route() {
        // And return the uni
        return Uni.createFrom()
                .item(KEYCLOAK_DEPLOYMENT)
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .invoke(map -> {
                    // If there are concurrent requests, only instantiate once
                    if (KEYCLOAK_DEPLOYMENT.isEmpty()) {
                        try {
                            KEYCLOAK_DEPLOYMENT.putAll(keycloakIntegrationService.createKeycloakDeploymentForWebApp());
                            log.info("Instantiated keycloak deployment for web application");
                        } catch (Exception ex) {
                            log.error(ex.getMessage(), ex);
                        }
                    }
                });
    }
}
