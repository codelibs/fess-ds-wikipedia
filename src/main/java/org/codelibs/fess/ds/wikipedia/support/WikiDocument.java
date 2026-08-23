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

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * A single page from a Wikipedia dump, in a form that does not depend on which
 * dump format it came from.
 */
public class WikiDocument {

    private String id;

    private String title;

    private int namespace;

    private String content;

    private String wikitext;

    private String format;

    private String model;

    private Date timestamp;

    private List<String> categories = Collections.emptyList();

    private List<String> links = Collections.emptyList();

    private boolean redirect;

    private String redirectTitle;

    private boolean stub;

    private boolean disambiguation;

    private String openingText;

    private String language;

    private String revisionId;

    private String wikibaseItem;

    private Double popularityScore;

    private Integer incomingLinks;

    private List<String> headings = Collections.emptyList();

    private List<String> externalLinks = Collections.emptyList();

    private List<String> templates = Collections.emptyList();

    private List<String> redirects = Collections.emptyList();

    private List<String> auxiliaryText = Collections.emptyList();

    private List<String> weightedTags = Collections.emptyList();

    /**
     * Creates an empty document.
     */
    public WikiDocument() {
        // nothing
    }

    /**
     * Returns the page identifier.
     *
     * @return the page id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the page identifier.
     *
     * @param id the page id
     */
    public void setId(final String id) {
        this.id = id;
    }

    /**
     * Returns the page title.
     *
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the page title.
     *
     * @param title the title
     */
    public void setTitle(final String title) {
        this.title = title;
    }

    /**
     * Returns the MediaWiki namespace number; 0 is an article.
     *
     * @return the namespace number
     */
    public int getNamespace() {
        return namespace;
    }

    /**
     * Sets the MediaWiki namespace number.
     *
     * @param namespace the namespace number
     */
    public void setNamespace(final int namespace) {
        this.namespace = namespace;
    }

    /**
     * Returns the page body as plain text.
     *
     * @return the plain text body
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets the page body as plain text.
     *
     * @param content the plain text body
     */
    public void setContent(final String content) {
        this.content = content;
    }

    /**
     * Returns the raw wiki markup, when the source carries it.
     *
     * @return the wiki markup, or null
     */
    public String getWikitext() {
        return wikitext;
    }

    /**
     * Sets the raw wiki markup.
     *
     * @param wikitext the wiki markup
     */
    public void setWikitext(final String wikitext) {
        this.wikitext = wikitext;
    }

    /**
     * Returns the content format.
     *
     * @return the format
     */
    public String getFormat() {
        return format;
    }

    /**
     * Sets the content format.
     *
     * @param format the format
     */
    public void setFormat(final String format) {
        this.format = format;
    }

    /**
     * Returns the content model.
     *
     * @return the model
     */
    public String getModel() {
        return model;
    }

    /**
     * Sets the content model.
     *
     * @param model the model
     */
    public void setModel(final String model) {
        this.model = model;
    }

