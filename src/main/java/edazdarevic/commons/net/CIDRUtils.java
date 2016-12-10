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

import javax.annotation.Nonnull;

/**
 * A class that enables to get an IP range from CIDR specification. It supports
 * both IPv4 and IPv6.
 */
public final class CIDRUtils {
    private static final int ADDRSIZE_V4 = 4;
    private static final int ADDRSIZE_V6 = 16;
    private static final int MAXPREFIX_V4 = 32;
    private static final int MAXPREFIX_V6 = 128;
    private static final String ILLEGAL_ARGUMENT_MSG = "Not a valid CIDR format!";
    private static final String INVALID_EMPTY_ADDR = "Invalid empty address!";
    private static final String INVALID_ADDR = "Invalid address!";

    private InetAddress inetAddress;
    private InetAddress startAddress;
    private InetAddress endAddress;
    private final int prefixLength;

    private static boolean isValidV4(@Nonnull final String addr) {
        final String[] parts = addr.split("\\.", -1);
        if (parts.length != 4) {
            return false;
        }
        try {
            for (String part : parts) {
                final int val = Integer.parseInt(part);
                if (val < 0 || val > 255) {
                    return false;
                }
            }
        } catch (NumberFormatException x) {
            return false;
        }
        return true;
    }

    // Manually modelled in Java after Paul Vixie's C implementation at
    // https://sourceware.org/git/?p=glibc.git;a=blob_plain;f=resolv/inet_pton.c;hb=HEAD
    // which in turn was inspired by Mark Andrews.
    private static boolean isValidV6(final String addr) {
        if (addr.length() < 2) {
            return false;
        }

        final char[] ca = addr.toCharArray();
        final int calen = ca.length;

        int i = 0;
        if (ca[i] == ':'  && ca[++i] != ':') {
            return false;
        }
        int curtok = i;
        int colonp = -1;
        boolean saw_xdigit = false;
        int val = 0;
        int sp = 0;
        while (i < calen) {
            final char ch = ca[i++];
            final int chval = Character.digit(ch, 16);
            if (chval != -1) {
                val <<= 4;
                val |= chval;
                if (val > 0xffff) {
                    return false;
                }
                saw_xdigit = true;
                continue;
            }
            if (ch == ':') {
                curtok = i;
                if (!saw_xdigit) {
                    if (colonp != -1) {
                        return false;
                    }
                    colonp = sp;
                    continue;
                } else if (i == calen) {
                    return false;
                }
                if (sp + 2 > ADDRSIZE_V6) {
                    return false;
                }
                sp += 2;
                saw_xdigit = false;
                val = 0;
                continue;
            }
            if (ch == '.' && (sp + ADDRSIZE_V4) <= ADDRSIZE_V6) {
                if (!isValidV4(addr.substring(curtok, calen))) {
                    return false;
                }
                sp += ADDRSIZE_V4;
                saw_xdigit = false;
                break;
            } else {
                return false;
            }
        }
        if (saw_xdigit) {
            if (sp + 2 > ADDRSIZE_V6) {
                return false;
            }
            sp += 2;
        }

        if (colonp != -1) {
            if (sp == ADDRSIZE_V6) {
                return false;
            }
            sp = ADDRSIZE_V6;
        }
        if (sp != ADDRSIZE_V6) {
            return false;
        }
        return true;
    }

    /**
     * Validates an IP address.
     * @param addr The IP address to be validated.
     * @throws UnknownHostException if the address does not contain a valid IPv4 or IPv6 address.
     */
    public static void validateIpAddress(final String addr) throws UnknownHostException {
        if (addr.isEmpty()) {
            throw new UnknownHostException(INVALID_EMPTY_ADDR);
        }
        if (!isValidV4(addr) && !isValidV6(addr)) {
            throw new UnknownHostException(INVALID_ADDR);
        }
    }

    /**
     * Creates a new instance.
     *
     * @param cidr A string, conatining the address/prefix expression.
     * @throws IllegalArgumentException if cidr is malformed or the prefix is not within bounds of
     *   a valid address prefix.
     * @throws UnknownHostException if the address part does not contain a valid address.
     */
    public CIDRUtils(@Nonnull final String cidr) throws UnknownHostException {

        /* split CIDR to address and prefix part */
        if (cidr.contains("/")) {
            int index = cidr.indexOf("/");
            String addressPart = cidr.substring(0, index);
            String networkPart = cidr.substring(index + 1);
            validateIpAddress(addressPart);
            inetAddress = InetAddress.getByName(addressPart);
            prefixLength = Integer.parseInt(networkPart);
            if (prefixLength < 1 || prefixLength > MAXPREFIX_V6
                    || prefixLength > MAXPREFIX_V4 && inetAddress.getAddress().length == ADDRSIZE_V4) {
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
        if (inetAddress.getAddress().length == ADDRSIZE_V4) {
            maskBuffer = ByteBuffer.allocate(ADDRSIZE_V4).putInt(-1);
            targetSize = ADDRSIZE_V4;
        } else {
            maskBuffer = ByteBuffer.allocate(ADDRSIZE_V6).putLong(-1L).putLong(-1L);
            targetSize = ADDRSIZE_V6;
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

    private byte[] toBytes(final byte[] array, final int targetSize) {
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
    @Nonnull
    public String getNetworkAddress() {

        return this.startAddress.getHostAddress();
    }

    /**
     * Fetches the broadcast address of this instance.
     * @return The broadcast address as a string.
     */
    @Nonnull
    public String getBroadcastAddress() {
        return this.endAddress.getHostAddress();
    }

    private boolean isInRange(@Nonnull final String ipAddress, final boolean broadcastOk) throws UnknownHostException {
        validateIpAddress(ipAddress);
        InetAddress address = InetAddress.getByName(ipAddress);
        BigInteger start = new BigInteger(1, this.startAddress.getAddress());
        BigInteger end = new BigInteger(1, this.endAddress.getAddress());
        BigInteger target = new BigInteger(1, address.getAddress());

        int st = start.compareTo(target);
        int te = target.compareTo(end);

        return (st == -1 || st == 0) && (te == -1 || te == 0 && broadcastOk);
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
