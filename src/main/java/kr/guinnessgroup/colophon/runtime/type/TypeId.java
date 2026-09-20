/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime.type;

/**
 * The canonical, in-memory form of a type identity. Two notations exist by design:
 * value primitives are bare ({@code string}, {@code number}, {@code boolean}) while
 * core-reference and add-on types are namespaced ({@code colophon:player},
 * {@code economy:account}). "Bare" means a built-in value primitive; "namespaced"
 * means something registered under an owner.
 * <p>
 * The on-disk / wire form is unchanged — always the string of {@link #asString()}.
 * Holding it as a sealed record instead of a raw string means matching is
 * {@code from.equals(to)} and future kinds (registry refs, containers) attach as new
 * permitted records here without touching call sites. (Type-system handoff §3.)
 */
public sealed interface TypeId {

    /** A value primitive: bare, owner-less, one of the frozen three. */
    record Builtin(String name) implements TypeId {}

    /** A registered type: {@code namespace:name} (core reference or add-on). */
    record Named(String namespace, String name) implements TypeId {}

    /** Parses the string form: no colon means a bare {@link Builtin}, else {@link Named}. */
    static TypeId parse(String s) {
        int i = s.indexOf(':');
        return (i < 0)
                ? new Builtin(s)
                : new Named(s.substring(0, i), s.substring(i + 1));
    }

    /** The string form used in JSON, the schema, and the editor. */
    default String asString() {
        return switch (this) {
            case Builtin b -> b.name();
            case Named n -> n.namespace() + ":" + n.name();
        };
    }
}
