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
package org.codelibs.fess.ds.wikipedia;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.Constants;
import org.codelibs.fess.app.service.FailureUrlService;
import org.codelibs.fess.crawler.exception.CrawlingAccessException;
import org.codelibs.fess.crawler.exception.MultipleCrawlingAccessException;
import org.codelibs.fess.ds.AbstractDataStore;
import org.codelibs.fess.ds.callback.IndexUpdateCallback;
import org.codelibs.fess.ds.wikipedia.exception.ParserStoppedException;
import org.codelibs.fess.ds.wikipedia.support.CirrusIndexDumpSource;
import org.codelibs.fess.ds.wikipedia.support.DumpFetcher;
import org.codelibs.fess.ds.wikipedia.support.DumpLocationResolver;
import org.codelibs.fess.ds.wikipedia.support.DumpSource;
import org.codelibs.fess.ds.wikipedia.support.PlainTextOptions;
import org.codelibs.fess.ds.wikipedia.support.WikiDocument;
import org.codelibs.fess.ds.wikipedia.support.XmlDumpSource;
import org.codelibs.fess.entity.DataStoreParams;
import org.codelibs.fess.exception.DataStoreCrawlingException;
import org.codelibs.fess.exception.DataStoreException;
import org.codelibs.fess.helper.CrawlerStatsHelper;
import org.codelibs.fess.helper.CrawlerStatsHelper.StatsAction;
import org.codelibs.fess.helper.CrawlerStatsHelper.StatsKeyObject;
import org.codelibs.fess.opensearch.config.exentity.DataConfig;
import org.codelibs.fess.util.ComponentUtil;

/**
 * This class is the main entry point for the Wikipedia Data Store plugin.
 * It extends AbstractDataStore and handles configuration parsing, orchestrates
 * Wikipedia XML parsing and indexing, and manages error handling and crawler statistics.
 */
public class WikipediaDataStore extends AbstractDataStore {

    private static final Logger logger = LogManager.getLogger(WikipediaDataStore.class);

    /**
     * Default constructor for WikipediaDataStore.
     */
    public WikipediaDataStore() {
        super();
    }

    private static final String DEFAULT_WIKIPEDIA_URL = "http://download.wikimedia.org/enwiki/latest/enwiki-latest-pages-articles.xml.bz2";

    /** The parameter name for the dump location. */
    protected static final String URL_PARAM = "url";

    /** The parameter name for the User-Agent header. */
    protected static final String USER_AGENT_PARAM = "user_agent";

    /**
     * The parameter name for the comma-separated templates whose argument text is dropped from
     * the plain-text content of {@code source=xml} pages, such as {@code Cite web,Cite news}, or
     * {@code *} for all templates. By default the text of every template is kept.
     */
    protected static final String DROP_TEMPLATES_PARAM = "drop_templates";

    /**
     * The parameter name for the comma-separated templates whose argument text is kept although
     * {@link #DROP_TEMPLATES_PARAM} matches them; with {@code drop_templates=*} this is an allowlist.
     */
    protected static final String KEEP_TEMPLATES_PARAM = "keep_templates";

    /** The parameter name for dropping image and gallery captions from {@code source=xml} content. */
    protected static final String DROP_CAPTIONS_PARAM = "drop_captions";

    /**
     * The parameter name for how {@code source=xml} content is converted to plain text:
     * {@code sweble} (the default) parses the wikitext, {@code regex} strips it with regular
     * expressions, which is faster but leaves nested and multi-line markup behind.
     */
    protected static final String TEXT_EXTRACTOR_PARAM = "text_extractor";

    /**
     * Used only when both the {@link #USER_AGENT_PARAM} parameter and the Fess crawler
     * User-Agent are blank, so that a request is never sent with an empty User-Agent header.
     */
    private static final String FALLBACK_USER_AGENT = "Mozilla/5.0 (compatible; Fess; +http://fess.codelibs.org/bot.html)";

