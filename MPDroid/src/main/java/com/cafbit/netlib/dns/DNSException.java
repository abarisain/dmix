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

public class DNSException extends RuntimeException {
    private static final long serialVersionUID = 372670807060894755L;
    
    public DNSException() {}
    public DNSException(String message) {
        super(message);
    }
    public DNSException(Throwable e) {
        super(e);
    }
    public DNSException(String message, Throwable e) {
        super(message, e);
    }
    
}
