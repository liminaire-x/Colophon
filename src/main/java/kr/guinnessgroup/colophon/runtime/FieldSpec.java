/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime;

import java.util.List;

/**
 * Describes a single configurable field of a node type (for the editor palette).
 * {@code options} is used only by {@code enum} fields (rendered as a dropdown);
 * other field types leave it empty.
 */
public record FieldSpec(String name, String type, String defaultValue, List<String> options) {

    public FieldSpec(String name, String type, String defaultValue) {
        this(name, type, defaultValue, List.of());
    }
}
