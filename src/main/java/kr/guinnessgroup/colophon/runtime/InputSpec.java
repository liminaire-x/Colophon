/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime;

import java.util.List;

/**
 * A single unified input of a node type (contract d): it merges the old config
 * {@link FieldSpec} and the typed data input ({@link DataPort}) into one concept.
 * Every input has a type, an inline default, and a {@code connectable} flag — an
 * input is either wired from an upstream output (when connected) or read from its
 * inline default (when not). Config knobs that must not be wired set
 * {@code connectable = false}; data inputs default to {@code true}.
 *
 * @param id           stable semantic id (the port/field id in the saved graph)
 * @param typeId       a {@link kr.guinnessgroup.colophon.runtime.type.TypeRegistry} id
 * @param label        human-facing label for the editor
 * @param defaultValue inline default as a string (empty when none)
 * @param connectable  whether this input accepts a wire (data) or is inline-only (config knob)
 * @param options      dropdown choices for a constrained input; empty otherwise
 */
public record InputSpec(String id, String typeId, String label, String defaultValue,
                        boolean connectable, List<String> options) {

    /** Maps an old {@link FieldSpec} form-type to a TypeRegistry id (bare value primitives). */
    public static String typeIdForFieldType(String fieldType) {
        return switch (fieldType == null ? "" : fieldType) {
            case "number" -> "number";
            case "boolean" -> "boolean";
            default -> "string"; // string, enum, and any unknown widget
        };
    }

    /** A config field becomes an inline-only input (not connectable). */
    public static InputSpec fromField(FieldSpec f) {
        return new InputSpec(f.name(), typeIdForFieldType(f.type()), f.name(),
                f.defaultValue(), false, f.options());
    }

    /** A typed data port becomes a connectable input with no inline default. */
    public static InputSpec fromDataPort(DataPort p) {
        return new InputSpec(p.id(), p.typeId(), p.label(), "", true, List.of());
    }
}
