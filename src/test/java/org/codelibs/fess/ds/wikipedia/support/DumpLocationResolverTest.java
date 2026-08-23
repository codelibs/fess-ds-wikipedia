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
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.codelibs.fess.ds.wikipedia.UnitDsTestCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import com.sun.net.httpserver.HttpServer;

public class DumpLocationResolverTest extends UnitDsTestCase {

    private HttpServer server;
    private int port;

    @Override
    public void setUp(final TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
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

    private DumpLocationResolver resolver() {
        return new DumpLocationResolver(new DumpFetcher("TestAgent/1.0"));
    }

    @Test
    public void test_resolve_singleFileIsReturnedAsIs() throws Exception {
        assertEquals(List.of("https://example.com/a.json.bz2"), resolver().resolve("https://example.com/a.json.bz2", ".json.bz2"));
    }

    @Test
    public void test_resolve_singleFileKeepsAMismatchedSuffix() throws Exception {
        // An explicitly named file is never dropped, whatever it is called.
        assertEquals(List.of("/var/tmp/dump.bin"), resolver().resolve("/var/tmp/dump.bin", ".json.bz2"));
    }

    @Test
    public void test_resolve_commaSeparatedKeepsOrder() throws Exception {
        assertEquals(List.of("https://example.com/b.json.bz2", "https://example.com/a.json.bz2"),
                resolver().resolve("https://example.com/b.json.bz2 , https://example.com/a.json.bz2", ".json.bz2"));
    }

    @Test
    public void test_resolve_localDirectoryIsSortedAndFiltered() throws Exception {
        final Path dir = Files.createTempDirectory("dump-locations-");
        try {
            Files.writeString(dir.resolve("wiki-00001.json.bz2"), "b", StandardCharsets.UTF_8);
            Files.writeString(dir.resolve("wiki-00000.json.bz2"), "a", StandardCharsets.UTF_8);
            Files.writeString(dir.resolve("_SUCCESS"), "", StandardCharsets.UTF_8);
            final List<String> resolved = resolver().resolve(dir.toAbsolutePath().toString(), ".json.bz2");
            assertEquals(2, resolved.size());
            assertTrue("first should be 00000 but was " + resolved.get(0), resolved.get(0).endsWith("wiki-00000.json.bz2"));
            assertTrue("second should be 00001 but was " + resolved.get(1), resolved.get(1).endsWith("wiki-00001.json.bz2"));
        } finally {
            try (var entries = Files.list(dir)) {
                for (final Path p : entries.toList()) {
                    Files.deleteIfExists(p);
                }
            }
            Files.deleteIfExists(dir);
        }
    }

    @Test
    public void test_resolve_httpDirectoryListing() throws Exception {
        server.createContext("/dumps/", exchange -> {
            final String html = "<html><body>" //
                    + "<a href=\"../\">../</a>" //
                    + "<a href=\"_SUCCESS\">_SUCCESS</a>" //
                    + "<a href=\"wiki-00001.json.bz2\">wiki-00001.json.bz2</a>" //
                    + "<a href=\"wiki-00000.json.bz2\">wiki-00000.json.bz2</a>" //
                    + "</body></html>";
            final byte[] body = html.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        final String base = "http://127.0.0.1:" + port + "/dumps/";
        assertEquals(List.of(base + "wiki-00000.json.bz2", base + "wiki-00001.json.bz2"), resolver().resolve(base, ".json.bz2"));
    }

    @Test
    public void test_resolve_emptyDirectoryFailsWithTheLocationNamed() throws Exception {
        final Path dir = Files.createTempDirectory("dump-locations-empty-");
        try {
            resolver().resolve(dir.toAbsolutePath().toString(), ".json.bz2");
            fail("an empty directory should not resolve silently");
        } catch (final IOException e) {
            assertTrue("message should name the directory but was: " + e.getMessage(),
                    e.getMessage().contains(dir.toAbsolutePath().toString()));
        } finally {
            Files.deleteIfExists(dir);
        }
    }
}
