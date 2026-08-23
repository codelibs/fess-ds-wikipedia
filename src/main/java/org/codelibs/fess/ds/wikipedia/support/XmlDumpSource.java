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

import java.util.function.Consumer;

/**
 * Reads a MediaWiki XML export dump.
 */
public class XmlDumpSource implements DumpSource {

    private final WikiXMLSAXParser parser;

    /**
     * Creates a source for the dump at the given location.
     *
     * @param location the URL or local path of the dump
     * @param fetcher the fetcher used to open the location
     */
    public XmlDumpSource(final String location, final DumpFetcher fetcher) {
        parser = new WikiXMLSAXParser(location, fetcher);
    }

    /**
     * Sets the total entity size limit applied to the XML parser.
     *
     * @param totalEntitySizeLimit the maximum total size of all entities in bytes
     */
    public void setTotalEntitySizeLimit(final int totalEntitySizeLimit) {
        parser.setTotalEntitySizeLimit(totalEntitySizeLimit);
    }

    @Override
    public void forEach(final Consumer<WikiDocument> consumer) {
        parser.setPageCallback(page -> consumer.accept(toDocument(page)));
        parser.parse();
    }

    /**
     * Converts a parsed page into the format-independent document.
     *
     * @param page the parsed page
     * @return the document
     */
    static WikiDocument toDocument(final WikiPage page) {
        final WikiDocument document = new WikiDocument();
        document.setId(page.getId());
        document.setTitle(page.getTitle());
        document.setNamespace(page.getNamespace());
        document.setContent(page.getText());
        document.setWikitext(page.getWikiText());
        document.setFormat(page.getFormat());
        document.setModel(page.getModel());
        document.setTimestamp(page.getTimestamp());
        document.setCategories(page.getCategories());
        document.setLinks(page.getLinks());
        document.setRedirect(page.isRedirect());
        document.setRedirectTitle(page.getRedirectPage());
        document.setStub(page.isStub());
        document.setDisambiguation(page.isDisambiguationPage());
        return document;
    }
}
