/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.lorebench.runtime;

/**
 * A placed, ready-to-run node. Runs on the server thread and must return quickly.
 */
@FunctionalInterface
public interface Node {

    NodeResult run(Context ctx);
}
