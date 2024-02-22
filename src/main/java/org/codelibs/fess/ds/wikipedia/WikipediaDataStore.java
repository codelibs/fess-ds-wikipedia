/*
 * Copyright 2012-2024 CodeLibs Project and the Others.
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

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.codelibs.fess.Constants;
import org.codelibs.fess.app.service.FailureUrlService;
import org.codelibs.fess.crawler.exception.CrawlingAccessException;
import org.codelibs.fess.crawler.exception.MultipleCrawlingAccessException;
import org.codelibs.fess.ds.AbstractDataStore;
import org.codelibs.fess.ds.callback.IndexUpdateCallback;
import org.codelibs.fess.ds.wikipedia.exception.ParserStoppedException;
import org.codelibs.fess.ds.wikipedia.support.WikiXMLSAXParser;
import org.codelibs.fess.entity.DataStoreParams;
import org.codelibs.fess.es.config.exentity.DataConfig;
import org.codelibs.fess.exception.DataStoreCrawlingException;
import org.codelibs.fess.exception.DataStoreException;
import org.codelibs.fess.helper.CrawlerStatsHelper;
import org.codelibs.fess.helper.CrawlerStatsHelper.StatsAction;
import org.codelibs.fess.helper.CrawlerStatsHelper.StatsKeyObject;
import org.codelibs.fess.util.ComponentUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WikipediaDataStore extends AbstractDataStore {

    private static final Logger logger = LoggerFactory.getLogger(WikipediaDataStore.class);

    private static final String DEFAULT_WIKIPEDIA_URL = "http://download.wikimedia.org/enwiki/latest/enwiki-latest-pages-articles.xml.bz2";

    @Override
    protected String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    protected void storeData(final DataConfig dataConfig, final IndexUpdateCallback callback, final DataStoreParams paramMap,
            final Map<String, String> scriptMap, final Map<String, Object> defaultDataMap) {
        final CrawlerStatsHelper crawlerStatsHelper = ComponentUtil.getCrawlerStatsHelper();

        final long readInterval = getReadInterval(paramMap);
        final URL wikipediaUrl = getWikipediaUrl(paramMap);
        final int limit = Integer.parseInt(paramMap.getAsString("limit", "0"));
        final int totalEntitySizeLimit = Integer.parseInt(paramMap.getAsString("total_entity_size_limit", "100000000"));
        final int maxDigestLength = Integer.parseInt(paramMap.getAsString("max_digest_length", "100"));
        final String scriptType = getScriptType(paramMap);
        logger.info("url: {}", wikipediaUrl);
        final AtomicInteger counter = new AtomicInteger();
        final WikiXMLSAXParser xmlParser = new WikiXMLSAXParser(wikipediaUrl);
        xmlParser.setTotalEntitySizeLimit(totalEntitySizeLimit);
        xmlParser.setPageCallback(page -> {
            final StatsKeyObject statsKey = new StatsKeyObject(dataConfig.getId() + "#" + page.getId());
            paramMap.put(Constants.CRAWLER_STATS_KEY, statsKey);
            final Map<String, Object> dataMap = new HashMap<>(defaultDataMap);
            final Map<String, Object> resultMap = new LinkedHashMap<>();
            try {
                crawlerStatsHelper.begin(statsKey);
                resultMap.putAll(paramMap.asMap());

                final String title = stripTitle(page.getTitle());
                final String content = page.getText();
                resultMap.put("id", page.getId());
                resultMap.put("title", title);
                resultMap.put("content", content);
                resultMap.put("encodedTitle", URLEncoder.encode(title, Constants.UTF_8));
                resultMap.put("digest", StringUtils.abbreviate(content, maxDigestLength));
                resultMap.put("format", page.getFormat());
                resultMap.put("model", page.getModel());
                resultMap.put("timestamp", page.getTimestamp());

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
                    throw new ParserStoppedException(page.getId());
                }

                final FailureUrlService failureUrlService = ComponentUtil.getComponent(FailureUrlService.class);
                failureUrlService.store(dataConfig, errorName, page.getId(), target);
                crawlerStatsHelper.record(statsKey, StatsAction.ACCESS_EXCEPTION);
            } catch (final Throwable t) {
                logger.warn("Crawling Access Exception at : {}", dataMap, t);
                final FailureUrlService failureUrlService = ComponentUtil.getComponent(FailureUrlService.class);
                failureUrlService.store(dataConfig, t.getClass().getCanonicalName(), page.getId(), t);

                if (readInterval > 0) {
                    sleep(readInterval);
                }
                crawlerStatsHelper.record(statsKey, StatsAction.EXCEPTION);
            } finally {
                crawlerStatsHelper.done(statsKey);
            }

            if (limit > 0 && counter.incrementAndGet() >= limit) {
                logger.info("Wikipedia crawler is stopped. ({} > {})", counter.get(), limit);
                throw new ParserStoppedException(page.getId());
            }
        });
        try {
            xmlParser.parse();
        } catch (final ParserStoppedException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Wikipedia crawler is stopped at " + e.getMessage(), e);
            }
        }
    }

    private URL getWikipediaUrl(final DataStoreParams paramMap) {
        try {
            return new URL(paramMap.getAsString("url", DEFAULT_WIKIPEDIA_URL));
        } catch (final MalformedURLException e) {
            throw new DataStoreException("Could not parse Wikipedia URL.", e);
        }
    }

    private String stripTitle(final String title) {
        final StringBuilder sb = new StringBuilder();
        sb.append(title);
        while (sb.length() > 0 && (sb.charAt(sb.length() - 1) == '\n' || (sb.charAt(sb.length() - 1) == ' '))) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }
}
