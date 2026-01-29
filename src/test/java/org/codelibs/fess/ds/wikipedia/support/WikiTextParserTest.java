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

import java.util.ArrayList;

import org.codelibs.fess.ds.wikipedia.UnitDsTestCase;

/**
 * Test class for WikiTextParser.
 *
 * @author CodeLibs
 */
public class WikiTextParserTest extends UnitDsTestCase {

    public void test_isRedirect_withValidRedirect() {
        final String wikiText = "#REDIRECT [[Target Page]]";
        final WikiTextParser parser = new WikiTextParser(wikiText);
        assertTrue(parser.isRedirect());
        assertEquals("Target Page", parser.getRedirectText());
    }

    public void test_isRedirect_withCaseInsensitive() {
        final String wikiText = "#redirect [[Another Page]]";
        final WikiTextParser parser = new WikiTextParser(wikiText);
        assertTrue(parser.isRedirect());
        assertEquals("Another Page", parser.getRedirectText());
    }

    public void test_isRedirect_withMixedCase() {
        final String wikiText = "#ReDiReCt [[Mixed Case Page]]";
        final WikiTextParser parser = new WikiTextParser(wikiText);
        assertTrue(parser.isRedirect());
        assertEquals("Mixed Case Page", parser.getRedirectText());
    }

    public void test_isRedirect_withNoRedirect() {
        final String wikiText = "This is a normal page with no redirect.";
        final WikiTextParser parser = new WikiTextParser(wikiText);
        assertFalse(parser.isRedirect());
        assertNull(parser.getRedirectText());
    }

    public void test_isStub_withStubPattern() {
        final String wikiText = "This is a short article {{geography-stub}}";
        final WikiTextParser parser = new WikiTextParser(wikiText);
        assertTrue(parser.isStub());
    }

    public void test_isStub_withDifferentStubType() {
        final String wikiText = "Short bio {{bio-stub}}";
        final WikiTextParser parser = new WikiTextParser(wikiText);
        assertTrue(parser.isStub());
    }

    public void test_isStub_withNoStub() {
        final String wikiText = "This is a complete article with no stub marker.";
        final WikiTextParser parser = new WikiTextParser(wikiText);
        assertFalse(parser.isStub());
    }

    public void test_isDisambiguationPage_withDisambigTag() {
        final String wikiText = "{{disambig}}";
        final WikiTextParser parser = new WikiTextParser(wikiText);
        assertTrue(parser.isDisambiguationPage());
    }

    public void test_isDisambiguationPage_withDisambiguationTag() {
        final String wikiText = "{{disambiguation}}";
        final WikiTextParser parser = new WikiTextParser(wikiText);
        assertTrue(parser.isDisambiguationPage());
    }

    public void test_isDisambiguationPage_withCapitalD() {
        final String wikiText = "{{Disambig}}";
        final WikiTextParser parser = new WikiTextParser(wikiText);
        assertTrue(parser.isDisambiguationPage());
    }

    public void test_isDisambiguationPage_withCapitalDisambiguation() {
        final String wikiText = "{{Disambiguation}}";
        final WikiTextParser parser = new WikiTextParser(wikiText);
        assertTrue(parser.isDisambiguationPage());
    }

    public void test_isDisambiguationPage_withNoDisambig() {
        final String wikiText = "This is a regular page.";
        final WikiTextParser parser = new WikiTextParser(wikiText);
        assertFalse(parser.isDisambiguationPage());
    }

    public void test_getCategories_withSingleCategory() {
        final String wikiText = "Some text [[Category:Test Category]] more text";
        final WikiTextParser parser = new WikiTextParser(wikiText);
        final ArrayList<String> categories = parser.getCategories();
        assertEquals(1, categories.size());
        assertEquals("Test Category", categories.get(0));
    }

    public void test_getCategories_withMultipleCategories() {
        final String wikiText = "Text [[Category:First]] middle [[Category:Second]] end [[Category:Third]]";
        final WikiTextParser parser = new WikiTextParser(wikiText);
        final ArrayList<String> categories = parser.getCategories();
        assertEquals(3, categories.size());
        assertEquals("First", categories.get(0));
        assertEquals("Second", categories.get(1));
        assertEquals("Third", categories.get(2));
    }

    public void test_getCategories_withCategoryAndSortKey() {
        final String wikiText = "[[Category:People|Smith, John]]";
        final WikiTextParser parser = new WikiTextParser(wikiText);
        final ArrayList<String> categories = parser.getCategories();
        assertEquals(1, categories.size());
        assertEquals("People", categories.get(0));
    }

    public void test_getCategories_withLowercaseCategory() {
        final String wikiText = "[[category:Lowercase Test]]";
        final WikiTextParser parser = new WikiTextParser(wikiText);
        final ArrayList<String> categories = parser.getCategories();
        assertEquals(1, categories.size());
        assertEquals("Lowercase Test", categories.get(0));
    }

