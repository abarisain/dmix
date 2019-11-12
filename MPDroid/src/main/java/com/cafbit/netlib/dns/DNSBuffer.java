/*
 * Copyright 2011 David Simmons
 * http://cafbit.com/entry/testing_multicast_support_on_android
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
package com.cafbit.netlib.dns;

import java.io.UnsupportedEncodingException;
import java.util.Stack;

/**
 * Encapsulate a byte buffer which maintains its own
 * traversal state and provides several utility methods
 * useful for consuming or producing the binary fields.
 * @author simmons
 */
public class DNSBuffer {
    
    public byte[] bytes;
    public int start;
    public int length;
    public int offset;

    public DNSBuffer(int length) {
        bytes = new byte[length];
        start = 0;
        this.length = length;
        offset = 0;
    }
    
    public DNSBuffer(byte[] bytes) {
        this.bytes = bytes;
        this.start = 0;
        this.length = bytes.length;
        this.offset = 0;
    }
    
    public DNSBuffer(byte[] bytes, int start, int length) {
        this.bytes = bytes;
        this.start = start;
        this.length = length;
        this.offset = start;
    }
    
    //
    
    public void reset() {
        offset = start;
    }
    
    public int remaining() {
        return length - (offset - start);
    }
    
    public void checkRemaining(int needsBytes) {
        if (remaining() < needsBytes) {
            throw new DNSException("insufficient buffer: "+remaining()+" < "+needsBytes);
        }
    }
    
    // read methods
    
    public byte readByte() {
        return bytes[offset++];
    }
    
    public byte[] readBytes(int numBytes) {
        byte[] ba = new byte[numBytes];
        System.arraycopy(bytes, offset, ba, 0, numBytes);
        offset += numBytes;
        return ba;
    }

    public short readShort() {
        byte hi = bytes[offset++];
        byte lo = bytes[offset++];
        return (short)((hi&0xFF)<<8 | (lo&0xFF));
    }

    public int readShortAsInt() {
        byte hi = bytes[offset++];
        byte lo = bytes[offset++];
        return (int)((hi&0xFF)<<8 | (lo&0xFF));
    }
    
    public int readInteger() {
        byte b1 = bytes[offset++];
        byte b2 = bytes[offset++];
        byte b3 = bytes[offset++];
        byte b4 = bytes[offset++];
        return ((b1&0xFF)<<24 | (b2&0xFF)<<16 | (b3&0xFF)<<8 | (b4&0xFF));
    }

    public String readString(int numBytes) {
        String string = bytesToString(bytes, offset, numBytes);
        offset += numBytes;
        return string;
    }
    
    public String readLabel() {
        byte lengthByte = readByte();
        byte hiBits = (byte) ((lengthByte>>>6) & 0x03);
        if (hiBits == 3) {
            // handle compressed names
            short compressionOffset =
                (short) ((short)((lengthByte & 0x3F) << 8) | readByte());
            pushOffset(start+compressionOffset);
            return readLabel();
        } else if (hiBits > 0) {
            throw new DNSException("unknown label compression format");
        }
        int length = (int) lengthByte;
        if ((length == 0) && (! offsetStack.isEmpty())) {
            // TODO: it turns out that the compression scheme is not a stack!  clean this up...
            while (! offsetStack.isEmpty()) {
                popOffset();
            }
            return null;
        }
        
        if (length > 63) {
            throw new DNSException("label length > 63");
        } else if (length == 0) {
            return null;
        }
        return readString(length);
    }

    public String readName() {
        StringBuilder sb = new StringBuilder();
        boolean needDot = false;
        String label;
        while ((label = readLabel()) != null) {
            if (needDot) {
                sb.append('.');
            } else {
                needDot = true;
            }
            sb.append(label);
        }
        return sb.toString();
    }
    
    public byte[] readRdata() {
        int length = (int) readShort();
        byte[] rdata = readBytes(length);
        return rdata;
    }
    
    public void rewind(int amount) {
        offset = offset - amount;
    }
    
    // write methods
    
    public void writeByte(byte b) {
        bytes[offset++] = b;
    }
    
    public void writeBytes(byte[] ba) {
        System.arraycopy(ba, 0, bytes, offset, ba.length);
        offset += ba.length;
    }
    
    public void writeShort(short s) {
        bytes[offset++] = (byte)((s>>>8) & 0xFF);
        bytes[offset++] = (byte)(s & 0xFF);
    }

    public void writeInteger(int i) {
        bytes[offset++] = (byte)((i>>>24) & 0xFF);
        bytes[offset++] = (byte)((i>>>16) & 0xFF);
        bytes[offset++] = (byte)((i>>>8) & 0xFF);
        bytes[offset++] = (byte)(i & 0xFF);
    }
    
    public void writeShort(int i) {
        writeShort((short) i);
    }
    
    public void writeString(String string) {
        byte[] stringBytes = stringToBytes(string);
        System.arraycopy(stringBytes, 0, bytes, offset, stringBytes.length);
        offset += stringBytes.length;
    }
    
    public void writeLabel(String label) {
        if (label.length() > 63) {
            throw new DNSException("label length > 63");
        }
        writeByte((byte) lengthInBytes(label));
        writeString(label);
    }
    
    public void writeName(String name) {
        String[] labels = nameToLabels(name);
        for (int i=0; i<labels.length; i++) {
            writeLabel(labels[i]);
        }
        writeByte((byte) 0); // terminating zero length
    }
    
    public void writeRdata(byte[] rdata) {
        writeShort((short) rdata.length);
        writeBytes(rdata);
    }
    
    // public utility methods
    
    public static int nameByteLength(String name) {
        int length = 0;
        for (String label : nameToLabels(name)) {
            length++; // label-length octet
            length += stringToBytes(label).length; // string length in bytes
        }
        return length;
    }

    public static byte[] stringToBytes(String string) {
        byte[] bytes;
        try {
            bytes = string.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new DNSException(e);
        }
        return bytes;
    }
    
    public static String bytesToString(byte[] bytes, int offset, int length) {
        String string;
        try {
            string = new String(bytes, offset, length, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new DNSException(e);
        }
        return string;
    }
    
    // support for offset stacks, used to parse compressed labels/names.
    
    private Stack<Integer> offsetStack = new Stack<Integer>();
    
    private void pushOffset(int newOffset) {
        offsetStack.push(offset);
        offset = newOffset;
    }
    
    private void popOffset() {
        offset = offsetStack.pop();
    }
    
    // private static utility methods
    
    private static String[] nameToLabels(String name) {
        return name.split("\\.");
    }
    
    // uncomment if needed
    /*
    private static String labelsToName(String[] labels) {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<labels.length; i++) {
            sb.append(labels[i]);
            if (i != (labels.length-1)) {
                sb.append('.');
            }
        }
        return sb.toString();
    }
    */
    
    private static int lengthInBytes(String string) {
        return stringToBytes(string).length;
    }
    
}
