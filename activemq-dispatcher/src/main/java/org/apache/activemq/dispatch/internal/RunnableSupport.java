/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.dispatch.internal;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.activemq.dispatch.DispatchQueue;

/**
 * 
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class RunnableSupport {

    private static Runnable NO_OP = new Runnable() {
        public void run() {
        }
        public String toString() {
            return "{}";
        };
    };
    
    public static Runnable runNoop() {
        return NO_OP;
    }
    
    public static Runnable runOnceAfter(final Runnable runnable, int count) {
        if( count <= 0 || runnable==null ) {
            return NO_OP;
        }
        if( count == 1 ) {
            return runnable;
        }
        final AtomicInteger counter = new AtomicInteger(count);
        return new Runnable() {
            public void run() {
                if( counter.decrementAndGet()==0 ) {
                    runnable.run();
                }
            }
            public String toString() {
                return "{"+runnable+"}";
            };
        };
    }
    
    public static Runnable runAfter(final Runnable runnable, int count) {
        if( count <= 0 || runnable==null ) {
            return NO_OP;
        }
        if( count == 1 ) {
            return runnable;
        }
        final AtomicInteger counter = new AtomicInteger(count);
        return new Runnable() {
            public void run() {
                if( counter.decrementAndGet()<=0 ) {
                    runnable.run();
                }
            }
            public String toString() {
                return "{"+runnable+"}";
            };
        };
    }
    
    public static Runnable runOnceAfter(final DispatchQueue queue, final Runnable runnable, int count) {
        if( count <= 0 || runnable==null ) {
            return NO_OP;
        }
        final AtomicInteger counter = new AtomicInteger(count);
        return new Runnable() {
            public void run() {
                if( counter.decrementAndGet()==0 ) {
                    queue.dispatchAsync(runnable);
                }
            }
            public String toString() {
                return "{"+runnable+"}";
            };
        };
    }
    
    public static Runnable runAfter(final DispatchQueue queue,  final Runnable runnable, int count) {
        if( count <= 0 || runnable==null ) {
            return NO_OP;
        }
        final AtomicInteger counter = new AtomicInteger(count);
        return new Runnable() {
            public void run() {
                if( counter.decrementAndGet()<=0 ) {
                    queue.dispatchAsync(runnable);
                }
            }
            public String toString() {
                return "{"+runnable.toString()+"}";
            };
        };
    }


}
