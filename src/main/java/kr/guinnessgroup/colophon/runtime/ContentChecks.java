/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.colophon.runtime;

/**
 * What publish asks the running game about quest content. Each returns why the
 * text cannot be used in this game, or {@code null} if it can.
 */
public interface ContentChecks {

    /** An item to make, as {@code /give} writes it (rewards, icons). */
    String item(String spec);

    /** An item to look for, as {@code /clear} reads it (hand-in goals). */
    String itemCondition(String spec);

    /** An entity type id (kill goals). */
    String entity(String id);
}
