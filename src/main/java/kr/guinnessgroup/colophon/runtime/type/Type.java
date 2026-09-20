/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime.type;

import kr.guinnessgroup.colophon.runtime.ExecContext;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A typed handle over a {@link TypeRegistry} id (type-system handoff §5): it carries
 * the type id plus the code to move a value between its stored form and its runtime
 * Java form {@code T}. Nodes read inputs type-safely via {@code ctx.get(portId, type)}.
 * <p>
 * Value types (string/number/boolean) store their value directly, so
 * {@link #resolve} is a cast. Reference types (player) store a handle (UUID) and
 * {@link #resolve} looks up the live object — which may fail, yielding {@code null}
 * (unset). {@link #fromInline} parses an unconnected input's inline default; a type
 * with no inline form (references) returns {@code null}.
 *
 * @param <T> the runtime Java type
 */
public final class Type<T> {

    private final String id;
    private final Function<String, Object> inlineParser;          // literal -> stored form, or null
    private final BiFunction<Object, ExecContext, T> resolver;    // stored form -> runtime T

    public Type(String id, Function<String, Object> inlineParser, BiFunction<Object, ExecContext, T> resolver) {
        this.id = id;
        this.inlineParser = inlineParser;
        this.resolver = resolver;
    }

    public String id() {
        return id;
    }

    /** Turns a stored value into its runtime form (may be {@code null} = unset/unresolved). */
    public T resolve(Object stored, ExecContext ctx) {
        return stored == null ? null : resolver.apply(stored, ctx);
    }

    /** Parses an inline-default literal into a stored value, or {@code null} if this type has no inline form. */
    public Object fromInline(String literal) {
        if (inlineParser == null || literal == null || literal.isEmpty()) {
            return null;
        }
        try {
            return inlineParser.apply(literal);
        } catch (RuntimeException e) {
            return null; // malformed inline default -> unset (defined result), never crash
        }
    }
}
