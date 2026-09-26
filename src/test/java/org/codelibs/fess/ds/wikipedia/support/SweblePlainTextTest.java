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
    public void nestedTemplates() {
        final String w = "A {{outer|x={{inner|y}}|z}} B";
        assertEquals("A B", sweble(w));
        assertTrue(regex(w).contains("}}"), regex(w));
    }

    @Test
    public void multiLineInfobox() {
        final String w = "{{Infobox person\n| name = X\n| birth = {{birth date|1900|1|1}}\n}}\n'''X''' was a person.";
        assertEquals("X was a person.", sweble(w));
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
        // a thumbnail breaks the paragraph, hence the newline
        assertEquals("Before\nafter", sweble(w));
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

    // ===== Real articles =====

    private static final Pattern LEFTOVER_MARKUP =
            Pattern.compile("\\{\\{|\\}\\}|\\[\\[|\\]\\]|'''|''|^=+|=+$|^\\s*[*#]|\\{\\||\\|\\}|<ref|</", Pattern.MULTILINE);

    @Test
    public void realArticles() throws IOException {
        final StringBuilder report = new StringBuilder();
        for (final String name : List.of("Apache_Lucene", "Germany", "Albert_Einstein")) {
            final String w = Files.readString(Path.of("src/test/resources/wikitext/" + name + ".txt"), StandardCharsets.UTF_8);

            // warm-up so timings exclude class loading
            sweble(w);
            regex(w);
            final int runs = 5;
            long t0 = System.nanoTime();
            String s = null;
            for (int i = 0; i < runs; i++) {
                s = sweble(w);
            }
            final long swebleMs = (System.nanoTime() - t0) / runs / 1_000_000;
            t0 = System.nanoTime();
            String r = null;
            for (int i = 0; i < runs; i++) {
                r = regex(w);
            }
            final long regexMs = (System.nanoTime() - t0) / runs / 1_000_000;

            final long swebleLeft = LEFTOVER_MARKUP.matcher(s).results().count();
            final long regexLeft = LEFTOVER_MARKUP.matcher(r).results().count();
            report.append(String.format(
                    "%-16s wikitext=%7d chars | sweble: %7d chars, %5d markup hits, %4d ms | regex: %7d chars, %5d markup hits, %4d ms%n",
                    name, w.length(), s.length(), swebleLeft, swebleMs, r.length(), regexLeft, regexMs));

            final Path out = Path.of("target/plaintext");
            Files.createDirectories(out);
            Files.writeString(out.resolve(name + ".sweble.txt"), s, StandardCharsets.UTF_8);
            Files.writeString(out.resolve(name + ".regex.txt"), r, StandardCharsets.UTF_8);

            assertTrue(swebleLeft < regexLeft, name + ": sweble leaves " + swebleLeft + " vs regex " + regexLeft);
        }
        System.out.println("\n=== PLAINTEXT REPORT ===\n" + report);
    }
}
