package org.jscsi.target.connection;

import org.jscsi.parser.ProtocolDataUnit;
import org.jscsi.parser.login.ISID;
import org.jscsi.parser.login.LoginRequestParser;
import org.jscsi.target.context.TargetContext;
import org.jscsi.target.TargetServer;

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

/**
 * @author wangyue1
 * @since 2019/9/20
 */
public class TargetSessionManager {

    /**
     * Contains all active {@link TargetSession}s.
     */
    private Collection<TargetSession> sessions = new Vector<>();

    public TargetSession createTargetSession(final ProtocolDataUnit pdu, Connection connection, TargetContext targetContext) {
        // get initiatorSessionID
        LoginRequestParser parser = (LoginRequestParser) pdu.getBasicHeaderSegment().getParser();
        ISID initiatorSessionID = parser.getInitiatorSessionID();

        /*
         * TODO get (new or existing) session based on TSIH But since we don't do session reinstatement and
         * MaxConnections=1, we can just create a new one.
         */
        TargetSession session = new TargetSession(
                (TargetServer) targetContext,
                connection,
                initiatorSessionID,
                parser.getCommandSequenceNumber(),// set ExpCmdSN (PDU is immediate, hence no ++)
                parser.getExpectedStatusSequenceNumber());

        this.add(session);
        return session;
    }

    public void add(TargetSession session) {
        sessions.add(session);
    }

    public void remove(TargetSession session) {
        sessions.remove(session);
    }

    public Iterator<TargetSession> iterator() {
        return sessions.iterator();
    }

    public Iterable<TargetSession> iterable() {
        return sessions;
    }
}
