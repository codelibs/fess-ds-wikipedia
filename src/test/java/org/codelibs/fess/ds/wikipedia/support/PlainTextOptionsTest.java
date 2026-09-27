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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class PlainTextOptionsTest {

    @Test
    public void defaultKeepsEverything() {
        assertTrue(PlainTextOptions.DEFAULT.keepsTemplate("Note"));
        assertTrue(PlainTextOptions.DEFAULT.keepsTemplate("Cite web"));
        assertTrue(PlainTextOptions.DEFAULT.keepsCaptions());
        assertTrue(PlainTextOptions.of(null, null, false).keepsTemplate("Note"));
        assertTrue(PlainTextOptions.of(" , ", "", false).keepsTemplate("Note"));
    }

    @Test
    public void dropTemplatesIsABlocklist() {
        final PlainTextOptions options = PlainTextOptions.of("Cite web, cite_news", null, true);
        assertFalse(options.keepsTemplate("Cite web"));
        assertFalse(options.keepsTemplate("Cite news"));
        assertTrue(options.keepsTemplate("Note"));
        assertFalse(options.keepsCaptions());
    }

    @Test
    public void dropAllWithKeepTemplatesIsAnAllowlist() {
        final PlainTextOptions options = PlainTextOptions.of(" * ", "Note,Warning", false);
        assertTrue(options.keepsTemplate("Note"));
        assertTrue(options.keepsTemplate("warning"));
        assertFalse(options.keepsTemplate("Infobox"));
        assertTrue(options.keepsCaptions());
    }

    @Test
    public void keepTemplatesWinsOverAnExplicitDrop() {
        assertTrue(PlainTextOptions.of("Note", "Note", false).keepsTemplate("Note"));
    }

    @Test
    public void templateNamesMatchLikeMediaWiki() {
        final PlainTextOptions options = PlainTextOptions.of(" note , Template:How_to  step,", null, false);
        // first letter case-insensitive, "_" is a space, the Template: prefix is optional
        assertFalse(options.keepsTemplate("Note"));
        assertFalse(options.keepsTemplate("note"));
        assertFalse(options.keepsTemplate("How to step"));
        assertFalse(options.keepsTemplate("how_to_step"));
        assertFalse(options.keepsTemplate("Template:How to  step"));
        // the rest of the name stays case-sensitive, as in MediaWiki
        assertTrue(options.keepsTemplate("NOTE"));
    }

    @Test
    public void normalizeTemplateName() {
        assertEquals("Ünicode name", PlainTextOptions.normalizeTemplateName("  template:ünicode__name "));
        assertEquals("", PlainTextOptions.normalizeTemplateName("Template:"));
    }

    @Test
    public void extractorIsSwebleUnlessRegexIsNamed() {
        assertEquals(PlainTextOptions.Extractor.SWEBLE, PlainTextOptions.DEFAULT.getExtractor());
        assertEquals(PlainTextOptions.Extractor.SWEBLE, PlainTextOptions.Extractor.of(null));
        assertEquals(PlainTextOptions.Extractor.SWEBLE, PlainTextOptions.Extractor.of(" "));
        assertEquals(PlainTextOptions.Extractor.SWEBLE, PlainTextOptions.Extractor.of("Sweble"));
        assertEquals(PlainTextOptions.Extractor.REGEX, PlainTextOptions.Extractor.of(" regex "));
        assertThrows(IllegalArgumentException.class, () -> PlainTextOptions.Extractor.of("html"));
    }

    @Test
    public void withMethodsKeepTheOtherOptions() {
        final PlainTextOptions options =
                PlainTextOptions.of("Note", null, true).withExtractor(PlainTextOptions.Extractor.REGEX).withSiteInfo(new SiteInfo());
        assertEquals(PlainTextOptions.Extractor.REGEX, options.getExtractor());
        assertFalse(options.keepsTemplate("Note"));
        assertFalse(options.keepsCaptions());
    }
}
