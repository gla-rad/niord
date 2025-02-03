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

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.niord.core.util.WebUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Implementations of this class should be used to load and modify textual web resources,
 * typically used for AngularJS JavaScript configuration files.
 */
public abstract class AbstractTextResourceRoute {

    /**
     * The path for which this abstract filter is application
     */
    final String base = "/META-INF/resources";

    /**
     * The path for which this abstract filter is application
     */
    final String path;

    /**
     * The time-to-live value for the cache used in this filter.
     */
    final int cacheTTL;

    /**
     * Constructor
     * @param cacheTTL the cache TTL in seconds
     */
    public AbstractTextResourceRoute(String path, int cacheTTL) {
        this.path = path;
        this.cacheTTL = cacheTTL;
    }

    /**
     * Main filter method
     */
    Uni<String> serveTextResource(RoutingContext rc) {
        final HttpServerRequest request = rc.request();
        final HttpServerResponse response = rc.response();
        final String path = rc.request().path();

        // Sanity Check
        if (!path.startsWith(this.path)) {
            return Uni.createFrom().failure(new IOException("Text resource not found"));
        }

        // Try to open the resource to be served
        try (InputStream inputStream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(this.base + this.path)) {
            // Check that the file exists
            if (inputStream == null) {
                return Uni.createFrom().failure(new IOException("Text resource not found"));
            }

            // Read file contents
            String buffer = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));

            // Add the caching headers
            if (cacheTTL > 0) {
                WebUtils.cache(response, cacheTTL);
            } else {
                WebUtils.nocache(response);
            }

            // Write the result back to the response
            return Uni.createFrom()
                    .item(buffer)
                    .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                    .map(buf -> updateResponse(rc, buf));

        } catch (IOException ex) {
            return Uni.createFrom().failure(ex);
        }
    }

    /**
     * Implementing class must override to update the response
     */
    abstract String updateResponse(RoutingContext rc, String response);
}
