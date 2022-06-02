/*
 * Copyright 2012-2022 CodeLibs Project and the Others.
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

import org.codelibs.fess.exception.DataStoreException;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * A SAX Parser for Wikipedia XML dumps.
 *
 * @author Jason Smith
 */
public class WikiXMLSAXParser extends WikiXMLParser {

    private PageCallbackHandler pageHandler = null;
    private SAXPageCallbackHandler handler = null;

    public WikiXMLSAXParser(final URL fileName) {
        super(fileName);
    }

    /**
     * Set a callback handler. The callback is executed every time a
     * page instance is detected in the stream. Custom handlers are
     * implementations of {@link PageCallbackHandler}
     *
     * @param handler
     * @throws Exception
     */
    @Override
    public void setPageCallback(final PageCallbackHandler handler) {
        pageHandler = handler;
    }

    /**
     * The main parse method.
     *
     * @throws Exception
     */
    @Override
    public void parse() {
        try {
            final XMLReader xmlReader = XMLReaderFactory.createXMLReader();
            handler = new SAXPageCallbackHandler(pageHandler);
            xmlReader.setContentHandler(handler);
            xmlReader.parse(getInputSource());
        } catch (IOException | SAXException e) {
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
}
