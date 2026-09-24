/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.lorebench.runtime;

import kr.guinnessgroup.lorebench.graph.GraphFormat;

/** What a node tells the {@link Runner} after it runs. */
public sealed interface NodeResult {

    /** Leave through {@code port}. If nothing is linked there, the run ends. */
    record Next(String port) implements NodeResult {}

    /** End this run quietly, e.g. a trigger that is not about this event. */
    record Stop() implements NodeResult {}

    /** Stop this run and log {@code reason} as a warning. */
    record Fail(String reason) implements NodeResult {}

    static NodeResult stop() {
        return new Stop();
    }

    static NodeResult next() {
        return new Next(GraphFormat.DEFAULT_OUT);
    }

    static NodeResult next(String port) {
        return new Next(port);
    }

    static NodeResult fail(String reason) {
        return new Fail(reason);
    }
}
