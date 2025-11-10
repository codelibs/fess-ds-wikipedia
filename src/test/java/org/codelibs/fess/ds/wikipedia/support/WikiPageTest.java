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

import java.util.Date;
import java.util.List;

import org.dbflute.utflute.core.PlainTestCase;

/**
 * Test class for WikiPage.
 *
 * @author CodeLibs
 */
public class WikiPageTest extends PlainTestCase {

    public void test_setAndGetTitle() {
        final WikiPage page = new WikiPage();
        page.setTitle("Test Title");
        assertEquals("Test Title", page.getTitle());
    }

    public void test_setAndGetId() {
        final WikiPage page = new WikiPage();
        page.setId("12345");
        assertEquals("12345", page.getId());
    }

    public void test_setAndGetTimestamp() {
        final WikiPage page = new WikiPage();
        final Date timestamp = new Date();
        page.setTimestamp(timestamp);
        assertEquals(timestamp, page.getTimestamp());
    }

    public void test_setAndGetFormat() {
        final WikiPage page = new WikiPage();
        page.setFormat("text/x-wiki");
        assertEquals("text/x-wiki", page.getFormat());
    }

    public void test_setAndGetModel() {
        final WikiPage page = new WikiPage();
        page.setModel("wikitext");
        assertEquals("wikitext", page.getModel());
    }

    public void test_setWikiText_initializesParser() {
        final WikiPage page = new WikiPage();
        page.setWikiText("Test wiki text");
        assertNotNull(page.getWikiText());
        assertEquals("Test wiki text", page.getWikiText());
    }

    public void test_isDisambiguationPage_withDisambigInTitle() {
        final WikiPage page = new WikiPage();
        page.setTitle("Test (disambiguation)");
        page.setWikiText("Regular text");
        assertTrue(page.isDisambiguationPage());
    }

    public void test_isDisambiguationPage_withDisambigTag() {
        final WikiPage page = new WikiPage();
        page.setTitle("Test Page");
        page.setWikiText("{{disambig}}");
        assertTrue(page.isDisambiguationPage());
    }

    public void test_isDisambiguationPage_withBoth() {
        final WikiPage page = new WikiPage();
        page.setTitle("Test (disambiguation)");
        page.setWikiText("{{disambig}}");
        assertTrue(page.isDisambiguationPage());
    }

    public void test_isDisambiguationPage_withNeither() {
        final WikiPage page = new WikiPage();
        page.setTitle("Test Page");
        page.setWikiText("Regular content");
        assertFalse(page.isDisambiguationPage());
    }

    public void test_isSpecialPage_withColon() {
        final WikiPage page = new WikiPage();
        page.setTitle("Category:Test");
        page.setWikiText("");
        assertTrue(page.isSpecialPage());
    }

    public void test_isSpecialPage_withWikipediaNamespace() {
        final WikiPage page = new WikiPage();
        page.setTitle("Wikipedia:Manual of Style");
        page.setWikiText("");
        assertTrue(page.isSpecialPage());
    }

    public void test_isSpecialPage_withFileNamespace() {
        final WikiPage page = new WikiPage();
        page.setTitle("File:Example.jpg");
        page.setWikiText("");
        assertTrue(page.isSpecialPage());
    }

    public void test_isSpecialPage_normalPage() {
        final WikiPage page = new WikiPage();
        page.setTitle("Regular Page Title");
        page.setWikiText("");
        assertFalse(page.isSpecialPage());
    }

    public void test_isRedirect_withRedirect() {
        final WikiPage page = new WikiPage();
        page.setWikiText("#REDIRECT [[Target Page]]");
        assertTrue(page.isRedirect());
    }

    public void test_isRedirect_withoutRedirect() {
        final WikiPage page = new WikiPage();
        page.setWikiText("Regular content");
        assertFalse(page.isRedirect());
    }

    public void test_getRedirectPage() {
        final WikiPage page = new WikiPage();
        page.setWikiText("#REDIRECT [[Target Page]]");
        assertEquals("Target Page", page.getRedirectPage());
    }

