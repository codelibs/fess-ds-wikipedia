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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.codelibs.fess.ds.wikipedia.UnitDsTestCase;
import org.codelibs.fess.exception.DataStoreException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class DumpFetcherTest extends UnitDsTestCase {

    private HttpServer server;
    private int port;
    private final List<String> receivedUserAgents = new ArrayList<>();

    @Override
    public void setUp(final TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        receivedUserAgents.clear();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();
        server.start();
    }

    @Override
    public void tearDown(final TestInfo testInfo) throws Exception {
        if (server != null) {
            server.stop(0);
        }
        super.tearDown(testInfo);
    }

    private void respond(final HttpExchange exchange, final String body) throws IOException {
        final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    @Test
    public void test_open_http_sendsConfiguredUserAgent() throws Exception {
        server.createContext("/dump", exchange -> {
            receivedUserAgents.add(exchange.getRequestHeaders().getFirst("User-Agent"));
            respond(exchange, "hello");
        });
        final DumpFetcher fetcher = new DumpFetcher("TestAgent/1.0");
        try (InputStream in = fetcher.open("http://127.0.0.1:" + port + "/dump")) {
            assertEquals("hello", new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
        assertEquals(1, receivedUserAgents.size());
        assertEquals("TestAgent/1.0", receivedUserAgents.get(0));
    }

    @Test
    public void test_open_http_403_reportsUserAgentAsTheLikelyCause() throws Exception {
        server.createContext("/forbidden", exchange -> {
            exchange.sendResponseHeaders(403, -1);
            exchange.close();
        });
        final DumpFetcher fetcher = new DumpFetcher("TestAgent/1.0");
        try {
            fetcher.open("http://127.0.0.1:" + port + "/forbidden");
            fail("DataStoreException should have been thrown.");
        } catch (final DataStoreException e) {
            assertTrue("message should mention User-Agent but was: " + e.getMessage(), e.getMessage().contains("User-Agent"));
        }
    }

    @Test
    public void test_open_http_retriesOn429UsingRetryAfter() throws Exception {
        final AtomicInteger calls = new AtomicInteger();
        server.createContext("/throttled", exchange -> {
            if (calls.getAndIncrement() == 0) {
                exchange.getResponseHeaders().add("Retry-After", "0");
                exchange.sendResponseHeaders(429, -1);
                exchange.close();
                return;
            }
            respond(exchange, "ok");
        });
        final DumpFetcher fetcher = new DumpFetcher("TestAgent/1.0");
        try (InputStream in = fetcher.open("http://127.0.0.1:" + port + "/throttled")) {
            assertEquals("ok", new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
        assertEquals(2, calls.get());
    }

    @Test
    public void test_open_http_500_isNotRetried() throws Exception {
        final AtomicInteger calls = new AtomicInteger();
        server.createContext("/broken", exchange -> {
            calls.incrementAndGet();
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        final DumpFetcher fetcher = new DumpFetcher("TestAgent/1.0");
        try {
            fetcher.open("http://127.0.0.1:" + port + "/broken");
            fail("DataStoreException should have been thrown.");
        } catch (final DataStoreException e) {
            assertTrue("message should mention 500 but was: " + e.getMessage(), e.getMessage().contains("500"));
        }
        assertEquals(1, calls.get());
    }

    @Test
    public void test_open_localPath() throws Exception {
        final Path file = Files.createTempFile("dump-fetcher-", ".txt");
        try {
            Files.writeString(file, "local-content", StandardCharsets.UTF_8);
            final DumpFetcher fetcher = new DumpFetcher("TestAgent/1.0");
            try (InputStream in = fetcher.open(file.toAbsolutePath().toString())) {
                assertEquals("local-content", new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    public void test_open_fileUrl() throws Exception {
        final Path file = Files.createTempFile("dump-fetcher-", ".txt");
        try {
            Files.writeString(file, "file-url-content", StandardCharsets.UTF_8);
            final DumpFetcher fetcher = new DumpFetcher("TestAgent/1.0");
            try (InputStream in = fetcher.open(file.toUri().toString())) {
                assertEquals("file-url-content", new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    public void test_open_http_uppercaseSchemeReachesHttpPathAndSendsUserAgent() throws Exception {
        server.createContext("/uppercase-dump", exchange -> {
            receivedUserAgents.add(exchange.getRequestHeaders().getFirst("User-Agent"));
            respond(exchange, "hello");
        });
        final DumpFetcher fetcher = new DumpFetcher("TestAgent/1.0");
        final String uppercaseUrl = "HTTP://127.0.0.1:" + port + "/uppercase-dump";
        try (InputStream in = fetcher.open(uppercaseUrl)) {
            assertEquals("hello", new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
        assertEquals(1, receivedUserAgents.size());
        assertEquals("TestAgent/1.0", receivedUserAgents.get(0));
    }

    @Test
    public void test_open_uppercaseFileSchemeReadsTheFile() throws Exception {
        final Path file = Files.createTempFile("dump-fetcher-", ".txt");
        try {
            Files.writeString(file, "uppercase-file-content", StandardCharsets.UTF_8);
            final DumpFetcher fetcher = new DumpFetcher("TestAgent/1.0");
            final String fileUrl = file.toUri().toString();
            final String uppercaseFileUrl = "FILE:" + fileUrl.substring("file:".length());
            try (InputStream in = fetcher.open(uppercaseFileUrl)) {
                assertEquals("uppercase-file-content", new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    public void test_open_windowsStylePathIsTreatedAsAFilesystemPathNotAScheme() {
        final DumpFetcher fetcher = new DumpFetcher("TestAgent/1.0");
        final String windowsPath = "C:\\dumps\\jawiki.xml.bz2";
        try {
            fetcher.open(windowsPath);
            fail("IOException should have been thrown.");
        } catch (final IOException e) {
            assertTrue("message should name the windows path but was: " + e.getMessage(), e.getMessage().contains(windowsPath));
        }
    }

    @Test
    public void test_open_relativePathWithNoSchemeStillWorks() throws Exception {
        final Path cwd = Path.of("").toAbsolutePath();
        final Path file = Files.createTempFile(cwd, "dump-fetcher-relative-", ".txt");
        try {
            Files.writeString(file, "relative-content", StandardCharsets.UTF_8);
            final DumpFetcher fetcher = new DumpFetcher("TestAgent/1.0");
            try (InputStream in = fetcher.open(file.getFileName().toString())) {
                assertEquals("relative-content", new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private HttpResponse<InputStream> sendRaw(final String path) throws IOException, InterruptedException {
        final HttpClient client = HttpClient.newHttpClient();
        final HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofInputStream());
    }

    @Test
    public void test_getRetryWait_parsesNumericRetryAfterHeaderAsSeconds() throws Exception {
        server.createContext("/retry-numeric", exchange -> {
            exchange.getResponseHeaders().add("Retry-After", "2");
            exchange.sendResponseHeaders(429, -1);
            exchange.close();
        });
        final DumpFetcher fetcher = new DumpFetcher("TestAgent/1.0");
        final HttpResponse<InputStream> response = sendRaw("/retry-numeric");
        try (InputStream ignored = response.body()) {
            assertEquals(2000L, fetcher.getRetryWait(response));
        }
    }

    @Test
    public void test_getRetryWait_defaultsTo1000MillisWhenHeaderIsMissing() throws Exception {
        server.createContext("/retry-missing", exchange -> {
            exchange.sendResponseHeaders(429, -1);
            exchange.close();
        });
        final DumpFetcher fetcher = new DumpFetcher("TestAgent/1.0");
        final HttpResponse<InputStream> response = sendRaw("/retry-missing");
        try (InputStream ignored = response.body()) {
            assertEquals(1000L, fetcher.getRetryWait(response));
        }
    }

    @Test
    public void test_getRetryWait_defaultsTo1000MillisWhenHeaderIsNotNumeric() throws Exception {
        server.createContext("/retry-non-numeric", exchange -> {
            exchange.getResponseHeaders().add("Retry-After", "soon");
            exchange.sendResponseHeaders(429, -1);
            exchange.close();
        });
        final DumpFetcher fetcher = new DumpFetcher("TestAgent/1.0");
        final HttpResponse<InputStream> response = sendRaw("/retry-non-numeric");
        try (InputStream ignored = response.body()) {
            assertEquals(1000L, fetcher.getRetryWait(response));
        }
    }

    @Test
    public void test_getRetryWait_clampsExcessiveRetryAfterToACeiling() throws Exception {
        server.createContext("/retry-excessive", exchange -> {
            exchange.getResponseHeaders().add("Retry-After", "3600");
            exchange.sendResponseHeaders(429, -1);
            exchange.close();
        });
        final DumpFetcher fetcher = new DumpFetcher("TestAgent/1.0");
        final HttpResponse<InputStream> response = sendRaw("/retry-excessive");
        try (InputStream ignored = response.body()) {
            assertEquals(5L * 60L * 1000L, fetcher.getRetryWait(response));
        }
    }

    @Test
    public void test_getRetryWait_clampsNegativeRetryAfterToZero() throws Exception {
        server.createContext("/retry-negative", exchange -> {
            exchange.getResponseHeaders().add("Retry-After", "-5");
            exchange.sendResponseHeaders(429, -1);
            exchange.close();
        });
        final DumpFetcher fetcher = new DumpFetcher("TestAgent/1.0");
        final HttpResponse<InputStream> response = sendRaw("/retry-negative");
        try (InputStream ignored = response.body()) {
            assertEquals(0L, fetcher.getRetryWait(response));
        }
    }

    @Test
    public void test_open_http_stopsRetryingWhenInterrupted() throws Exception {
        final AtomicInteger calls = new AtomicInteger();
        final Thread callingThread = Thread.currentThread();
        server.createContext("/interrupted", exchange -> {
            calls.incrementAndGet();
            exchange.getResponseHeaders().add("Retry-After", "5");
            exchange.sendResponseHeaders(429, -1);
            exchange.close();
            // Interrupting from inside the handler guarantees the response has already been
            // received (calls == 1) before the interrupt can be observed by the calling thread,
            // whether that happens inside HttpClient#send or inside the retry wait. Interrupting
            // from a timer thread instead is a race: on a slow or loaded runner the interrupt can
            // land before the handler even runs, making the calls == 1 assertion flaky for a
            // reason unrelated to the behaviour under test.
            callingThread.interrupt();
        });
        final DumpFetcher fetcher = new DumpFetcher("TestAgent/1.0");
        try {
            try {
                fetcher.open("http://127.0.0.1:" + port + "/interrupted");
                fail("IOException should have been thrown.");
            } catch (final IOException e) {
                assertTrue("message should mention the location but was: " + e.getMessage(), e.getMessage().contains("/interrupted"));
            }
        } finally {
            Thread.interrupted();
        }
        assertEquals(1, calls.get());
    }
}
