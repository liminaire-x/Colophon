/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime.type;

/**
 * Whether a type is a plain value (flows and is stored by value) or a reference
 * to a live thing (a handle such as a UUID, resolved at the point of use).
 */
public enum TypeKind {
    VALUE,
    REFERENCE
}
