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

/**
 * This class represents a DNS "question" component.
 * @author simmons
 */
public class DNSQuestion extends DNSComponent {
    
    public Type type;
    public String name;
    
    public DNSQuestion(Type type, String name) {
        this.type = type;
        this.name = name;
    }
    
    public DNSQuestion(DNSBuffer buffer) {
        parse(buffer);
    }
    
    /**
     * Return the expected byte length of this question.
     */
    public int length() {
        int length = DNSBuffer.nameByteLength(name);
        length += 5; // zero-terminating length byte, qtype short, qclass short 
        return length;
    }
    
    /**
     * Render this DNS question into a byte buffer
     */
    public void serialize(DNSBuffer buffer) {
        buffer.checkRemaining(length());
        buffer.writeName(name); // qname
        buffer.writeShort(type.qtype); // qtype
        buffer.writeShort(1); // qclass (IN)
    }

    /**
     * Parse a question from the byte buffer
     * @param buffer
     */
    private void parse(DNSBuffer buffer) {
        name = buffer.readName();
        type = Type.getType(buffer.readShort());
        int qclass = buffer.readShort();
        if (qclass != 1) {
            throw new DNSException("only class IN supported.  (got "+qclass+")");
        }
    }
    
    public String toString() {
        return type.toString()+"? "+name;
    }

}
