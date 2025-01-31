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
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.lang.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Niord is using HTML5 app cache, but still, when a new version is rolled out, aggressive
 * "cachers", such as Chrome, may not update partial HTML files (loaded e.g. from directives),
 * because of the usual caching handling.
 * <p>
 * Please refer to {@code dk.dma.embryo.common.servlet.ETagFilter} of the Embryo project.
 */
@ApplicationScoped
public class ETagServletFilter {

    static final String HEADER_USER_AGENT           = "User-Agent";
    static final String HEADER_IF_MODIFIED_SINCE    = "If-Modified-Since";
    static final String HEADER_LAST_MODIFIED        = "Last-Modified";
    static final String HEADER_IF_NONE_MATCH        = "If-None-Match";
    static final String HEADER_ETAG                 = "ETag";

    @Inject
    Logger log;

    @ConfigProperty(name = "quarkus.http.root-path", defaultValue = "/")
    String basePath;

    @RouteFilter
    public void doFilter(RoutingContext rc) throws IOException {
        final String path = rc.request().path();
        if(path.startsWith("/index.html") || path.startsWith("/app/") ) {
            HttpServerRequest request = rc.request();
            HttpServerResponse response = rc.response();

            // Initially, we only use the ETag mechanism for Chrome, which caches aggressively
            if (isChrome(request) && !isAppConfFile(request)) {
                // Return the file associated with the servlet path
                Path file = getFile(path);
                // If the file exists
                if (file != null) {
                    EntityTag etag = etagForFile(file);

                    if (evaluatePreconditions(etag, request, response)) {
                        log.trace("ETag match for path " + path);
                        return;

                    } else if (request.getHeader(HEADER_IF_MODIFIED_SINCE) != null) {
                        request.headers().remove(HEADER_IF_MODIFIED_SINCE);
                    }

                    log.trace("No ETag match for path " + path);
                    response.headers().remove(HEADER_LAST_MODIFIED);
                }
            }
        }

        rc.next();
    }

    /**
     * Check if the request is for an /conf/* file.
     * These files are generated dynamically, and should not be
     * processed with E-Tags based on file modification dates.
     */
    private boolean isAppConfFile(HttpServerRequest request) {
        return request.path().startsWith("/conf/");
    }


    /** Returns if the request is issued by a chrome browser **/
    private boolean isChrome(HttpServerRequest request) {
        String userAgent = request.getHeader(HEADER_USER_AGENT);
        return StringUtils.isNotBlank(userAgent) && userAgent.contains("Chrome");
    }


    /** Returns the file path associated with the given servlet path; otherwise return null **/
    private Path getFile(String servletPath) {

        try {
            Path file = Paths.get(basePath, URLDecoder.decode(servletPath, "UTF-8"));

            // Ensure that this is a valid file
            if (Files.isRegularFile(file)) {
                return file;
            }
        } catch (Exception ignored) {
        }

        // No joy
        return null;
    }


    /** Returns the etag for the given file **/
    private EntityTag etagForFile(Path f) throws IOException {
        return new EntityTag("" + Files.getLastModifiedTime(f).toMillis() + "_" + Files.size(f), true);
    }


    /**
     * Inspired by javax.ws.rs.core.Request.evaluatePreconditions().
     * Evaluate request preconditions based on the passed in value.
     *
     * @param etag an ETag for the current state of the resource
     * @return if the preconditions are met.
     */
    private boolean evaluatePreconditions(EntityTag etag, HttpServerRequest request, HttpServerResponse response) {
        if (etag == null) {
            return false;
        }

        response.putHeader(HEADER_ETAG, etag.toString());

        boolean match = request.headers().getAll(HEADER_IF_NONE_MATCH)
                .stream()
                .map(this::trimEtagValue) // Strip any "-gzip" suffix added by Apache modules
                .anyMatch(val -> val.equals(etag.toString()));

        if (match) {
            response.setStatusCode(HttpServletResponse.SC_NOT_MODIFIED);
            return true;
        }

        // No matching etag headers
        return false;
    }


    /** Some Apache modules will add an "-gzip" suffix within the quoted ETag value **/
    private String trimEtagValue(String etag) {
        if (etag != null && etag.contains("-gzip")) {
            etag = etag.replace("-gzip", "");
        }
        return etag;
    }

    /** Simple weak etag implementation **/
    @SuppressWarnings("unused")
    private static class EntityTag {

        private String value;
        private boolean weak;

        /** Constructor **/
        public EntityTag(final String value) {
            this(value, false);
        }

        /** Designated constructor **/
        public EntityTag(final String value, final boolean weak) {
            if (value == null) {
                throw new IllegalArgumentException("value==null");
            }
            this.value = value;
            this.weak = weak;
        }

        @Override
        public String toString() {
            // Format according to https://en.wikipedia.org/wiki/HTTP_ETag
            return weak
                    ? "W/" + "\"" + value + "\""
                    : "\"" + value + "\"";
        }
    }

}
