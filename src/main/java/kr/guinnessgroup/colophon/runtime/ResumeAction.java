/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime;

/**
 * Work a suspending node asks the {@link TickScheduler} to run at the moment it
 * resumes, before execution steps on (contract e). Its purpose is to push the value
 * an async operation produced into the node's data outputs ({@code ctx.set(...)}),
 * since the node itself is not re-executed on resume. The scheduler binds the
 * context to the suspending node first, so a {@code ctx.set} targets its ports.
 */
@FunctionalInterface
public interface ResumeAction {

    void run(ExecContext ctx);
}
