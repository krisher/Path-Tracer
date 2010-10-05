package edu.rit.krisher.fileparser.ply;

import edu.rit.krisher.scene.geometry.buffer.Vec3fBuffer;

public class VertexReceiver implements ElementReceiver {

   private Vec3fBuffer buffer;

   @Override
   public void receive(final Element element, final ElementAttributeValues values) {
      buffer = new Vec3fBuffer(element.count);
      final int x = element.indexOf("x");
      final int y = element.indexOf("y");
      final int z = element.indexOf("z");
      for (int idx = 0; idx < element.count; ++idx) {
         values.nextElement();
         buffer.put(values.getScalarComponent(x).floatValue(), values.getScalarComponent(y).floatValue(), values.getScalarComponent(z).floatValue());
      }
      buffer.flip();
   }

   public Vec3fBuffer getBuffer() {
      return buffer;
   }

}
