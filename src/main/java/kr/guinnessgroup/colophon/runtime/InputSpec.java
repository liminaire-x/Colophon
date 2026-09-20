/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime;

import java.util.List;

/**
 * A single unified input of a node type (contract d): config knobs and typed data
 * inputs are one concept. Every input has a type, an inline default, and a
 * {@code connectable} flag — an
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

    /** An inline-only config knob (not connectable), labelled by its id. */
    public static InputSpec knob(String id, String typeId, String defaultValue) {
        return new InputSpec(id, typeId, id, defaultValue, false, List.of());
    }

    /** An inline-only enum knob: a constrained string with dropdown options. */
    public static InputSpec enumKnob(String id, String defaultValue, List<String> options) {
        return new InputSpec(id, "string", id, defaultValue, false, options);
    }

    /** A connectable typed data input with no inline default. */
    public static InputSpec data(String id, String typeId, String label) {
        return new InputSpec(id, typeId, label, "", true, List.of());
    }
}