    public void test_getRedirectPage_noRedirect() {
        final WikiPage page = new WikiPage();
        page.setWikiText("Regular content");
        assertNull(page.getRedirectPage());
    }

    public void test_isStub_withStub() {
        final WikiPage page = new WikiPage();
        page.setWikiText("Short article {{geo-stub}}");
        assertTrue(page.isStub());
    }

    public void test_isStub_withoutStub() {
        final WikiPage page = new WikiPage();
        page.setWikiText("Complete article");
        assertFalse(page.isStub());
    }

    public void test_getText_returnsPlainText() {
        final WikiPage page = new WikiPage();
        page.setWikiText("'''Bold''' text");
        final String plainText = page.getText();
        assertFalse(plainText.contains("'''"));
        assertTrue(plainText.contains("Bold"));
        assertTrue(plainText.contains("text"));
    }

    public void test_getCategories() {
        final WikiPage page = new WikiPage();
        page.setWikiText("[[Category:Test]] [[Category:Example]]");
        final List<String> categories = page.getCategories();
        assertEquals(2, categories.size());
        assertTrue(categories.contains("Test"));
        assertTrue(categories.contains("Example"));
    }

    public void test_getCategories_empty() {
        final WikiPage page = new WikiPage();
        page.setWikiText("No categories here");
        final List<String> categories = page.getCategories();
        assertEquals(0, categories.size());
    }

    public void test_getLinks() {
        final WikiPage page = new WikiPage();
        page.setWikiText("[[Link One]] and [[Link Two]]");
        final List<String> links = page.getLinks();
        assertEquals(2, links.size());
        assertTrue(links.contains("Link One"));
        assertTrue(links.contains("Link Two"));
    }

    public void test_getLinks_empty() {
        final WikiPage page = new WikiPage();
        page.setWikiText("No links here");
        final List<String> links = page.getLinks();
        assertEquals(0, links.size());
    }

    public void test_getInfoBox() {
        final WikiPage page = new WikiPage();
        page.setWikiText("{{Infobox person\n|name=John\n}}");
        final InfoBox infoBox = page.getInfoBox();
        assertNotNull(infoBox);
        assertTrue(infoBox.dumpRaw().contains("Infobox"));
    }

    public void test_getInfoBox_noInfoBox() {
        final WikiPage page = new WikiPage();
        page.setWikiText("Regular text");
        final InfoBox infoBox = page.getInfoBox();
        assertNull(infoBox);
    }

    public void test_getTranslatedTitle() {
        final WikiPage page = new WikiPage();
        page.setWikiText("[[en:English Title]]\n[[ja:日本語タイトル]]");
        assertEquals("English Title", page.getTranslatedTitle("en"));
        assertEquals("日本語タイトル", page.getTranslatedTitle("ja"));
    }

    public void test_getTranslatedTitle_noTranslation() {
        final WikiPage page = new WikiPage();
        page.setWikiText("No translations");
        assertNull(page.getTranslatedTitle("en"));
    }

    public void test_complexPage_allFeatures() {
        final WikiPage page = new WikiPage();
        page.setTitle("Test Article");
        page.setId("98765");
        page.setFormat("text/x-wiki");
        page.setModel("wikitext");
        page.setTimestamp(new Date());
        page.setWikiText(
                "'''Test Article''' is a test.\n"
                + "[[Category:Testing]]\n"
                + "[[Link Page]]\n"
                + "{{Infobox test\n|field=value\n}}"
        );

        assertEquals("Test Article", page.getTitle());
        assertEquals("98765", page.getId());
        assertNotNull(page.getTimestamp());
        assertFalse(page.isRedirect());
        assertFalse(page.isStub());
        assertFalse(page.isSpecialPage());
        assertFalse(page.isDisambiguationPage());
        assertEquals(1, page.getCategories().size());
        assertEquals(1, page.getLinks().size());
        assertNotNull(page.getInfoBox());
    }

    public void test_nullSafety() {
        final WikiPage page = new WikiPage();
        assertNull(page.getTitle());
        assertNull(page.getId());
        assertNull(page.getTimestamp());
        assertNull(page.getFormat());
        assertNull(page.getModel());
    }
}
