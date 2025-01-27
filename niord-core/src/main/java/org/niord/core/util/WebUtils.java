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
package org.niord.core.util;

import io.vertx.core.MultiMap;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.jboss.resteasy.reactive.server.multipart.FormValue;
import org.jboss.resteasy.reactive.server.multipart.MultipartFormDataInput;

import jakarta.ws.rs.core.MultivaluedMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

/**
 * Web-related utility functions
 */
@SuppressWarnings("unused")
public class WebUtils {

    private WebUtils() {
    }

    /**
     * Returns the base URL of the request
     * @param request the request
     * @return the base URL
     */
    public static String getWebAppUrl(HttpServerRequest request, String... appends) {
        StringBuilder result = new StringBuilder(String.format(
                "%s://%s%s%s",
                request.scheme(),
                request.localAddress().hostName(),
                request.localAddress().port() == 80 || request.localAddress().port() == 443 ? "" : ":" + request.localAddress().port(),
                request.path()));
        for (String a : appends) {
            result.append(a);
        }
        return result.toString();
    }

    /**
     * Returns the base URL of the request
     * @param request the request
     * @return the base URL
     */
    public static String getServletUrl(HttpServerRequest request, String... appends) {
        String[] args = (String[])ArrayUtils.addAll(new String[] { request.path() }, appends);
        return getWebAppUrl(request, args);
    }

    /**
     * Returns the cookie with the given name or null if not found
     * @param request the request
     * @param name the name
     * @return the cookie with the given name or null if not found
     */
    public static Cookie getCookie(HttpServerRequest request, String name) {
        Set<Cookie> cookies = request.cookies();
        if (cookies != null) {
            for (Cookie c : request.cookies()) {
                if (c.getName().equals(name)) {
                    return c;
                }
            }
        }
        return null;
    }

    /**
     * Returns the value of the cookie with the given name or null if not found
     * @param request the request
     * @param name the name
     * @return the value of the cookie with the given name or null if not found
     */
    public static String getCookieValue(HttpServerRequest request, String name) {
        Cookie c = getCookie(request, name);
        return (c == null) ? null : c.getValue();
    }

