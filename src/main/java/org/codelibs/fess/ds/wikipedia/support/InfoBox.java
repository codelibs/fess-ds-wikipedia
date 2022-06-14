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

/**
 * A class abstracting Wiki infobox
 *
 * @author Delip Rao
 * @see <a href="https://github.com/elastic/elasticsearch-river-wikipedia">Wikipedia River Plugin for Elasticsearch</a>
 */
public class InfoBox {
    String infoBoxWikiText = null;

    InfoBox(final String infoBoxWikiText) {
        this.infoBoxWikiText = infoBoxWikiText;
    }

    public String dumpRaw() {
        return infoBoxWikiText;
    }
}
