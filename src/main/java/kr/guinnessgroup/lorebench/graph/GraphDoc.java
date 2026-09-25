/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.lorebench.graph;

import com.google.gson.JsonObject;
import kr.guinnessgroup.lorebench.Folders.Folder;

import java.util.List;

/**
 * The saved graph document, exactly as it lives in {@code graphs.json}: plain data,
 * no behavior. {@link GraphFormat} reads and writes it; the runtime builds runnable
 * graphs from it. See docs/decisions/0001-storage-format.md.
 */
public record GraphDoc(List<Folder> folders, List<DocGraph> graphs) {

    public static final GraphDoc EMPTY = new GraphDoc(List.of(), List.of());

    /**
     * One graph. {@code id} is stable (never renamed); {@code name} is for people.
     * {@code folder} is the editor folder id it sits in, or "" for the top
     * ({@link kr.guinnessgroup.lorebench.Folders}).
     */
    public record DocGraph(String id, String name, List<DocNode> nodes, List<DocLink> links, String folder) {}

    /** One placed node: its type id, its settings, and where it sits on the canvas. */
    public record DocNode(String id, String type, JsonObject config, int x, int y) {}

    /** "After node {@code from} leaves through {@code out}, go to node {@code to}." */
    public record DocLink(String from, String out, String to) {}
}