    /**
     * Parses the URL (or part of the URL) to extract a request parameter map.
     * @param url the URL to parse
     * @return the parsed request parameter map
     */
    public static Map<String, String[]> parseParameterMap(String url) {
        Map<String, List<String>> params = new HashMap<>();

        if (StringUtils.isNotBlank(url)) {
            int index = url.indexOf("?");
            if (index > -1) {
                url = url.substring(index + 1);
            }
        }
        if (StringUtils.isNotBlank(url)) {
            for (String kv : url.split("&")) {
                String key, value = "";
                int x = kv.indexOf("=");
                if (x == -1) {
                    key = urlDecode(kv);
                } else {
                    key = urlDecode(kv.substring(0, x));
                    value = urlDecode(kv.substring(x + 1));
                }
                params.computeIfAbsent(key, v -> new ArrayList<>())
                        .add(value);
            }
        }
        return params.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().toArray(new String[e.getValue().size()])));
    }

    /**
     * Parses the URL (or part of the URL) to extract a request parameter map
     * into a  vertx multi-map.
     * @param url the URL to parse
     * @return the parsed request parameter MultiMap
     */
    public static MultiMap parseParameterMultiMap(String url) {
        final MultiMap multiMap = MultiMap.caseInsensitiveMultiMap();
        WebUtils.parseParameterMap(url).forEach((k, v) -> multiMap.add(k,Arrays.stream(v).toList()));
        return multiMap;
    }

    /**
     * Add headers to the response to ensure no caching takes place
     * @param response the response
     * @return the response
     */
    public static HttpServerResponse nocache(HttpServerResponse response) {
        response.headers().add("Cache-Control","no-cache");
        response.headers().add("Cache-Control","no-store");
        response.headers().add("Pragma","no-cache");
        response.headers().add ("Expires", "0");
        return response;
    }

    /**
     * Add headers to the response to ensure caching in the given duration
     * @param response the response
     * @param seconds the number of seconds to cache the response
     * @return the response
     */
    public static HttpServerResponse cache(HttpServerResponse response, int seconds) {
        long now = System.currentTimeMillis();
        response.headers().add("Cache-Control", "max-age=" + seconds);
        response.headers().add("Last-Modified", String.valueOf(now));
        response.headers().add("Expires", String.valueOf(now + seconds * 1000L));
        return response;
    }

    /**
     * Encode identically to the javascript encodeURIComponent() method
     * @param s the string to encode
     * @return the encoded string
     */
    public static String encodeURIComponent(String s) {
        String result;

        try {
            result = URLEncoder.encode(s, "UTF-8")
                    .replaceAll("\\+", "%20")
                    .replaceAll("\\%21", "!")
                    .replaceAll("\\%27", "'")
                    .replaceAll("\\%28", "(")
                    .replaceAll("\\%29", ")")
                    .replaceAll("\\%7E", "~");
        } catch (UnsupportedEncodingException e) {
            result = s;
        }

        return result;
    }

    /**
     * Encode identically to the javascript encodeURI() method
     * @param s the string to encode
     * @return the encoded string
     */
    public static String encodeURI(String s) {
        return encodeURIComponent(s)
                    .replaceAll("\\%3A", ":")
                    .replaceAll("\\%2F", "/")
                    .replaceAll("\\%3B", ";")
                    .replaceAll("\\%3F", "?");
    }


    /**
     * Wraps a call to URLDecoder.decode(value, "UTF-8") to prevent the exception signature
     * @param value the value to decode
     * @return the decoded value or the original value in case of an exception
     */
    public static String urlDecode(String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }

    /**
     * A helper function that retrieves all the form parameters on the multi-part
     * request and returns them as a map of objects based on their number, i.e.
     * <ul>
     *     <li>For zero entries the value is null</li>
     *     <li>For one entries the value is a single string</li>
     *     <li>For multiple entries the value is list of strings</li>
     * </ul>
     *
     * @param input the multi-part form input request
     * @return the extracted parameters' map
     */
    public static Map<String, Object> getMultipartInputFormParams(MultipartFormDataInput input) {
        // Initialise the lists of parameters
        final Map<String, Collection<FormValue>> uploadForm = input.getValues();
        final Map<String, Object> formParams = new HashMap<>();

        // Now iterate for all input parts
        for(Map.Entry<String, Collection<FormValue>> paramEntry : uploadForm.entrySet()) {
            if(paramEntry.getValue().stream().allMatch(not(FormValue::isFileItem))) {
                switch(paramEntry.getValue().size()) {
                    case 0:
                        formParams.put(paramEntry.getKey(), null);
                        break;
                    case 1:
                        formParams.put(paramEntry.getKey(), paramEntry.getValue().stream().findFirst().orElse(null));
                        break;
                    default:
                        final List<String> paramEntryList = new ArrayList<>();
                        for(FormValue inputPart : paramEntry.getValue()) {
                            paramEntryList.add(inputPart.getValue());
                        }
                        formParams.put(paramEntry.getKey(), paramEntryList);
                        break;
                }
            }
        }

        // And return the populated parameters' map
        return formParams;
    }

    /**
     * A helper function that retrieves all the uploaded files from the multi-part
     * request and returns them as a map based on their provided file names.
     *
     * @param input the multi-part input request
     * @return the uploaded files map
     */
    public static Map<String, InputStream> getMultipartInputFiles(MultipartFormDataInput input) throws IOException {
        // Initialise the lists of parameters
        final Map<String, Collection<FormValue>> uploadForm = input.getValues();
        final Map<String, InputStream> inputFiles = new HashMap<>();

        // For all input parts
        // Now iterate for all input parts
        for(Map.Entry<String, Collection<FormValue>> paramEntry : uploadForm.entrySet()) {
            if (paramEntry.getValue().stream().allMatch(FormValue::isFileItem)) {
                // If this looks like a file, i.e. has the same content type as the input
                for (FormValue inputPart : paramEntry.getValue()) {
                    inputFiles.put(
                            WebUtils.getFileName(inputPart.getHeaders()),
                            inputPart.getFileItem().getInputStream()
                    );
                 }
            }
        }

        // And return the populated parameters' map
        return inputFiles;
    }

    /**
     * Retrieves a file name from the request headers from the "filename" field
     * of the request headers.
     *
     * @param header the multivalued map of the request headers
     * @return the name of the file as specified in the filename header field
     */
    protected static String getFileName(MultivaluedMap<String, String> header) {
        // Initialise the content disposition array
        final String[] contentDisposition = header.getFirst("Content-Disposition").split(";");
        // Now iterate for all provided filenames
        for (String filename : contentDisposition) {
            if ((filename.trim().startsWith("filename"))) {
                String[] name = filename.split("=");
                String finalFileName = name[1].trim().replaceAll("\"", "");
                return finalFileName;
            }
        }
        return "unknown";
    }

}
