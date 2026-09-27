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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sweble.wikitext.engine.config.Namespace;
import org.sweble.wikitext.engine.config.NamespaceImpl;
import org.sweble.wikitext.engine.config.WikiConfig;
import org.sweble.wikitext.engine.config.WikiConfigImpl;
import org.sweble.wikitext.engine.utils.DefaultConfigEnWp;

/**
 * Builds the Sweble configuration the plain-text conversion parses with.
 * <p>
 * Sweble only ships the English Wikipedia configuration, which knows {@code File:} and
 * {@code Category:} but not {@code ファイル:} or {@code Kategorie:}: on any other wiki an image
 * would be read as an ordinary link and leak its options, and a category statement would leak
 * as text. The configuration built here also knows the wiki's own namespace names from the
 * dump's {@code <siteinfo>}, and the common aliases of the file and category namespaces that
 * {@code <siteinfo>} doesn't list (such as {@code 画像:} and {@code カテゴリ:} on the Japanese
 * Wikipedia).
 * </p>
 */
public final class WikiConfigFactory {

    private static final Logger logger = LogManager.getLogger(WikiConfigFactory.class);

    private static final int FILE_NAMESPACE = 6;

    private static final int CATEGORY_NAMESPACE = 14;

    /**
     * Localized names and aliases of the file and category namespaces of the larger Wikipedias,
     * so that they are recognized even without {@code <siteinfo>}, which lists only one name per
     * namespace.
     */
    private static final Map<Integer, List<String>> BUILTIN_ALIASES = Map.of(//
            FILE_NAMESPACE,
            List.of("ファイル", "画像", "Datei", "Bild", "Fichier", "Archivo", "Imagen", "Immagine", "Ficheiro", "Arquivo", "Imagem", "Файл",
                    "Изображение", "文件", "檔案", "档案", "图像", "圖像", "파일", "그림", "Bestand", "Afbeelding", "Plik", "Grafika", "Fil"),
            CATEGORY_NAMESPACE, List.of("カテゴリ", "Kategorie", "Catégorie", "Categoría", "Categoria", "Категория", "分类", "分類", "분류",
                    "Categorie", "Kategoria", "Kategori"));

    private static final WikiConfig DEFAULT = create(null);

    private WikiConfigFactory() {
    }

    /**
     * Returns the configuration for a dump without {@code <siteinfo>}.
     *
     * @return the shared default configuration; it is immutable once built
     */
    public static WikiConfig getDefault() {
        return DEFAULT;
    }

    /**
     * Builds the configuration for a wiki. Building one is expensive (it loads namespace and
     * interwiki tables), so build it once per dump.
     *
     * @param siteInfo the dump's {@code <siteinfo>}, or null to use the default names only
     * @return the configuration
     */
    public static WikiConfig create(final SiteInfo siteInfo) {
        final WikiConfigImpl config = new WikiConfigImpl();
        new LocalizedConfig(siteInfo).configure(config);
        return config;
    }

    private static final class LocalizedConfig extends DefaultConfigEnWp {

        private final SiteInfo siteInfo;

        LocalizedConfig(final SiteInfo siteInfo) {
            this.siteInfo = siteInfo;
        }

        void configure(final WikiConfigImpl config) {
            configureWiki(config);
        }

        @Override
        protected void configureSiteProperties(final WikiConfigImpl config) {
            super.configureSiteProperties(config);
            final String language = siteInfo == null ? null : siteInfo.getLanguage();
            if (language != null) {
                config.setContentLang(language);
                config.setIwPrefix(language);
            }
        }

        @Override
        protected void addNamespaces(final WikiConfigImpl config) {
            final WikiConfigImpl english = new WikiConfigImpl();
            super.addNamespaces(english);

            final Map<Integer, String> localNames = siteInfo == null ? Map.of() : siteInfo.getNamespaces();
            final Map<Integer, Namespace> namespaces = new TreeMap<>();
            english.getNamespaces().forEach(ns -> namespaces.put(ns.getId(), ns));
            localNames.keySet().forEach(id -> namespaces.putIfAbsent(id, null));

            // A name must identify one namespace: the wiki's own names and the canonical names come
            // first, and an alias that one of them already uses is left out.
            final Set<String> taken = new HashSet<>();
            final Map<Integer, String> names = new TreeMap<>();
            namespaces.forEach((id, base) -> {
                final String local = localNames.get(id);
                final String name = local != null ? local : base.getName();
                names.put(id, name);
                taken.add(key(name));
                if (base != null) {
                    taken.add(key(base.getCanonical()));
                }
            });

            namespaces.forEach((id, base) -> {
                final String name = names.get(id);
                final String canonical = base != null ? base.getCanonical() : name;
                final List<String> candidates = new ArrayList<>();
                if (base != null) {
                    candidates.add(base.getName());
                    candidates.addAll(base.getAliases());
                }
                candidates.addAll(BUILTIN_ALIASES.getOrDefault(id, List.of()));
                final TreeSet<String> aliases = new TreeSet<>();
                for (final String alias : candidates) {
                    final String key = key(alias);
                    if (!key.equals(key(name)) && !key.equals(key(canonical)) && taken.add(key)) {
                        aliases.add(alias);
                    }
                }
                final NamespaceImpl namespace = base != null
                        ? new NamespaceImpl(id, name, canonical, base.isCanHaveSubpages(), base.isFileNs(), base.getCase(), aliases)
                        : new NamespaceImpl(id, name, canonical, false, false, aliases);
                try {
                    config.addNamespace(namespace);
                } catch (final IllegalArgumentException e) {
                    logger.warn("Skipped the namespace {} ({}): {}", id, name, e.getMessage());
                }
            });
            config.setDefaultNamespace(config.getNamespace(0));
            config.setTemplateNamespace(config.getNamespace(10));
        }

        private static String key(final String name) {
            return name.toLowerCase(Locale.ROOT);
        }
    }
}
