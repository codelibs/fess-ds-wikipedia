Wikipedia Data Store for Fess
[![Java CI with Maven](https://github.com/codelibs/fess-ds-wikipedia/actions/workflows/maven.yml/badge.svg)](https://github.com/codelibs/fess-ds-wikipedia/actions/workflows/maven.yml)
==========================

## Overview

Wikipedia Data Store crawls Wikipedia pages from a dump file.

## Download

See [Maven Repository](https://repo1.maven.org/maven2/org/codelibs/fess/fess-ds-wikipedia/).

## Installation

See [Plugin](https://fess.codelibs.org/14.2/admin/plugin-guide.html) of Administration guide.

### Crawling Setting

```
# Parameter
url=http://download.wikimedia.org/jawiki/latest/jawiki-latest-pages-articles.xml.bz2
limit=10000

# Script
lang="ja"
filetype=format
filename=title
url="https://ja.wikipedia.org/wiki/" + encodedTitle
host="ja.wikipedia.org"
site="ja.wikipedia.org"
title=title
content=content
digest=digest
anchor=
content_length=content.length()
last_modified=timestamp
timestamp=timestamp
```

### Choosing a dump

The data store reads two kinds of dumps, picked by the `source` parameter
(`auto`, the default, picks by the location):

- `source=cirrus`: a CirrusSearch index dump, which Wikimedia publishes for its
  wikis under <https://dumps.wikimedia.org/other/cirrus_search_index/>. It
  carries the text MediaWiki itself rendered, with templates expanded, so no
  wikitext markup can reach the index. Prefer it for Wikipedia and other
  Wikimedia wikis.
- `source=xml`: a MediaWiki XML export (`*-pages-articles.xml.bz2`, or the
  `Special:Export` / `dumpBackup.php` output of any MediaWiki). It carries
  wikitext, which the data store converts to plain text as described below.

### Plain text of XML dumps

The wikitext of `source=xml` dumps is converted with the
[Sweble](https://github.com/rzo1/sweble-wikitext) wikitext parser. The
namespace names are read from the export's `<siteinfo>`, so images and category
statements are recognized in any language (`[[ファイル:...]]`,
`[[Kategorie:...]]`) and in wikis with their own namespace names. A page Sweble
fails on is converted with regular expressions instead.

```
# sweble (default) or regex; regex is several times faster but leaves nested
# templates, tables and multi-line markup behind
text_extractor=sweble
```

### Template text and captions (XML dumps)

For `source=xml` dumps, the `content` field is plain text converted from the
wikitext. By default it keeps the argument text of every template (without
parameter names) and every image and gallery caption, so that text can be
found. Three optional parameters filter it:

```
# Drop these templates' text, also when nested ("*" drops all templates)
drop_templates=Cite web,Cite news,Use dmy dates
# Keep these templates although drop_templates matches them
# (drop_templates=* with keep_templates=Note,Warning keeps only those two)
keep_templates=
# Drop image and gallery captions
drop_captions=false
```

Template names match the way MediaWiki resolves them: the first letter is
case-insensitive, `_` equals a space, and the `Template:` prefix is optional.
Templates aren't expanded, so a kept template contributes its argument text,
not what MediaWiki would render. CirrusSearch dumps (`source=cirrus`) already
contain MediaWiki's rendered text and aren't affected.
