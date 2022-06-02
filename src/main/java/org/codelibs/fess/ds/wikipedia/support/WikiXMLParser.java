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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import org.codelibs.fess.ds.wikipedia.bzip2.CBZip2InputStream;
import org.xml.sax.InputSource;

/**
 * @author Delip Rao
 * @author Jason Smith
 */
public abstract class WikiXMLParser {

    private URL wikiXMLFile = null;
    protected WikiPage currentPage = null;
    private BufferedReader br;

    public WikiXMLParser(final URL fileName) {
        wikiXMLFile = fileName;
    }

    /**
     * Set a callback handler. The callback is executed every time a
     * page instance is detected in the stream. Custom handlers are
     * implementations of {@link PageCallbackHandler}
     *
     * @param handler
     * @throws Exception
     */
    public abstract void setPageCallback(PageCallbackHandler handler);

    /**
     * The main parse method.
     *
     * @throws Exception
     */
    public abstract void parse();

    /**
     * @return an iterator to the list of pages
     * @throws Exception
     */
    public abstract WikiPageIterator getIterator();

    /**
     * @return An InputSource created from wikiXMLFile
     * @throws Exception
     */
    protected InputSource getInputSource() throws IOException {
        if (wikiXMLFile.toExternalForm().endsWith(".gz")) {
            br = new BufferedReader(new InputStreamReader(new GZIPInputStream(wikiXMLFile.openStream()), "UTF-8"));
        } else if (wikiXMLFile.toExternalForm().endsWith(".bz2")) {
            final InputStream fis = wikiXMLFile.openStream();
            final byte[] ignoreBytes = new byte[2];
            fis.read(ignoreBytes); //"B", "Z" bytes from commandline tools
            final CBZip2InputStream cbZip2InputStream = new CBZip2InputStream(fis);
            br = new BufferedReader(new InputStreamReader(cbZip2InputStream, "UTF-8"));
        } else {
            br = new BufferedReader(new InputStreamReader(wikiXMLFile.openStream(), "UTF-8"));
        }

        return new InputSource(br);
    }

    protected void notifyPage(final WikiPage page) {
        currentPage = page;
    }

    public void close() throws IOException {
        if (br != null) {
            br.close();
        }
    }
}