    @Override
    protected String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    protected void storeData(final DataConfig dataConfig, final IndexUpdateCallback callback, final DataStoreParams paramMap,
            final Map<String, String> scriptMap, final Map<String, Object> defaultDataMap) {
        final CrawlerStatsHelper crawlerStatsHelper = ComponentUtil.getCrawlerStatsHelper();

        final long readInterval = getReadInterval(paramMap);
        final int limit = Integer.parseInt(paramMap.getAsString("limit", "0"));
        final int totalEntitySizeLimit = Integer.parseInt(paramMap.getAsString("total_entity_size_limit", "100000000"));
        final int maxDigestLength = Integer.parseInt(paramMap.getAsString("max_digest_length", "100"));
        final String scriptType = getScriptType(paramMap);
        final String dumpLocation = getDumpLocation(paramMap);
        final String userAgent = getUserAgent(paramMap);
        logger.info("url: {}", dumpLocation);
        final AtomicInteger counter = new AtomicInteger();
        final DumpFetcher fetcher = new DumpFetcher(userAgent);
        final boolean cirrus = isCirrusSource(paramMap, dumpLocation);
        final String siteHost = getSiteHost(dumpLocation);
        final String siteLanguage = getSiteLanguage(dumpLocation);
        final DumpSource dumpSource = createDumpSource(paramMap, dumpLocation, fetcher, totalEntitySizeLimit);
        if (dumpSource instanceof final XmlDumpSource xmlSource) {
            xmlSource.setPlainTextOptions(getPlainTextOptions(paramMap));
        }
        try {
            dumpSource.forEach(document -> {
                final StatsKeyObject statsKey = new StatsKeyObject(dataConfig.getId() + "#" + document.getId());
                paramMap.put(Constants.CRAWLER_STATS_KEY, statsKey);
                final Map<String, Object> dataMap = new HashMap<>(defaultDataMap);
                final Map<String, Object> resultMap = new LinkedHashMap<>();
                try {
                    crawlerStatsHelper.begin(statsKey);
                    resultMap.putAll(paramMap.asMap());

                    putDocumentValues(resultMap, document, maxDigestLength);
                    putSiteValues(resultMap, document, siteHost, siteLanguage);
                    if (cirrus) {
                        putCirrusValues(resultMap, document);
                    }

                    crawlerStatsHelper.record(statsKey, StatsAction.PREPARED);

                    if (logger.isDebugEnabled()) {
                        for (final Map.Entry<String, Object> entry : resultMap.entrySet()) {
                            logger.debug("{}={}", entry.getKey(), entry.getValue());
                        }
                    }

                    final Map<String, Object> crawlingContext = new HashMap<>();
                    crawlingContext.put("doc", dataMap);
                    resultMap.put("crawlingContext", crawlingContext);
                    for (final Map.Entry<String, String> entry : scriptMap.entrySet()) {
                        final Object convertValue = convertValue(scriptType, entry.getValue(), resultMap);
                        if (convertValue != null) {
                            dataMap.put(entry.getKey(), convertValue);
                        }
                    }

                    crawlerStatsHelper.record(statsKey, StatsAction.EVALUATED);

                    if (logger.isDebugEnabled()) {
                        for (final Map.Entry<String, Object> entry : dataMap.entrySet()) {
                            logger.debug("{}={}", entry.getKey(), entry.getValue());
                        }
                    }

                    if (dataMap.get("url") instanceof final String url) {
                        statsKey.setUrl(url);
                    }

                    callback.store(paramMap, dataMap);
                    crawlerStatsHelper.record(statsKey, StatsAction.FINISHED);
                } catch (final CrawlingAccessException e) {
                    logger.warn("Crawling Access Exception at : {}", dataMap, e);

                    Throwable target = e;
                    if (target instanceof final MultipleCrawlingAccessException ex) {
                        final Throwable[] causes = ex.getCauses();
                        if (causes.length > 0) {
                            target = causes[causes.length - 1];
                        }
                    }

                    String errorName;
                    final Throwable cause = target.getCause();
                    if (cause != null) {
                        errorName = cause.getClass().getCanonicalName();
                    } else {
                        errorName = target.getClass().getCanonicalName();
                    }

                    if (target instanceof final DataStoreCrawlingException dce && dce.aborted()) {
                        throw new ParserStoppedException(document.getId());
                    }

                    final FailureUrlService failureUrlService = ComponentUtil.getComponent(FailureUrlService.class);
                    failureUrlService.store(dataConfig, errorName, document.getId(), target);
                    crawlerStatsHelper.record(statsKey, StatsAction.ACCESS_EXCEPTION);
                } catch (final Throwable t) {
                    logger.warn("Crawling Access Exception at : {}", dataMap, t);
                    final FailureUrlService failureUrlService = ComponentUtil.getComponent(FailureUrlService.class);
                    failureUrlService.store(dataConfig, t.getClass().getCanonicalName(), document.getId(), t);

                    if (readInterval > 0) {
                        sleep(readInterval);
                    }
                    crawlerStatsHelper.record(statsKey, StatsAction.EXCEPTION);
                } finally {
                    crawlerStatsHelper.done(statsKey);
                }

                if (limit > 0 && counter.incrementAndGet() >= limit) {
                    logger.info("Wikipedia crawler is stopped. ({} > {})", counter.get(), limit);
                    throw new ParserStoppedException(document.getId());
                }
            });
        } catch (final ParserStoppedException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Wikipedia crawler is stopped at " + e.getMessage(), e);
            }
        }
    }

    /**
     * Returns the dump location from the parameters.
     *
     * @param paramMap the data store parameters
     * @return the dump URL or local path
     */
    private String getDumpLocation(final DataStoreParams paramMap) {
        return paramMap.getAsString(URL_PARAM, DEFAULT_WIKIPEDIA_URL);
    }

    /**
     * Returns the User-Agent used when downloading the dump.
     *
     * @param paramMap the data store parameters
     * @return the configured User-Agent, the Fess crawler User-Agent when unset, or a fallback
     *         literal when both are blank
     */
    private String getUserAgent(final DataStoreParams paramMap) {
        final String userAgent = paramMap.getAsString(USER_AGENT_PARAM);
        if (StringUtil.isNotBlank(userAgent)) {
            return userAgent;
        }
        final String fessUserAgent = ComponentUtil.getFessConfig().getUserAgentName();
        if (StringUtil.isNotBlank(fessUserAgent)) {
            return fessUserAgent;
        }
        return FALLBACK_USER_AGENT;
    }

    /**
     * Returns which template text and captions the plain-text content of XML pages drops.
     * A CirrusSearch dump carries MediaWiki's own rendered text, so it is not affected.
     *
     * @param paramMap the data store parameters
     * @return the options from {@link #DROP_TEMPLATES_PARAM}, {@link #KEEP_TEMPLATES_PARAM},
     *         {@link #DROP_CAPTIONS_PARAM} and {@link #TEXT_EXTRACTOR_PARAM}
     */
    protected PlainTextOptions getPlainTextOptions(final DataStoreParams paramMap) {
        final String extractor = paramMap.getAsString(TEXT_EXTRACTOR_PARAM);
        final PlainTextOptions options =
                PlainTextOptions.of(paramMap.getAsString(DROP_TEMPLATES_PARAM), paramMap.getAsString(KEEP_TEMPLATES_PARAM),
                        Boolean.parseBoolean(paramMap.getAsString(DROP_CAPTIONS_PARAM, "false").trim()));
        try {
            return options.withExtractor(PlainTextOptions.Extractor.of(extractor));
        } catch (final IllegalArgumentException e) {
            throw new DataStoreException("Unknown " + TEXT_EXTRACTOR_PARAM + ": " + extractor + " (use sweble or regex)", e);
        }
    }

    /** The parameter name selecting which dump format to read. */
    protected static final String SOURCE_PARAM = "source";

    /** The suffix a compressed CirrusSearch chunk carries. */
    private static final String CIRRUS_SUFFIX = ".json.bz2";

    /** The suffix an already-decompressed CirrusSearch chunk carries. */
    private static final String CIRRUS_JSON_SUFFIX = ".json";

    private static final Pattern WIKI_NAME_PATTERN = Pattern.compile("(?:^|[/=])([a-z][a-z-]{1,11})wiki[-_.]");

    /**
     * Returns whether the location looks like a CirrusSearch index dump.
     *
     * @param location the dump URL or path
     * @return true when the location is a CirrusSearch dump
     */
    protected boolean isCirrusLocation(final String location) {
        return location.contains("cirrus_search_index") || location.endsWith(CIRRUS_SUFFIX) || location.endsWith(CIRRUS_JSON_SUFFIX);
    }

    /**
     * Returns the site host the dump belongs to, derived from its file name.
     * <p>
     * Only Wikipedia itself is derived. A sister project such as Wikibooks is left
     * undecided rather than guessed, because a wrong host produces dead links.
     * </p>
     *
     * @param location the dump URL or path
     * @return the host, or null when it cannot be determined
     */
    protected String getSiteHost(final String location) {
        final String language = getSiteLanguage(location);
        if (language == null) {
            return null;
        }
        return language + ".wikipedia.org";
    }

    /**
     * Returns the language code the dump belongs to, derived from its file name.
     *
     * @param location the dump URL or path
     * @return the language code, or null when it cannot be determined
     */
    protected String getSiteLanguage(final String location) {
        final Matcher matcher = WIKI_NAME_PATTERN.matcher(location);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Returns whether the configured source should be read as a CirrusSearch dump.
     *
     * @param paramMap the data store parameters
     * @param dumpLocation the dump URL or path
     * @return true when the {@link #SOURCE_PARAM} parameter forces {@code cirrus}, or is left at
     *         {@code auto} and the location looks like a CirrusSearch dump
     */
    private boolean isCirrusSource(final DataStoreParams paramMap, final String dumpLocation) {
        final String sourceType = paramMap.getAsString(SOURCE_PARAM, "auto");
        return "cirrus".equalsIgnoreCase(sourceType) || "auto".equalsIgnoreCase(sourceType) && isCirrusLocation(dumpLocation);
    }

    /**
     * Builds the source to read the dump from, selecting the format via the {@link #SOURCE_PARAM}
     * parameter.
     * <p>
     * The CirrusSearch branch expands {@code dumpLocation} through a {@link DumpLocationResolver}.
     * The XML branch reads {@code dumpLocation} as a single file, unresolved, and applies
     * {@code totalEntitySizeLimit} to it.
     * </p>
     *
     * @param paramMap the data store parameters
     * @param dumpLocation the dump URL or path
     * @param fetcher the fetcher used to open the dump
     * @param totalEntitySizeLimit the total entity size limit applied to the XML parser
     * @return the source to read the dump from
     */
    protected DumpSource createDumpSource(final DataStoreParams paramMap, final String dumpLocation, final DumpFetcher fetcher,
            final int totalEntitySizeLimit) {
        if (isCirrusSource(paramMap, dumpLocation)) {
            final List<String> locations;
            try {
                locations = new DumpLocationResolver(fetcher).resolve(dumpLocation, List.of(CIRRUS_SUFFIX, CIRRUS_JSON_SUFFIX));
            } catch (final IOException e) {
                throw new DataStoreException("Could not resolve the dump location: " + dumpLocation, e);
            }
            logger.info("Reading {} CirrusSearch file(s).", locations.size());
            return new CirrusIndexDumpSource(locations, fetcher);
        }
        final XmlDumpSource xmlSource = new XmlDumpSource(dumpLocation, fetcher);
        xmlSource.setTotalEntitySizeLimit(totalEntitySizeLimit);
        return xmlSource;
    }

    /**
     * Copies the values a script can reference out of the document.
     *
     * @param resultMap the map the script is evaluated against
     * @param document the page being indexed
     * @param maxDigestLength the maximum length of the digest
     */
    protected void putDocumentValues(final Map<String, Object> resultMap, final WikiDocument document, final int maxDigestLength) {
        final String title = stripTitle(document.getTitle());
        final String content = document.getContent();
        resultMap.put("id", document.getId());
        resultMap.put("title", title);
        resultMap.put("content", content);
        resultMap.put("encodedTitle", encodeTitle(title));
        resultMap.put("digest", StringUtils.abbreviate(content, maxDigestLength));
        resultMap.put("format", document.getFormat());
        resultMap.put("model", document.getModel());
        resultMap.put("timestamp", document.getTimestamp());
        resultMap.put("ns", document.getNamespace());
        resultMap.put("categories", document.getCategories());
        resultMap.put("links", document.getLinks());
        resultMap.put("wikitext", document.getWikitext());
        resultMap.put("redirect", document.isRedirect());
        resultMap.put("redirectTitle", document.getRedirectTitle());
        resultMap.put("stub", document.isStub());
        resultMap.put("disambiguation", document.isDisambiguation());
        resultMap.put("contentLength", content == null ? 0 : content.length());
    }

    /**
     * Copies the site-level values a script can reference.
     * <p>
     * Independent of {@link #putDocumentValues(Map, WikiDocument, int)}: it derives the encoded
     * title from {@code document} itself, so it may be called on its own, before, or after that
     * method.
     * </p>
     *
     * @param resultMap the map the script is evaluated against
     * @param document the page being indexed
     * @param host the site host, or null when it could not be determined
     * @param language the language code derived from the dump, or null
     */
    protected void putSiteValues(final Map<String, Object> resultMap, final WikiDocument document, final String host,
            final String language) {
        final String documentLanguage = document.getLanguage() != null ? document.getLanguage() : language;
        if (documentLanguage != null) {
            resultMap.put("lang", documentLanguage);
        }
        if (host != null) {
            resultMap.put("host", host);
            resultMap.put("site", host);
            resultMap.put("url", "https://" + host + "/wiki/" + encodeTitle(stripTitle(document.getTitle())));
        }
    }

    /**
     * Copies the values only a CirrusSearch dump carries.
     *
     * @param resultMap the map the script is evaluated against
     * @param document the page being indexed
     */
    protected void putCirrusValues(final Map<String, Object> resultMap, final WikiDocument document) {
        resultMap.put("openingText", document.getOpeningText());
        resultMap.put("headings", document.getHeadings());
        resultMap.put("externalLinks", document.getExternalLinks());
        resultMap.put("templates", document.getTemplates());
        resultMap.put("redirects", document.getRedirects());
        resultMap.put("auxiliaryText", document.getAuxiliaryText());
        resultMap.put("weightedTags", document.getWeightedTags());
        resultMap.put("wikibaseItem", document.getWikibaseItem());
        resultMap.put("incomingLinks", document.getIncomingLinks());
        resultMap.put("popularityScore", document.getPopularityScore());
        resultMap.put("revisionId", document.getRevisionId());
    }

    private String stripTitle(final String title) {
        final StringBuilder sb = new StringBuilder();
        sb.append(title);
        while (sb.length() > 0 && (sb.charAt(sb.length() - 1) == '\n' || (sb.charAt(sb.length() - 1) == ' '))) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    /**
     * Encodes a (already-stripped) title for use in an article URL: spaces become underscores,
     * then the result is percent-encoded.
     *
     * @param title the stripped title
     * @return the encoded title
     */
    private String encodeTitle(final String title) {
        return URLEncoder.encode(title.replace(' ', '_'), Constants.CHARSET_UTF_8);
    }
}
