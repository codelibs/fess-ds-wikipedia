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

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.sweble.wikitext.engine.EngineException;
import org.sweble.wikitext.engine.PageId;
import org.sweble.wikitext.engine.PageTitle;
import org.sweble.wikitext.engine.WtEngineImpl;
import org.sweble.wikitext.engine.config.WikiConfig;
import org.sweble.wikitext.engine.nodes.EngNowiki;
import org.sweble.wikitext.engine.nodes.EngPage;
import org.sweble.wikitext.engine.nodes.EngProcessedPage;
import org.sweble.wikitext.engine.output.HtmlSanitizer;
import org.sweble.wikitext.parser.nodes.WtDefinitionListDef;
import org.sweble.wikitext.parser.nodes.WtDefinitionListTerm;
import org.sweble.wikitext.parser.nodes.WtExternalLink;
import org.sweble.wikitext.parser.nodes.WtIgnored;
import org.sweble.wikitext.parser.nodes.WtIllegalCodePoint;
import org.sweble.wikitext.parser.nodes.WtImageLink;
import org.sweble.wikitext.parser.nodes.WtInternalLink;
import org.sweble.wikitext.parser.nodes.WtLctRuleConv;
import org.sweble.wikitext.parser.nodes.WtLctVarConv;
import org.sweble.wikitext.parser.nodes.WtListItem;
import org.sweble.wikitext.parser.nodes.WtNewline;
import org.sweble.wikitext.parser.nodes.WtNode;
import org.sweble.wikitext.parser.nodes.WtPageSwitch;
import org.sweble.wikitext.parser.nodes.WtParagraph;
import org.sweble.wikitext.parser.nodes.WtRedirect;
import org.sweble.wikitext.parser.nodes.WtSection;
import org.sweble.wikitext.parser.nodes.WtSemiPreLine;
import org.sweble.wikitext.parser.nodes.WtSignature;
import org.sweble.wikitext.parser.nodes.WtTableCaption;
import org.sweble.wikitext.parser.nodes.WtTableCell;
import org.sweble.wikitext.parser.nodes.WtTableHeader;
import org.sweble.wikitext.parser.nodes.WtTableRow;
import org.sweble.wikitext.parser.nodes.WtTagExtension;
import org.sweble.wikitext.parser.nodes.WtTemplate;
import org.sweble.wikitext.parser.nodes.WtTemplateArgument;
import org.sweble.wikitext.parser.nodes.WtTemplateParameter;
import org.sweble.wikitext.parser.nodes.WtText;
import org.sweble.wikitext.parser.nodes.WtUrl;
import org.sweble.wikitext.parser.nodes.WtWhitespace;
import org.sweble.wikitext.parser.nodes.WtXmlAttributes;
import org.sweble.wikitext.parser.nodes.WtXmlCharRef;
import org.sweble.wikitext.parser.nodes.WtXmlComment;
import org.sweble.wikitext.parser.nodes.WtXmlElement;
import org.sweble.wikitext.parser.nodes.WtXmlEmptyTag;
import org.sweble.wikitext.parser.nodes.WtXmlEndTag;
import org.sweble.wikitext.parser.nodes.WtXmlEntityRef;
import org.sweble.wikitext.parser.nodes.WtXmlStartTag;
import org.sweble.wikitext.parser.parser.LinkTargetException;
import org.sweble.wikitext.parser.utils.WtRtDataPrinter;

import de.fau.cs.osr.ptk.common.AstVisitor;

/**
 * Converts a Sweble wikitext AST into plain text for indexing.
 * <p>
 * Adapted from {@code org.sweble.wikitext.example.TextConverter} (swc-example-basic,
 * Copyright 2011 The Open Source Research Group, University of Erlangen-Nürnberg,
 * Apache License 2.0). Unlike the example, no markup is emitted for bold/italics,
 * headings or external links, and lines are not wrapped: only readable text survives.
 * Templates, references, images, categories and comments are dropped, matching the
 * previous regex-based {@link WikiTextParser#getPlainText()}.
 * <p>
 * Tag extensions ({@code <poem>}, {@code <gallery>}, ...) arrive with their body unparsed.
 * Code bodies are kept as literal text, prose bodies are parsed as wikitext, and every other
 * extension is dropped, so an extension's own syntax never reaches the output.
 * <p>
 * The argument text of templates and the captions of images and galleries are kept, since
 * text that is dropped can't be found; {@link PlainTextOptions} can drop them.
 */
