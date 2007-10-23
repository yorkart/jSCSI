
package org.jscsi.scsi.protocol.mode;

import java.io.DataInputStream;
import java.nio.ByteBuffer;

public abstract class ExtendedModePage extends ModePage
{
   public static final byte PAGE_CODE = 0x15;

   private int PAGE_LENGTH;

   public ExtendedModePage(int subPageCode, int pageLength)
   {
      super(PAGE_CODE, subPageCode);
      PAGE_LENGTH = pageLength;
   }

   @Override
   abstract protected void decodeModeParameters(int dataLength, DataInputStream inputStream);

   @Override
   abstract protected void encodeModeParameters(ByteBuffer output);

   @Override
   protected int getPageLength()
   {
      return PAGE_LENGTH;
   }
}