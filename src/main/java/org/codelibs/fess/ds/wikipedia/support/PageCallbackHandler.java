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

/**
 * Interface to allow streamed processing of pages.
 * This allows a SAX style processing of Wikipedia XML files.
 * The registered callback is executed on each page
 * element in the XML file.
 * Using callbacks will consume lesser memory, an useful feature for large
 * dumps like English and German.
 *
 * @author Delip Rao
 * @see WikiPage
 * @see <a href="https://github.com/elastic/elasticsearch-river-wikipedia">Wikipedia River Plugin for Elasticsearch</a>
 */
public interface PageCallbackHandler {
    /**
     * This is the callback method that should be implemented before
     * registering with <code>WikiXMLDOMParser</code>
     *
     * @param page a wikipedia page object
     * @see WikiPage
     */
    void process(WikiPage page);
}