public class SwebleTextConverter extends AstVisitor<WtNode> {

    private static final Pattern WS = Pattern.compile("\\s+");

    private static final String CLOSING_PUNCTUATION = ",.;:!?)]}%»”’";

    private static final String OPENING_PUNCTUATION = "([{«„“‘";

    /** Tag extensions whose body is literal text (code), kept as is. */
    private static final Set<String> LITERAL_TAG_EXTENSIONS = Set.of("pre", "source", "syntaxhighlight");

    /**
     * Tag extensions whose body is wikitext prose, parsed and converted like the page itself.
     * Any tag extension in neither set (gallery, imagemap, inputbox, ref, math, templatestyles,
     * graph, ...) carries markup, data or code for another renderer and is dropped.
     */
    private static final Set<String> WIKITEXT_TAG_EXTENSIONS = Set.of("poem", "indicator", "langconvert");

    private static final Set<String> BLOCK_ELEMENTS = Set.of("blockquote", "caption", "center", "dd", "div", "dt", "h1", "h2", "h3", "h4",
            "h5", "h6", "li", "p", "tr", "table", "ul", "ol", "dl", "pre");

    private final WikiConfig config;

    private final PlainTextOptions options;

    private StringBuilder sb;

    private boolean needNewline;

    private boolean needSpace;

    /**
     * A space requested by removed or replaced markup (a template, a dropped tag extension): it
     * separates words but is left out before closing and after opening punctuation, so
     * "Deutschland}};" doesn't become "Deutschland ;" nor "({{lang|de|Haus}}" "( de Haus".
     */
    private boolean softSpace;

    /** Paragraph breaks inside table cells would split a row over several lines. */
    private int cellDepth;

    public SwebleTextConverter(final WikiConfig config) {
        this(config, PlainTextOptions.DEFAULT);
    }

    public SwebleTextConverter(final WikiConfig config, final PlainTextOptions options) {
        this.config = config;
        this.options = options;
    }

    /**
     * Parses wikitext and converts it to plain text.
     *
     * @param config the wiki configuration
     * @param options which template text and captions to drop
     * @param wikiText the wikitext to convert
     * @return the plain text
     * @throws EngineException if Sweble fails to process the text
     * @throws LinkTargetException if the placeholder page title is invalid
     */
    public static String toPlainText(final WikiConfig config, final PlainTextOptions options, final String wikiText)
            throws EngineException, LinkTargetException {
        final PageId pageId = new PageId(PageTitle.make(config, "Page"), -1);
        final EngProcessedPage page = new WtEngineImpl(config).postprocess(pageId, wikiText, null);
        return (String) new SwebleTextConverter(config, options).go(page.getPage());
    }

    @Override
    protected WtNode before(final WtNode node) {
        sb = new StringBuilder();
        needNewline = false;
        needSpace = false;
        softSpace = false;
        cellDepth = 0;
        return super.before(node);
    }

    @Override
    protected Object after(final WtNode node, final Object result) {
        return sb.toString();
    }

    // =========================================================================

    public void visit(final WtNode n) {
        iterate(n);
    }

    public void visit(final EngPage p) {
        iterate(p);
    }

    public void visit(final WtText text) {
        write(text.getContent());
    }

    public void visit(final EngNowiki nowiki) {
        write(nowiki.getContent());
    }

    public void visit(final WtWhitespace w) {
        needSpace = true;
    }

    public void visit(final WtNewline n) {
        needSpace = true;
    }

    public void visit(final WtParagraph p) {
        if (cellDepth > 0) {
            needSpace = true;
            iterate(p);
            needSpace = true;
            return;
        }
        newline();
        iterate(p);
        newline();
    }

    public void visit(final WtSection s) {
        newline();
        iterate(s.getHeading());
        newline();
        iterate(s.getBody());
    }

    public void visit(final WtListItem item) {
        newline();
        iterate(item);
        newline();
    }

    public void visit(final WtDefinitionListTerm term) {
        newline();
        iterate(term);
        newline();
    }

