package edu.rit.krisher.fileparser.ply;

/**
 * Collects a set of triangle vertex indices from a PLY file's 'vertex_indices' element.
 */
public class TrianglesIndexBufferReceiver implements ElementReceiver {

   private int[] buffer;

   @Override
   public void receive(final Element element, final ElementAttributeValues values) {
      buffer = new int[element.count * 3];
      final int indicesAttr = element.indexOf("vertex_indices");

      int indicesOffs = 0;
      for (int idx = 0; idx < element.count; ++idx) {
         values.nextElement();
         final Number[] indices = values.getVectorComponent(indicesAttr);
         buffer[indicesOffs] = indices[0].intValue();
         buffer[indicesOffs + 1] = indices[1].intValue();
         buffer[indicesOffs + 2] = indices[2].intValue();
         indicesOffs += 3;
      }
   }

   public int[] getIndexList() {
      return buffer;
   }

}
