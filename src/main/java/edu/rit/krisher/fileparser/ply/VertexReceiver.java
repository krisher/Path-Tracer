package edu.rit.krisher.fileparser.ply;

import edu.rit.krisher.scene.geometry.buffer.Vec3fBuffer;
import edu.rit.krisher.vecmath.Vec3;

public class VertexReceiver implements ElementReceiver {

   private Vec3fBuffer buffer;

   @Override
   public void receive(final Element element, final ElementAttributeValues values) {
      buffer = new Vec3fBuffer(element.count);
      final int x = element.indexOf("x");
      final int y = element.indexOf("y");
      final int z = element.indexOf("z");

      final Vec3 vertex = new Vec3();
      for (int idx = 0; idx < element.count; ++idx) {
         values.nextElement();
         vertex.x = values.getScalarComponent(x).floatValue();
         vertex.y = values.getScalarComponent(y).floatValue();
         vertex.z = values.getScalarComponent(z).floatValue();
         buffer.put(vertex);
      }
      buffer.flip();
   }

   public Vec3fBuffer getBuffer() {
      return buffer;
   }

}
