/*
 * The MIT License
 *
 * Copyright (c) 2013 Edin Dazdarevic (edin.dazdarevic@gmail.com)
 * Misc. fixes & improvements Copyright (c) 2016 Fritz Elfert
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

package edazdarevic.commons.net;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

/**
 * A class that enables to get an IP range from CIDR specification. It supports
 * both IPv4 and IPv6.
 */
public class CIDRUtils {
    private final static int MAXPREFIX_V4 = 32;
    private final static int MAXPREFIX_V6 = 128;
    private final static String ILLEGAL_ARGUMENT_MSG = "Not a valid CIDR format!";
    private final static String INVALID_EMPTY = "Invalid empty address!";

    private final String cidr;

    private InetAddress inetAddress;
    private InetAddress startAddress;
    private InetAddress endAddress;
    private final int prefixLength;


    /**
     * Creates a new instance.
     *
     * @param cidr A string, conatining the address/prefix expression.
     * @throws IllegalArgumentException if cidr is malformed or the prefix is not within bounds of a valid address prefix.
     * @throws UnknownHostException if the address part does not contain a valid address.
     */
    public CIDRUtils(@Nonnull final String cidr) throws UnknownHostException {

        this.cidr = cidr;

        /* split CIDR to address and prefix part */
        if (this.cidr.contains("/")) {
            int index = this.cidr.indexOf("/");
            String addressPart = this.cidr.substring(0, index);
            String networkPart = this.cidr.substring(index + 1);
            if (addressPart.isEmpty()) {
                throw new UnknownHostException(INVALID_EMPTY);
            }
            inetAddress = InetAddress.getByName(addressPart);
            prefixLength = Integer.parseInt(networkPart);
            if (prefixLength < 1 || prefixLength > MAXPREFIX_V6 ||
                    (prefixLength > MAXPREFIX_V4 && inetAddress.getAddress().length == 4)) {
                throw new IllegalArgumentException(ILLEGAL_ARGUMENT_MSG);
            }
            calculate();
        } else {
            throw new IllegalArgumentException(ILLEGAL_ARGUMENT_MSG);
        }
    }


    private void calculate() throws UnknownHostException {

        ByteBuffer maskBuffer;
        int targetSize;
        if (inetAddress.getAddress().length == 4) {
            maskBuffer =
                ByteBuffer
                .allocate(4)
                .putInt(-1);
            targetSize = 4;
        } else {
            maskBuffer = ByteBuffer.allocate(16)
                .putLong(-1L)
                .putLong(-1L);
            targetSize = 16;
        }

        BigInteger mask = new BigInteger(1, maskBuffer.array()).not().shiftRight(prefixLength);

        ByteBuffer buffer = ByteBuffer.wrap(inetAddress.getAddress());
        BigInteger ipVal = new BigInteger(1, buffer.array());

        BigInteger startIp = ipVal.and(mask);
        BigInteger endIp = startIp.add(mask.not());

        byte[] startIpArr = toBytes(startIp.toByteArray(), targetSize);
        byte[] endIpArr = toBytes(endIp.toByteArray(), targetSize);

        this.startAddress = InetAddress.getByAddress(startIpArr);
        this.endAddress = InetAddress.getByAddress(endIpArr);

    }

    private byte[] toBytes(byte[] array, int targetSize) {
        int counter = 0;
        List<Byte> newArr = new ArrayList<Byte>();
        while (counter < targetSize && array.length - 1 - counter >= 0) {
            newArr.add(0, array[array.length - 1 - counter]);
            counter++;
        }

        int size = newArr.size();
        for (int i = 0; i < (targetSize - size); i++) {

            newArr.add(0, (byte) 0);
        }

        byte[] ret = new byte[newArr.size()];
        for (int i = 0; i < newArr.size(); i++) {
            ret[i] = newArr.get(i);
        }
        return ret;
    }

    /**
     * Fetches the network address of this instance.
     * @return The network address as a string.
     */
    public @Nonnull String getNetworkAddress() {

        return this.startAddress.getHostAddress();
    }

    /**
     * Fetches the broadcast address of this instance.
     * @return The broadcast address as a string.
     */
    public @Nonnull String getBroadcastAddress() {
        return this.endAddress.getHostAddress();
    }

    private boolean isInRange(@Nonnull final String ipAddress, boolean broadcastOk) throws UnknownHostException {
        if (ipAddress.isEmpty()) {
                throw new UnknownHostException(INVALID_EMPTY);
        }
        InetAddress address = InetAddress.getByName(ipAddress);
        BigInteger start = new BigInteger(1, this.startAddress.getAddress());
        BigInteger end = new BigInteger(1, this.endAddress.getAddress());
        BigInteger target = new BigInteger(1, address.getAddress());

        int st = start.compareTo(target);
        int te = target.compareTo(end);

        return (st == -1 || st == 0) && (te == -1 || (te == 0 && broadcastOk));
    }

    /**
     * Tests, if the supplied address is contained in the address range, specified by this instance.
     *
     * @param ipAddress The address to be tested.
     * @return {@code true}, if the specified address is contained in this network.
     * @throws UnknownHostException if the address does not contain a valid address.
     */
    public boolean isInRange(@Nonnull final String ipAddress) throws UnknownHostException {
        return isInRange(ipAddress, true);
    }

    /**
     * Tests, if the supplied address is contained in the address range for hosts,  specified by this instance.
     * This excludes the broadcast address of the network.
     *
     * @param ipAddress The address to be tested.
     * @return {@code true}, if the specified address is contained in this network.
     * @throws UnknownHostException if the address does not contain a valid address.
     */
    public boolean isInHostsRange(@Nonnull final String ipAddress) throws UnknownHostException {
        return isInRange(ipAddress, false);
    }
}
