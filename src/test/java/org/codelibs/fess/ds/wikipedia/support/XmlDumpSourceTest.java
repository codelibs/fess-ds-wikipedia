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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.codelibs.fess.ds.wikipedia.UnitDsTestCase;
import org.junit.jupiter.api.Test;

public class XmlDumpSourceTest extends UnitDsTestCase {

    private List<WikiDocument> parse(final String fixture) {
        return parse(fixture, PlainTextOptions.DEFAULT);
    }

    private List<WikiDocument> parse(final String fixture, final PlainTextOptions options) {
        final java.net.URL url = getClass().getResource("/fixtures/" + fixture);
        Objects.requireNonNull(url, "fixture not found: " + fixture);
        final XmlDumpSource source = new XmlDumpSource(url.toString(), new DumpFetcher("TestAgent/1.0"));
        source.setPlainTextOptions(options);
        final List<WikiDocument> documents = new ArrayList<>();
        source.forEach(documents::add);
        return documents;
    }

    @Test
    public void test_forEach_readsEveryPage() {
        assertEquals(3, parse("wiki-namespaces.xml").size());
    }

    @Test
    public void test_forEach_mapsTheArticle() {
        final WikiDocument document = parse("wiki-namespaces.xml").get(0);
        assertEquals("1", document.getId());
        assertEquals("Article", document.getTitle());
        assertEquals(0, document.getNamespace());
        assertEquals("wikitext", document.getModel());
        assertEquals("text/x-wiki", document.getFormat());
        assertFalse(document.isRedirect());
    }

    @Test
    public void test_forEach_readsTheNamespaceOfANonArticlePage() {
        final WikiDocument document = parse("wiki-namespaces.xml").get(1);
        assertEquals("Category:Cats", document.getTitle());
        assertEquals(14, document.getNamespace());
    }

    @Test
    public void test_forEach_exposesCategoriesAndLinks() {
        final WikiDocument document = parse("wiki-namespaces.xml").get(0);
        assertEquals(List.of("Cats"), document.getCategories());
        assertTrue("links should contain Tokyo Tower but were " + document.getLinks(), document.getLinks().contains("Tokyo Tower"));
    }

    @Test
    public void test_forEach_detectsARedirect() {
        final WikiDocument document = parse("wiki-namespaces.xml").get(2);
        assertTrue("page 3 should be a redirect", document.isRedirect());
        assertEquals("Article", document.getRedirectTitle());
    }

    @Test
    public void test_forEach_carriesTheRawWikitext() {
        final WikiDocument document = parse("wiki-namespaces.xml").get(1);
        assertEquals("Pages about cats.", document.getWikitext());
    }

    @Test
    public void test_plainTextOptions_reachTheDocumentContent() {
        assertEquals("Our servers.\nRestart Apache after changing the config.\nThe rack in room 2",
                parse("wiki-options.xml").get(0).getContent());
        assertEquals("Our servers.", parse("wiki-options.xml", PlainTextOptions.of("Note", null, true)).get(0).getContent());
    }
}