    public void visit(final WtDefinitionListDef def) {
        newline();
        iterate(def);
        newline();
    }

    public void visit(final WtSemiPreLine n) {
        iterate(n);
        newline();
    }

    public void visit(final WtTableRow row) {
        newline();
        iterate(row.getBody());
        newline();
    }

    public void visit(final WtTableCaption caption) {
        newline();
        iterate(caption.getBody());
        newline();
    }

    public void visit(final WtTableHeader header) {
        cell(header.getBody());
    }

    public void visit(final WtTableCell cell) {
        cell(cell.getBody());
    }

    public void visit(final WtXmlCharRef cr) {
        final int codePoint = cr.getCodePoint();
        if (HtmlSanitizer.isValidCharReference(codePoint)) {
            write(new String(Character.toChars(codePoint)));
        }
    }

    public void visit(final WtXmlEntityRef er) {
        final String ch = er.getResolved();
        write(ch != null ? ch : "&" + er.getName() + ";");
    }

    public void visit(final WtUrl url) {
        // a bare URL is rendered as itself, and people search for URLs and host names
        final String protocol = url.getProtocol();
        write(protocol == null || protocol.isEmpty() ? url.getPath() : protocol + ":" + url.getPath());
    }

    public void visit(final WtExternalLink link) {
        // "[http://example.com label]" -> "label"; "[http://example.com]" -> the URL, so it can be found
        if (link.hasTitle()) {
            iterate(link.getTitle());
        } else {
            dispatch(link.getTarget());
        }
    }

    public void visit(final WtInternalLink link) {
        try {
            if (link.getTarget().isResolved()) {
                final PageTitle page = PageTitle.make(config, link.getTarget().getAsString());
                // "[[Category:Foo]]" is a category statement and "[[de:Foo]]" an interlanguage link:
                // neither is rendered. "[[:Category:Foo]]" is an ordinary link.
                if (!page.hasInitialColon() && (page.getNamespace().equals(config.getNamespace("Category")) || page.isInterwiki())) {
                    return;
                }
            }
        } catch (final LinkTargetException e) {
            // not a valid title; render it like MediaWiki would
        }

        write(link.getPrefix());
        if (!link.hasTitle()) {
            if (link.getTarget().isResolved()) {
                String target = link.getTarget().getAsString();
                if (target.startsWith(":")) {
                    target = target.substring(1);
                }
                write(target);
            } else {
                iterate(link.getTarget());
            }
        } else {
            iterate(link.getTitle());
        }
        write(link.getPostfix());
    }

    public void visit(final WtLctVarConv n) {
        iterate(n.getText());
    }

    public void visit(final WtXmlElement e) {
        final String name = e.getName().toLowerCase(Locale.ROOT);
        if ("br".equals(name) || BLOCK_ELEMENTS.contains(name)) {
            newline();
            iterate(e.getBody());
            newline();
        } else if ("td".equals(name) || "th".equals(name)) {
            cell(e.getBody());
        } else {
            iterate(e.getBody());
        }
    }

    public void visit(final WtTagExtension n) {
        final String name = n.getName().trim().toLowerCase(Locale.ROOT);
        final boolean keptGallery = "gallery".equals(name) && options.keepsCaptions();
        if (!n.hasBody() || !keptGallery && !LITERAL_TAG_EXTENSIONS.contains(name) && !WIKITEXT_TAG_EXTENSIONS.contains(name)) {
            // keep the neighbours apart, as for a dropped template
            softSpace = true;
            return;
        }
        final String body = n.getBody().getContent();
        newline();
        if ("gallery".equals(name)) {
            galleryCaptions(body);
        } else if (LITERAL_TAG_EXTENSIONS.contains(name)) {
            write(body);
        } else {
            // MediaWiki renders every line break of a <poem> as <br>
            writeWikitext("poem".equals(name) ? body.replace("\n", "<br />\n") : body, true);
        }
        newline();
    }

    // =========================================================================
    // Stuff we want to hide

    public void visit(final WtRedirect n) {
    }

    public void visit(final WtSignature n) {
    }

    public void visit(final WtImageLink n) {
        // "[[File:x.jpg|thumb|alt=...|Caption]]": only the caption is prose; options and alt text are not
        if (options.keepsCaptions() && n.hasTitle()) {
            newline();
            iterate(n.getTitle());
            newline();
        }
    }

