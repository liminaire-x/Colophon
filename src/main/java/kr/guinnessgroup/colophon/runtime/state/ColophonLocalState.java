/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime.state;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/**
 * LOCAL scope backed by Minecraft {@link SavedData}. Attached to the overworld's
 * data storage, so it is server-wide and this-world-only. {@code setDirty()} lets
 * it ride the vanilla world save cycle &mdash; no separate flush needed, and it
 * lands on the same snapshot as the H2-backed scopes.
 */
public final class ColophonLocalState extends SavedData implements LocalStore {

    public static final String ID = "colophon_local";

    private final Map<String, String> vars = new HashMap<>();

    public ColophonLocalState() {}

    public static SavedData.Factory<ColophonLocalState> factory() {
        return new SavedData.Factory<>(ColophonLocalState::new, ColophonLocalState::load, null);
    }

    private static ColophonLocalState load(CompoundTag tag, HolderLookup.Provider registries) {
        ColophonLocalState state = new ColophonLocalState();
        CompoundTag stored = tag.getCompound("vars");
        for (String key : stored.getAllKeys()) {
            state.vars.put(key, stored.getString(key));
        }
        return state;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag stored = new CompoundTag();
        vars.forEach(stored::putString);
        tag.put("vars", stored);
        return tag;
    }

    @Override
    public String get(String key) {
        return vars.get(key);
    }

    @Override
    public void set(String key, String value) {
        vars.put(key, value);
        setDirty();
    }
}
