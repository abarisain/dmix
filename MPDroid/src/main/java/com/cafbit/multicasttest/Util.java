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
package com.cafbit.multicasttest;

/**
 * Various mundate utility methods.
 * @author simmons
 */
public class Util {

    public static String hexDump(byte[] bytes) {
        return hexDump(bytes, 0, bytes.length);
    }

    public static String hexDump(byte[] bytes, int offset, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<length; i+=16) {
            int rowSize = length - i;
            if (rowSize > 16) { rowSize = 16; }
            byte[] row = new byte[rowSize];
            System.arraycopy(bytes, offset+i, row, 0, rowSize);
            hexDumpRow(sb, row, i);
        }
        return sb.toString();
    }

    private static void hexDumpRow(StringBuilder sb, byte[] bytes, int offset) {
        sb.append(String.format("%04X: ",offset));
        for (int i=0; i<16; i++) {
            if (bytes.length > i) {
                sb.append(String.format("%02X ",bytes[i]));
            } else {
                sb.append("   ");
            }
        }
        for (int i=0; i<16; i++) {
            if (bytes.length > i) {
                char c = '.';
                int v = (int)bytes[i];
                if ((v > 0x20) && (v < 0x7F)) {
                    c = (char)v;
                }
                sb.append(c);
            }
        }
        sb.append('\n');
    }

}
