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

package org.niord.web;

import io.quarkus.vertx.web.RouteFilter;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import org.niord.core.NiordApp;
import org.slf4j.Logger;

import jakarta.inject.Inject;

/**
 * In development, return a dummy app cache manifest with no caching.
 * In test and production mode, return the real cache manifest,
 * as generated by the niord-web maven plug-in.
 */
@ApplicationScoped
public class AppCacheManifestServletFilter {

    static final String TEMPLATE_CACHE = "CACHE MANIFEST\n" +
                                         "\n" +
                                         "# Placeholder cache manifest file.\n" +
                                         "\n" +
                                         "CACHE:\n" +
                                         "\n" +
                                         "NETWORK:\n" +
                                         "*\n" +
                                         "\n";

    @Inject
    Logger log;

    @Inject
    NiordApp app;

    /**
     * In development, return a dummy manifest with no caching.
     * In test and production mode, return the real cache manifest,
     * as generated by the niord-web maven plug-in.
     */
    @RouteFilter
    public void doFilter(RoutingContext rc) {
        final String path = rc.request().path();
        if (path.startsWith("/index.manifest")) {
            // Always fix the content type in the end
            rc.addHeadersEndHandler((v) -> rc.response()
                    .putHeader("Content-Type", "text/cache-manifest;charset=UTF-8"));

            // Now populate the content
            Uni.createFrom().voidItem()
                    .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                    .invoke(v -> {
                        if (app.getExecutionMode() == NiordApp.ExecutionMode.DEVELOPMENT) {
                            rc.response().end(TEMPLATE_CACHE);
                        }
                    })
                    .subscribe()
                    .with(success -> rc.next(), ex -> log.error(ex.getMessage(), ex));
        } else {
            rc.next();
        }
    }
}
