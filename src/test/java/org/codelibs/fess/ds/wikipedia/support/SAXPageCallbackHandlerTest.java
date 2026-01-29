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

import org.junit.jupiter.api.TestInfo;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.codelibs.fess.ds.wikipedia.UnitDsTestCase;
import org.xml.sax.InputSource;

/**
 * Test class for SAXPageCallbackHandler.
 *
 * @author CodeLibs
 */
public class SAXPageCallbackHandlerTest extends UnitDsTestCase {

    public void test_parseSinglePage() throws Exception {
        final String xml = "<mediawiki>" + "<page>" + "<title>Test Page</title>" + "<id>123</id>" + "<revision>"
                + "<timestamp>2023-01-15T10:30:00Z</timestamp>" + "<format>text/x-wiki</format>" + "<model>wikitext</model>"
                + "<text>Test content</text>" + "</revision>" + "</page>" + "</mediawiki>";

        final List<WikiPage> pages = new ArrayList<>();
        final PageCallbackHandler callback = page -> pages.add(page);
        final SAXPageCallbackHandler handler = new SAXPageCallbackHandler(callback);

        parseXML(xml, handler);

        assertEquals(1, pages.size());
        final WikiPage page = pages.get(0);
        assertEquals("Test Page", page.getTitle());
        assertEquals("123", page.getId());
        assertEquals("text/x-wiki", page.getFormat());
        assertEquals("wikitext", page.getModel());
        assertNotNull(page.getTimestamp());
        assertEquals("Test content", page.getWikiText());
    }

    public void test_parseMultiplePages() throws Exception {
        final String xml = "<mediawiki>" + "<page>" + "<title>Page One</title>" + "<id>1</id>" + "<revision>"
                + "<timestamp>2023-01-01T00:00:00Z</timestamp>" + "<text>Content one</text>" + "</revision>" + "</page>" + "<page>"
                + "<title>Page Two</title>" + "<id>2</id>" + "<revision>" + "<timestamp>2023-01-02T00:00:00Z</timestamp>"
                + "<text>Content two</text>" + "</revision>" + "</page>" + "</mediawiki>";

        final List<WikiPage> pages = new ArrayList<>();
        final PageCallbackHandler callback = page -> pages.add(page);
        final SAXPageCallbackHandler handler = new SAXPageCallbackHandler(callback);

        parseXML(xml, handler);

        assertEquals(2, pages.size());
        assertEquals("Page One", pages.get(0).getTitle());
        assertEquals("1", pages.get(0).getId());
        assertEquals("Page Two", pages.get(1).getTitle());
        assertEquals("2", pages.get(1).getId());
    }

    public void test_parsePageWithMultilineText() throws Exception {
        final String xml = "<mediawiki>" + "<page>" + "<title>Multiline Page</title>" + "<id>100</id>" + "<revision>" + "<text>Line one\n"
                + "Line two\n" + "Line three</text>" + "</revision>" + "</page>" + "</mediawiki>";

        final List<WikiPage> pages = new ArrayList<>();
        final PageCallbackHandler callback = page -> pages.add(page);
        final SAXPageCallbackHandler handler = new SAXPageCallbackHandler(callback);

        parseXML(xml, handler);

        assertEquals(1, pages.size());
        final WikiPage page = pages.get(0);
        assertTrue(page.getWikiText().contains("Line one"));
        assertTrue(page.getWikiText().contains("Line two"));
        assertTrue(page.getWikiText().contains("Line three"));
    }

    public void test_parsePageWithSpecialCharacters() throws Exception {
        final String xml = "<mediawiki>" + "<page>" + "<title>Special &amp; Characters &lt;&gt;</title>" + "<id>999</id>" + "<revision>"
                + "<text>Content with &amp;amp; and &lt;tags&gt;</text>" + "</revision>" + "</page>" + "</mediawiki>";

        final List<WikiPage> pages = new ArrayList<>();
        final PageCallbackHandler callback = page -> pages.add(page);
        final SAXPageCallbackHandler handler = new SAXPageCallbackHandler(callback);

        parseXML(xml, handler);

        assertEquals(1, pages.size());
        final WikiPage page = pages.get(0);
        assertTrue(page.getTitle().contains("&"));
        assertTrue(page.getTitle().contains("<"));
        assertTrue(page.getTitle().contains(">"));
    }

    public void test_parsePageWithOnlyRequiredFields() throws Exception {
        final String xml = "<mediawiki>" + "<page>" + "<title>Minimal Page</title>" + "<revision>" + "<text>Minimal content</text>"
                + "</revision>" + "</page>" + "</mediawiki>";

        final List<WikiPage> pages = new ArrayList<>();
        final PageCallbackHandler callback = page -> pages.add(page);
        final SAXPageCallbackHandler handler = new SAXPageCallbackHandler(callback);

        parseXML(xml, handler);

        assertEquals(1, pages.size());
        final WikiPage page = pages.get(0);
        assertEquals("Minimal Page", page.getTitle());
        assertEquals("Minimal content", page.getWikiText());
    }

