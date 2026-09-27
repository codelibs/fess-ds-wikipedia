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
import java.util.Map;
import java.util.TreeMap;

/**
 * The {@code <siteinfo>} header of a MediaWiki XML export: the wiki's own namespace names.
 * <p>
 * A wiki that isn't in English names its namespaces in its own language ({@code [[ファイル:x.jpg]]},
 * {@code [[Kategorie:Foo]]}), so the plain-text conversion needs these names to tell an image or
 * a category statement from an ordinary link.
 * </p>
 */
public class SiteInfo {

    private final Map<Integer, String> namespaces = new TreeMap<>();

    private String language;

    /**
     * Sets the wiki's content language, the {@code xml:lang} of the export.
     *
     * @param language the language code, such as {@code ja}; null if unknown
     */
    public void setLanguage(final String language) {
        this.language = language == null || language.isBlank() ? null : language.trim();
    }

    /**
     * Returns the wiki's content language. A link prefixed with it ({@code [[ja:東京]]} on the
     * Japanese Wikipedia) is an ordinary link, while other language prefixes are interlanguage
     * links, which aren't rendered.
     *
     * @return the language code, or null if unknown
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Adds a namespace as the export lists it.
     *
     * @param key the namespace number, such as 6 for files and 14 for categories
     * @param name the wiki's name for the namespace; blank for the main namespace
     */
    public void addNamespace(final int key, final String name) {
        namespaces.put(key, name == null ? "" : name.trim());
    }

    /**
     * Returns the wiki's namespace names by namespace number.
     *
     * @return the namespace names, ordered by number
     */
    public Map<Integer, String> getNamespaces() {
        return Collections.unmodifiableMap(namespaces);
    }
}
