package edu.rit.krisher.fileparser.ply;

import edu.rit.krisher.scene.geometry.buffer.IndexBuffer;

public class TrianglesIndexBufferReceiver implements ElementReceiver {

   private IndexBuffer buffer;

   @Override
   public void receive(final Element element, final ElementAttributeValues values) {
      buffer = new IndexBuffer(element.count * 3);
      final int indicesAttr = element.indexOf("vertex_indices");

      for (int idx = 0; idx < element.count; ++idx) {
         values.nextElement();
         final Number[] indices = values.getVectorComponent(indicesAttr);
         buffer.put(indices[0].intValue()).put(indices[1].intValue()).put(indices[2].intValue());
      }
      buffer.flip();
   }

   public IndexBuffer getBuffer() {
      return buffer;
   }

}
