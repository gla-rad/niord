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
package org.niord.core.repo;

import io.quarkus.vertx.web.RouteFilter;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;

import jakarta.inject.Inject;

import java.io.IOException;

/**
 * Some uses of the repository, e.g. for serving videos displayed in message lists in Safari, will generate
 * a lot of GET's for the same file, but seemingly, the client closes the connection before the file is streamed.
 * This filter will catch the resulting "Broken piper" IOException's to avoid them filling up in the log.
 * <p>
 * NB: The usual method would be to defined a @Provider-annotated ExceptionMapper class for IOException, but
 * we do not really want to handle all IOException silently.
 */
@ApplicationScoped
public class RepositoryServletFilter {

    @Inject
    Logger log;

    @RouteFilter
    void doFilter(RoutingContext rc) {
        final String path = rc.request().path();
        if (path.startsWith("/rest/repo/file/")) {
            rc.addEndHandler(event -> {
                if (rc.failed()) {
                    Throwable ex = rc.failure();
                    if (ex.getCause() instanceof IOException && "Broken pipe".equals(ex.getCause().getMessage())) {
                        log.trace("Received Broken pipe IOException: " + rc.request().absoluteURI());
                    }
                }
            });
        }
        // Pass the error to default handlers
        rc.next();
    }
}
