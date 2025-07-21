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

/**
 * Data structures for a wikipedia page.
 *
 * @author Delip Rao
 * @see <a href="https://github.com/elastic/elasticsearch-river-wikipedia">Wikipedia River Plugin for Elasticsearch</a>
 */
public class WikiPage {

    /**
     * Constructs a new WikiPage object.
     */
    public WikiPage() {
        // default constructor
    }

    private String title = null;
    private WikiTextParser wikiTextParser = null;
    private String id = null;
    private Date timestamp;
    private String format;
    private String model;

    /**
     * Set the page title. This is not intended for direct use.
     *
     * @param title the title of the Wikipedia page
     */
    public void setTitle(final String title) {
        this.title = title;
    }

    /**
     * Set the wiki text associated with this page.
     * This setter also introduces side effects. This is not intended for direct use.
     *
     * @param wtext wiki-formatted text
     */
    public void setWikiText(final String wtext) {
        wikiTextParser = new WikiTextParser(wtext);
    }

    /**
     * Returns the title of the Wikipedia page.
     *
     * @return a string containing the page title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the title translated into the specified language.
     * @param languageCode The language code for the desired translation (e.g., "en", "fr").
     * @return A string containing the title translated in the given languageCode.
     */
    public String getTranslatedTitle(final String languageCode) {
        return wikiTextParser.getTranslatedTitle(languageCode);
    }

    /**
     * Checks if this page is a disambiguation page.
     *
     * @return true if this a disambiguation page.
     */
    public boolean isDisambiguationPage() {
        if (title.contains("(disambiguation)") || wikiTextParser.isDisambiguationPage()) {
            return true;
        }
        return false;
    }

    /**
     * Checks if this page is a special page.
     *
     * @return true for "special pages" -- like Category:, Wikipedia:, etc
     */
    public boolean isSpecialPage() {
        return title.contains(":");
    }

    /**
     * Use this method to get the wiki text associated with this page.
     * Useful for custom processing the wiki text.
     *
     * @return a string containing the wiki text.
     */
    public String getWikiText() {
        return wikiTextParser.getText();
    }

    /**
     * Checks if this page is a redirection page.
     *
     * @return true if this is a redirection page
     */
    public boolean isRedirect() {
        return wikiTextParser.isRedirect();
    }

    /**
     * Checks if this page is a stub page.
     *
     * @return true if this is a stub page
     */
    public boolean isStub() {
        return wikiTextParser.isStub();
    }

    /**
     * Returns the title of the page being redirected to.
     *
     * @return the title of the page being redirected to.
     */
    public String getRedirectPage() {
        return wikiTextParser.getRedirectText();
    }

    /**
     * Returns the plain text content of the Wikipedia page, stripped of all wiki formatting.
     *
     * @return plain text stripped of all wiki formatting.
     */
    public String getText() {
        return wikiTextParser.getPlainText();
    }

    /**
     * Returns a list of categories the page belongs to.
     *
     * @return a list of categories the page belongs to, null if this a redirection/disambiguation page
     */
    public List<String> getCategories() {
        return wikiTextParser.getCategories();
    }

    /**
     * Returns a list of links contained in the page.
     *
     * @return a list of links contained in the page
     */
    public List<String> getLinks() {
        return wikiTextParser.getLinks();
    }

    /**
     * Sets the ID of the Wikipedia page.
     *
     * @param id a string containing the page ID.
     */
    public void setId(final String id) {
        this.id = id;
    }

    /**
     * Returns the InfoBox associated with this Wikipedia page.
     *
     * @return an InfoBox object containing structured data from the page's infobox.
     */
    public InfoBox getInfoBox() {
        return wikiTextParser.getInfoBox();
    }

    /**
     * Returns the ID of the Wikipedia page.
     *
     * @return a string containing the page ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the timestamp of the Wikipedia page.
     *
     * @param timestamp a Date object representing the page's timestamp.
     */
    public void setTimestamp(final Date timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Returns the timestamp of the Wikipedia page.
     *
     * @return a Date object representing the page's timestamp.
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the format of the Wikipedia page content.
     *
     * @param format a string representing the content format.
     */
    public void setFormat(final String format) {
        this.format = format;
    }

    /**
     * Returns the format of the Wikipedia page content.
     *
     * @return a string representing the content format.
     */
    public String getFormat() {
        return format;
    }

    /**
     * Sets the model of the Wikipedia page content.
     *
     * @param model a string representing the content model.
     */
    public void setModel(final String model) {
        this.model = model;
    }

    /**
     * Returns the model of the Wikipedia page content.
     *
     * @return a string representing the content model.
     */
    public String getModel() {
        return model;
    }
}