    /**
     * Returns the revision timestamp.
     *
     * @return the timestamp
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the revision timestamp.
     *
     * @param timestamp the timestamp
     */
    public void setTimestamp(final Date timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Returns the categories the page belongs to.
     *
     * @return the categories, never null
     */
    public List<String> getCategories() {
        return categories;
    }

    /**
     * Sets the categories the page belongs to.
     *
     * @param categories the categories; null becomes an empty list
     */
    public void setCategories(final List<String> categories) {
        this.categories = categories == null ? Collections.emptyList() : categories;
    }

    /**
     * Returns the titles this page links to.
     *
     * @return the link targets, never null
     */
    public List<String> getLinks() {
        return links;
    }

    /**
     * Sets the titles this page links to.
     *
     * @param links the link targets; null becomes an empty list
     */
    public void setLinks(final List<String> links) {
        this.links = links == null ? Collections.emptyList() : links;
    }

    /**
     * Returns whether this page is itself a redirect to another page.
     *
     * @return true when the page is a redirect
     */
    public boolean isRedirect() {
        return redirect;
    }

    /**
     * Sets whether this page is itself a redirect.
     *
     * @param redirect true when the page is a redirect
     */
    public void setRedirect(final boolean redirect) {
        this.redirect = redirect;
    }

    /**
     * Returns the title this page redirects to.
     *
     * @return the redirect target, or null
     */
    public String getRedirectTitle() {
        return redirectTitle;
    }

    /**
     * Sets the title this page redirects to.
     *
     * @param redirectTitle the redirect target
     */
    public void setRedirectTitle(final String redirectTitle) {
        this.redirectTitle = redirectTitle;
    }

    /**
     * Returns whether the page is marked as a stub.
     * Only the XML source populates this flag; on a CirrusSearch record it is always false.
     *
     * @return true when the page is a stub
     */
    public boolean isStub() {
        return stub;
    }

    /**
     * Sets whether the page is marked as a stub.
     *
     * @param stub true when the page is a stub
     */
    public void setStub(final boolean stub) {
        this.stub = stub;
    }

    /**
     * Returns whether the page is a disambiguation page.
     * Only the XML source populates this flag; on a CirrusSearch record it is always false.
     *
     * @return true when the page disambiguates a title
     */
    public boolean isDisambiguation() {
        return disambiguation;
    }

    /**
     * Sets whether the page is a disambiguation page.
     *
     * @param disambiguation true when the page disambiguates a title
     */
    public void setDisambiguation(final boolean disambiguation) {
        this.disambiguation = disambiguation;
    }

    /**
     * Returns the summary paragraph rendered from the page, when the source carries it.
     *
     * @return the opening text, or null
     */
    public String getOpeningText() {
        return openingText;
    }

    /**
     * Sets the summary paragraph rendered from the page.
     *
     * @param openingText the opening text
     */
    public void setOpeningText(final String openingText) {
        this.openingText = openingText;
    }

    /**
     * Returns the page's content language, when the source carries it.
     *
     * @return the language code, or null
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Sets the page's content language.
     *
     * @param language the language code
     */
    public void setLanguage(final String language) {
        this.language = language;
    }

    /**
     * Returns the identifier of the revision this document was built from.
     *
     * @return the revision id, or null
     */
    public String getRevisionId() {
        return revisionId;
    }

    /**
     * Sets the identifier of the revision this document was built from.
     *
     * @param revisionId the revision id
     */
    public void setRevisionId(final String revisionId) {
        this.revisionId = revisionId;
    }

    /**
     * Returns the Wikidata item identifier linked to this page, when the source carries it.
     *
     * @return the Wikidata item id, or null
     */
    public String getWikibaseItem() {
        return wikibaseItem;
    }

    /**
     * Sets the Wikidata item identifier linked to this page.
     *
     * @param wikibaseItem the Wikidata item id
     */
    public void setWikibaseItem(final String wikibaseItem) {
        this.wikibaseItem = wikibaseItem;
    }

    /**
     * Returns the search ranking popularity score, when the source carries it.
     *
     * @return the popularity score, or null
     */
    public Double getPopularityScore() {
        return popularityScore;
    }

    /**
     * Sets the search ranking popularity score.
     *
     * @param popularityScore the popularity score
     */
    public void setPopularityScore(final Double popularityScore) {
        this.popularityScore = popularityScore;
    }

    /**
     * Returns the number of pages known to link to this one, when the source carries it.
     *
     * @return the incoming link count, or null
     */
    public Integer getIncomingLinks() {
        return incomingLinks;
    }

    /**
     * Sets the number of pages known to link to this one.
     *
     * @param incomingLinks the incoming link count
     */
    public void setIncomingLinks(final Integer incomingLinks) {
        this.incomingLinks = incomingLinks;
    }

    /**
     * Returns the section headings on the page.
     *
     * @return the headings, never null
     */
    public List<String> getHeadings() {
        return headings;
    }

    /**
     * Sets the section headings on the page.
     *
     * @param headings the headings; null becomes an empty list
     */
    public void setHeadings(final List<String> headings) {
        this.headings = headings == null ? Collections.emptyList() : headings;
    }

    /**
     * Returns the URLs this page links to outside of Wikipedia.
     *
     * @return the external links, never null
     */
    public List<String> getExternalLinks() {
        return externalLinks;
    }

    /**
     * Sets the URLs this page links to outside of Wikipedia.
     *
     * @param externalLinks the external links; null becomes an empty list
     */
    public void setExternalLinks(final List<String> externalLinks) {
        this.externalLinks = externalLinks == null ? Collections.emptyList() : externalLinks;
    }

    /**
     * Returns the templates transcluded on the page.
     *
     * @return the templates, never null
     */
    public List<String> getTemplates() {
        return templates;
    }

    /**
     * Sets the templates transcluded on the page.
     *
     * @param templates the templates; null becomes an empty list
     */
    public void setTemplates(final List<String> templates) {
        this.templates = templates == null ? Collections.emptyList() : templates;
    }

    /**
     * Returns the titles of the pages that redirect to this one.
     * <p>
     * This is not the same as {@link #isRedirect()}, which says whether this page is
     * itself a redirect. A CirrusSearch content index carries the former and never the
     * latter, because redirect pages are folded into their target rather than indexed.
     * </p>
     *
     * @return the titles redirecting here, never null
     */
    public List<String> getRedirects() {
        return redirects;
    }

    /**
     * Sets the titles of the pages that redirect to this one.
     *
     * @param redirects the redirecting titles; null becomes an empty list
     */
    public void setRedirects(final List<String> redirects) {
        this.redirects = redirects == null ? Collections.emptyList() : redirects;
    }

    /**
     * Returns text rendered from templates such as infoboxes, kept separate from the body.
     *
     * @return the auxiliary text, never null
     */
    public List<String> getAuxiliaryText() {
        return auxiliaryText;
    }

    /**
     * Sets the text rendered from templates such as infoboxes.
     *
     * @param auxiliaryText the auxiliary text; null becomes an empty list
     */
    public void setAuxiliaryText(final List<String> auxiliaryText) {
        this.auxiliaryText = auxiliaryText == null ? Collections.emptyList() : auxiliaryText;
    }

    /**
     * Returns the machine-classified topic tags carried by the page, with their weights.
     *
     * @return the weighted tags, never null
     */
    public List<String> getWeightedTags() {
        return weightedTags;
    }

    /**
     * Sets the machine-classified topic tags carried by the page.
     *
     * @param weightedTags the weighted tags; null becomes an empty list
     */
    public void setWeightedTags(final List<String> weightedTags) {
        this.weightedTags = weightedTags == null ? Collections.emptyList() : weightedTags;
    }
}
