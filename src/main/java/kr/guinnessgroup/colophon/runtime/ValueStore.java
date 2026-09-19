/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-execution store of data-port values, living for one whole flow run
 * (contract c). Exec nodes push their data outputs here after they run; pure
 * nodes are pulled and their computed outputs cached here. A port that has not
 * been produced yet is <em>unset</em> — modelled as absence, distinct from a
 * stored {@code null} — so consumers can fall back to a defined result rather
 * than mistaking "not produced" for "produced null".
 * <p>
 * Not thread-safe: an execution runs on the main server thread only.
 */
public final class ValueStore {

    private final Map<PortRef, Object> values = new HashMap<>();

    /** Whether a value has been produced for this port in this execution. */
    public boolean has(String nodeId, String portId) {
        return values.containsKey(new PortRef(nodeId, portId));
    }

    /** The produced value for this port, or {@code null} if it is unset. */
    public Object get(String nodeId, String portId) {
        return values.get(new PortRef(nodeId, portId));
    }

    /** Records a produced value for this port (may be re-produced by pure pulls). */
    public void put(String nodeId, String portId, Object value) {
        values.put(new PortRef(nodeId, portId), value);
    }
}
