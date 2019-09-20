package org.jscsi.target.connection;

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
