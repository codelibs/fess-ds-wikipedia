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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.exception.DataStoreException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * A SAX parser for Wikipedia XML dumps.
 *
 * @see <a href="https://github.com/elastic/elasticsearch-river-wikipedia">Wikipedia River Plugin for Elasticsearch</a>
 */
public class WikiXMLSAXParser {

    private static final String TOTAL_ENTITY_SIZE_LIMIT = "http://www.oracle.com/xml/jaxp/properties/totalEntitySizeLimit";

    private final String location;

    private final DumpFetcher fetcher;

    private PageCallbackHandler pageHandler;

    private int totalEntitySizeLimit = 50000000;

    /**
     * Constructs a parser for the dump at the given location.
     *
     * @param location the URL or local path of the Wikipedia XML dump
     * @param fetcher the fetcher used to open the location
     */
    public WikiXMLSAXParser(final String location, final DumpFetcher fetcher) {
        this.location = location;
        this.fetcher = fetcher;
    }

    /**
     * Sets a callback handler that is executed for every page element in the stream.
     *
     * @param handler the callback handler
     */
    public void setPageCallback(final PageCallbackHandler handler) {
        pageHandler = handler;
    }

    /**
     * Sets the total entity size limit applied to the XML parser.
     *
     * @param totalEntitySizeLimit the maximum total size of all entities in bytes
     */
    public void setTotalEntitySizeLimit(final int totalEntitySizeLimit) {
        this.totalEntitySizeLimit = totalEntitySizeLimit;
    }

    /**
     * Parses the dump, invoking the page callback for every page element.
     */
    public void parse() {
        try (InputStream in = fetcher.open(location);
                InputStream decompressed = CompressedStreamFactory.open(in);
                BufferedReader reader = new BufferedReader(new InputStreamReader(decompressed, StandardCharsets.UTF_8))) {
            final SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setFeature(org.codelibs.fess.crawler.Constants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature(org.codelibs.fess.crawler.Constants.FEATURE_EXTERNAL_GENERAL_ENTITIES, false);
            factory.setFeature(org.codelibs.fess.crawler.Constants.FEATURE_EXTERNAL_PARAMETER_ENTITIES, false);
            final SAXParser parser = factory.newSAXParser();
            parser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, StringUtil.EMPTY);
            parser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, StringUtil.EMPTY);
            parser.setProperty(TOTAL_ENTITY_SIZE_LIMIT, totalEntitySizeLimit);
            parser.parse(new InputSource(reader), new SAXPageCallbackHandler(pageHandler));
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new DataStoreException("Could not parse wikipedia file: " + location, e);
        }
    }
}