    public void test_getCategories_withNoCategories() {
        final String wikiText = "This text has no categories.";
        final WikiTextParser parser = new WikiTextParser(wikiText);
        final ArrayList<String> categories = parser.getCategories();
        assertEquals(0, categories.size());
    }

    public void test_getLinks_withSingleLink() {
        final String wikiText = "This is a link to [[Test Page]]";
        final WikiTextParser parser = new WikiTextParser(wikiText);
        final ArrayList<String> links = parser.getLinks();
        assertEquals(1, links.size());
        assertEquals("Test Page", links.get(0));
    }

    public void test_getLinks_withMultipleLinks() {
        final String wikiText = "Links: [[First Page]] and [[Second Page]] and [[Third Page]]";
        final WikiTextParser parser = new WikiTextParser(wikiText);
        final ArrayList<String> links = parser.getLinks();
        assertEquals(3, links.size());
        assertEquals("First Page", links.get(0));
        assertEquals("Second Page", links.get(1));
        assertEquals("Third Page", links.get(2));
    }

    public void test_getLinks_withPipedLink() {
        final String wikiText = "[[Target Page|Display Text]]";
        final WikiTextParser parser = new WikiTextParser(wikiText);
        final ArrayList<String> links = parser.getLinks();
        assertEquals(1, links.size());
        assertEquals("Target Page", links.get(0));
    }

    public void test_getLinks_excludesNamespacedLinks() {
        final String wikiText = "[[Normal Link]] [[File:Image.jpg]] [[Category:Test]]";
        final WikiTextParser parser = new WikiTextParser(wikiText);
        final ArrayList<String> links = parser.getLinks();
        assertEquals(1, links.size());
        assertEquals("Normal Link", links.get(0));
    }

    public void test_getLinks_withNoLinks() {
        final String wikiText = "Plain text with no links";
        final WikiTextParser parser = new WikiTextParser(wikiText);
        final ArrayList<String> links = parser.getLinks();
        assertEquals(0, links.size());
    }

    public void test_getPlainText_removesHTMLTags() {
        final String wikiText = "Text with <b>bold</b> and <i>italic</i>";
        final WikiTextParser parser = new WikiTextParser(wikiText);
        final String plainText = parser.getPlainText();
        assertFalse(plainText.contains("<b>"));
        assertFalse(plainText.contains("</b>"));
        assertFalse(plainText.contains("<i>"));
        assertFalse(plainText.contains("</i>"));
    }

    public void test_getPlainText_removesRefTags() {
        final String wikiText = "Some text<ref>Reference content</ref> more text";
        final WikiTextParser parser = new WikiTextParser(wikiText);
        final String plainText = parser.getPlainText();
        assertFalse(plainText.contains("<ref>"));
        assertFalse(plainText.contains("Reference content"));
    }

    public void test_getPlainText_removesTemplates() {
        final String wikiText = "Text {{template content}} more text";
        final WikiTextParser parser = new WikiTextParser(wikiText);
        final String plainText = parser.getPlainText();
        assertFalse(plainText.contains("{{"));
        assertFalse(plainText.contains("template content"));
    }

    public void test_getPlainText_removesNamespacedLinks() {
        final String wikiText = "[[File:Image.jpg]] text [[Category:Test]]";
        final WikiTextParser parser = new WikiTextParser(wikiText);
        final String plainText = parser.getPlainText();
        assertFalse(plainText.contains("File:"));
        assertFalse(plainText.contains("Category:"));
    }

    public void test_getPlainText_convertsSimpleLinks() {
        final String wikiText = "Link to [[Test Page]]";
        final WikiTextParser parser = new WikiTextParser(wikiText);
        final String plainText = parser.getPlainText();
        assertTrue(plainText.contains("Test Page"));
        assertFalse(plainText.contains("[["));
        assertFalse(plainText.contains("]]"));
    }

    public void test_getPlainText_removesExternalLinks() {
        final String wikiText = "External [http://example.com link]";
        final WikiTextParser parser = new WikiTextParser(wikiText);
        final String plainText = parser.getPlainText();
        assertFalse(plainText.contains("["));
        assertFalse(plainText.contains("http://"));
    }

    public void test_getPlainText_removesApostrophes() {
        final String wikiText = "Text with '''bold''' and ''italic''";
        final WikiTextParser parser = new WikiTextParser(wikiText);
        final String plainText = parser.getPlainText();
        assertFalse(plainText.contains("'''"));
        assertFalse(plainText.contains("''"));
    }

