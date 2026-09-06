/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime.state;

/**
 * The LOCAL-scope store: server-wide, this-world-only variables keyed by name.
 * Backed by Minecraft SavedData so it rides the world save cycle. Kept as an
 * interface so {@link StorageService} stays free of Minecraft types.
 */
public interface LocalStore {

    String get(String key);

    void set(String key, String value);
}
