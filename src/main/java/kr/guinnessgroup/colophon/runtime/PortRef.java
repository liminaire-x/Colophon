/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime;

/**
 * Identifies one data port on one node instance within a graph: the node's
 * instance id plus the port's semantic id. Used as the key into the
 * {@link ValueStore} for a running execution. (Contract c.)
 *
 * @param nodeId the graph-unique node instance id
 * @param portId the semantic port id ({@link DataPort#id()})
 */
public record PortRef(String nodeId, String portId) {
}
