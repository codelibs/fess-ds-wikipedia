/*
 * Copyright 2012-2024 CodeLibs Project and the Others.
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * For internal use only -- Used by the {@link WikiPage} class.
 * Can also be used as a stand alone class to parse wiki formatted text.
 *
 * @author Delip Rao
 * @see <a href="https://github.com/elastic/elasticsearch-river-wikipedia">Wikipedia River Plugin for Elasticsearch</a>
 */
public class WikiTextParser {

    private String wikiText = null;
    private ArrayList<String> pageCats = null;
    private ArrayList<String> pageLinks = null;
    private boolean redirect = false;
    private String redirectString = null;
    private static Pattern redirectPattern = Pattern.compile("#REDIRECT\\s+\\[\\[(.*?)\\]\\]", Pattern.CASE_INSENSITIVE);
    private boolean stub = false;
    private boolean disambiguation = false;
    private static Pattern stubPattern = Pattern.compile("\\-stub\\}\\}");
    // the first letter of pages is case-insensitive
    private static Pattern disambCatPattern = Pattern.compile("\\{\\{[Dd]isambig(uation)?\\}\\}");
    private InfoBox infoBox = null;

    public WikiTextParser(final String wtext) {
        wikiText = wtext;
        Matcher matcher = redirectPattern.matcher(wikiText);
        if (matcher.find()) {
            redirect = true;
            if (matcher.groupCount() == 1) {
                redirectString = matcher.group(1);
            }
        }
        matcher = stubPattern.matcher(wikiText);
        stub = matcher.find();
        matcher = disambCatPattern.matcher(wikiText);
        disambiguation = matcher.find();
    }

    public boolean isRedirect() {
        return redirect;
    }

    public boolean isStub() {
        return stub;
    }

    public String getRedirectText() {
        return redirectString;
    }

    public String getText() {
        return wikiText;
    }

    public ArrayList<String> getCategories() {
        if (pageCats == null) {
            parseCategories();
        }
        return pageCats;
    }

    public ArrayList<String> getLinks() {
        if (pageLinks == null) {
            parseLinks();
        }
        return pageLinks;
    }

    private void parseCategories() {
        pageCats = new ArrayList<>();
        final Pattern catPattern = Pattern.compile("\\[\\[[Cc]ategory:(.*?)\\]\\]", Pattern.MULTILINE);
        final Matcher matcher = catPattern.matcher(wikiText);
        while (matcher.find()) {
            final String[] temp = matcher.group(1).split("\\|");
            pageCats.add(temp[0]);
        }
    }

    private void parseLinks() {
        pageLinks = new ArrayList<>();

        final Pattern catPattern = Pattern.compile("\\[\\[(.*?)\\]\\]", Pattern.MULTILINE);
        final Matcher matcher = catPattern.matcher(wikiText);
        while (matcher.find()) {
            final String[] temp = matcher.group(1).split("\\|");
            if (temp == null || temp.length == 0) {
                continue;
            }
            final String link = temp[0];
            if (!link.contains(":")) {
                pageLinks.add(link);
            }
        }
    }

    public String getPlainText() {
        String text = wikiText.replace("&gt;", ">");
        text = text.replace("&lt;", "<");
        text = text.replaceAll("<ref>.*?</ref>", " ");
        text = text.replaceAll("</?.*?>", " ");
        text = text.replaceAll("\\{\\{.*?\\}\\}", " ");
        text = text.replaceAll("\\[\\[.*?:.*?\\]\\]", " ");
        text = text.replaceAll("\\[\\[(.*?)\\]\\]", "$1");
        text = text.replaceAll("\\s(.*?)\\|(\\w+\\s)", " $2");
        text = text.replaceAll("\\[.*?\\]", " ");
        return text.replaceAll("\\'+", "");
    }

    public InfoBox getInfoBox() {
        //parseInfoBox is expensive. Doing it only once like other parse* methods
        if (infoBox == null) {
            infoBox = parseInfoBox();
        }
        return infoBox;
    }

    private InfoBox parseInfoBox() {
        final String INFOBOX_CONST_STR = "{{Infobox";
        final int startPos = wikiText.indexOf(INFOBOX_CONST_STR);
        if (startPos < 0) {
            return null;
        }
        int bracketCount = 2;
        int endPos = startPos + INFOBOX_CONST_STR.length();
        for (; endPos < wikiText.length(); endPos++) {
            switch (wikiText.charAt(endPos)) {
            case '}':
                bracketCount--;
                break;
            case '{':
                bracketCount++;
                break;
            default:
            }
            if (bracketCount == 0) {
                break;
            }
        }
        String infoBoxText = wikiText.substring(startPos, endPos + 1);
        infoBoxText = stripCite(infoBoxText); // strip clumsy {{cite}} tags
        // strip any html formatting
        infoBoxText = infoBoxText.replace("&gt;", ">");
        infoBoxText = infoBoxText.replace("&lt;", "<");
        infoBoxText = infoBoxText.replaceAll("<ref.*?>.*?</ref>", " ");
        infoBoxText = infoBoxText.replaceAll("</?.*?>", " ");
        return new InfoBox(infoBoxText);
    }

    private String stripCite(String text) {
        final String CITE_CONST_STR = "{{cite";
        final int startPos = text.indexOf(CITE_CONST_STR);
        if (startPos < 0) {
            return text;
        }
        int bracketCount = 2;
        int endPos = startPos + CITE_CONST_STR.length();
        for (; endPos < text.length(); endPos++) {
            switch (text.charAt(endPos)) {
            case '}':
                bracketCount--;
                break;
            case '{':
                bracketCount++;
                break;
            default:
            }
            if (bracketCount == 0) {
                break;
            }
        }
        text = text.substring(0, startPos - 1) + text.substring(endPos);
        return stripCite(text);
    }

    public boolean isDisambiguationPage() {
        return disambiguation;
    }

    public String getTranslatedTitle(final String languageCode) {
        final Pattern pattern = Pattern.compile("^\\[\\[" + languageCode + ":(.*?)\\]\\]$", Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(wikiText);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

}
