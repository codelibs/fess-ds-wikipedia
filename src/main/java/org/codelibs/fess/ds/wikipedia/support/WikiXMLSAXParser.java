/*
 * Copyright 2012-2023 CodeLibs Project and the Others.
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

import java.io.IOException;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.exception.DataStoreException;
import org.xml.sax.SAXException;

/**
 * A SAX Parser for Wikipedia XML dumps.
 *
 * @author Jason Smith
 * @see <a href="https://github.com/elastic/elasticsearch-river-wikipedia">Wikipedia River Plugin for Elasticsearch</a>
 */
public class WikiXMLSAXParser extends WikiXMLParser {

    private static final String TOTAL_ENTITY_SIZE_LIMIT = "http://www.oracle.com/xml/jaxp/properties/totalEntitySizeLimit";

    private PageCallbackHandler pageHandler = null;

    private int totalEntitySizeLimit = 50000000;

    public WikiXMLSAXParser(final URL fileName) {
        super(fileName);
    }

    /**
     * Set a callback handler. The callback is executed every time a
     * page instance is detected in the stream. Custom handlers are
     * implementations of {@link PageCallbackHandler}
     *
     * @param handler
     */
    @Override
    public void setPageCallback(final PageCallbackHandler handler) {
        pageHandler = handler;
    }

    /**
     * The main parse method.
     */
    @Override
    public void parse() {
        try {
            final SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setFeature(org.codelibs.fess.crawler.Constants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature(org.codelibs.fess.crawler.Constants.FEATURE_EXTERNAL_GENERAL_ENTITIES, false);
            factory.setFeature(org.codelibs.fess.crawler.Constants.FEATURE_EXTERNAL_PARAMETER_ENTITIES, false);
            final SAXParser parser = factory.newSAXParser();
            parser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, StringUtil.EMPTY);
            parser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, StringUtil.EMPTY);
            parser.setProperty(TOTAL_ENTITY_SIZE_LIMIT, totalEntitySizeLimit);
            parser.parse(getInputSource(), new SAXPageCallbackHandler(pageHandler));
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new DataStoreException("Could not parse wikipedia file.", e);
        }
    }

    /**
     * This parser is event driven, so it
     * can't provide a page iterator.
     */
    @Override
    public WikiPageIterator getIterator() {
        if (!(pageHandler instanceof IteratorHandler)) {
            throw new DataStoreException("Custom page callback found. Will not iterate.");
        }
        throw new UnsupportedOperationException();
    }

    public void setTotalEntitySizeLimit(final int totalEntitySizeLimit) {
        this.totalEntitySizeLimit = totalEntitySizeLimit;
    }
}
