/*
 * Copyright (c) 2024, Red Hat, Inc.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package jdk.tools.jlink.internal.runtimelink;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Generates a delta between packaged modules (as an ImageResource) and an
 * optimized jimage (lib/modules) as an ImageResource. The result can be
 * serialized to a file using {@link ResourceDiff}.
 */
public class JimageDiffGenerator {

    /**
     * A resource used for linking. Either packaged modules or
     * packaged modules transformed to an optimized run-time image by applying
     * the jlink plug-in pipeline. The canonical source, the packaged modules,
     * are being used to devise the delta to the transformed run-time image. The
     * delta can can then be used for jlink input together *with* a prepared
     * run-time image.
     */
    @SuppressWarnings("try")
    public interface ImageResource extends AutoCloseable {
        public List<String> getEntries();
        public byte[] getResourceBytes(String name);
        public InputStream getResource(String name);
    }

    /**
     * Produce a difference between packaged modules' resources (base) and the
     * result of all plug-ins being applied on those resources (image).
     *
     * @param base
     *            The ImageResource view of unmodified resources coming from
     *            packaged modules.
     * @param image
     *            The ImageResource view of the jlink plug-in pipeline having
     *            been applied to the resources in base.
     * @return The list of resource differences across all modules.
     */
    public List<ResourceDiff> generateDiff(ImageResource base, ImageResource image) throws Exception {
        List<String> baseResources;
        Set<String> resources = new HashSet<>();
        List<ResourceDiff> diffs = new ArrayList<>();
        try (base; image) {
            resources.addAll(image.getEntries());
            baseResources = base.getEntries();
            for (String item: baseResources) {
                // First check that every item in the base image exist in
                // the optimized image as well. If it does not, it's a removed
                // item in the optimized image.
                if (!resources.remove(item)) {
                    // keep track of original bytes for removed item in the
                    // optimized image, since we need to restore them for the
                    // runtime image link
                    ResourceDiff.Builder builder = new ResourceDiff.Builder();
                    ResourceDiff diff = builder.setKind(ResourceDiff.Kind.REMOVED)
                           .setName(item)
                           .setResourceBytes(base.getResourceBytes(item))
                           .build();
                    diffs.add(diff);
                    continue;
                }
                // Verify resource bytes are equal if present in both images
                if (!compareStreams(base.getResource(item), image.getResource(item))) {
                    // keep track of original bytes (non-optimized)
                    ResourceDiff.Builder builder = new ResourceDiff.Builder();
                    ResourceDiff diff = builder.setKind(ResourceDiff.Kind.MODIFIED)
                        .setName(item)
                        .setResourceBytes(base.getResourceBytes(item))
                        .build();
                    diffs.add(diff);
                }
            }
        }
        // What's now left in the set are the resources only present in the
        // optimized image (generated by some plugins; not present in jmods)
        for (String e: resources) {
            ResourceDiff.Builder builder = new ResourceDiff.Builder();
            ResourceDiff diff = builder.setKind(ResourceDiff.Kind.ADDED)
                                    .setName(e)
                                    .build();
            diffs.add(diff);
        }
        return diffs;
    }

    /**
     * Compare the contents of the two input streams (byte-by-byte).
     *
     * @param is1 The first input stream
     * @param is2 The second input stream
     * @return {@code true} iff the two streams contain the same number of
     *         bytes and each byte of the streams are equal. {@code false}
     *         otherwise.
     */
    private boolean compareStreams(InputStream is1, InputStream is2) {
        byte[] buf1 = new byte[1024];
        byte[] buf2 = new byte[1024];
        int bytesRead1, bytesRead2 = 0;
        try {
            try (is1; is2) {
                while ((bytesRead1 = is1.read(buf1)) != -1 &&
                       (bytesRead2 = is2.read(buf2)) != -1) {
                    if (bytesRead1 != bytesRead2) {
                        return false;
                    }
                    if (bytesRead1 == buf1.length) {
                        if (!Arrays.equals(buf1, buf2)) {
                            return false;
                        }
                    } else {
                        for (int i = 0; i < bytesRead1; i++) {
                            if (buf1[i] != buf2[i]) {
                                return false;
                            }
                        }
                    }
                }
                // ensure we read both to the end
                if (bytesRead1 == -1) {
                    bytesRead2 = is2.read(buf2);
                    if (bytesRead2 != -1) {
                        return false;
                    }
                    return true;
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("IO exception when comparing bytes", e);
        }
        return false;
    }

}
