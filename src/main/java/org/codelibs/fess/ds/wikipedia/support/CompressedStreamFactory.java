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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Opens a possibly compressed dump stream, detecting the format from its content.
 * <p>
 * Wikipedia dumps are distributed as bzip2, and the multistream variants are several
 * bzip2 streams concatenated into one file, so decompression must continue past the
 * end of the first stream.
 * </p>
 */
public final class CompressedStreamFactory {

    private static final Logger logger = LogManager.getLogger(CompressedStreamFactory.class);

    private CompressedStreamFactory() {
        // nothing
    }

    /**
     * Wraps the given stream in a decompressor chosen by inspecting its first bytes.
     *
     * @param in the raw stream; it is buffered internally when it is not already a
     *            {@link BufferedInputStream}
     * @return the decompressed stream, or the buffered input itself when it is not compressed
     * @throws IOException if the compressed stream cannot be opened, or the signature could not
     *             be read because of a genuine I/O error
     */
    public static InputStream open(final InputStream in) throws IOException {
        final BufferedInputStream buffered = in instanceof final BufferedInputStream b ? b : new BufferedInputStream(in);
        final String name;
        try {
            name = CompressorStreamFactory.detect(buffered);
        } catch (final CompressorException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof final IOException ioe) {
                // A genuine I/O failure (e.g. a dropped connection) while reading the signature,
                // not "no compressor matched". Surfacing it as the underlying IOException keeps
                // the real cause visible instead of a downstream "Premature end of file" from the
                // XML parser reading a stream that was never actually valid content.
                throw ioe;
            }
            // No compressor matched the signature. A plain XML dump is a valid input.
            logger.debug("Could not detect a compressor for the stream; treating it as uncompressed.", e);
            return buffered;
        }
        try {
            // decompressConcatenated is passed as the literal `true` 3rd argument here, which is
            // what actually takes effect. The `new CompressorStreamFactory(true)` instance's own
            // decompressConcatenated flag is never consulted by this 3-arg overload; it is kept
            // only for readability at the call site, not because it does anything.
            return new CompressorStreamFactory(true).createCompressorInputStream(name, buffered, true);
        } catch (final CompressorException e) {
            throw new IOException("Failed to open the " + name + " stream.", e);
        }
    }
}
