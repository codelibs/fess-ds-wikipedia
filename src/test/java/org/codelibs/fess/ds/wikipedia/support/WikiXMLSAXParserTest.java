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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.codelibs.fess.ds.wikipedia.UnitDsTestCase;
import org.junit.jupiter.api.Test;

public class WikiXMLSAXParserTest extends UnitDsTestCase {

    private List<WikiPage> parse(final String location) {
        final WikiXMLSAXParser parser = new WikiXMLSAXParser(location, new DumpFetcher("TestAgent/1.0"));
        final List<WikiPage> pages = new ArrayList<>();
        parser.setPageCallback(pages::add);
        parser.parse();
        return pages;
    }

    private String fixtureUrl(final String name) {
        return getClass().getResource("/fixtures/" + name).toString();
    }

    @Test
    public void test_parse_uncompressedXml() {
        final List<WikiPage> pages = parse(fixtureUrl("wiki.xml"));
        assertEquals(2, pages.size());
        assertEquals("First", pages.get(0).getTitle());
        assertEquals("1", pages.get(0).getId());
        assertEquals("Second", pages.get(1).getTitle());
        assertEquals("2", pages.get(1).getId());
    }

    @Test
    public void test_parse_singleStreamBzip2() {
        assertEquals(2, parse(fixtureUrl("wiki-single.xml.bz2")).size());
    }

    @Test
    public void test_parse_multistreamBzip2_readsEveryPage() {
        // Before this change the bundled reader stopped after the first bzip2 stream,
        // yielding 1 page and then a DataStoreException from the truncated XML.
        assertEquals(2, parse(fixtureUrl("wiki-multistream.xml.bz2")).size());
    }

    @Test
    public void test_parse_localPathWithoutAScheme() throws Exception {
        final Path path = Path.of(getClass().getResource("/fixtures/wiki-multistream.xml.bz2").toURI());
        assertEquals(2, parse(path.toAbsolutePath().toString()).size());
    }
}
