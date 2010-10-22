package edu.rit.krisher.fileparser.ply;

import java.nio.FloatBuffer;

public class VertexReceiver implements ElementReceiver {

   private FloatBuffer buffer;

   @Override
   public void receive(final Element element, final ElementAttributeValues values) {
      buffer = FloatBuffer.wrap(new float[element.count * 3]);
      final int x = element.indexOf("x");
      final int y = element.indexOf("y");
      final int z = element.indexOf("z");
      for (int idx = 0; idx < element.count; ++idx) {
         values.nextElement();
         buffer.put(values.getScalarComponent(x).floatValue()).put(values.getScalarComponent(y).floatValue()).put(values.getScalarComponent(z).floatValue());
      }
   }

   public float[] getBuffer() {
      return buffer.array();
   }

}
