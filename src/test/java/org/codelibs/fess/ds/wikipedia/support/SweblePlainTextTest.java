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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * Evaluates the Sweble-based {@link WikiTextParser#getPlainText()} against the
 * previous regex implementation, which is kept as the fallback.
 */
public class SweblePlainTextTest {

    private static String sweble(final String wikiText) {
        return new WikiTextParser(wikiText).getPlainText();
    }

    private static String regex(final String wikiText) {
        return new WikiTextParser(wikiText).getPlainTextByRegex();
    }

    // ===== Expectations carried over from WikiTextParserTest =====

    @Test
    public void removesHtmlTags() {
        final String t = sweble("Text with <b>bold</b> and <i>italic</i>");
        assertEquals("Text with bold and italic", t);
    }

    @Test
    public void removesRefTags() {
        assertEquals("Some text more text", sweble("Some text<ref>Reference content</ref> more text"));
        assertEquals("A B C", sweble("A<ref name=\"x\" group=\"n\">Ref</ref> B<ref name=\"x\" /> C"));
    }

    @Test
    public void removesTemplates() {
        assertEquals("Text more text", sweble("Text {{template content}} more text"));
    }

    @Test
    public void removesNamespacedLinks() {
        final String t = sweble("[[File:Image.jpg]] text [[Category:Test]]");
        assertEquals("text", t);
    }

    @Test
    public void convertsLinks() {
        assertEquals("Link to Test Page", sweble("Link to [[Test Page]]"));
        assertEquals("See the target for details", sweble("See [[Target Page|the target]] for details"));
        assertEquals("Many dogs", sweble("Many [[dog]]s"));
    }

    @Test
    public void externalLinks() {
        final String t = sweble("External [http://example.com link]");
        assertFalse(t.contains("["));
        assertFalse(t.contains("http://"));
        assertEquals("External link", t);
    }

    @Test
    public void removesBoldItalicMarkup() {
        assertEquals("Text with bold and italic", sweble("Text with '''bold''' and ''italic''"));
    }

    @Test
    public void sectionHeaders() {
        final String t = sweble("Intro text\n== Hosting ==\nMore text\n=== Details ===\nEven more");
        assertEquals("Intro text\nHosting\nMore text\nDetails\nEven more", t);
    }

    @Test
    public void listMarkers() {
        final String t = sweble("== Hosting ==\n* VM at provider\n** Apache\n# Step one\n: Indented note");
        assertEquals("Hosting\nVM at provider\nApache\nStep one\nIndented note", t);
    }

    @Test
    public void emptyText() {
        assertEquals("", sweble(""));
    }

    // ===== Cases the regex chain gets wrong =====

    @Test
    public void keepsApostrophesInWords() {
        // regex strips every "'" -> "dont", "Einsteins"
        assertEquals("don't stop Einstein's", sweble("don't stop Einstein's"));
        assertEquals("dont stop Einsteins", regex("don't stop Einstein's"));
    }

    @Test
    public void droppedTemplateKeepsWordBoundary() {
        assertEquals("(14 March 1879 18 April 1955)", sweble("(14 March 1879{{snd}}18 April 1955)"));
    }

    @Test
    public void templateLeavesNoSpaceBeforeClosingPunctuation() {
        final String w = "The {{langx|de|Haus}}; and the formula{{math|x}}, which";
        assertEquals("The de Haus; and the formula x, which", sweble(w));
        // the space before "{{langx" is real whitespace in the source, so it stays
        assertEquals("The ; and the formula, which", sweble(w, "*", null, false));
    }

    @Test
    public void nestedTemplates() {
        final String w = "A {{outer|x={{inner|y}}|z}} B";
        // template text is kept by default, nested templates included
        assertEquals("A y z B", sweble(w));
        assertEquals("A B", sweble(w, "*", null, false));
        assertTrue(regex(w).contains("}}"), regex(w));
    }

    @Test
    public void multiLineInfobox() {
        final String w = "{{Infobox person\n| name = X\n| birth = {{birth date|1900|1|1}}\n}}\n'''X''' was a person.";
        // infobox values are kept by default, parameter names are not
        assertEquals("X 1900 1 1\nX was a person.", sweble(w));
        assertEquals("X was a person.", sweble(w, "Infobox person", null, false));
        assertTrue(regex(w).contains("name = X"), regex(w));
    }

    @Test
    public void multiLineCommentsAndRefs() {
        final String w = "A <!-- hidden\ncomment --> B <ref>line1\nline2</ref> C";
        assertEquals("A B C", sweble(w));
        assertTrue(regex(w).contains("hidden"), regex(w));
    }

    @Test
    public void imageWithNestedLinkInCaption() {
        final String w = "Before [[File:X.jpg|thumb|A caption with [[link]]]] after";
        // a thumbnail breaks the paragraph, hence the newlines; its caption is kept by default
        assertEquals("Before\nA caption with link\nafter", sweble(w));
        assertEquals("Before\nafter", sweble(w, null, null, true));
        assertTrue(regex(w).contains("]"), regex(w));
    }

    @Test
    public void tables() {
        final String w = "{| class=\"wikitable\"\n|-\n! Name !! Age\n|-\n| Alice || 30\n|}";
        final String t = sweble(w);
        assertFalse(t.contains("{|"), t);
        assertFalse(t.contains("wikitable"), t);
        assertEquals("Name Age\nAlice 30", t);
    }

    @Test
    public void entitiesAndMagicWords() {
        assertEquals("a b c – d", sweble("__NOTOC__ a b&nbsp;c &ndash; d"));
    }

    // ===== Options: drop_templates / keep_templates / drop_captions =====

    private static String sweble(final String wikiText, final String dropTemplates, final String keepTemplates,
            final boolean dropCaptions) {
        return new WikiTextParser(wikiText).getPlainText(PlainTextOptions.of(dropTemplates, keepTemplates, dropCaptions));
    }

    @Test
    public void templateArgumentTextIsKeptByDefault() {
        final String w = "Before {{Note|Restart [[Apache|the web server]] after '''changes'''.|title=Careful}} after";
        // argument values are parsed as wikitext; the parameter name "title=" is not kept
        assertEquals("Before Restart the web server after changes. Careful after", sweble(w));
        // nested templates are kept as well
        assertEquals("A Keep this B", sweble("A {{Note|Keep {{nowrap|this}}}} B"));
    }

    @Test
    public void dropTemplatesFiltersTheListedOnes() {
        final String w = "A {{Note|Keep {{Cite web|title=Cited title|url=https://example.com}}}} B";
        // a bare URL in an argument is dropped like any bare URL
        assertEquals("A Keep Cited title B", sweble(w));
        // dropped also when nested in a kept template
        assertEquals("A Keep B", sweble(w, "Cite web", null, false));
    }

    @Test
    public void dropAllWithKeepTemplatesIsAnAllowlist() {
        final String w = "A {{Note|Keep {{nowrap|this}}}}{{Infobox|name=X}} B";
        assertEquals("A Keep B", sweble(w, "*", "Note", false));
        assertEquals("A Keep this B", sweble(w, "*", "Note, Nowrap", false));
        assertEquals("A B", sweble(w, "*", null, false));
    }

    @Test
    public void captionsAreKeptUnlessDropped() {
        final String w = "Intro\n[[File:X.jpg|thumb|upright=1.2|alt=Alt text|A caption with [[Link|a link]]]]\nOutro";
        // options and alt text are not prose; only the caption is kept
        assertEquals("Intro\nA caption with a link\nOutro", sweble(w));
        assertEquals("Intro\nOutro", sweble(w, null, null, true));
    }

    @Test
    public void galleryCaptionsAreKeptUnlessDropped() {
        final String w =
                "A\n<gallery>\nFile:Berlin.jpg|The [[Reichstag]] at night\nBonn.jpg|alt=x|Bonn from '''above'''\nNoCaption.jpg\n</gallery>\nB";
        assertEquals("A\nThe Reichstag at night\nBonn from above\nB", sweble(w));
        assertEquals("A B", sweble(w, null, null, true));
    }

    // ===== Tag extensions =====

    @Test
    public void galleryIsDroppedWithItsCaptions() {
        // file names never reach the text; with drop_captions nothing of the gallery does
        final String w = "A\n<gallery>\nFile:Berlin.jpg|The [[Reichstag]] at night\nFile:Bonn.jpg\n</gallery>\nB";
        assertEquals("A\nThe Reichstag at night\nB", sweble(w));
        assertEquals("A B", sweble(w, null, null, true));
    }

    @Test
    public void poemBodyIsParsedLineByLine() {
        assertEquals("Roses are red,\nViolets are blue", sweble("<poem>\nRoses are '''red''',\n[[Violet]]s are blue\n</poem>"));
    }

    @Test
    public void codeBodiesAreKeptLiterally() {
        assertEquals("Code:\nsudo systemctl restart apache2 ''x''\ndone",
                sweble("Code: <syntaxhighlight lang=\"bash\">sudo systemctl restart apache2 ''x''</syntaxhighlight> done"));
        assertEquals("raw [[not a link]]", sweble("<pre>raw [[not a link]]</pre>"));
    }

    @Test
    public void extensionsWithMarkupOrDataAreDropped() {
        assertEquals("A B", sweble("A <imagemap>File:X.png|thumb\nrect 0 0 1 1 [[Page]]</imagemap> B"));
        assertEquals("A B", sweble("A <inputbox>type=search</inputbox> B"));
        assertEquals("A B", sweble("A <templatestyles src=\"Box/styles.css\" /> B"));
        assertEquals("A B", sweble("A <math>E = mc^2</math> B"));
    }

    @Test
    public void unknownTagsAreParsedAsWikitext() {
        // not registered as tag extensions, so Sweble parses their content like any other wikitext
        assertEquals("Some translatable text", sweble("<translate>Some '''translatable''' [[Link|text]]</translate>"));
    }

    // ===== Real articles =====
    // Regression checks on real, messy wikitext (CC BY-SA fixtures, see src/test/resources/wikitext/README.md):
    // no markup may survive, and prose from every part of the article must.

    private static final Pattern LEFTOVER_MARKUP =
            Pattern.compile("\\{\\{|\\}\\}|\\[\\[|\\]\\]|''|^=+|=+$|^\\s*[*#]|\\{\\||\\|\\}|<ref|</", Pattern.MULTILINE);

    private static String article(final String name) throws IOException {
        return article(name, PlainTextOptions.DEFAULT);
    }

    private static String article(final String name, final PlainTextOptions options) throws IOException {
        final String wikiText = Files.readString(Path.of("src/test/resources/wikitext/" + name + ".txt"), StandardCharsets.UTF_8);
        final String text = new WikiTextParser(wikiText).getPlainText(options);
        final List<String> leftovers = LEFTOVER_MARKUP.matcher(text).results().map(m -> m.group()).toList();
        assertTrue(leftovers.isEmpty(), name + ": leftover markup " + leftovers);
        return text;
    }

    private static void assertContains(final String text, final String expected) {
        assertTrue(text.contains(expected), () -> "missing: " + expected.replace("\n", "\\n"));
    }

    private static void assertStartsWith(final String text, final String expected) {
        assertTrue(text.startsWith(expected), () -> "expected to start with: " + expected + "\nbut starts with: "
                + text.substring(0, Math.min(text.length(), expected.length() + 40)));
    }

    @Test
    public void realArticle_apacheLucene() throws IOException {
        final String t = article("Apache_Lucene");
        // template text is kept by default: the short description and infobox values precede the lead
        assertStartsWith(t, "Java library for full-text search\n");
        // bold title and links in the lead
        final String lead =
                "Apache Lucene is a free and open-source search engine software library, originally written in Java by Doug Cutting.";
        assertContains(t, "\n" + lead);
        assertStartsWith(article("Apache_Lucene", PlainTextOptions.of("*", null, false)), lead);
        // section heading on its own line
        assertContains(t, "\nHistory\n");
        // list item near the end, list marker stripped
        assertContains(t, "\nOpenSearch – an open source enterprise search server based on a fork of Elasticsearch 7");
    }

    @Test
    public void realArticle_germany() throws IOException {
        final String t = article("Germany");
        assertStartsWith(t, "Country in Europe\n");
        // footnote templates ({{efn|...}}) keep their text where the footnote marker is, inside the lead
        assertContains(t, "\nGermany, de Deutschland; de ");
        assertContains(t, "officially the Federal Republic of Germany,");
        assertContains(t, "is a country in Western and Central Europe.");
        // lead after a large infobox and hatnote templates, all dropped
        final String lead = "Germany, officially the Federal Republic of Germany, is a country in Western and Central Europe.";
        assertStartsWith(article("Germany", PlainTextOptions.of("*", null, false)), lead);
        // apostrophe inside a word
        assertContains(t, "The nation's capital and most populous city is Berlin");
        // prose from the Culture section, late in the article
        assertContains(t, "The Berlin Fashion Week and the fashion trade fair Bread & Butter are held twice a year.");
    }

    @Test
    public void realArticle_albertEinstein() throws IOException {
        final String t = article("Albert_Einstein");
        // lead after nested {{efn|{{IPAc-en|...}}}} templates
        assertContains(t, "was a German-born theoretical physicist best known for developing the theory of relativity.");
        assertContains(t, "has been called \"the world's most famous equation\".");
        // non-ASCII text from linked names
        assertContains(t, "Born as a subject to the Kingdom of Württemberg, part of the German Empire");
        // prose late in the article
        assertContains(t, "In addition to longtime collaborators Leopold Infeld, Nathan Rosen, Peter Bergmann and others");
    }
}
