package org.jscsi.target.connection;

import org.jscsi.exception.InternetSCSIException;
import org.jscsi.parser.OperationCode;
import org.jscsi.parser.ProtocolDataUnit;
import org.jscsi.parser.login.ISID;
import org.jscsi.parser.login.LoginRequestParser;
import org.jscsi.target.TargetServer;
import org.jscsi.target.settings.SettingsException;

import java.io.IOException;
import java.security.DigestException;

/**
 * @author wangyue1
 * @since 2019/9/20
 */
public class ConnectionPrepare {

    private TargetConnection connection;
    private TargetSessionManager sessionManager;
    private TargetServer targetServer;

    public ConnectionPrepare(TargetConnection connection, TargetSessionManager sessionManager, TargetServer targetServer) {
        this.connection = connection;
        this.sessionManager = sessionManager;
        this.targetServer = targetServer;
    }

    public void execute() throws DigestException, InternetSCSIException, SettingsException, IOException {
        final ProtocolDataUnit pdu = connection.receivePdu();
        // confirm OpCode-
        if (pdu.getBasicHeaderSegment().getOpCode() != OperationCode.LOGIN_REQUEST) {
            throw new InternetSCSIException();
        }

        // get initiatorSessionID
        LoginRequestParser parser = (LoginRequestParser) pdu.getBasicHeaderSegment().getParser();
        ISID initiatorSessionID = parser.getInitiatorSessionID();

        /*
         * TODO get (new or existing) session based on TSIH But since we don't do session reinstatement and
         * MaxConnections=1, we can just create a new one.
         */
        TargetSession session = new TargetSession(
                targetServer,
                connection,
                initiatorSessionID,
                parser.getCommandSequenceNumber(),// set ExpCmdSN (PDU is immediate, hence no ++)
                parser.getExpectedStatusSequenceNumber());

        sessionManager.add(session);
    }
}
