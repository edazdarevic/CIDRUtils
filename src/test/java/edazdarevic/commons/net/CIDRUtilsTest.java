/*
 * Copyright 2016 Fritz Elfert
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edazdarevic.commons.net;

import java.net.UnknownHostException;

import org.junit.Test;
import org.junit.Rule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class CIDRUtilsTest {

    @Test(expected=IllegalArgumentException.class)
    public void testEmptyArg() throws Exception {
        new CIDRUtils("");
    }

    @Test(expected=UnknownHostException.class)
    public void testOnlySlashArg() throws Exception {
        new CIDRUtils("/");
    }

    @Test(expected=UnknownHostException.class)
    public void testOnlyPrefix() throws Exception {
        new CIDRUtils("/1");
    }

    @Test(expected=IllegalArgumentException.class)
    public void testInvalidPrefix1() throws Exception {
        new CIDRUtils("192.168.1.0/0");
    }

    @Test(expected=IllegalArgumentException.class)
    public void testInvalidPrefix2() throws Exception {
        new CIDRUtils("192.168.1.0/33");
    }

    @Test(expected=IllegalArgumentException.class)
    public void testInvalidPrefix3() throws Exception {
        new CIDRUtils("::1/129");
    }

    @Test
    public void testInRangeV4() throws Exception {
        CIDRUtils cu = new CIDRUtils("192.168.1.2/24");
        assertEquals("Network address", cu.getNetworkAddress(), "192.168.1.0");
        assertEquals("Broadcast address", cu.getBroadcastAddress(), "192.168.1.255");
        assertTrue("In range", cu.isInRange("192.168.1.22"));
        assertTrue("In range", cu.isInRange("192.168.1.255"));
        assertTrue("In range", cu.isInHostsRange("192.168.1.254"));
        assertFalse("In range", cu.isInHostsRange("192.168.1.255"));
    }
}