    public void test_parsePageWithEmptyText() throws Exception {
        final String xml = "<mediawiki>" + "<page>" + "<title>Empty Page</title>" + "<id>42</id>" + "<revision>" + "<text></text>"
                + "</revision>" + "</page>" + "</mediawiki>";

        final List<WikiPage> pages = new ArrayList<>();
        final PageCallbackHandler callback = page -> pages.add(page);
        final SAXPageCallbackHandler handler = new SAXPageCallbackHandler(callback);

        parseXML(xml, handler);

        assertEquals(1, pages.size());
        final WikiPage page = pages.get(0);
        assertEquals("Empty Page", page.getTitle());
        assertEquals("", page.getWikiText());
    }

    public void test_parsePageWithMultipleIds_usesFirst() throws Exception {
        final String xml = "<mediawiki>" + "<page>" + "<title>Test</title>" + "<id>111</id>" + "<revision>" + "<id>222</id>"
                + "<text>Content</text>" + "</revision>" + "</page>" + "</mediawiki>";

        final List<WikiPage> pages = new ArrayList<>();
        final PageCallbackHandler callback = page -> pages.add(page);
        final SAXPageCallbackHandler handler = new SAXPageCallbackHandler(callback);

        parseXML(xml, handler);

        assertEquals(1, pages.size());
        assertEquals("111", pages.get(0).getId());
    }

    public void test_parseTimestamp_validFormat() throws Exception {
        final String xml = "<mediawiki>" + "<page>" + "<title>Test</title>" + "<revision>" + "<timestamp>2025-03-15T14:45:30Z</timestamp>"
                + "<text>Content</text>" + "</revision>" + "</page>" + "</mediawiki>";

        final List<WikiPage> pages = new ArrayList<>();
        final PageCallbackHandler callback = page -> pages.add(page);
        final SAXPageCallbackHandler handler = new SAXPageCallbackHandler(callback);

        parseXML(xml, handler);

        assertEquals(1, pages.size());
        assertNotNull(pages.get(0).getTimestamp());
    }

    public void test_parseTimestamp_invalidFormat_logsWarning() throws Exception {
        final String xml = "<mediawiki>" + "<page>" + "<title>Test</title>" + "<revision>" + "<timestamp>invalid-date</timestamp>"
                + "<text>Content</text>" + "</revision>" + "</page>" + "</mediawiki>";

        final List<WikiPage> pages = new ArrayList<>();
        final PageCallbackHandler callback = page -> pages.add(page);
        final SAXPageCallbackHandler handler = new SAXPageCallbackHandler(callback);

        parseXML(xml, handler);

        assertEquals(1, pages.size());
        assertNull(pages.get(0).getTimestamp());
    }

    public void test_parseFormat() throws Exception {
        final String xml = "<mediawiki>" + "<page>" + "<title>Test</title>" + "<revision>" + "<format>application/json</format>"
                + "<text>Content</text>" + "</revision>" + "</page>" + "</mediawiki>";

        final List<WikiPage> pages = new ArrayList<>();
        final PageCallbackHandler callback = page -> pages.add(page);
        final SAXPageCallbackHandler handler = new SAXPageCallbackHandler(callback);

        parseXML(xml, handler);

        assertEquals(1, pages.size());
        assertEquals("application/json", pages.get(0).getFormat());
    }

    public void test_parseModel() throws Exception {
        final String xml = "<mediawiki>" + "<page>" + "<title>Test</title>" + "<revision>" + "<model>custom-model</model>"
                + "<text>Content</text>" + "</revision>" + "</page>" + "</mediawiki>";

        final List<WikiPage> pages = new ArrayList<>();
        final PageCallbackHandler callback = page -> pages.add(page);
        final SAXPageCallbackHandler handler = new SAXPageCallbackHandler(callback);

        parseXML(xml, handler);

        assertEquals(1, pages.size());
        assertEquals("custom-model", pages.get(0).getModel());
    }

    public void test_parseWithWhitespace() throws Exception {
        final String xml = "<mediawiki>" + "<page>" + "<title>  Title with spaces  </title>" + "<id>  456  </id>" + "<revision>"
                + "<format>  text/x-wiki  </format>" + "<model>  wikitext  </model>" + "<text>Content</text>" + "</revision>" + "</page>"
                + "</mediawiki>";

        final List<WikiPage> pages = new ArrayList<>();
        final PageCallbackHandler callback = page -> pages.add(page);
        final SAXPageCallbackHandler handler = new SAXPageCallbackHandler(callback);

        parseXML(xml, handler);

        assertEquals(1, pages.size());
        final WikiPage page = pages.get(0);
        assertEquals("  Title with spaces  ", page.getTitle());
        assertEquals("456", page.getId());
        assertEquals("text/x-wiki", page.getFormat());
        assertEquals("wikitext", page.getModel());
    }

    public void test_emptyMediawiki() throws Exception {
        final String xml = "<mediawiki></mediawiki>";

        final List<WikiPage> pages = new ArrayList<>();
        final PageCallbackHandler callback = page -> pages.add(page);
        final SAXPageCallbackHandler handler = new SAXPageCallbackHandler(callback);

        parseXML(xml, handler);

        assertEquals(0, pages.size());
    }

    private void parseXML(final String xml, final SAXPageCallbackHandler handler) throws Exception {
        final SAXParserFactory factory = SAXParserFactory.newInstance();
        final SAXParser parser = factory.newSAXParser();
        final InputSource inputSource = new InputSource(new StringReader(xml));
        parser.parse(inputSource, handler);
    }
}
