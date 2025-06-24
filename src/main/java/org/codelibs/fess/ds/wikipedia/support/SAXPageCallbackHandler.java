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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.core.lang.StringUtil;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A Wrapper class for the PageCallbackHandler
 *
 * @author Jason Smith
 * @see <a href="https://github.com/elastic/elasticsearch-river-wikipedia">Wikipedia River Plugin for Elasticsearch</a>
 */
public class SAXPageCallbackHandler extends DefaultHandler {

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    private static final Logger logger = LogManager.getLogger(SAXPageCallbackHandler.class);

    public static final TimeZone TIMEZONE_UTC = TimeZone.getTimeZone("UTC");

    private final PageCallbackHandler pageHandler;
    private WikiPage currentPage;
    private String currentTag;

    private String currentWikitext;
    private String currentTitle;

    public SAXPageCallbackHandler(final PageCallbackHandler ph) {
        pageHandler = ph;
    }

    @Override
    public void startElement(final String uri, final String name, final String qName, final Attributes attr) {
        currentTag = qName;
        if ("page".equals(qName)) {
            currentPage = new WikiPage();
            currentWikitext = StringUtil.EMPTY;
            currentTitle = StringUtil.EMPTY;
        }
    }

    @Override
    public void endElement(final String uri, final String name, final String qName) {
        if ("page".equals(qName)) {
            currentPage.setTitle(currentTitle);
            currentPage.setWikiText(currentWikitext);
            pageHandler.process(currentPage);
        } else if ("mediawiki".equals(qName)) {
            // TODO hasMoreElements() should now return false
        }
    }

    @Override
    public void characters(final char ch[], final int start, final int length) {
        switch (currentTag) {
        case "title": {
            currentTitle = currentTitle.concat(new String(ch, start, length));
            break;
        }
        case "text": {
            currentWikitext = currentWikitext.concat(new String(ch, start, length));
            break;
        }
        case "id": {
            if (StringUtil.isBlank(currentPage.getId())) {
                final String value = new String(ch, start, length);
                if (StringUtil.isNotBlank(value)) {
                    currentPage.setId(value.trim());
                }
            }
            break;
        }
        case "format": {
            final String value = new String(ch, start, length);
            if (StringUtil.isNotBlank(value)) {
                currentPage.setFormat(value.trim());
            }
            break;
        }
        case "model": {
            final String value = new String(ch, start, length);
            if (StringUtil.isNotBlank(value)) {
                currentPage.setModel(value.trim());
            }
            break;
        }
        case "timestamp": {
            final String value = new String(ch, start, length);
            if (StringUtil.isNotBlank(value)) {
                try {
                    final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
                    sdf.setTimeZone(TIMEZONE_UTC);
                    currentPage.setTimestamp(sdf.parse(value));
                } catch (final ParseException e) {
                    logger.warn("Failed to parse " + value, e);
                }
            }
            break;
        }
        default:
            break;
        }
    }
}
