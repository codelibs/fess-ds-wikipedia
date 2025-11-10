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

import java.lang.reflect.Method;

import org.codelibs.fess.util.ComponentUtil;
import org.dbflute.utflute.lastaflute.LastaFluteTestCase;

/**
 * Test class for WikipediaDataStore.
 *
 * @author CodeLibs
 */
public class WikipediaDataStoreTest extends LastaFluteTestCase {

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
    public void setUp() throws Exception {
        super.setUp();
        dataStore = new WikipediaDataStore();
    }

    @Override
    public void tearDown() throws Exception {
        ComponentUtil.setFessConfig(null);
        super.tearDown();
    }

    public void test_getName() {
        assertEquals("WikipediaDataStore", dataStore.getName());
    }

    public void test_stripTitle_withTrailingNewline() throws Exception {
        final Method method = WikipediaDataStore.class.getDeclaredMethod("stripTitle", String.class);
        method.setAccessible(true);
        final String result = (String) method.invoke(dataStore, "Test Title\n");
        assertEquals("Test Title", result);
    }

    public void test_stripTitle_withMultipleTrailingNewlines() throws Exception {
        final Method method = WikipediaDataStore.class.getDeclaredMethod("stripTitle", String.class);
        method.setAccessible(true);
        final String result = (String) method.invoke(dataStore, "Test Title\n\n\n");
        assertEquals("Test Title", result);
    }

    public void test_stripTitle_withTrailingSpaces() throws Exception {
        final Method method = WikipediaDataStore.class.getDeclaredMethod("stripTitle", String.class);
        method.setAccessible(true);
        final String result = (String) method.invoke(dataStore, "Test Title   ");
        assertEquals("Test Title", result);
    }

    public void test_stripTitle_withMixedTrailingWhitespace() throws Exception {
        final Method method = WikipediaDataStore.class.getDeclaredMethod("stripTitle", String.class);
        method.setAccessible(true);
        final String result = (String) method.invoke(dataStore, "Test Title \n \n  ");
        assertEquals("Test Title", result);
    }

    public void test_stripTitle_withNoTrailingWhitespace() throws Exception {
        final Method method = WikipediaDataStore.class.getDeclaredMethod("stripTitle", String.class);
        method.setAccessible(true);
        final String result = (String) method.invoke(dataStore, "Test Title");
        assertEquals("Test Title", result);
    }

    public void test_stripTitle_withOnlyWhitespace() throws Exception {
        final Method method = WikipediaDataStore.class.getDeclaredMethod("stripTitle", String.class);
        method.setAccessible(true);
        final String result = (String) method.invoke(dataStore, "   \n\n  ");
        assertEquals("", result);
    }

    public void test_stripTitle_withEmptyString() throws Exception {
        final Method method = WikipediaDataStore.class.getDeclaredMethod("stripTitle", String.class);
        method.setAccessible(true);
        final String result = (String) method.invoke(dataStore, "");
        assertEquals("", result);
    }

    public void test_stripTitle_withInternalWhitespace() throws Exception {
        final Method method = WikipediaDataStore.class.getDeclaredMethod("stripTitle", String.class);
        method.setAccessible(true);
        final String result = (String) method.invoke(dataStore, "Test  Title  With  Spaces\n");
        assertEquals("Test  Title  With  Spaces", result);
    }

    public void test_stripTitle_preservesLeadingWhitespace() throws Exception {
        final Method method = WikipediaDataStore.class.getDeclaredMethod("stripTitle", String.class);
        method.setAccessible(true);
        final String result = (String) method.invoke(dataStore, "  Leading spaces\n");
        assertEquals("  Leading spaces", result);
    }

    public void test_stripTitle_withSpecialCharacters() throws Exception {
        final Method method = WikipediaDataStore.class.getDeclaredMethod("stripTitle", String.class);
        method.setAccessible(true);
        final String result = (String) method.invoke(dataStore, "Title (disambiguation)\n");
        assertEquals("Title (disambiguation)", result);
    }

    public void test_stripTitle_withUnicodeCharacters() throws Exception {
        final Method method = WikipediaDataStore.class.getDeclaredMethod("stripTitle", String.class);
        method.setAccessible(true);
        final String result = (String) method.invoke(dataStore, "日本語タイトル\n");
        assertEquals("日本語タイトル", result);
    }

    public void test_stripTitle_withMultibyteCharacters() throws Exception {
        final Method method = WikipediaDataStore.class.getDeclaredMethod("stripTitle", String.class);
        method.setAccessible(true);
        final String result = (String) method.invoke(dataStore, "Tëst Tïtlé  \n");
        assertEquals("Tëst Tïtlé", result);
    }

    public void test_constructor() {
        final WikipediaDataStore store = new WikipediaDataStore();
        assertNotNull(store);
    }

    public void test_dataStoreNotNull() {
        assertNotNull(dataStore);
    }
}
