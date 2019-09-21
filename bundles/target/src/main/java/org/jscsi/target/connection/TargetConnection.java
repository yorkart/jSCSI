package org.jscsi.target.connection;

import org.jscsi.exception.InternetSCSIException;
import org.jscsi.parser.OperationCode;
import org.jscsi.parser.ProtocolDataUnit;
import org.jscsi.target.context.TargetContext;
import org.jscsi.target.connection.phase.TargetFullFeaturePhase;
import org.jscsi.target.connection.phase.TargetLoginPhase;
import org.jscsi.target.connection.phase.TargetPhase;
import org.jscsi.target.connection.stage.fullfeature.PingStage;
import org.jscsi.target.connection.stage.fullfeature.ReadStage;
import org.jscsi.target.settings.ConnectionSettingsNegotiator;
import org.jscsi.target.settings.SessionSettingsNegotiator;
import org.jscsi.target.settings.Settings;
import org.jscsi.target.settings.SettingsException;
import org.jscsi.target.util.FastByteArrayProvider;
import org.jscsi.target.util.SerialArithmeticNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.OperationNotSupportedException;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.security.DigestException;

/**
 * @author wangyue1
 * @since 2019/9/20
 */
public class TargetConnection implements Connection {

    private static final Logger LOGGER = LoggerFactory.getLogger(TargetConnection.class);

    /**
     * The {@link TargetSession} this connection belongs to.
     */
    private TargetSession targetSession;

    /**
     * The {@link TargetSenderWorker} used by this connection for sending and receiving {@link ProtocolDataUnit}s.
     */
    private TargetSenderWorker senderWorker;

    /**
     * The {@link ConnectionSettingsNegotiator} of this connection responsible for negotiating and storing
     * connection parameters which have been negotiated with or declared by the initiator.
     */
    private ConnectionSettingsNegotiator connectionSettingsNegotiator;

    /**
     * The current {@link TargetPhase} describing a general state of the connection.
     */
    private TargetPhase phase;

    /**
     * A counter for the <code>StatSN</code> field of sent {@link ProtocolDataUnit} objects with Status.
     */
    private SerialArithmeticNumber statusSequenceNumber;

    /**
     * Will manage and serve as a source of byte arrays to be used for sending Data In PDUs in the {@link ReadStage}
     * .
     */
    private FastByteArrayProvider dataInArrayProvider = new FastByteArrayProvider(4);

    /**
     * <code>true</code> if and only if this connection is the first connection to be associated with its parent
     * session.
     * <p>
     * This distinction is necessary because some parameters may only be declared over the leading connection.
     */
    private final boolean isLeadingConnection;

    private final TargetContext targetContext;
    /**
     * The last {@link ProtocolDataUnit} received on this connection.
     */
    private ProtocolDataUnit lastReceivedPDU;

    /**
     * The {@link TargetConnection} constructor.
     *
     * @param socketChannel       used for sending and receiving PDUs
     * @param isLeadingConnection <code>true</code> if and only if this connection is the first connection
     *                            associated with its enclosing session
     */
    public TargetConnection(SocketChannel socketChannel, final boolean isLeadingConnection, final TargetContext targetContext) {
        this.isLeadingConnection = isLeadingConnection;
        this.senderWorker = new TargetSenderWorker(this, socketChannel);
        this.targetContext = targetContext;
    }

    /**
     * Returns a byte array that can be used for holding data segment data of Data In PDUs sent during the
     * {@link ReadStage}.
     *
     * @param length the length of the array
     * @return a byte array of the specified length
     */
    public byte[] getDataInArray(final int length) {
        return dataInArrayProvider.getArray(length);
    }

    /**
     * Returns the {@link TargetSession} this connection belongs to.
     *
     * @return the {@link TargetSession} this connection belongs to
     */
    TargetSession getSession() {
        return targetSession;
    }

    /**
     * Sets the {@link TargetSession} this connection belongs to.
     *
     * @param session the {@link TargetSession} this connection belongs to
     */
    public void setSession(TargetSession session) {
        this.targetSession = session;
        senderWorker.setSession(session);
    }

