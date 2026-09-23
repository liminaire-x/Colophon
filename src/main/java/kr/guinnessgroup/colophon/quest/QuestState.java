/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.colophon.quest;

/**
 * Where a player is with a quest. Stored as the player's record whose key is the
 * quest id itself (e.g. {@code quest_k3f9x2ma}). Only two values are stored:
 * no record = hidden, {@code active}, {@code done}. "Ready" is never stored: it is
 * an active quest whose goals are in the player's inventory right now, so dropping
 * the items can never leave a stale "ready" behind.
 */
public enum QuestState {
    HIDDEN("hidden"),
    ACTIVE("active"),
    READY("ready"),
    DONE("done");

    /** The way out of the Quest State node, saved in graphs. Never rename. */
    public final String out;

    QuestState(String out) {
        this.out = out;
    }

    /** Stored record values. Never rename. */
    public static final String ACTIVE_VALUE = "active";
    public static final String DONE_VALUE = "done";

    /** The stored state, before looking at the inventory: HIDDEN, ACTIVE or DONE. */
    public static QuestState fromRecord(String value) {
        if (ACTIVE_VALUE.equals(value)) {
            return ACTIVE;
        }
        if (DONE_VALUE.equals(value)) {
            return DONE;
        }
        return HIDDEN;
    }
}
