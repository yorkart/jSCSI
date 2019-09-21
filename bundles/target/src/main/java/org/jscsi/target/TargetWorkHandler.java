package org.jscsi.target;

import org.jscsi.target.connection.TargetConnection;
import org.jscsi.target.context.TargetContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.SocketChannel;

/**
 * @author wangyue1
 * @since 2019/9/20
 */
public class TargetWorkHandler {
    protected static final Logger LOGGER = LoggerFactory.getLogger(TargetWorkHandler.class);

    private final SocketChannel socketChannel;
    private final TargetContext targetContext;

    public TargetWorkHandler(SocketChannel socketChannel, TargetContext targetContext) {
        this.socketChannel = socketChannel;
        this.targetContext = targetContext;
    }

    public void handle() {
        TargetConnection newConnection = new TargetConnection(socketChannel, true, targetContext);
//        ConnectionPrepare connectionPrepare = new ConnectionPrepare(newConnection, targetServer.getSessionManager(), targetServer);

        try {
//            connectionPrepare.execute();
            newConnection.establish();
        } catch (Exception e) {
            LOGGER.error("running target error:", e);
        } finally {
            // todo remove target, why???
            // coming back from call() means the session is ended
            // we can delete the target from local cache.
            targetContext.removeTarget(newConnection);
        }
    }

}
