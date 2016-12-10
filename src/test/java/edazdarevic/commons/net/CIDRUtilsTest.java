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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CIDRUtilsTest {

    @Test(expected=UnknownHostException.class)
    public void testInvalidAddr01() throws Exception {
        CIDRUtils.validateIpAddress("1200::AB00:1234::2552:7777:1313");
    }

    @Test(expected=UnknownHostException.class)
    public void testInvalidAddr02() throws Exception {
        CIDRUtils.validateIpAddress(":2001:db8:0:1");
    }

    @Test(expected=UnknownHostException.class)
    public void testInvalidAddr03() throws Exception {
        CIDRUtils.validateIpAddress("2001:db8:0:1");
    }

    @Test(expected=UnknownHostException.class)
    public void testInvalidAddr04() throws Exception {
        CIDRUtils.validateIpAddress("12001:0000:1234:0000:0000:C1C0:ABCD:0876");
    }

    @Test
    public void testValidAddr01() throws Exception {
        CIDRUtils.validateIpAddress("::192.168.1.1");
    }

    @Test
    public void testValidAddr02() throws Exception {
        CIDRUtils.validateIpAddress("::1");
    }

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

    @Test(expected=UnknownHostException.class)
    public void testInRangeInvalid1() throws Exception {
        CIDRUtils cu = new CIDRUtils("192.168.1.2/24");
        cu.isInRange("");
    }

    @Test(expected=UnknownHostException.class)
    public void testInRangeInvalid2() throws Exception {
        CIDRUtils cu = new CIDRUtils("192.168.1.2/24");
        cu.isInHostsRange("");
    }

    @Test
    public void testInRangeV4() throws Exception {
        CIDRUtils cu = new CIDRUtils("192.168.1.2/24");
        assertNotNull(cu.getNetworkAddress());
        assertNotNull(cu.getBroadcastAddress());
        assertEquals("Network address", cu.getNetworkAddress(), "192.168.1.0");
        assertEquals("Broadcast address", cu.getBroadcastAddress(), "192.168.1.255");
        assertTrue("In range", cu.isInRange("192.168.1.22"));
        assertTrue("In range", cu.isInRange("192.168.1.255"));
        assertTrue("In range", cu.isInHostsRange("192.168.1.254"));
        assertFalse("In range", cu.isInHostsRange("192.168.1.255"));
    }

    @Test
    public void testInRangeV6() throws Exception {
        CIDRUtils cu = new CIDRUtils("435:23f::45:23/101");
        assertNotNull(cu.getNetworkAddress());
        assertNotNull(cu.getBroadcastAddress());
        assertEquals("Network address", cu.getNetworkAddress(), "435:23f:0:0:0:0:0:0");
        assertEquals("Broadcast address", cu.getBroadcastAddress(), "435:23f:0:0:0:0:7ff:ffff");
        assertTrue("In range", cu.isInRange("435:23f:0:0:0:0:0:1"));
        assertTrue("In range", cu.isInRange("435:23f:0:0:0:0:7ff:ffff"));
        assertTrue("In range", cu.isInHostsRange("435:23f:0:0:0:0:7ff:fffe"));
        assertFalse("In range", cu.isInHostsRange("435:23f:0:0:0:0:7ff:ffff"));
    }
}
