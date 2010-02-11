/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * his work for additional information regarding copyright ownership.
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
package org.apache.activemq.amqp.protocol.marshaller.v1_0_0;

import java.io.DataInput;
import java.io.IOException;
import org.apache.activemq.amqp.protocol.marshaller.AmqpEncodingError;
import org.apache.activemq.amqp.protocol.marshaller.Encoded;
import org.apache.activemq.amqp.protocol.marshaller.UnexpectedTypeException;
import org.apache.activemq.amqp.protocol.marshaller.v1_0_0.Encoder;
import org.apache.activemq.amqp.protocol.marshaller.v1_0_0.Encoder.*;
import org.apache.activemq.amqp.protocol.types.AmqpMap;
import org.apache.activemq.amqp.protocol.types.AmqpSequenceNo;
import org.apache.activemq.amqp.protocol.types.AmqpSessionError;
import org.apache.activemq.amqp.protocol.types.AmqpString;
import org.apache.activemq.amqp.protocol.types.AmqpSymbol;
import org.apache.activemq.amqp.protocol.types.AmqpType;
import org.apache.activemq.amqp.protocol.types.AmqpUbyte;
import org.apache.activemq.amqp.protocol.types.AmqpUlong;
import org.apache.activemq.amqp.protocol.types.AmqpUshort;
import org.apache.activemq.amqp.protocol.types.IAmqpList;
import org.apache.activemq.util.buffer.Buffer;

public class AmqpSessionErrorMarshaller implements DescribedTypeMarshaller<AmqpSessionError>{

    static final AmqpSessionErrorMarshaller SINGLETON = new AmqpSessionErrorMarshaller();
    private static final Encoded<IAmqpList> NULL_ENCODED = new Encoder.NullEncoded<IAmqpList>();

    public static final String SYMBOLIC_ID = "amqp:session-error:list";
    //Format code: 0x00000001:0x00000102:
    public static final long CATEGORY = 1;
    public static final long DESCRIPTOR_ID = 258;
    public static final long NUMERIC_ID = CATEGORY << 32 | DESCRIPTOR_ID; //(4294967554L)
    //Hard coded descriptor:
    public static final EncodedBuffer DESCRIPTOR = FormatCategory.createBuffer(new Buffer(new byte [] {
        (byte) 0x80,                                         // ulong descriptor encoding)
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,  // CATEGORY CODE
        (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x02   // DESCRIPTOR ID CODE
    }), 0);

    private static final ListDecoder DECODER = new ListDecoder() {
        public final AmqpType<?, ?> unmarshalType(int pos, DataInput in) throws IOException {
            switch(pos) {
            case 0: {
                return AmqpUshort.AmqpUshortBuffer.create(AmqpUshortMarshaller.createEncoded(in));
            }
            case 1: {
                return AmqpSequenceNo.AmqpSequenceNoBuffer.create(AmqpUintMarshaller.createEncoded(in));
            }
            case 2: {
                return AmqpUbyte.AmqpUbyteBuffer.create(AmqpUbyteMarshaller.createEncoded(in));
            }
            case 3: {
                return AmqpUbyte.AmqpUbyteBuffer.create(AmqpUbyteMarshaller.createEncoded(in));
            }
            case 4: {
                return AmqpString.AmqpStringBuffer.create(AmqpStringMarshaller.createEncoded(in));
            }
            case 5: {
                return AmqpMap.AmqpMapBuffer.create(AmqpMapMarshaller.createEncoded(in));
            }
            default: {
                return AmqpMarshaller.SINGLETON.unmarshalType(in);
            }
            }
        }

        public final AmqpType<?, ?> decodeType(int pos, EncodedBuffer buffer) throws AmqpEncodingError {
            switch(pos) {
            case 0: {
                return AmqpUshort.AmqpUshortBuffer.create(AmqpUshortMarshaller.createEncoded(buffer));
            }
            case 1: {
                return AmqpSequenceNo.AmqpSequenceNoBuffer.create(AmqpUintMarshaller.createEncoded(buffer));
            }
            case 2: {
                return AmqpUbyte.AmqpUbyteBuffer.create(AmqpUbyteMarshaller.createEncoded(buffer));
            }
            case 3: {
                return AmqpUbyte.AmqpUbyteBuffer.create(AmqpUbyteMarshaller.createEncoded(buffer));
            }
            case 4: {
                return AmqpString.AmqpStringBuffer.create(AmqpStringMarshaller.createEncoded(buffer));
            }
            case 5: {
                return AmqpMap.AmqpMapBuffer.create(AmqpMapMarshaller.createEncoded(buffer));
            }
            default: {
                return AmqpMarshaller.SINGLETON.decodeType(buffer);
            }
            }
        }
    };

    public static class AmqpSessionErrorEncoded extends DescribedEncoded<IAmqpList> {

        public AmqpSessionErrorEncoded(DescribedBuffer buffer) {
            super(buffer);
        }

        public AmqpSessionErrorEncoded(AmqpSessionError value) {
            super(AmqpListMarshaller.encode(value));
        }

        protected final String getSymbolicId() {
            return SYMBOLIC_ID;
        }

        protected final long getNumericId() {
            return NUMERIC_ID;
        }

        protected final Encoded<IAmqpList> decodeDescribed(EncodedBuffer encoded) throws AmqpEncodingError {
            return AmqpListMarshaller.createEncoded(encoded, DECODER);
        }

        protected final Encoded<IAmqpList> unmarshalDescribed(DataInput in) throws IOException {
            return AmqpListMarshaller.createEncoded(in, DECODER);
        }

        protected final EncodedBuffer getDescriptor() {
            return DESCRIPTOR;
        }
    }

    public static final Encoded<IAmqpList> encode(AmqpSessionError value) throws AmqpEncodingError {
        return new AmqpSessionErrorEncoded(value);
    }

    static final Encoded<IAmqpList> createEncoded(Buffer source, int offset) throws AmqpEncodingError {
        return createEncoded(FormatCategory.createBuffer(source, offset));
    }

    static final Encoded<IAmqpList> createEncoded(DataInput in) throws IOException, AmqpEncodingError {
        return createEncoded(FormatCategory.createBuffer(in.readByte(), in));
    }

    static final Encoded<IAmqpList> createEncoded(EncodedBuffer buffer) throws AmqpEncodingError {
        byte fc = buffer.getEncodingFormatCode();
        if (fc == Encoder.NULL_FORMAT_CODE) {
            return NULL_ENCODED;
        }

        DescribedBuffer db = buffer.asDescribed();
        AmqpType<?, ?> descriptor = AmqpMarshaller.SINGLETON.decodeType(db.getDescriptorBuffer());
        if(!(descriptor instanceof AmqpUlong && ((AmqpUlong)descriptor).getValue().longValue() == NUMERIC_ID ||
               descriptor instanceof AmqpSymbol && ((AmqpSymbol)descriptor).getValue().equals(SYMBOLIC_ID))) {
            throw new UnexpectedTypeException("descriptor mismatch: " + descriptor);
        }
        return new AmqpSessionErrorEncoded(db);
    }

    public final AmqpSessionError.AmqpSessionErrorBuffer decodeDescribedType(AmqpType<?, ?> descriptor, DescribedBuffer encoded) throws AmqpEncodingError {
        return AmqpSessionError.AmqpSessionErrorBuffer.create(new AmqpSessionErrorEncoded(encoded));
    }
}