/**
 * Copyright (c) 2012, University of Konstanz, Distributed Systems Group All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met: * Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer. * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or other materials provided with the
 * distribution. * Neither the name of the University of Konstanz nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.jscsi.initiator.connection.state;


import org.jscsi.exception.InternetSCSIException;
import org.jscsi.initiator.connection.Connection;
import org.jscsi.parser.OperationCode;
import org.jscsi.parser.ProtocolDataUnit;
import org.jscsi.parser.datasegment.DataSegmentFactory;
import org.jscsi.parser.datasegment.DataSegmentFactory.DataSegmentFormat;
import org.jscsi.parser.datasegment.IDataSegment;
import org.jscsi.parser.datasegment.IDataSegmentIterator;
import org.jscsi.parser.datasegment.IDataSegmentIterator.IDataSegmentChunk;
import org.jscsi.parser.datasegment.OperationalTextKey;
import org.jscsi.parser.datasegment.SettingsMap;
import org.jscsi.parser.text.TextRequestParser;


/**
 * <h1>GetConnectionsRequestState</h1>
 * <p/>
 * This state requests a list of all possible connections to a specific target. So it sends a TextRequest PDU with
 * <code>SendTargets=</code> as the only <code>OperationalTextKey</code> as data segment.
 *
 * @author Volker Wildi
 */
public final class GetConnectionsRequestState extends AbstractState {

    // --------------------------------------------------------------------------
    // --------------------------------------------------------------------------

    /**
     * Constructor to create a <code>GetConnectionsRequestState</code> instance, which uses the given connection for
     * transmission.
     *
     * @param initConnection The context connection, which is used for the network transmission.
     */
    public GetConnectionsRequestState(final Connection initConnection) {

        super(initConnection);
    }

    // --------------------------------------------------------------------------
    // --------------------------------------------------------------------------

    /** {@inheritDoc} */
    public final void execute() throws InternetSCSIException {

        final ProtocolDataUnit protocolDataUnit = protocolDataUnitFactory.create(false, true, OperationCode.TEXT_REQUEST, connection.getSetting(OperationalTextKey.HEADER_DIGEST), connection.getSetting(OperationalTextKey.DATA_DIGEST));
        final TextRequestParser parser = (TextRequestParser) protocolDataUnit.getBasicHeaderSegment().getParser();

        final SettingsMap settings = new SettingsMap();
        settings.add(OperationalTextKey.SEND_TARGETS, "");

        final IDataSegment dataSegment = DataSegmentFactory.create(settings.asByteBuffer(), DataSegmentFormat.TEXT, connection.getSettingAsInt(OperationalTextKey.MAX_RECV_DATA_SEGMENT_LENGTH));

        int bytes2Process = dataSegment.getLength();
        for (IDataSegmentIterator dataSegmentIterator = dataSegment.iterator(); dataSegmentIterator.hasNext(); ) {
            IDataSegmentChunk dataSegmentChunk = dataSegmentIterator.next(bytes2Process);
            protocolDataUnit.setDataSegment(dataSegmentChunk);
            parser.setTargetTransferTag(0xFFFFFFFF);
        }

        connection.send(protocolDataUnit);
        connection.nextState(new GetConnectionsResponseState(connection));
        super.stateFollowing = true;
        // return true;
    }

    // --------------------------------------------------------------------------
    // --------------------------------------------------------------------------
    // --------------------------------------------------------------------------
    // --------------------------------------------------------------------------

}
