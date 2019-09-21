package org.jscsi.target.context;

import org.jscsi.target.connection.TargetConnection;
import org.jscsi.target.connection.TargetSession;
import org.jscsi.target.connection.TargetSessionManager;
import org.jscsi.target.context.Configuration;
import org.jscsi.target.context.Target;
import org.jscsi.target.scsi.inquiry.DeviceIdentificationVpdPage;

/**
 * @author wangyue1
 * @since 2019/9/20
 */
public interface TargetContext {

    Configuration getConfig();

    DeviceIdentificationVpdPage getDeviceIdentificationVpdPage();

    TargetSessionManager getSessionManager();

    boolean isValidTargetName(String checkTargetName);

    Target getTarget(String targetName);

    String[] getTargetNames();

    void removeTargetSession(TargetSession session);

    void removeTarget(TargetConnection targetConnection);
}
