/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime;

/**
 * A typed data port on a node type. Unlike flow ports (which model control flow),
 * a data port carries a value of a specific type from the open
 * {@link kr.guinnessgroup.colophon.runtime.type.TypeRegistry}.
 * <p>
 * Connections between data ports are validated nominally: two data ports may be
 * wired only when their {@code typeId} strings are equal (no subtyping, no
 * implicit conversion — see the v2 data-port contract b). A data port and a flow
 * port may never be connected.
 * <p>
 * The {@code id} is an immutable, semantic string (contract e): it identifies the
 * port in the saved graph and must not change once shipped. The {@code label} is
 * a human-facing name for the editor (TODO: becomes a translation key — contract e).
 * Values do not actually flow yet; contract b only locks the port surface and its
 * connection validation. The value store and pull/push wiring arrive in contract c.
 *
 * @param id     immutable semantic identity of the port, e.g. "amount" or "victim"
 * @param typeId a {@code TypeRegistry} id this port carries, e.g. "colophon:number"
 * @param label  human-facing label for the editor
 */
public record DataPort(String id, String typeId, String label) {
}
