Wikipedia Data Store for Fess
[![Java CI with Maven](https://github.com/codelibs/fess-ds-wikipedia/actions/workflows/maven.yml/badge.svg)](https://github.com/codelibs/fess-ds-wikipedia/actions/workflows/maven.yml)
==========================

## Overview

Wikipedia Data Store crawls Wikipedia pages from a dump file.

## Download

See [Maven Repository](http://central.maven.org/maven2/org/codelibs/fess/fess-ds-wikipedia/).

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