    public void test_getPlainText_decodesHTMLEntities() {
        final String wikiText = "Text with &lt;tag&gt; entities";
        final WikiTextParser parser = new WikiTextParser(wikiText);
        final String plainText = parser.getPlainText();
        // HTML entities are decoded to < and >, but then HTML tags are removed
        assertFalse(plainText.contains("&lt;"));
        assertFalse(plainText.contains("&gt;"));
        // The decoded <tag> is removed as it's treated as an HTML tag
        assertFalse(plainText.contains("<tag>"));
        assertTrue(plainText.contains("Text with"));
        assertTrue(plainText.contains("entities"));
    }

    public void test_getText_returnsOriginalText() {
        final String wikiText = "Original '''wiki''' text";
        final WikiTextParser parser = new WikiTextParser(wikiText);
        assertEquals(wikiText, parser.getText());
    }

    public void test_getInfoBox_withValidInfoBox() {
        final String wikiText = "Text before {{Infobox person\n|name=John Doe\n|born=1990\n}} text after";
        final WikiTextParser parser = new WikiTextParser(wikiText);
        final InfoBox infoBox = parser.getInfoBox();
        assertNotNull(infoBox);
        final String raw = infoBox.dumpRaw();
        assertTrue(raw.contains("Infobox"));
        assertTrue(raw.contains("John Doe"));
    }

    public void test_getInfoBox_withNestedBraces() {
        final String wikiText = "{{Infobox test\n|field={{nested content}}\n}}";
        final WikiTextParser parser = new WikiTextParser(wikiText);
        final InfoBox infoBox = parser.getInfoBox();
        assertNotNull(infoBox);
    }

    public void test_getInfoBox_withNoInfoBox() {
        final String wikiText = "Regular text with no infobox";
        final WikiTextParser parser = new WikiTextParser(wikiText);
        final InfoBox infoBox = parser.getInfoBox();
        assertNull(infoBox);
    }

    public void test_getInfoBox_caching() {
        final String wikiText = "{{Infobox test\n|data=value\n}}";
        final WikiTextParser parser = new WikiTextParser(wikiText);
        final InfoBox infoBox1 = parser.getInfoBox();
        final InfoBox infoBox2 = parser.getInfoBox();
        assertSame(infoBox1, infoBox2);
    }

    public void test_getTranslatedTitle_withValidLanguageCode() {
        final String wikiText = "Text\n[[en:English Title]]\nMore text";
        final WikiTextParser parser = new WikiTextParser(wikiText);
        final String translatedTitle = parser.getTranslatedTitle("en");
        assertEquals("English Title", translatedTitle);
    }

    public void test_getTranslatedTitle_withMultipleLanguages() {
        final String wikiText = "[[en:English]]\n[[fr:French]]\n[[de:German]]";
        final WikiTextParser parser = new WikiTextParser(wikiText);
        assertEquals("English", parser.getTranslatedTitle("en"));
        assertEquals("French", parser.getTranslatedTitle("fr"));
        assertEquals("German", parser.getTranslatedTitle("de"));
    }

    public void test_getTranslatedTitle_withNoMatch() {
        final String wikiText = "[[en:English Title]]";
        final WikiTextParser parser = new WikiTextParser(wikiText);
        final String translatedTitle = parser.getTranslatedTitle("ja");
        assertNull(translatedTitle);
    }

    public void test_getCategories_caching() {
        final String wikiText = "[[Category:Test]]";
        final WikiTextParser parser = new WikiTextParser(wikiText);
        final ArrayList<String> categories1 = parser.getCategories();
        final ArrayList<String> categories2 = parser.getCategories();
        assertSame(categories1, categories2);
    }

    public void test_getLinks_caching() {
        final String wikiText = "[[Test Link]]";
        final WikiTextParser parser = new WikiTextParser(wikiText);
        final ArrayList<String> links1 = parser.getLinks();
        final ArrayList<String> links2 = parser.getLinks();
        assertSame(links1, links2);
    }

    public void test_complexWikiText_withMixedElements() {
        final String wikiText = "#REDIRECT [[Target]]\n" + "{{disambig}}\n" + "{{geography-stub}}\n" + "[[Category:Test]]\n"
                + "[[Link Page]]\n" + "Regular text";
        final WikiTextParser parser = new WikiTextParser(wikiText);
        assertTrue(parser.isRedirect());
        assertTrue(parser.isDisambiguationPage());
        assertTrue(parser.isStub());
        assertEquals(1, parser.getCategories().size());
        // Both [[Target]] from redirect and [[Link Page]] are counted as links
        assertEquals(2, parser.getLinks().size());
    }

    public void test_emptyWikiText() {
        final String wikiText = "";
        final WikiTextParser parser = new WikiTextParser(wikiText);
        assertFalse(parser.isRedirect());
        assertFalse(parser.isStub());
        assertFalse(parser.isDisambiguationPage());
        assertEquals(0, parser.getCategories().size());
        assertEquals(0, parser.getLinks().size());
        assertEquals("", parser.getPlainText());
        assertNull(parser.getInfoBox());
    }
}
