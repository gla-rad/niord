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

package org.niord.core.web;

import io.quarkus.vertx.web.RouteFilter;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import org.niord.core.domain.DomainResolver;
import org.niord.core.domain.DomainService;
import org.slf4j.Logger;

import jakarta.inject.Inject;

/**
 * Resolves the domain from the request and stores it in the current thread
 */
@ApplicationScoped
public class DomainServletFilter {

    /* The domain resolver instance */
    final DomainResolver domainResolver = DomainResolver.newInstance();

    @Inject
    Logger log;

    @Inject
    DomainService domainService;

    /**
     * Main filter method
     */
    @RouteFilter
    void doFilter(RoutingContext rc) {
        // If there are concurrent requests, only instantiate once
        // And resolve the current domain and add it to the local context
        String domainId = domainResolver.resolveDomain(rc.request());

        // Attach the domain to the current context
        // Be careful because we need to run on a separate thread cau the
        // domainService might need access to the database to initialise.
        Uni.createFrom().voidItem()
                .runSubscriptionOn(Infrastructure.getDefaultExecutor()) // Move execution off the IO thread
                .attachContext()
                .invoke((result) -> domainService.setDomainForCurrentThread(domainId))
                .subscribe()
                .with(success -> rc.next(), ex -> log.error(ex.getMessage(), ex));

        // And remove the domain at the end
        rc.addEndHandler((end) ->
            Uni.createFrom().voidItem()
                    .runSubscriptionOn(Infrastructure.getDefaultExecutor()) // Move execution off the IO thread
                    .attachContext()
                    .invoke((result) -> domainService.removeDomainForCurrentThread())
                    .subscribe()
                    .with(success -> {}, ex -> log.error(ex.getMessage(), ex))
        );
    }
}
