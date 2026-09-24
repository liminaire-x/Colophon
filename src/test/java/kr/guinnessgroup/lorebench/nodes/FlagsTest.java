/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.lorebench.nodes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The flag record key. Changing it makes every saved flag disappear. */
class FlagsTest {

    @Test
    void flagIsStoredUnderItsKind() {
        assertEquals("flag_greeted", Flags.key("greeted"));
    }

    @Test
    void rejectsNamesThatCannotBeStored() {
        assertThrows(IllegalArgumentException.class, () -> Flags.requireName(" "));
        assertThrows(IllegalArgumentException.class, () -> Flags.requireName("Greeted!"));
    }
}
