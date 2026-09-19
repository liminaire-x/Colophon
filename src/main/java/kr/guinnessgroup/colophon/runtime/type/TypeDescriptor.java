/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime.type;

/**
 * Editor-facing descriptor of a data type in the open {@link TypeRegistry}. The
 * engine knows nothing about concrete types; core primitives and add-on types
 * are contributed the same way (mirrors NodeRegistry's philosophy).
 *
 * @param id           namespaced, immutable identity, e.g. "colophon:number" or "impactor:account"
 * @param displayName  human-facing label (TODO: becomes a translation key — contract e)
 * @param color        editor handle/wire color as hex, e.g. "#2f9e44"
 * @param kind         VALUE (flows/stored by value) or REFERENCE (handle, resolved at use)
 * @param serializable whether values of this type can be stored in the saved graph / state
 */
public record TypeDescriptor(String id, String displayName, String color,
                             TypeKind kind, boolean serializable) {
}
