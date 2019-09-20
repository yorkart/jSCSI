package org.jscsi.target.connection;

import org.jscsi.exception.InternetSCSIException;
import org.jscsi.parser.ProtocolDataUnit;
import org.jscsi.target.settings.ConnectionSettingsNegotiator;
import org.jscsi.target.settings.SessionSettingsNegotiator;
import org.jscsi.target.settings.Settings;
import org.jscsi.target.settings.SettingsException;
import org.jscsi.target.util.SerialArithmeticNumber;

import java.io.IOException;
import java.security.DigestException;
import java.util.concurrent.Callable;

/**
 * A class for objects representing an iSCSI connection with all necessary variables.
 * <p>
 * Each {@link TargetConnection} runs in a separate {@link Thread}. The conceptually most important parts of its
 * behavior can be likened to a finite state machine (FSM), in which the most basic states (stages) are grouped into
 * more general states (phases). Commands send by the initiator are carried out in these stages, usually without
 * transitioning to a different phase. A connection's current phase determines which stages are reachable, limiting the
 * kind of commands the initiator may issue at any given moment.
 *
 * @author Andreas Ergenzinger
 */
public interface Connection {

    Settings getSettings();

    SerialArithmeticNumber getStatusSequenceNumber();

    boolean isLeadingConnection();

    ProtocolDataUnit receivePdu() throws DigestException, InternetSCSIException, IOException, SettingsException;

    void sendPdu(ProtocolDataUnit pDataUnit) throws InterruptedException, IOException, InternetSCSIException;

    ConnectionSettingsNegotiator getConnectionSettingsNegotiator();

    void setSession(TargetSession pSession);

    TargetSession getTargetSession();

    void setStatusSequenceNumber(int pStatusSequenceNumber);

    void initializeConnectionSettingsNegotiator(SessionSettingsNegotiator pSettingsNegotiator);

    byte[] getDataInArray(int pLength);

    public boolean stop();

}
