/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.lorebench.record;

import java.util.List;
import java.util.Map;

/** Where records are durably kept. {@link RecordStore} caches in front of it. */
public interface RecordBackend {

    /** Every record of one owner. Empty if none. */
    Map<String, String> load(Owner owner);

    /** Apply a batch of writes together, all or nothing. */
    void write(List<Write> writes);

    void close();

    /** One pending change. A {@code null} value deletes the record. */
    record Write(Owner owner, String key, String value) {}
}
