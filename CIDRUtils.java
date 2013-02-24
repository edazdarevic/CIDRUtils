/*
* The MIT License
*
* Copyright (c) 2013 Edin Dazdarevic (edin.dazdarevic@gmail.com)

* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:

* The above copyright notice and this permission notice shall be included in
* all copies or substantial portions of the Software.

* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
* THE SOFTWARE.
*
* */

package edazdarevic.commons.net;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

/**
 * A class that enables to get an IP range from CIDR specification. It supports
 * both IPv4 and IPv6.
 */
public class CIDRUtils {
    private final String cidr;

    private InetAddress inetAddress;
    private InetAddress startAddress;
    private InetAddress endAddress;
    private final int prefixLength;


    public CIDRUtils(String cidr) throws UnknownHostException {

        this.cidr = cidr;

        /* split CIDR to address and prefix part */
        if (this.cidr.contains("/")) {
            int index = this.cidr.indexOf("/");
            String addressPart = this.cidr.substring(0, index);
            String networkPart = this.cidr.substring(index + 1);

            inetAddress = InetAddress.getByName(addressPart);

            prefixLength = Integer.parseInt(networkPart);

            if (inetAddress.getAddress().length == 4)
                calculateIpv4();
            else
                calculateIpv6();
        } else {
            throw new IllegalArgumentException("not an valid CIDR format!");
        }
    }


    private void calculateIpv6() throws UnknownHostException {

        ByteBuffer maxLong = ByteBuffer.allocate(16)
                .putLong(Long.MAX_VALUE)
                .putLong(Long.MAX_VALUE);


        BigInteger mask = (new BigInteger(maxLong.array())).not().shiftRight(prefixLength - 1);

        ByteBuffer buffer = ByteBuffer.wrap(inetAddress.getAddress());

        BigInteger ipValue = new BigInteger(buffer.array());

        BigInteger startIp = ipValue.and(mask);
        BigInteger endIp = startIp.add((mask.not()));


        this.startAddress = InetAddress.getByAddress(startIp.toByteArray());
        this.endAddress = InetAddress.getByAddress(endIp.toByteArray());
    }


    private void calculateIpv4() throws UnknownHostException {
        int mask = ~((0x7fffffff) >> (prefixLength - 1));

        IntBuffer buffer = ByteBuffer.wrap(inetAddress.getAddress()).asIntBuffer();

        int ipValue = buffer.get();

        int startIp = ipValue & mask;
        int endIp = startIp + ~(mask);

        ByteBuffer startFinal = ByteBuffer.allocate(4).putInt(startIp);
        ByteBuffer endFinal = ByteBuffer.allocate(4).putInt(endIp);

        this.startAddress = InetAddress.getByAddress(startFinal.array());
        this.endAddress = InetAddress.getByAddress(endFinal.array());

    }

    public String getNetworkAddress() {

        return this.startAddress.getHostAddress();
    }

    public String getBroadcastAddress() {
        return this.endAddress.getHostAddress();
    }

    public boolean isInRange(String ipAddress) throws UnknownHostException {
        InetAddress address = InetAddress.getByName(ipAddress);
        BigInteger start = new BigInteger(this.startAddress.getAddress());
        BigInteger end = new BigInteger(this.endAddress.getAddress());
        BigInteger target = new BigInteger(address.getAddress());

        int st = start.compareTo(target);
        int te = target.compareTo(end);

        return (st == -1 || st == 0) && (te == -1 || te == 0);
    }
}
