/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.colophon.runtime;

import java.util.Set;

/**
 * The content being published alongside the graphs, so a node can reject a
 * reference to something that does not exist (e.g. an NPC id with a typo).
 */
public record Catalog(Set<String> npcIds, Set<String> questIds) {

    public static final Catalog EMPTY = new Catalog(Set.of(), Set.of());

    public boolean hasNpc(String id) {
        return npcIds.contains(id);
    }

    public boolean hasQuest(String id) {
        return questIds.contains(id);
    }
}