    /**
     * Returns the next {@link ProtocolDataUnit} to be received on the connection.
     * <p>
     * The method will block until a PDU has been completely received.
     *
     * @return the next received PDU
     * @throws DigestException       if a digest error has occured
     * @throws InternetSCSIException if a general iSCSI protocol error has been detected
     * @throws IOException           if the connection was closed
     * @throws SettingsException     will not happen
     */
    public ProtocolDataUnit receivePdu() throws DigestException, InternetSCSIException, IOException, SettingsException {
        lastReceivedPDU = senderWorker.receiveFromWire();

        if (lastReceivedPDU.getBasicHeaderSegment().getOpCode().equals(OperationCode.NOP_OUT)) {
            try {
                // System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Handling ping immediately..");
                // System.out.println("******************************\nRecieving\nSystem Time: " + new
                // java.sql.Timestamp(System.currentTimeMillis()).toString() + "\n" + lastReceivedPDU +
                // "\n******************************");
                new PingStage(new TargetFullFeaturePhase(this)).execute(lastReceivedPDU);
            } catch (InterruptedException e) {
            }
            lastReceivedPDU = senderWorker.receiveFromWire();
        }

        // System.out.println("******************************\nRecieving\nSystem Time: " + new
        // java.sql.Timestamp(System.currentTimeMillis()).toString() + "\n" + lastReceivedPDU +
        // "\n******************************");
        LOGGER.info("receiving pdu: " + lastReceivedPDU);
        return lastReceivedPDU;
    }

    /**
     * Serializes and sends a {@link ProtocolDataUnit} over the connection.
     *
     * @param pdu the PDU to send
     * @throws InterruptedException
     * @throws IOException
     * @throws InternetSCSIException
     */
    public void sendPdu(ProtocolDataUnit pdu) throws InterruptedException, IOException, InternetSCSIException {
        LOGGER.info("sending pdu: " + lastReceivedPDU);
        // System.out.println("******************************\nSending\nSystem Time: " + new
        // java.sql.Timestamp(System.currentTimeMillis()).toString() + "\n" + pdu +
        // "\n******************************");
        senderWorker.sendOverWire(pdu);
    }

    /**
     * Starts the processing of PDUs by this connection.
     * <p>
     * For this method to work properly, the leading PDU send by the initiator over this connection must have been
     * received via {@link #receivePdu()}.
     */
    public void establish() {
        try {
            final ProtocolDataUnit pdu = receivePdu();
            // confirm OpCode-
            if (pdu.getBasicHeaderSegment().getOpCode() != OperationCode.LOGIN_REQUEST) {
                throw new InternetSCSIException();
            }

            this.targetSession = this.targetContext.getSessionManager().createTargetSession(pdu, this, targetContext);

            // *** login phase ***
            phase = new TargetLoginPhase(this);
            if (phase.execute(lastReceivedPDU)) {
                LOGGER.info("Login Phase successful");

                // if this is the leading connection, set the session type
                final Settings settings = getSettings();
                if (isLeadingConnection) {
                    targetSession.setSessionType(SessionType.getSessionType(settings.getSessionType()));
                }
                targetSession.setTargetName(settings.getTargetName());
                // *** full feature phase ***
                phase = new TargetFullFeaturePhase(this);

                phase.execute();
            }
            senderWorker.close();
        } catch (OperationNotSupportedException | IOException | InterruptedException | InternetSCSIException | DigestException | SettingsException e) {
            LOGGER.error("Exception throws", e);
        }

        targetSession.removeTargetConnection(this);

        LOGGER.info("closed connection");
    }

    public TargetSession getTargetSession() {
        return targetSession;
    }

    /**
     * Returns <code>true</code> if this is the leading connection, i.e. the first TargetConnection in the
     * connection's {@link TargetSession}. Otherwise <code>false</code> is returned.
     *
     * @return <code>true</code> if this is the leading connection
     */
    public boolean isLeadingConnection() {
        return isLeadingConnection;
    }

    /**
     * Initializes {@link #connectionSettingsNegotiator}.
     * <p>
     * This method must be be called after the this connection has been added to its session.
     */
    public void initializeConnectionSettingsNegotiator(final SessionSettingsNegotiator sessionSettingsNegotiator) {
        connectionSettingsNegotiator = new ConnectionSettingsNegotiator(sessionSettingsNegotiator);
    }

    /**
     * Returns a {@link Settings} object with a snapshot of the current connection and session parameters.
     *
     * @return the current {@link Settings}
     */
    public Settings getSettings() {
        return connectionSettingsNegotiator.getSettings();
    }

    public ConnectionSettingsNegotiator getConnectionSettingsNegotiator() {
        return connectionSettingsNegotiator;
    }

    public SerialArithmeticNumber getStatusSequenceNumber() {
        return statusSequenceNumber;
    }

    public void setStatusSequenceNumber(final int statusSequenceNumber) {
        this.statusSequenceNumber = new SerialArithmeticNumber(statusSequenceNumber);
    }

    public boolean stop() {
        if (phase instanceof TargetFullFeaturePhase) {
            ((TargetFullFeaturePhase) phase).stop();
            return true;
        }

        return false;
    }
}
