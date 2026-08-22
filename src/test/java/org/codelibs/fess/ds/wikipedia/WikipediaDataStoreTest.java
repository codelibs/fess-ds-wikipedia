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

import org.codelibs.fess.util.ComponentUtil;
import org.codelibs.fess.ds.wikipedia.UnitDsTestCase;

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
        final java.lang.reflect.Method method =
                WikipediaDataStore.class.getDeclaredMethod("getDumpLocation", org.codelibs.fess.entity.DataStoreParams.class);
        method.setAccessible(true);
        final org.codelibs.fess.entity.DataStoreParams params = new org.codelibs.fess.entity.DataStoreParams();
        params.put("url", "https://example.com/jawiki.xml.bz2");
        assertEquals("https://example.com/jawiki.xml.bz2", method.invoke(dataStore, params));
    }

    @Test
    public void test_getDumpLocation_acceptsAPlainLocalPath() throws Exception {
        final java.lang.reflect.Method method =
                WikipediaDataStore.class.getDeclaredMethod("getDumpLocation", org.codelibs.fess.entity.DataStoreParams.class);
        method.setAccessible(true);
        final org.codelibs.fess.entity.DataStoreParams params = new org.codelibs.fess.entity.DataStoreParams();
        params.put("url", "/var/tmp/jawiki.xml.bz2");
        assertEquals("/var/tmp/jawiki.xml.bz2", method.invoke(dataStore, params));
    }

    @Test
    public void test_getUserAgent_usesTheParameterWhenPresent() throws Exception {
        final java.lang.reflect.Method method =
                WikipediaDataStore.class.getDeclaredMethod("getUserAgent", org.codelibs.fess.entity.DataStoreParams.class);
        method.setAccessible(true);
        final org.codelibs.fess.entity.DataStoreParams params = new org.codelibs.fess.entity.DataStoreParams();
        params.put("user_agent", "MyBot/1.0 (+https://example.com/bot)");
        assertEquals("MyBot/1.0 (+https://example.com/bot)", method.invoke(dataStore, params));
    }
}
