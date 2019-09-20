package org.jscsi.target.scsi.cdb;


import java.nio.ByteBuffer;


/**
 * This abstract class represents Command Descriptor Blocks for <code>WRITE</code> SCSI commands.
 *
 * @author Andreas Ergenzinger
 * @see Write6Cdb
 * @see Write10Cdb
 */
public abstract class WriteCdb extends ReadOrWriteCdb {

    public WriteCdb(ByteBuffer buffer) {
        super(buffer);
    }

}