    public void visit(final WtIllegalCodePoint n) {
    }

    public void visit(final WtXmlComment n) {
    }

    public void visit(final WtTemplate n) {
        // an inline template such as {{snd}} usually renders as separator text; don't glue its neighbours
        softSpace = true;
        if (!n.getName().isResolved() || !options.keepsTemplate(n.getName().getAsString())) {
            return;
        }
        // A template on its own line (infobox, maintenance tag, short description) is a block of
        // its own; one inside a sentence ({{lang|de|Haus}}) continues the line.
        final boolean block = n.isPrecededByNewline() || WtRtDataPrinter.print(n).indexOf('\n') >= 0;
        if (block) {
            newline();
        }
        // Argument values are unparsed wikitext; parameter names ("title=") are not prose.
        boolean first = true;
        for (final WtNode arg : n.getArgs()) {
            if (arg instanceof final WtTemplateArgument argument) {
                // the first argument joins the preceding text like the template itself; later ones are separate words
                if (first) {
                    softSpace = true;
                } else {
                    needSpace = true;
                }
                first = false;
                writeWikitext(WtRtDataPrinter.print(argument.getValue()), false);
            }
        }
        softSpace = true;
        if (block) {
            newline();
        }
    }

    public void visit(final WtTemplateArgument n) {
    }

    public void visit(final WtTemplateParameter n) {
    }

    public void visit(final WtPageSwitch n) {
    }

    public void visit(final WtLctRuleConv n) {
    }

    public void visit(final WtIgnored n) {
    }

    public void visit(final WtXmlAttributes n) {
    }

    public void visit(final WtXmlStartTag n) {
    }

    public void visit(final WtXmlEndTag n) {
    }

    public void visit(final WtXmlEmptyTag n) {
    }

    // =========================================================================

    private void cell(final WtNode body) {
        needSpace = true;
        cellDepth++;
        iterate(body);
        cellDepth--;
        needSpace = true;
    }

    /**
     * Writes each gallery line's caption. A line is an image link without the brackets
     * ("File:x.jpg|alt=...|Caption"), and the "File:" prefix is optional in a gallery.
     */
    private void galleryCaptions(final String body) {
        for (final String line : body.split("\n")) {
            final String entry = line.trim();
            if (entry.isEmpty()) {
                continue;
            }
            final int pipe = entry.indexOf('|');
            final String target = pipe < 0 ? entry : entry.substring(0, pipe);
            writeWikitext("[[" + (target.contains(":") ? "" : "File:") + entry + "]]", true);
        }
    }

    /**
     * Parses wikitext and writes its plain text, with the same options.
     *
     * @param wikiText the wikitext
     * @param block true to put each line of the result on its own line, false to continue the
     *        current line (for inline text such as a template argument)
     */
    private void writeWikitext(final String wikiText, final boolean block) {
        final String text;
        try {
            text = toPlainText(config, options, wikiText);
        } catch (final EngineException | LinkTargetException e) {
            // unparsable body: drop it rather than leak its markup
            return;
        }
        final String[] lines = text.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (i > 0 || block) {
                newline();
            }
            write(lines[i]);
        }
        if (block) {
            newline();
        }
    }

    private void newline() {
        if (sb.length() > 0) {
            needNewline = true;
        }
    }

    private void write(final String s) {
        if (s == null || s.isEmpty()) {
            return;
        }
        final boolean leadingWs = Character.isWhitespace(s.charAt(0));
        final boolean trailingWs = Character.isWhitespace(s.charAt(s.length() - 1));
        final String collapsed = WS.matcher(s.trim()).replaceAll(" ");
        if (collapsed.isEmpty()) {
            needSpace = true;
            return;
        }
        if (sb.length() > 0) {
            if (needNewline) {
                sb.append('\n');
            } else if (needSpace || leadingWs || softSpace && CLOSING_PUNCTUATION.indexOf(collapsed.charAt(0)) < 0
                    && OPENING_PUNCTUATION.indexOf(sb.charAt(sb.length() - 1)) < 0) {
                sb.append(' ');
            }
        }
        needNewline = false;
        needSpace = trailingWs;
        softSpace = false;
        sb.append(collapsed);
    }
}
