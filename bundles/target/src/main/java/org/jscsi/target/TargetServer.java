package org.jscsi.target;

import org.jscsi.parser.ProtocolDataUnit;
import org.jscsi.target.connection.TargetConnection;
import org.jscsi.target.connection.TargetSession;
import org.jscsi.target.connection.TargetSessionManager;
import org.jscsi.target.context.Configuration;
import org.jscsi.target.context.Target;
import org.jscsi.target.context.TargetContext;
import org.jscsi.target.scsi.inquiry.DeviceIdentificationVpdPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The central class of the jSCSI Target, which keeps track of all active {@link TargetSession}s, stores target-wide
 * parameters and variables, and which contains the {@link TargetApplication#main(String[])} method for starting the program.
 *
 * @author Andreas Ergenzinger, University of Konstanz
 * @author Sebastian Graf, University of Konstanz
 */
public class TargetServer implements Callable<Void>, TargetContext {
    protected static final Logger LOGGER = LoggerFactory.getLogger(TargetServer.class);

    /**
     * A {@link SocketChannel} used for listening to incoming connections.
     */
    private ServerSocketChannel serverSocketChannel;

    /**
     * Contains all active {@link TargetSession}s.
     */
    private TargetSessionManager sessionManager = new TargetSessionManager();

    /**
     * The jSCSI Target's global parameters.
     */
    private Configuration config;

    /**
     *
     */
    private DeviceIdentificationVpdPage deviceIdentificationVpdPage;

    /**
     * The table of targets
     */
    private final HashMap<String, Target> targets = new HashMap<>();

    /**
     * The thread pool.
     */
    private final ExecutorService workerPool;

    /**
     * A target-wide counter used for providing the value of sent {@link ProtocolDataUnit}s'
     * <code>Target Transfer Tag</code> field, unless that field is reserved.
     */
    private static final AtomicInteger nextTargetTransferTag = new AtomicInteger();

    /**
     * while this value is true, the target is active.
     */
    private boolean running = true;

    public TargetServer(final Configuration conf) {
        this.config = conf;

        LOGGER.info("Starting jSCSI-target: ");

        // read target settings from configuration file
        LOGGER.info("   port:           " + getConfig().getPort());
        LOGGER.info("   loading targets.");
        // open the storage medium
        List<Target> targetInfo = getConfig().getTargets();
        for (Target curTargetInfo : targetInfo) {
            targets.put(curTargetInfo.getTargetName(), curTargetInfo);
            // print configuration and medium details
            LOGGER.info("   target name:    " + curTargetInfo.getTargetName() + " loaded.");
        }

        this.deviceIdentificationVpdPage = new DeviceIdentificationVpdPage(this);
        this.workerPool = Executors.newCachedThreadPool();
    }

    /**
     * Gets and increments the value to use in the next unreserved <code>Target Transfer Tag</code> field of the next
     * PDU to be sent by the jSCSI Target.
     *
     * @return the value to use in the next unreserved <code>Target Transfer Tag
     * </code> field
     * @see #nextTargetTransferTag
     */
    public static int getNextTargetTransferTag() {
        // value 0xffffffff is reserved
        int tag;
        do {
            tag = nextTargetTransferTag.getAndIncrement();
        } while (tag == -1);
        return tag;
    }

    private class ConnectionHandler implements Callable<Void> {

        private final TargetConnection targetConnection;

        ConnectionHandler(TargetConnection targetConnection) {
            this.targetConnection = targetConnection;
        }

        @Override
        public Void call() throws Exception {
            try {
                targetConnection.establish();
            } catch (Exception e) {
                LOGGER.error("running target error:", e);
            } finally {
                // todo remove target, why???
                // coming back from call() means the session is ended
                // we can delete the target from local cache.
                synchronized (targets) {
                    Target target = targetConnection.getTargetSession().getTarget();
                    if (target != null) {
                        targets.remove(target.getTargetName());
                        try {
                            target.getStorageModule().close();
                        } catch (Exception e) {
                            LOGGER.error("Error when closing storage:", e);
                        }
                        LOGGER.info("closed local storage module");
                    } else {
                        LOGGER.warn("No target to delete on logout?");
                    }
                }
            }
            return null;
        }
    }

    public Void call() throws Exception {

        // Create a blocking server socket and check for connections
        try {
            // Create a blocking server socket channel on the specified/default
            // port
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(true);

            // Making sure the socket is bound to the address used in the config.
            serverSocketChannel.socket().bind(new InetSocketAddress(getConfig().getTargetAddress(), getConfig().getPort()));

            while (running) {
                // Accept the connection request.
                // If serverSocketChannel is blocking, this method blocks.
                // The returned channel is in blocking mode.
                final SocketChannel socketChannel = serverSocketChannel.accept();

                // deactivate Nagle algorithm
                socketChannel.socket().setTcpNoDelay(true);

                workerPool.submit(() -> new TargetWorkHandler(socketChannel, this).handle());
            }
        } catch (IOException e) {
            // this block is entered if the desired port is already in use
            LOGGER.error("Throws Exception", e);
        }

        System.out.println("Closing socket channel.");
        serverSocketChannel.close();
        for (TargetSession session : sessionManager.iterable()) {
            System.out.println("Commiting uncommited changes.");
            session.getStorageModule().close();
        }
        return null;
    }

    public Configuration getConfig() {
        return config;
    }

    public DeviceIdentificationVpdPage getDeviceIdentificationVpdPage() {
        return deviceIdentificationVpdPage;
    }

    public Target getTarget(String targetName) {
        synchronized (targets) {
            return targets.get(targetName);
        }
    }

    public void removeTarget(TargetConnection targetConnection) {
        synchronized (targets) {
            Target target = targetConnection.getTargetSession().getTarget();
            if (target != null) {
                targets.remove(target.getTargetName());
                try {
                    target.getStorageModule().close();
                } catch (Exception e) {
                    LOGGER.error("Error when closing storage:", e);
                }
                LOGGER.info("closed local storage module");
            } else {
                LOGGER.warn("No target to delete on logout?");
            }
        }
    }

    public TargetSessionManager getSessionManager() {
        return sessionManager;
    }

    /**
     * Removes a session from the jSCSI Target's list of active sessions.
     *
     * @param session the session to remove from the list of active sessions
     */
    public synchronized void removeTargetSession(TargetSession session) {
        sessionManager.remove(session);
    }

    public String[] getTargetNames() {
        String[] returnNames = new String[targets.size()];
        returnNames = targets.keySet().toArray(returnNames);
        return returnNames;
    }

    /**
     * Checks to see if this target name is valid.
     *
     * @param checkTargetName targetName for check
     * @return true if the the target name is configured
     */
    public boolean isValidTargetName(String checkTargetName) {
        return targets.containsKey(checkTargetName);
    }

    /**
     * Stop this target server
     */
    public void stop() {
        this.running = false;
        for (TargetSession session : sessionManager.iterable()) {
            if (!session.getConnection().stop()) {
                this.running = true;
                LOGGER.error("Unable to stop session for " + session.getTargetName());
            }
        }
    }

}
