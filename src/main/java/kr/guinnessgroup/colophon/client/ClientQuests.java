/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.colophon.client;

import kr.guinnessgroup.colophon.quest.QuestSyncPayload;

import java.util.List;

/**
 * The quests the server has revealed to this player, as last received. Holds only
 * what the server sent; nothing else exists on the client. Uses no client-only
 * classes, so the payload registration can point here on both sides.
 */
public final class ClientQuests {

    private static volatile List<QuestSyncPayload.Entry> quests = List.of();

    private ClientQuests() {}

    public static void accept(QuestSyncPayload payload) {
        quests = payload.quests();
    }

    public static List<QuestSyncPayload.Entry> all() {
        return quests;
    }

    /** Leaving a server forgets its quests. */
    public static void clear() {
        quests = List.of();
    }
}
