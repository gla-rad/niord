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

import jakarta.inject.Inject;
import org.slf4j.Logger;

/**
 * Will store the host name used for accessing Niord for the duration of the request.
 * <p>
 * If Niord is running behind, say, an Apache SSL proxy, ensure that the original server name
 * is passed on.
 * <p>
 * Example configuration:
 * <pre>
 * &lt;VirtualHost *:443&gt;
 *     ServerName niord.host.name
 *     Include /path/to/ssl.conf
 *
 *     ProxyPass           /robots.txt !
 *     ProxyPass           /  http://localhost:8080/
 *     ProxyPassReverse    /  http://localhost:8080/
 *
 *     RequestHeader set originalScheme "https"
 *     ProxyRequests Off
 *     ProxyPreserveHost On
 *     RequestHeader set X-Forwarded-Proto "https"
 *     RequestHeader set X-Forwarded-Port "443"
 * &lt;/VirtualHost&gt;
 * </pre>
 */
@ApplicationScoped
public class ServerNameServletFilter {

    @Inject
    Logger log;

    @Inject
    NiordApp app;

    /**
     * Main filter method
     */
    @RouteFilter
    void doFilter(RoutingContext rc) {
        // Attach the domain to the current context
        // Be careful because we need to run on a separate thread cau the
        // domainService might need access to the database to initialise.
        Uni.createFrom().voidItem()
                .runSubscriptionOn(Infrastructure.getDefaultExecutor()) // Move execution off the IO thread
                .attachContext()
                .invoke((result) -> {
                    app.registerServerNameForCurrentThread(rc.request());
                })
                .subscribe()
                .with(success -> rc.next(), ex -> log.error(ex.getMessage(), ex));

        // And remove the domain at the end
        rc.addEndHandler((end) ->
                Uni.createFrom().voidItem()
                        .runSubscriptionOn(Infrastructure.getDefaultExecutor()) // Move execution off the IO thread
                        .attachContext()
                        .invoke((result) -> app.removeServerNameForCurrentThread())
                        .subscribe()
                        .with(success -> {}, ex -> log.error(ex.getMessage(), ex))
        );
    }
}
