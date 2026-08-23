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
}
