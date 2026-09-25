/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.lorebench.client;

import kr.guinnessgroup.lorebench.quest.DialoguePayload;

/**
 * The last dialogue the server sent, until the client tick shows it. Uses no
 * client-only classes, so the payload registration can point here on both sides.
 */
public final class ClientDialogue {

    private static volatile DialoguePayload pending;

    private ClientDialogue() {}

    public static void accept(DialoguePayload payload) {
        pending = payload;
    }

    /** The dialogue waiting to be shown, once; null if none. */
    static DialoguePayload take() {
        DialoguePayload p = pending;
        pending = null;
        return p;
    }

    /** Leaving a server forgets it. */
    static void clear() {
        pending = null;
    }
}
