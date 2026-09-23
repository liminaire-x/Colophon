/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.colophon.quest;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The kill progress record. Changing it loses everyone's kill counts. */
class QuestProgressTest {

    @Test
    void storedUnderItsKindNextToTheQuestState() {
        assertEquals("progress_quest_k3f9x2ma", QuestProgress.key("quest_k3f9x2ma"));
    }

    @Test
    void countsByEntityIdRoundTrip() {
        String value = QuestProgress.write(Map.of("minecraft:wolf", 2, "minecraft:zombie", 1));
        assertEquals("{\"minecraft:wolf\":2,\"minecraft:zombie\":1}", value);
        assertEquals(Map.of("minecraft:wolf", 2, "minecraft:zombie", 1), QuestProgress.read(value));
    }

    @Test
    void missingOrBrokenMeansNoKillsYet() {
        assertTrue(QuestProgress.read(null).isEmpty());
        assertTrue(QuestProgress.read("not json").isEmpty());
        assertTrue(QuestProgress.read("{\"minecraft:wolf\":\"many\"}").isEmpty());
    }
}
