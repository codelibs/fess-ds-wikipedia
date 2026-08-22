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

/**
 * Opens a possibly compressed dump stream, detecting the format from its content.
 * <p>
 * Wikipedia dumps are distributed as bzip2, and the multistream variants are several
 * bzip2 streams concatenated into one file, so decompression must continue past the
 * end of the first stream.
 * </p>
 */
public final class CompressedStreamFactory {

    private CompressedStreamFactory() {
        // nothing
    }

    /**
     * Wraps the given stream in a decompressor chosen by inspecting its first bytes.
     *
     * @param in the raw stream; it is buffered internally when it does not support mark
     * @return the decompressed stream, or the buffered input itself when it is not compressed
     * @throws IOException if the compressed stream cannot be opened
     */
    public static InputStream open(final InputStream in) throws IOException {
        final BufferedInputStream buffered = in instanceof final BufferedInputStream b ? b : new BufferedInputStream(in);
        final String name;
        try {
            name = CompressorStreamFactory.detect(buffered);
        } catch (final CompressorException e) {
            // Not a compressed stream. A plain XML dump is a valid input.
            return buffered;
        }
        try {
            // The boolean is decompressConcatenated. Without it only the first stream of a
            // multistream dump is read, and the parse ends silently in the middle of the XML.
            return new CompressorStreamFactory(true).createCompressorInputStream(name, buffered, true);
        } catch (final CompressorException e) {
            throw new IOException("Failed to open the " + name + " stream.", e);
        }
    }
}
