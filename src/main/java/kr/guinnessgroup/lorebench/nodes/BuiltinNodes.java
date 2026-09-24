/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.lorebench.nodes;

import kr.guinnessgroup.lorebench.runtime.NodeRegistry;

/** Lorebench's own node types. The order here is the editor palette order. */
public final class BuiltinNodes {

    private BuiltinNodes() {}

    public static void registerAll(NodeRegistry registry) {
        registry.register(new OnPlayerJoin());
        registry.register(new OnNpcInteract());
        registry.register(new HasFlag());
        registry.register(new SetFlag());
        registry.register(new QuestStateSwitch());
        registry.register(new SendMessage());
        registry.register(new RevealQuest());
        registry.register(new CompleteQuest());
        registry.register(new PlayNpcAnimation());
    }
}
