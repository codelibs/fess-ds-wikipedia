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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.codelibs.fess.ds.wikipedia.UnitDsTestCase;
import org.junit.jupiter.api.Test;

public class CirrusIndexDumpSourceTest extends UnitDsTestCase {

    private List<WikiDocument> read(final String fixture) {
        final java.net.URL url = getClass().getResource("/fixtures/" + fixture);
        Objects.requireNonNull(url, "fixture not found: " + fixture);
        final CirrusIndexDumpSource source = new CirrusIndexDumpSource(List.of(url.toString()), new DumpFetcher("TestAgent/1.0"));
        final List<WikiDocument> documents = new ArrayList<>();
        source.forEach(documents::add);
        return documents;
    }

    @Test
    public void test_forEach_skipsActionLines() {
        // Four lines, two of which are bulk action lines.
        assertEquals(2, read("cirrus-sample.json").size());
    }

    @Test
    public void test_forEach_readsBzip2TheSameWay() {
        assertEquals(2, read("cirrus-sample.json.bz2").size());
    }

    @Test
    public void test_forEach_mapsTheCoreFields() {
        final WikiDocument document = read("cirrus-sample.json").get(0);
        assertEquals("4348353", document.getId());
        assertEquals("ReSTARTING", document.getTitle());
        assertEquals(0, document.getNamespace());
        assertEquals("A song released in 2021.", document.getContent());
        assertEquals("A song released in 2021.", document.getOpeningText());
        assertEquals("'''A song''' released in 2021.", document.getWikitext());
        assertEquals("109913677", document.getRevisionId());
        assertEquals("ja", document.getLanguage());
        assertEquals("wikitext", document.getModel());
    }

    @Test
    public void test_forEach_mapsTheLists() {
        final WikiDocument document = read("cirrus-sample.json").get(0);
        assertEquals(List.of("Songs", "2021 singles"), document.getCategories());
        assertEquals(List.of("Overview", "References"), document.getHeadings());
        assertEquals(List.of("Template:Infobox_Single", "2021"), document.getLinks());
        assertEquals(List.of("https://example.com/item/1"), document.getExternalLinks());
        assertEquals(List.of("Template:Infobox Single"), document.getTemplates());
        assertEquals(List.of("Infobox rendered text"), document.getAuxiliaryText());
        assertEquals(List.of("classification.prediction.articletopic/Culture.Media.Music|1000"), document.getWeightedTags());
    }

    @Test
    public void test_forEach_flattensRedirectObjectsToTitles() {
        // The cirrus redirect field lists the pages that point AT this one,
        // which is the opposite of the XML dump's redirect flag.
        final WikiDocument document = read("cirrus-sample.json").get(0);
        assertEquals(List.of("ReSTARTING!!"), document.getRedirects());
        assertFalse(document.isRedirect());
    }

    @Test
    public void test_forEach_mapsTheRankingSignals() {
        final WikiDocument document = read("cirrus-sample.json").get(0);
        assertEquals("Q109362192", document.getWikibaseItem());
        assertEquals(Integer.valueOf(20), document.getIncomingLinks());
        assertEquals(Double.valueOf(7.024558386473268e-08), document.getPopularityScore());
    }

    @Test
    public void test_forEach_parsesTheTimestamp() {
        final WikiDocument document = read("cirrus-sample.json").get(0);
        assertEquals(java.util.Date.from(java.time.Instant.parse("2026-06-13T02:15:01Z")), document.getTimestamp());
    }

    @Test
    public void test_forEach_toleratesAMinimalRecord() {
        final WikiDocument document = read("cirrus-sample.json").get(1);
        assertEquals("99", document.getId());
        assertEquals("Minimal", document.getTitle());
        assertEquals("Just text.", document.getContent());
        assertNull(document.getTimestamp());
        assertEquals(List.of(), document.getCategories());
        assertEquals(List.of(), document.getRedirects());
        assertNull(document.getPopularityScore());
        assertNull(document.getIncomingLinks());
    }

    @Test
    public void test_forEach_readsEveryLocationInOrder() {
        final java.net.URL url = getClass().getResource("/fixtures/cirrus-sample.json");
        Objects.requireNonNull(url, "fixture not found");
        final CirrusIndexDumpSource source =
                new CirrusIndexDumpSource(List.of(url.toString(), url.toString()), new DumpFetcher("TestAgent/1.0"));
        final List<WikiDocument> documents = new ArrayList<>();
        source.forEach(documents::add);
        assertEquals(4, documents.size());
    }
}
