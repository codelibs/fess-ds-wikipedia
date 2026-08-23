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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.core.lang.StringUtil;

/**
 * Expands a dump location setting into the list of files to read.
 * <p>
 * Wikipedia dumps are published in chunks, so a location may name a directory
 * holding many files rather than a single file.
 * </p>
 */
public class DumpLocationResolver {

    private static final Logger logger = LogManager.getLogger(DumpLocationResolver.class);

    private static final Pattern HREF_PATTERN = Pattern.compile("href=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);

    private final DumpFetcher fetcher;

    /**
     * Creates a resolver that reads directory listings with the given fetcher.
     *
     * @param fetcher the fetcher used to read an HTTP directory listing
     */
    public DumpLocationResolver(final DumpFetcher fetcher) {
        this.fetcher = fetcher;
    }

    /**
     * Expands a location setting into the files to read, in the order to read them.
     *
     * @param spec a comma-separated list of files, local directories, and HTTP directory URLs
     * @param requiredSuffix the suffix a file must carry to be picked up from a directory
     * @return the resolved locations
     * @throws IOException if a directory cannot be listed or holds no matching file
     */
    public List<String> resolve(final String spec, final String requiredSuffix) throws IOException {
        final List<String> locations = new ArrayList<>();
        for (final String entry : spec.split(",")) {
            final String trimmed = entry.trim();
            if (StringUtil.isBlank(trimmed)) {
                continue;
            }
            locations.addAll(resolveEntry(trimmed, requiredSuffix));
        }
        if (locations.isEmpty()) {
            throw new IOException("No dump file was found for: " + spec);
        }
        return locations;
    }

    /**
     * Expands one entry of the location setting.
     *
     * @param entry a single file, local directory, or HTTP directory URL
     * @param requiredSuffix the suffix a file must carry to be picked up from a directory
     * @return the resolved locations for this entry
     * @throws IOException if a directory cannot be listed or holds no matching file
     */
    protected List<String> resolveEntry(final String entry, final String requiredSuffix) throws IOException {
        final File local = new File(entry);
        if (local.isDirectory()) {
            return listDirectory(local, requiredSuffix);
        }
        if (entry.endsWith("/")) {
            return listRemoteDirectory(entry, requiredSuffix);
        }
        return List.of(entry);
    }

    /**
     * Lists the matching files of a local directory, sorted by name.
     *
     * @param directory the directory to list
     * @param requiredSuffix the suffix a file must carry
     * @return the absolute paths of the matching files
     * @throws IOException if the directory holds no matching file
     */
    protected List<String> listDirectory(final File directory, final String requiredSuffix) throws IOException {
        final File[] files = directory.listFiles();
        if (files == null) {
            throw new IOException("Could not list the directory: " + directory.getAbsolutePath());
        }
        final List<String> locations = Arrays.stream(files)
                .filter(File::isFile)
                .filter(f -> f.getName().endsWith(requiredSuffix))
                .map(File::getAbsolutePath)
                .sorted()
                .toList();
        if (locations.isEmpty()) {
            throw new IOException("No " + requiredSuffix + " file in the directory: " + directory.getAbsolutePath());
        }
        return locations;
    }

    /**
     * Lists the matching files of an HTTP directory index, sorted by name.
     *
     * @param baseUrl the directory URL, ending with a slash
     * @param requiredSuffix the suffix a file must carry
     * @return the absolute URLs of the matching files
     * @throws IOException if the listing cannot be read or holds no matching file
     */
    protected List<String> listRemoteDirectory(final String baseUrl, final String requiredSuffix) throws IOException {
        final List<String> names = new ArrayList<>();
        try (InputStream in = fetcher.open(baseUrl);
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                final Matcher matcher = HREF_PATTERN.matcher(line);
                while (matcher.find()) {
                    final String href = matcher.group(1);
                    if (href.endsWith(requiredSuffix) && href.indexOf('/') < 0) {
                        names.add(href);
                    }
                }
            }
        }
        if (names.isEmpty()) {
            throw new IOException("No " + requiredSuffix + " file in the listing of: " + baseUrl);
        }
        names.sort(Comparator.naturalOrder());
        if (logger.isDebugEnabled()) {
            logger.debug("Resolved {} file(s) from {}", names.size(), baseUrl);
        }
        return names.stream().map(name -> baseUrl + name).toList();
    }
}
