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
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.exception.DataStoreException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Reads a CirrusSearch index dump, the search index Wikipedia itself serves.
 * <p>
 * The dump is an OpenSearch bulk stream: pairs of lines where the first names the
 * operation and the second is the document. The document already carries the article
 * body as plain text, so no wiki markup has to be parsed.
 * </p>
 */
public class CirrusIndexDumpSource implements DumpSource {

    private static final Logger logger = LogManager.getLogger(CirrusIndexDumpSource.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final List<String> locations;

    private final DumpFetcher fetcher;

    /**
     * Creates a source that reads the given dump files in order.
     *
     * @param locations the dump files, typically the chunks of one index
     * @param fetcher the fetcher used to open each location
     */
    public CirrusIndexDumpSource(final List<String> locations, final DumpFetcher fetcher) {
        this.locations = locations;
        this.fetcher = fetcher;
    }

    @Override
    public void forEach(final Consumer<WikiDocument> consumer) {
        for (final String location : locations) {
            logger.info("Reading {}", location);
            readLocation(location, consumer);
        }
    }

    /**
     * Reads one dump file.
     *
     * @param location the dump file
     * @param consumer the callback invoked for each document
     */
    protected void readLocation(final String location, final Consumer<WikiDocument> consumer) {
        try (InputStream in = fetcher.open(location);
                InputStream decompressed = CompressedStreamFactory.open(in);
                BufferedReader reader = new BufferedReader(new InputStreamReader(decompressed, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (StringUtil.isBlank(line)) {
                    continue;
                }
                final JsonNode node = MAPPER.readTree(line);
                if (isActionLine(node)) {
                    continue;
                }
                consumer.accept(toDocument(node));
            }
        } catch (final IOException e) {
            throw new DataStoreException("Could not read the CirrusSearch dump: " + location, e);
        }
    }

    /**
     * Returns whether the line is a bulk action rather than a document.
     *
     * @param node the parsed line
     * @return true when the line names a bulk operation
     */
    protected static boolean isActionLine(final JsonNode node) {
        return node.has("index") || node.has("create") || node.has("update") || node.has("delete");
    }

    /**
     * Converts one dump record into the format-independent document.
     *
     * @param node the parsed document line
     * @return the document
     */
    static WikiDocument toDocument(final JsonNode node) {
        final WikiDocument document = new WikiDocument();
        document.setId(text(node, "page_id"));
        document.setTitle(text(node, "title"));
        document.setNamespace(node.path("namespace").asInt(0));
        document.setContent(text(node, "text"));
        document.setOpeningText(text(node, "opening_text"));
        document.setWikitext(text(node, "source_text"));
        document.setModel(text(node, "content_model"));
        document.setFormat(toFormat(text(node, "content_model")));
        document.setLanguage(text(node, "language"));
        document.setRevisionId(text(node, "version"));
        document.setTimestamp(toDate(text(node, "timestamp")));
        document.setCategories(strings(node, "category"));
        document.setHeadings(strings(node, "heading"));
        document.setLinks(strings(node, "outgoing_link"));
        document.setExternalLinks(strings(node, "external_link"));
        document.setTemplates(strings(node, "template"));
        document.setAuxiliaryText(strings(node, "auxiliary_text"));
        document.setWeightedTags(strings(node, "weighted_tags"));
        document.setRedirects(redirectTitles(node));
        document.setWikibaseItem(text(node, "wikibase_item"));
        if (node.hasNonNull("incoming_links")) {
            document.setIncomingLinks(node.get("incoming_links").asInt());
        }
        if (node.hasNonNull("popularity_score")) {
            document.setPopularityScore(node.get("popularity_score").asDouble());
        }
        // A CirrusSearch content index never contains redirect pages themselves; the
        // redirect field lists the pages pointing here. So this is always false.
        document.setRedirect(false);
        return document;
    }

    /**
     * Returns a field as text, whatever JSON type it carries.
     *
     * @param node the document
     * @param field the field name
     * @return the value as text, or null when absent
     */
    protected static String text(final JsonNode node, final String field) {
        if (!node.hasNonNull(field)) {
            return null;
        }
        return node.get(field).asText();
    }

    /**
     * Returns a string-array field as a list.
     *
     * @param node the document
     * @param field the field name
     * @return the values, never null
     */
    protected static List<String> strings(final JsonNode node, final String field) {
        final JsonNode array = node.path(field);
        if (!array.isArray()) {
            return List.of();
        }
        final List<String> values = new ArrayList<>(array.size());
        array.forEach(element -> values.add(element.asText()));
        return values;
    }

    /**
     * Returns the titles of the redirect objects pointing at this page.
     *
     * @param node the document
     * @return the redirecting titles, never null
     */
    protected static List<String> redirectTitles(final JsonNode node) {
        final JsonNode array = node.path("redirect");
        if (!array.isArray()) {
            return List.of();
        }
        final List<String> titles = new ArrayList<>(array.size());
        array.forEach(element -> {
            if (element.hasNonNull("title")) {
                titles.add(element.get("title").asText());
            }
        });
        return titles;
    }

    /**
     * Returns the content format matching a content model.
     *
     * @param contentModel the content model, such as wikitext
     * @return the format, or null when the model is unknown
     */
    protected static String toFormat(final String contentModel) {
        if ("wikitext".equals(contentModel)) {
            return "text/x-wiki";
        }
        return null;
    }

    /**
     * Parses an ISO-8601 timestamp.
     *
     * @param value the timestamp text
     * @return the parsed date, or null when absent or unparseable
     */
    protected static Date toDate(final String value) {
        if (StringUtil.isBlank(value)) {
            return null;
        }
        try {
            return Date.from(Instant.parse(value));
        } catch (final DateTimeParseException e) {
            logger.warn("Failed to parse a timestamp: {}", value);
            return null;
        }
    }
}
