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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Reads a MediaWiki XML export dump.
 */
public class XmlDumpSource implements DumpSource {

    private static final Logger logger = LogManager.getLogger(XmlDumpSource.class);

    private final WikiXMLSAXParser parser;

    private PlainTextOptions plainTextOptions = PlainTextOptions.DEFAULT;

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

    /**
     * Sets which template text and captions the plain-text content drops, and how it is
     * converted. The namespace names of the dump's {@code <siteinfo>} are added to them.
     *
     * @param plainTextOptions the options
     */
    public void setPlainTextOptions(final PlainTextOptions plainTextOptions) {
        this.plainTextOptions = plainTextOptions;
    }

    @Override
    public void forEach(final Consumer<WikiDocument> consumer) {
        parser.setPageCallback(new PageCallbackHandler() {
            private PlainTextOptions options = plainTextOptions;

            @Override
            public void processSiteInfo(final SiteInfo siteInfo) {
                // the regex extractor doesn't use the namespace names; don't pay for building them
                if (options.getExtractor() == PlainTextOptions.Extractor.SWEBLE) {
                    try {
                        options = options.withSiteInfo(siteInfo);
                    } catch (final RuntimeException e) {
                        // the pages can still be converted, only without the wiki's own namespace names
                        logger.warn("Failed to read the namespaces of the dump; using the default names.", e);
                    }
                }
            }

            @Override
            public void process(final WikiPage page) {
                consumer.accept(toDocument(page, options));
            }
        });
        parser.parse();
    }

    /**
     * Converts a parsed page into the format-independent document.
     *
     * @param page the parsed page
     * @return the document
     */
    static WikiDocument toDocument(final WikiPage page) {
        return toDocument(page, PlainTextOptions.DEFAULT);
    }

    /**
     * Converts a parsed page into the format-independent document.
     *
     * @param page the parsed page
     * @param plainTextOptions which template text and captions the content drops
     * @return the document
     */
    static WikiDocument toDocument(final WikiPage page, final PlainTextOptions plainTextOptions) {
        final WikiDocument document = new WikiDocument();
        document.setId(page.getId());
        document.setTitle(page.getTitle());
        document.setNamespace(page.getNamespace());
        document.setContent(page.getText(plainTextOptions));
        document.setWikitext(page.getWikiText());
        document.setFormat(page.getFormat());
        document.setModel(page.getModel());
        document.setTimestamp(page.getTimestamp());
        // getCategories() and getLinks() each scan the wikitext with a regex, so every page pays
        // for two extra passes here. A later filtering task may want to reorder this so pages it
        // discards do not pay that cost.
        document.setCategories(page.getCategories());
        document.setLinks(page.getLinks());
        document.setRedirect(page.isRedirect());
        document.setRedirectTitle(page.getRedirectPage());
        document.setStub(page.isStub());
        document.setDisambiguation(page.isDisambiguationPage());
        return document;
    }
}
