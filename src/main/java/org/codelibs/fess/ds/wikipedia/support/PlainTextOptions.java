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

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controls which template text and captions {@link WikiTextParser#getPlainText(PlainTextOptions)}
 * drops.
 * <p>
 * Text that is dropped can't be found, so by default the argument text of every template and
 * every image and gallery caption is kept. Installations filter out what they consider noise
 * through the {@code drop_templates}, {@code keep_templates} and {@code drop_captions} handler
 * parameters.
 * </p>
 */
public final class PlainTextOptions {

    /** Keeps the text of all templates and captions. */
    public static final PlainTextOptions DEFAULT = new PlainTextOptions(Set.of(), false, Set.of(), false);

    /** The {@code drop_templates} value that drops every template not listed in {@code keep_templates}. */
    public static final String ALL = "*";

    private static final String TEMPLATE_NAMESPACE = "template:";

    private final Set<String> droppedTemplates;

    private final boolean dropAllTemplates;

    private final Set<String> keptTemplates;

    private final boolean dropCaptions;

    private PlainTextOptions(final Set<String> droppedTemplates, final boolean dropAllTemplates, final Set<String> keptTemplates,
            final boolean dropCaptions) {
        this.droppedTemplates = droppedTemplates;
        this.dropAllTemplates = dropAllTemplates;
        this.keptTemplates = keptTemplates;
        this.dropCaptions = dropCaptions;
    }

    /**
     * Creates options from the handler parameter values.
     *
     * @param dropTemplates comma-separated names of templates whose text is dropped, such as
     *        {@code Cite web,Cite news}, or {@code *} for all; null or blank drops none
     * @param keepTemplates comma-separated names of templates whose text is kept even though
     *        {@code dropTemplates} matches them, which turns {@code *} into an allowlist; null
     *        or blank lists none
     * @param dropCaptions whether image and gallery captions are dropped
     * @return the options
     */
    public static PlainTextOptions of(final String dropTemplates, final String keepTemplates, final boolean dropCaptions) {
        final Set<String> dropped = parseNames(dropTemplates);
        final boolean dropAll = dropped.contains(ALL);
        return new PlainTextOptions(dropAll ? Set.of() : dropped, dropAll, parseNames(keepTemplates), dropCaptions);
    }

    private static Set<String> parseNames(final String names) {
        if (names == null) {
            return Set.of();
        }
        return Arrays.stream(names.split(","))
                .map(name -> ALL.equals(name.trim()) ? ALL : normalizeTemplateName(name))
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Returns whether the argument text of the named template is kept.
     *
     * @param templateName the template name as written in the wikitext
     * @return true if the template's argument text is kept
     */
    public boolean keepsTemplate(final String templateName) {
        final String name = normalizeTemplateName(templateName);
        if (keptTemplates.contains(name)) {
            return true;
        }
        return !dropAllTemplates && !droppedTemplates.contains(name);
    }

    /**
     * Returns whether image and gallery captions are kept.
     *
     * @return true if captions are kept
     */
    public boolean keepsCaptions() {
        return !dropCaptions;
    }

    /**
     * Normalizes a template name the way MediaWiki resolves it: the {@code Template:} prefix is
     * optional, underscores and runs of spaces are one space, and the first letter is
     * case-insensitive.
     *
     * @param name the template name
     * @return the normalized name
     */
    static String normalizeTemplateName(final String name) {
        String n = name.replace('_', ' ').trim().replaceAll("\\s+", " ");
        if (n.toLowerCase(Locale.ROOT).startsWith(TEMPLATE_NAMESPACE)) {
            n = n.substring(TEMPLATE_NAMESPACE.length()).trim();
        }
        if (n.isEmpty()) {
            return n;
        }
        final int first = n.codePointAt(0);
        return new StringBuilder().appendCodePoint(Character.toUpperCase(first)).append(n.substring(Character.charCount(first))).toString();
    }
}
