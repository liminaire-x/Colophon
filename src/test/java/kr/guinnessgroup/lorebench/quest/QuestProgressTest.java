/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.lorebench.quest;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The progress record (kills, harvests). Changing it loses everyone's counts. */
class QuestProgressTest {

    @Test
    void storedUnderItsKindNextToTheQuestState() {
        assertEquals("progress_quest_k3f9x2ma", QuestProgress.key("quest_k3f9x2ma"));
    }

    @Test
    void eachCountedGoalIsKeyedByKindAndTarget() {
        assertEquals("kill:minecraft:wolf", QuestDoc.Goal.kill("minecraft:wolf", 3).progressKey());
        assertEquals("harvest:minecraft:wheat", QuestDoc.Goal.harvest("minecraft:wheat", 10).progressKey());
    }

    @Test
    void countsByKindAndTargetRoundTrip() {
        Map<String, Integer> counts = Map.of("kill:minecraft:wolf", 2, "kill:minecraft:zombie", 1, "harvest:minecraft:wheat", 3);
        String value = QuestProgress.write(counts);
        assertEquals("{\"harvest:minecraft:wheat\":3,\"kill:minecraft:wolf\":2,\"kill:minecraft:zombie\":1}", value);
        assertEquals(counts, QuestProgress.read(value));
    }

    @Test
    void missingOrBrokenMeansNothingDoneYet() {
        assertTrue(QuestProgress.read(null).isEmpty());
        assertTrue(QuestProgress.read("not json").isEmpty());
        assertTrue(QuestProgress.read("{\"kill:minecraft:wolf\":\"many\"}").isEmpty());
    }
}
