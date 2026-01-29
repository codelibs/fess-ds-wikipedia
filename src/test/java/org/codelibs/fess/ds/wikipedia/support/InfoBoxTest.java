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

import org.codelibs.fess.ds.wikipedia.UnitDsTestCase;

/**
 * Test class for InfoBox.
 *
 * @author CodeLibs
 */
public class InfoBoxTest extends UnitDsTestCase {

    public void test_dumpRaw_returnsOriginalText() {
        final String infoBoxText = "{{Infobox person\n|name=John Doe\n|born=1990\n}}";
        final InfoBox infoBox = new InfoBox(infoBoxText);
        assertEquals(infoBoxText, infoBox.dumpRaw());
    }

    public void test_dumpRaw_withSimpleInfoBox() {
        final String infoBoxText = "{{Infobox\n|field=value\n}}";
        final InfoBox infoBox = new InfoBox(infoBoxText);
        assertEquals(infoBoxText, infoBox.dumpRaw());
    }

    public void test_dumpRaw_withComplexInfoBox() {
        final String infoBoxText =
                "{{Infobox country\n" + "|name=Test Country\n" + "|capital=Test City\n" + "|population=1000000\n" + "|area=50000\n" + "}}";
        final InfoBox infoBox = new InfoBox(infoBoxText);
        assertEquals(infoBoxText, infoBox.dumpRaw());
    }

    public void test_dumpRaw_withEmptyString() {
        final String infoBoxText = "";
        final InfoBox infoBox = new InfoBox(infoBoxText);
        assertEquals("", infoBox.dumpRaw());
    }

    public void test_dumpRaw_withNestedBraces() {
        final String infoBoxText = "{{Infobox test\n|field={{nested content}}\n}}";
        final InfoBox infoBox = new InfoBox(infoBoxText);
        assertEquals(infoBoxText, infoBox.dumpRaw());
    }

    public void test_dumpRaw_withSpecialCharacters() {
        final String infoBoxText = "{{Infobox\n|field=value with & special < > characters\n}}";
        final InfoBox infoBox = new InfoBox(infoBoxText);
        assertEquals(infoBoxText, infoBox.dumpRaw());
    }

    public void test_dumpRaw_withUnicodeCharacters() {
        final String infoBoxText = "{{Infobox\n|name=日本語\n|description=テスト\n}}";
        final InfoBox infoBox = new InfoBox(infoBoxText);
        assertEquals(infoBoxText, infoBox.dumpRaw());
    }

    public void test_dumpRaw_withMultilineValues() {
        final String infoBoxText = "{{Infobox\n" + "|description=This is a\n" + "multiline value\n" + "with several lines\n" + "}}";
        final InfoBox infoBox = new InfoBox(infoBoxText);
        assertEquals(infoBoxText, infoBox.dumpRaw());
    }

    public void test_dumpRaw_consistency() {
        final String infoBoxText = "{{Infobox test}}";
        final InfoBox infoBox = new InfoBox(infoBoxText);
        assertEquals(infoBox.dumpRaw(), infoBox.dumpRaw());
    }

    public void test_constructor_withNullText() {
        final InfoBox infoBox = new InfoBox(null);
        assertNull(infoBox.dumpRaw());
    }
}
