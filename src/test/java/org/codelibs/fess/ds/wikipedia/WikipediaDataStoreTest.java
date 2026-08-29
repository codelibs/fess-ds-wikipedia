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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.lang.reflect.Method;

import org.codelibs.fess.entity.DataStoreParams;
import org.codelibs.fess.mylasta.direction.FessConfig;
import org.codelibs.fess.util.ComponentUtil;
import org.codelibs.fess.ds.wikipedia.UnitDsTestCase;
import org.codelibs.fess.ds.wikipedia.support.WikiDocument;

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
}
