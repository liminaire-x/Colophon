/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.lorebench.quest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Stored quest state and the graph's ways out. Changing these breaks saved records and graphs. */
class QuestStateTest {

    @Test
    void onlyActiveAndDoneAreStored() {
        assertEquals("active", QuestState.ACTIVE_VALUE);
        assertEquals("done", QuestState.DONE_VALUE);
        assertEquals(QuestState.HIDDEN, QuestState.fromRecord(null));
        assertEquals(QuestState.ACTIVE, QuestState.fromRecord("active"));
        assertEquals(QuestState.DONE, QuestState.fromRecord("done"));
        assertEquals(QuestState.HIDDEN, QuestState.fromRecord("ready")); // never stored
    }

    @Test
    void waysOutKeepTheirNames() {
        assertEquals("hidden", QuestState.HIDDEN.out);
        assertEquals("active", QuestState.ACTIVE.out);
        assertEquals("ready", QuestState.READY.out);
        assertEquals("done", QuestState.DONE.out);
    }
}
