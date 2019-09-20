package org.jscsi.target.settings;

import org.jscsi.target.settings.entry.Entry;

/**
 * The {@link NegotiationType} of an {@link Entry} affects the way the {@link Entry}'s parameter final value is reached.
 *
 * @author Andreas Ergenzinger
 */
public enum NegotiationType {
    /**
     * The parameter is declared, i.e. the iSCSI initiator will determine the value and only inform the jSCSI Target
     * about its selection.
     * target要选择一个值给initiator
     */
    DECLARED,
    /**
     * The parameter must be negotiated, i.e. the jSCSI Target must try to select a mutually supported value and return
     * the result to the initiator.
     * 当前值要原封不动返回给initiator，（已协商，也等于不用协商）
     */
    NEGOTIATED
}
