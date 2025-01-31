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
import org.niord.core.user.TicketService;
import org.slf4j.Logger;

import jakarta.inject.Inject;

/**
 * If a request contains a "ticket" parameter, attempt to set the current ticket data (domain, user, roles)
 * in a tread local
 */
@ApplicationScoped
public class TicketServletFilter {

    private final static String TICKET_PARAM = "ticket";

    @Inject
    Logger log;

    @Inject
    TicketService ticketService;

    /**
     * Main filter method
     * @param rc the routing context
     */
    @RouteFilter
    void doFilter(RoutingContext rc) {
        final String path = rc.request().path();
        if (path.startsWith("/rest/*")) {
            // Attach the domain to the current context
            // Be careful because we need to run on a separate thread cau the
            // domainService might need access to the database to initialise.
            Uni.createFrom().voidItem()
                    .runSubscriptionOn(Infrastructure.getDefaultExecutor()) // Move execution off the IO thread
                    .attachContext()
                    .invoke((result) -> {
                        boolean ticketResolved = ticketService.resolveTicketForCurrentThread(rc.request().getParam(TICKET_PARAM));
                        if (ticketResolved) {
                            log.info("Ticket resolved for request " + rc.request().absoluteURI());
                        }
                    })
                    .subscribe()
                    .with(success -> rc.next(), ex -> log.error(ex.getMessage(), ex));

            // And remove the domain at the end
            rc.addEndHandler((end) ->
                    Uni.createFrom().voidItem()
                            .runSubscriptionOn(Infrastructure.getDefaultExecutor()) // Move execution off the IO thread
                            .attachContext()
                            .invoke((result) -> ticketService.removeTicketForCurrentThread())
                            .subscribe()
                            .with(success -> {}, ex -> log.error(ex.getMessage(), ex))
            );
        } else {
            // Proceed with the request
            rc.next();
        }
    }
}
