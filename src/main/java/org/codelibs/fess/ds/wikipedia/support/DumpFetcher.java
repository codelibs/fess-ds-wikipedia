/*
 * Copyright 2012-2025 CodeLibs Project and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.codelibs.fess.ds.wikipedia.support;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.fess.exception.DataStoreException;

/**
 * Opens a Wikipedia dump that lives either on the local filesystem or on an HTTP server.
 * <p>
 * dumps.wikimedia.org rejects requests whose User-Agent is empty or is the JDK default
 * with HTTP 403, so a descriptive User-Agent is sent on every request.
 * </p>
 */
public class DumpFetcher {

    private static final Logger logger = LogManager.getLogger(DumpFetcher.class);

    private static final long DEFAULT_RETRY_WAIT = 1000L;

    /** The longest Retry-After wait that is honoured, regardless of what the server sends. */
    private static final long MAX_RETRY_WAIT = 5L * 60L * 1000L;

    /** The largest error response body that is read before the connection is closed unread. */
    private static final int MAX_DRAIN_BYTES = 4096;

    /**
     * Matches an RFC 3986 URI scheme (a leading letter followed by one or more letters, digits,
     * {@code +}, {@code -} or {@code .}, then a colon) at the start of a location. Requiring at
     * least two characters before the colon keeps a Windows path such as {@code C:\dumps\x.bz2}
     * from being misread as a one-letter scheme.
     */
    private static final Pattern SCHEME_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9+.-]+:");

    private final String userAgent;

    private final HttpClient httpClient;

    private int maxRetries = 3;

    /**
     * Creates a fetcher that sends the given User-Agent on every HTTP request.
     *
     * @param userAgent the User-Agent header value
     */
    public DumpFetcher(final String userAgent) {
        this.userAgent = userAgent;
        httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(30)).build();
    }

    /**
     * Sets how many times a retryable response (429 or 503) is retried.
     *
     * @param maxRetries the retry count; 0 disables retrying
     */
    public void setMaxRetries(final int maxRetries) {
        this.maxRetries = maxRetries;
    }

    /**
     * Opens the given location as a stream.
     *
     * @param location an http/https URL, a file URL, another scheme's URL (e.g. ftp), or a local
     *            filesystem path
     * @return the stream; the caller is responsible for closing it
     * @throws IOException if the location cannot be opened
     */
    public InputStream open(final String location) throws IOException {
        final Matcher schemeMatcher = SCHEME_PATTERN.matcher(location);
        if (schemeMatcher.find()) {
            final String scheme = schemeMatcher.group().substring(0, schemeMatcher.group().length() - 1);
            if (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")) {
                return openHttp(location);
            }
            if (scheme.equalsIgnoreCase("file")) {
                return Files.newInputStream(new File(URI.create(location)).toPath());
            }
            // Any other scheme (e.g. ftp:) is resolved the way the previous release did, via
            // java.net.URL. It never carried a User-Agent, so none is sent here either.
            return URI.create(location).toURL().openStream();
        }
        return Files.newInputStream(Path.of(location));
    }

    /**
     * Opens an HTTP location, retrying throttled responses.
     *
     * @param location the http or https URL
     * @return the response body stream
     * @throws IOException if the request cannot be sent, or the thread is interrupted while
     *             waiting to retry
     */
    protected InputStream openHttp(final String location) throws IOException {
        for (int attempt = 0;; attempt++) {
            final HttpRequest request = HttpRequest.newBuilder(URI.create(location)).header("User-Agent", userAgent).GET().build();
            final HttpResponse<InputStream> response;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while fetching " + location, e);
            }
            final int status = response.statusCode();
            if (status == 200) {
                return response.body();
            }
            drain(response);
            if ((status == 429 || status == 503) && attempt < maxRetries) {
                final long waitMillis = getRetryWait(response);
                logger.warn("HTTP {} from {}. Retrying in {} ms ({}/{}).", status, location, waitMillis, attempt + 1, maxRetries);
                sleep(waitMillis, location);
                continue;
            }
            if (status == 403) {
                throw new DataStoreException("HTTP 403 from " + location
                        + ". The server rejected the request; dumps.wikimedia.org requires a descriptive User-Agent."
                        + " The sent User-Agent was: " + userAgent);
            }
            throw new DataStoreException("HTTP " + status + " from " + location);
        }
    }

    /**
     * Consumes and discards at most a small amount of the response body so that the connection
     * can be reused, without buffering an arbitrarily large remote error response in memory.
     *
     * @param response the response whose body is discarded
     */
    protected void drain(final HttpResponse<InputStream> response) {
        try (InputStream body = response.body()) {
            final byte[] buffer = new byte[MAX_DRAIN_BYTES];
            int total = 0;
            int read;
            while (total < MAX_DRAIN_BYTES && (read = body.read(buffer, total, MAX_DRAIN_BYTES - total)) != -1) {
                total += read;
            }
        } catch (final IOException e) {
            logger.debug("Failed to drain the response body.", e);
        }
    }

    /**
     * Returns how long to wait before retrying, based on the Retry-After header.
     *
     * @param response the throttled response
     * @return the wait time in milliseconds, clamped to between 0 and {@link #MAX_RETRY_WAIT}
     */
    protected long getRetryWait(final HttpResponse<InputStream> response) {
        return response.headers().firstValue("Retry-After").map(value -> {
            try {
                return Math.clamp(Long.parseLong(value.trim()) * 1000L, 0L, MAX_RETRY_WAIT);
            } catch (final NumberFormatException e) {
                return DEFAULT_RETRY_WAIT;
            }
        }).orElse(DEFAULT_RETRY_WAIT);
    }

    /**
     * Sleeps for the given duration before retrying a request, aborting the retry loop by
     * throwing when the thread is interrupted while waiting.
     *
     * @param millis the duration in milliseconds
     * @param location the location being fetched, named in the exception message if interrupted
     * @throws IOException if the thread is interrupted while waiting
     */
    protected void sleep(final long millis, final String location) throws IOException {
        try {
            Thread.sleep(millis);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while fetching " + location, e);
        }
    }
}
