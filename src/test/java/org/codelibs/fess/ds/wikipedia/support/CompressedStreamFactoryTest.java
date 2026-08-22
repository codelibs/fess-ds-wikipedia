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
import java.nio.charset.StandardCharsets;

import org.codelibs.fess.ds.wikipedia.UnitDsTestCase;
import org.junit.jupiter.api.Test;

public class CompressedStreamFactoryTest extends UnitDsTestCase {

    private static final String EXPECTED = "PART-ONE\nPART-TWO\n";

    /** An input stream whose every read fails, simulating a dropped remote connection. */
    private static final class FailingInputStream extends InputStream {
        @Override
        public int read() throws IOException {
            throw new IOException("boom");
        }

        @Override
        public int read(final byte[] b, final int off, final int len) throws IOException {
            throw new IOException("boom");
        }
    }

    private String read(final String fixture) throws Exception {
        try (InputStream raw = getClass().getResourceAsStream("/fixtures/" + fixture)) {
            assertNotNull(raw);
            try (InputStream in = CompressedStreamFactory.open(raw)) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
    }

    @Test
    public void test_open_uncompressed() throws Exception {
        assertEquals(EXPECTED, read("sample.txt"));
    }

    @Test
    public void test_open_bzip2() throws Exception {
        assertEquals(EXPECTED, read("sample.txt.bz2"));
    }

    @Test
    public void test_open_multistreamBzip2_readsEveryStream() throws Exception {
        // The bundled CBZip2InputStream stopped after the first stream, which silently
        // truncated *-pages-articles-multistream.xml.bz2 to roughly the first 100 pages.
        assertEquals(EXPECTED, read("sample-multistream.txt.bz2"));
    }

    @Test
    public void test_open_gzip() throws Exception {
        assertEquals(EXPECTED, read("sample.txt.gz"));
    }

    @Test
    public void test_open_xz() throws Exception {
        assertEquals(EXPECTED, read("sample.txt.xz"));
    }

    @Test
    public void test_open_zstd() throws Exception {
        assertEquals(EXPECTED, read("sample.txt.zst"));
    }

    @Test
    public void test_open_rethrowsAGenuineIOErrorInsteadOfTreatingItAsUncompressed() throws Exception {
        // A read failure while detecting the format (e.g. a dropped connection) must not be
        // treated the same as "no compressor matched"; that would hand a broken stream to the
        // XML parser, which would then report a confusing "Premature end of file" instead of
        // the real cause.
        try {
            CompressedStreamFactory.open(new FailingInputStream());
            fail("IOException should have been thrown.");
        } catch (final IOException e) {
            assertEquals("boom", e.getMessage());
        }
    }
}
