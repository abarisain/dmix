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

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * This class represents a DNS "answer" component.
 * @author simmons
 */
public class DNSAnswer extends DNSComponent {
    
    public String name;
    public Type type;
    public int ttl;
    public byte[] rdata;
    public String rdataString;

    public DNSAnswer(DNSBuffer buffer) {
        parse(buffer);
    }

    @Override
    public int length() {
        // TODO: implement
        return 0;
    }

    @Override
    public void serialize(DNSBuffer buffer) {
        // TODO: implement
    }

    private void parse(DNSBuffer buffer) {
        name = buffer.readName();
        type = Type.getType(buffer.readShort());
        
        // the most significant bit of the rrclass is special
        // in Multicast DNS -- it is used as a "cache flush" bit,
        // and only the least significant 15 bits should be used
        // as the class.
        // see:
        //   http://tools.ietf.org/html/draft-cheshire-dnsext-multicastdns-05
        //   section 11.3
        int aclass = buffer.readShortAsInt();
        //boolean cacheFlush = ((aclass & 0x8000) != 0);
        aclass = aclass & 0x7FFF;
        if (aclass != 1) {
            throw new DNSException("only class IN supported.  (got "+aclass+")");
        }
        
        ttl = buffer.readInteger();
        rdata = buffer.readRdata();
        
        if (type.equals(Type.A) || type.equals(Type.AAAA)) {
            try {
                rdataString = InetAddress.getByAddress(rdata).toString();
            } catch (UnknownHostException e) {
                throw new DNSException("problem parsing rdata");
            }
        } else if (type.equals(Type.TXT)) {
            rdataString = "";
            for (int i=0; i<rdata.length; ) {
                int length = rdata[i++];
                rdataString += DNSBuffer.bytesToString(rdata, i, length);
                i += length;
                if (i != rdata.length) {
                    rdataString += " // ";
                }
            }
        } else if (type.equals(Type.PTR)) {
            // rewind the buffer to the beginning of the
            // name (just after the 16-bit name-length field)
            // and reparse the name to allow for compression
            // offsets.
            int oldoffset = buffer.offset;
            buffer.offset -= rdata.length;
            rdataString = buffer.readName();
            if (oldoffset != buffer.offset) {
                throw new DNSException("bad PTR rdata");
            }
        } else {
            rdataString = "data["+rdata.length+"]";
        }

    }
    
    public String toString() {
        return name+" "+type.toString()+" "+getRdataString();
    }

    public String getRdataString() {
        return rdataString;
    }
}