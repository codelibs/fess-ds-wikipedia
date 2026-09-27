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
package org.codelibs.fess.ds.wikipedia;

import org.codelibs.fess.ds.wikipedia.support.PlainTextOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.lang.reflect.Method;

import org.codelibs.fess.entity.DataStoreParams;
import org.codelibs.fess.mylasta.direction.FessConfig;
import org.codelibs.fess.util.ComponentUtil;
import org.codelibs.fess.ds.wikipedia.UnitDsTestCase;
import org.codelibs.fess.ds.wikipedia.support.CirrusIndexDumpSource;
import org.codelibs.fess.ds.wikipedia.support.DumpFetcher;
import org.codelibs.fess.ds.wikipedia.support.DumpSource;
import org.codelibs.fess.ds.wikipedia.support.WikiDocument;
import org.codelibs.fess.ds.wikipedia.support.XmlDumpSource;

/**
 * Test class for WikipediaDataStore.
 *
 * @author CodeLibs
 */
public class WikipediaDataStoreTest extends UnitDsTestCase {

    private WikipediaDataStore dataStore;

    @Override
    protected String prepareConfigFile() {
        return "test_app.xml";
    }

    @Override
    protected boolean isSuppressTestCaseTransaction() {
        return true;
    }

    @Override
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        dataStore = new WikipediaDataStore();
    }

    @Override
    public void tearDown(TestInfo testInfo) throws Exception {
        ComponentUtil.setFessConfig(null);
        super.tearDown(testInfo);
    }

    @Test
    public void test_getName() {
        assertEquals("WikipediaDataStore", dataStore.getName());
    }

    @Test
    public void test_stripTitle_withTrailingNewline() throws Exception {
        final Method method = WikipediaDataStore.class.getDeclaredMethod("stripTitle", String.class);
        method.setAccessible(true);
        final String result = (String) method.invoke(dataStore, "Test Title\n");
        assertEquals("Test Title", result);
    }

    @Test
    public void test_stripTitle_withMultipleTrailingNewlines() throws Exception {
        final Method method = WikipediaDataStore.class.getDeclaredMethod("stripTitle", String.class);
        method.setAccessible(true);
        final String result = (String) method.invoke(dataStore, "Test Title\n\n\n");
        assertEquals("Test Title", result);
    }

    @Test
    public void test_stripTitle_withTrailingSpaces() throws Exception {
        final Method method = WikipediaDataStore.class.getDeclaredMethod("stripTitle", String.class);
        method.setAccessible(true);
        final String result = (String) method.invoke(dataStore, "Test Title   ");
        assertEquals("Test Title", result);
    }

    @Test
    public void test_stripTitle_withMixedTrailingWhitespace() throws Exception {
        final Method method = WikipediaDataStore.class.getDeclaredMethod("stripTitle", String.class);
        method.setAccessible(true);
        final String result = (String) method.invoke(dataStore, "Test Title \n \n  ");
        assertEquals("Test Title", result);
    }

    @Test
    public void test_stripTitle_withNoTrailingWhitespace() throws Exception {
        final Method method = WikipediaDataStore.class.getDeclaredMethod("stripTitle", String.class);
        method.setAccessible(true);
        final String result = (String) method.invoke(dataStore, "Test Title");
        assertEquals("Test Title", result);
    }

    @Test
    public void test_stripTitle_withOnlyWhitespace() throws Exception {
        final Method method = WikipediaDataStore.class.getDeclaredMethod("stripTitle", String.class);
        method.setAccessible(true);
        final String result = (String) method.invoke(dataStore, "   \n\n  ");
        assertEquals("", result);
    }

    @Test
    public void test_stripTitle_withEmptyString() throws Exception {
        final Method method = WikipediaDataStore.class.getDeclaredMethod("stripTitle", String.class);
        method.setAccessible(true);
        final String result = (String) method.invoke(dataStore, "");
        assertEquals("", result);
    }

    @Test
    public void test_stripTitle_withInternalWhitespace() throws Exception {
        final Method method = WikipediaDataStore.class.getDeclaredMethod("stripTitle", String.class);
        method.setAccessible(true);
        final String result = (String) method.invoke(dataStore, "Test  Title  With  Spaces\n");
        assertEquals("Test  Title  With  Spaces", result);
    }

    @Test
    public void test_stripTitle_preservesLeadingWhitespace() throws Exception {
        final Method method = WikipediaDataStore.class.getDeclaredMethod("stripTitle", String.class);
        method.setAccessible(true);
        final String result = (String) method.invoke(dataStore, "  Leading spaces\n");
        assertEquals("  Leading spaces", result);
    }

    @Test
    public void test_stripTitle_withSpecialCharacters() throws Exception {
        final Method method = WikipediaDataStore.class.getDeclaredMethod("stripTitle", String.class);
        method.setAccessible(true);
        final String result = (String) method.invoke(dataStore, "Title (disambiguation)\n");
        assertEquals("Title (disambiguation)", result);
    }

    @Test
    public void test_stripTitle_withUnicodeCharacters() throws Exception {
        final Method method = WikipediaDataStore.class.getDeclaredMethod("stripTitle", String.class);
        method.setAccessible(true);
        final String result = (String) method.invoke(dataStore, "日本語タイトル\n");
        assertEquals("日本語タイトル", result);
    }

    @Test
    public void test_stripTitle_withMultibyteCharacters() throws Exception {
        final Method method = WikipediaDataStore.class.getDeclaredMethod("stripTitle", String.class);
        method.setAccessible(true);
        final String result = (String) method.invoke(dataStore, "Tëst Tïtlé  \n");
        assertEquals("Tëst Tïtlé", result);
    }

    @Test
    public void test_constructor() {
        final WikipediaDataStore store = new WikipediaDataStore();
        assertNotNull(store);
    }

    @Test
    public void test_dataStoreNotNull() {
        assertNotNull(dataStore);
    }

    @Test
    public void test_getDumpLocation_returnsTheUrlParameterAsIs() throws Exception {
        final Method method = WikipediaDataStore.class.getDeclaredMethod("getDumpLocation", DataStoreParams.class);
        method.setAccessible(true);
        final DataStoreParams params = new DataStoreParams();
        params.put("url", "https://example.com/jawiki.xml.bz2");
        assertEquals("https://example.com/jawiki.xml.bz2", method.invoke(dataStore, params));
    }

    @Test
    public void test_getDumpLocation_acceptsAPlainLocalPath() throws Exception {
        final Method method = WikipediaDataStore.class.getDeclaredMethod("getDumpLocation", DataStoreParams.class);
        method.setAccessible(true);
        final DataStoreParams params = new DataStoreParams();
        params.put("url", "/var/tmp/jawiki.xml.bz2");
        assertEquals("/var/tmp/jawiki.xml.bz2", method.invoke(dataStore, params));
    }

    @Test
    public void test_getUserAgent_usesTheParameterWhenPresent() throws Exception {
        final Method method = WikipediaDataStore.class.getDeclaredMethod("getUserAgent", DataStoreParams.class);
        method.setAccessible(true);
        final DataStoreParams params = new DataStoreParams();
        params.put("user_agent", "MyBot/1.0 (+https://example.com/bot)");
        assertEquals("MyBot/1.0 (+https://example.com/bot)", method.invoke(dataStore, params));
    }

    @Test
    public void test_getPlainTextOptions_defaultsToKeepingEverything() throws Exception {
        final Method method = WikipediaDataStore.class.getDeclaredMethod("getPlainTextOptions", DataStoreParams.class);
        method.setAccessible(true);
        final PlainTextOptions options = (PlainTextOptions) method.invoke(dataStore, new DataStoreParams());
        assertTrue(options.keepsTemplate("Note"));
        assertTrue(options.keepsTemplate("Cite web"));
        assertTrue(options.keepsCaptions());
    }

    @Test
    public void test_getPlainTextOptions_readsTheHandlerParameters() throws Exception {
        final Method method = WikipediaDataStore.class.getDeclaredMethod("getPlainTextOptions", DataStoreParams.class);
        method.setAccessible(true);
        final DataStoreParams params = new DataStoreParams();
        params.put("drop_templates", "*");
        params.put("keep_templates", "Note,Warning");
        params.put("drop_captions", " true ");
        final PlainTextOptions options = (PlainTextOptions) method.invoke(dataStore, params);
        assertTrue(options.keepsTemplate("Note"));
        assertTrue(options.keepsTemplate("warning"));
        assertFalse(options.keepsTemplate("Infobox"));
        assertFalse(options.keepsCaptions());
    }

    @Test
    public void test_getUserAgent_fallsBackToTheFessCrawlerUserAgentWhenParameterIsAbsent() throws Exception {
        final Method method = WikipediaDataStore.class.getDeclaredMethod("getUserAgent", DataStoreParams.class);
        method.setAccessible(true);
        final DataStoreParams params = new DataStoreParams();
        final FessConfig mockConfig = new FessConfig.SimpleImpl() {
            @Override
            public String getUserAgentName() {
                return "Mozilla/5.0 (compatible; Fess/15.8.0; +http://fess.codelibs.org/bot.html)";
            }
        };
        ComponentUtil.setFessConfig(mockConfig);
        try {
            assertEquals("Mozilla/5.0 (compatible; Fess/15.8.0; +http://fess.codelibs.org/bot.html)", method.invoke(dataStore, params));
        } finally {
            ComponentUtil.setFessConfig(null);
        }
    }

    @Test
    public void test_getUserAgent_fallsBackToTheFessCrawlerUserAgentWhenParameterIsBlank() throws Exception {
        final Method method = WikipediaDataStore.class.getDeclaredMethod("getUserAgent", DataStoreParams.class);
        method.setAccessible(true);
        final DataStoreParams params = new DataStoreParams();
        params.put("user_agent", "   ");
        final FessConfig mockConfig = new FessConfig.SimpleImpl() {
            @Override
            public String getUserAgentName() {
                return "Mozilla/5.0 (compatible; Fess/15.8.0; +http://fess.codelibs.org/bot.html)";
            }
        };
        ComponentUtil.setFessConfig(mockConfig);
        try {
            assertEquals("Mozilla/5.0 (compatible; Fess/15.8.0; +http://fess.codelibs.org/bot.html)", method.invoke(dataStore, params));
        } finally {
            ComponentUtil.setFessConfig(null);
        }
    }

    @Test
    public void test_getUserAgent_fallsBackToALiteralWhenTheFessUserAgentIsBlank() throws Exception {
        final Method method = WikipediaDataStore.class.getDeclaredMethod("getUserAgent", DataStoreParams.class);
        method.setAccessible(true);
        final DataStoreParams params = new DataStoreParams();
        final FessConfig mockConfig = new FessConfig.SimpleImpl() {
            @Override
            public String getUserAgentName() {
                return "";
            }
        };
        ComponentUtil.setFessConfig(mockConfig);
        try {
            final Object result = method.invoke(dataStore, params);
            assertTrue("fallback User-Agent should not be blank but was: " + result,
                    result instanceof String && !((String) result).isBlank());
        } finally {
            ComponentUtil.setFessConfig(null);
        }
    }

    @Test
    public void test_putDocumentValues_keepsTheExistingKeysUnchanged() throws Exception {
        final WikiDocument document = new WikiDocument();
        document.setId("42");
        document.setTitle("Tokyo Tower");
        document.setContent("A tall tower in Tokyo.");
        document.setFormat("text/x-wiki");
        document.setModel("wikitext");
        final java.util.Date timestamp = new java.util.Date(0L);
        document.setTimestamp(timestamp);

        final java.util.Map<String, Object> resultMap = new java.util.LinkedHashMap<>();
        dataStore.putDocumentValues(resultMap, document, 100);

        assertEquals("42", resultMap.get("id"));
        assertEquals("Tokyo Tower", resultMap.get("title"));
        assertEquals("A tall tower in Tokyo.", resultMap.get("content"));
        assertEquals("A tall tower in Tokyo.", resultMap.get("digest"));
        assertEquals("text/x-wiki", resultMap.get("format"));
        assertEquals("wikitext", resultMap.get("model"));
        assertEquals(timestamp, resultMap.get("timestamp"));
        assertNotNull(resultMap.get("encodedTitle"));
    }

    @Test
    public void test_putDocumentValues_abbreviatesTheDigest() throws Exception {
        final WikiDocument document = new WikiDocument();
        document.setTitle("T");
        document.setContent("0123456789012345678901234567890123456789");

        final java.util.Map<String, Object> resultMap = new java.util.LinkedHashMap<>();
        dataStore.putDocumentValues(resultMap, document, 10);

        assertEquals("0123456...", resultMap.get("digest"));
    }

    @Test
    public void test_putDocumentValues_addsTheNewKeys() throws Exception {
        final WikiDocument document = new WikiDocument();
        document.setId("7");
        document.setTitle("Cats");
        document.setContent("body");
        document.setNamespace(14);
        document.setWikitext("[[Category:Cats]] body");
        document.setCategories(java.util.List.of("Cats"));
        document.setLinks(java.util.List.of("Tokyo Tower"));
        document.setRedirect(true);
        document.setRedirectTitle("Article");
        document.setStub(true);
        document.setDisambiguation(true);

        final java.util.Map<String, Object> resultMap = new java.util.LinkedHashMap<>();
        dataStore.putDocumentValues(resultMap, document, 100);

        assertEquals(14, resultMap.get("ns"));
        assertEquals(java.util.List.of("Cats"), resultMap.get("categories"));
        assertEquals(java.util.List.of("Tokyo Tower"), resultMap.get("links"));
        assertEquals("[[Category:Cats]] body", resultMap.get("wikitext"));
        assertEquals(Boolean.TRUE, resultMap.get("redirect"));
        assertEquals("Article", resultMap.get("redirectTitle"));
        assertEquals(Boolean.TRUE, resultMap.get("stub"));
        assertEquals(Boolean.TRUE, resultMap.get("disambiguation"));
        assertEquals(4, resultMap.get("contentLength"));
    }

    @Test
    public void test_putDocumentValues_encodesSpacesAsUnderscores() throws Exception {
        // "Tokyo+Tower" is a 404 on Wikipedia; "Tokyo_Tower" is the real page.
        final WikiDocument document = new WikiDocument();
        document.setTitle("Tokyo Tower");
        document.setContent("body");
        final java.util.Map<String, Object> resultMap = new java.util.LinkedHashMap<>();
        dataStore.putDocumentValues(resultMap, document, 100);
        assertEquals("Tokyo_Tower", resultMap.get("encodedTitle"));
    }

    @Test
    public void test_putDocumentValues_percentEncodesTheRest() throws Exception {
        final WikiDocument document = new WikiDocument();
        document.setTitle("Mercury (planet)");
        document.setContent("body");
        final java.util.Map<String, Object> resultMap = new java.util.LinkedHashMap<>();
        dataStore.putDocumentValues(resultMap, document, 100);
        assertEquals("Mercury_%28planet%29", resultMap.get("encodedTitle"));
    }

    @Test
    public void test_isCirrusLocation() throws Exception {
        assertTrue("a cirrus chunk", dataStore.isCirrusLocation(
                "https://dumps.wikimedia.org/other/cirrus_search_index/20260816/index_name=jawiki_content/jawiki_content-20260816-00000.json.bz2"));
        assertTrue("a cirrus directory",
                dataStore.isCirrusLocation("https://dumps.wikimedia.org/other/cirrus_search_index/20260816/index_name=jawiki_content/"));
        assertTrue("a plain ndjson chunk", dataStore.isCirrusLocation("/var/tmp/jawiki_content-00000.json.bz2"));
        assertTrue("an uncompressed ndjson chunk", dataStore.isCirrusLocation("/var/tmp/jawiki_content-00000.json"));
        assertFalse("an xml dump",
                dataStore.isCirrusLocation("https://dumps.wikimedia.org/jawiki/latest/jawiki-latest-pages-articles.xml.bz2"));
    }

    @Test
    public void test_getSiteHost_derivesFromTheDumpName() throws Exception {
        assertEquals("ja.wikipedia.org",
                dataStore.getSiteHost("https://dumps.wikimedia.org/jawiki/latest/jawiki-latest-pages-articles.xml.bz2"));
        assertEquals("en.wikipedia.org", dataStore.getSiteHost("/var/tmp/enwiki_content-20260816-00000.json.bz2"));
        assertEquals("ja.wikipedia.org",
                dataStore.getSiteHost("https://dumps.wikimedia.org/other/cirrus_search_index/20260816/index_name=jawiki_content/"));
    }

    @Test
    public void test_getSiteHost_staysSilentWhenItCannotTell() throws Exception {
        // A sister project is not wikipedia.org, and a guess would produce dead links.
        assertNull(dataStore.getSiteHost("/var/tmp/jawikibooks_content-00000.json.bz2"));
        assertNull(dataStore.getSiteHost("/var/tmp/dump.xml.bz2"));
        assertNull(dataStore.getSiteHost("/home/wiki/dumps/something.xml.bz2"));
    }

    @Test
    public void test_getSiteLanguage() throws Exception {
        assertEquals("ja", dataStore.getSiteLanguage("/var/tmp/jawiki-latest-pages-articles.xml.bz2"));
        assertNull(dataStore.getSiteLanguage("/var/tmp/dump.xml.bz2"));
    }

    @Test
    public void test_putSiteValues_buildsTheArticleUrl() throws Exception {
        final WikiDocument document = new WikiDocument();
        document.setTitle("Tokyo Tower");
        document.setContent("body");
        final java.util.Map<String, Object> resultMap = new java.util.LinkedHashMap<>();
        dataStore.putDocumentValues(resultMap, document, 100);
        dataStore.putSiteValues(resultMap, document, "ja.wikipedia.org", "ja");

        assertEquals("ja", resultMap.get("lang"));
        assertEquals("ja.wikipedia.org", resultMap.get("host"));
        assertEquals("ja.wikipedia.org", resultMap.get("site"));
        assertEquals("https://ja.wikipedia.org/wiki/Tokyo_Tower", resultMap.get("url"));
    }

    @Test
    public void test_putSiteValues_prefersTheLanguageOnTheDocument() throws Exception {
        final WikiDocument document = new WikiDocument();
        document.setTitle("T");
        document.setContent("body");
        document.setLanguage("en");
        final java.util.Map<String, Object> resultMap = new java.util.LinkedHashMap<>();
        dataStore.putDocumentValues(resultMap, document, 100);
        dataStore.putSiteValues(resultMap, document, "ja.wikipedia.org", "ja");
        assertEquals("en", resultMap.get("lang"));
    }

    @Test
    public void test_putSiteValues_omitsTheUrlWhenTheHostIsUnknown() throws Exception {
        final WikiDocument document = new WikiDocument();
        document.setTitle("T");
        document.setContent("body");
        final java.util.Map<String, Object> resultMap = new java.util.LinkedHashMap<>();
        dataStore.putDocumentValues(resultMap, document, 100);
        dataStore.putSiteValues(resultMap, document, null, null);
        assertNull(resultMap.get("url"));
        assertNull(resultMap.get("host"));
        assertNull(resultMap.get("site"));
    }

    @Test
    public void test_putSiteValues_doesNotDependOnPutDocumentValues() throws Exception {
        // putSiteValues must derive the encoded title itself; it must not read a key that
        // only putDocumentValues would have put into resultMap.
        final WikiDocument document = new WikiDocument();
        document.setTitle("Tokyo Tower");
        document.setContent("body");
        final java.util.Map<String, Object> resultMap = new java.util.LinkedHashMap<>();
        dataStore.putSiteValues(resultMap, document, "ja.wikipedia.org", "ja");

        assertEquals("https://ja.wikipedia.org/wiki/Tokyo_Tower", resultMap.get("url"));
    }

    @Test
    public void test_putCirrusValues() throws Exception {
        final WikiDocument document = new WikiDocument();
        document.setOpeningText("A song.");
        document.setHeadings(java.util.List.of("Overview"));
        document.setExternalLinks(java.util.List.of("https://example.com/1"));
        document.setTemplates(java.util.List.of("Template:Infobox"));
        document.setRedirects(java.util.List.of("Alias"));
        document.setAuxiliaryText(java.util.List.of("Infobox text"));
        document.setWeightedTags(java.util.List.of("topic/Music|1000"));
        document.setWikibaseItem("Q1");
        document.setIncomingLinks(20);
        document.setPopularityScore(0.5d);
        document.setRevisionId("109913677");

        final java.util.Map<String, Object> resultMap = new java.util.LinkedHashMap<>();
        dataStore.putCirrusValues(resultMap, document);

        assertEquals("A song.", resultMap.get("openingText"));
        assertEquals(java.util.List.of("Overview"), resultMap.get("headings"));
        assertEquals(java.util.List.of("https://example.com/1"), resultMap.get("externalLinks"));
        assertEquals(java.util.List.of("Template:Infobox"), resultMap.get("templates"));
        assertEquals(java.util.List.of("Alias"), resultMap.get("redirects"));
        assertEquals(java.util.List.of("Infobox text"), resultMap.get("auxiliaryText"));
        assertEquals(java.util.List.of("topic/Music|1000"), resultMap.get("weightedTags"));
        assertEquals("Q1", resultMap.get("wikibaseItem"));
        assertEquals(Integer.valueOf(20), resultMap.get("incomingLinks"));
        assertEquals(Double.valueOf(0.5d), resultMap.get("popularityScore"));
        assertEquals("109913677", resultMap.get("revisionId"));
    }

    private String fixtureLocation(final String fixture) {
        final java.net.URL url = getClass().getResource("/fixtures/" + fixture);
        java.util.Objects.requireNonNull(url, "fixture not found: " + fixture);
        return url.toString();
    }

    @Test
    public void test_createDumpSource_autoPicksXmlForAnXmlLocation() throws Exception {
        final DataStoreParams params = new DataStoreParams();
        final DumpFetcher fetcher = new DumpFetcher("TestAgent/1.0");
        final DumpSource source = dataStore.createDumpSource(params,
                "https://dumps.wikimedia.org/jawiki/latest/jawiki-latest-pages-articles.xml.bz2", fetcher, 100);
        assertTrue("expected an XmlDumpSource but was " + source.getClass(), source instanceof XmlDumpSource);
    }

    @Test
    public void test_createDumpSource_autoPicksCirrusForACirrusLocation() throws Exception {
        // Real fixture so the DumpLocationResolver has something to resolve.
        final DataStoreParams params = new DataStoreParams();
        final DumpFetcher fetcher = new DumpFetcher("TestAgent/1.0");
        final DumpSource source = dataStore.createDumpSource(params, fixtureLocation("cirrus-sample.json"), fetcher, 100);
        assertTrue("expected a CirrusIndexDumpSource but was " + source.getClass(), source instanceof CirrusIndexDumpSource);
    }

    @Test
    public void test_createDumpSource_xmlExplicitlyOverridesACirrusLookingLocation() throws Exception {
        // source=xml on a location isCirrusLocation would otherwise call cirrus.
        final DataStoreParams params = new DataStoreParams();
        params.put("source", "xml");
        final DumpFetcher fetcher = new DumpFetcher("TestAgent/1.0");
        final DumpSource source = dataStore.createDumpSource(params, fixtureLocation("cirrus-sample.json"), fetcher, 100);
        assertTrue("expected an XmlDumpSource but was " + source.getClass(), source instanceof XmlDumpSource);
    }

    @Test
    public void test_createDumpSource_cirrusExplicitlyOverridesAnXmlLookingLocation() throws Exception {
        // source=cirrus on a location isCirrusLocation would otherwise call xml: the case the
        // ||/&& precedence of the source-selection expression decides. With the wrong grouping
        // ("cirrus".equals(sourceType) || "auto".equals(sourceType)) && isCirrusLocation(...),
        // this would incorrectly fall through to XmlDumpSource. Real fixture so the
        // DumpLocationResolver has something to resolve.
        final DataStoreParams params = new DataStoreParams();
        params.put("source", "cirrus");
        final DumpFetcher fetcher = new DumpFetcher("TestAgent/1.0");
        final DumpSource source = dataStore.createDumpSource(params, fixtureLocation("wiki-single.xml.bz2"), fetcher, 100);
        assertTrue("expected a CirrusIndexDumpSource but was " + source.getClass(), source instanceof CirrusIndexDumpSource);
    }
}
