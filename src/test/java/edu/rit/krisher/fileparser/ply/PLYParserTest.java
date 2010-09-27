package edu.rit.krisher.fileparser.ply;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.zip.ZipInputStream;

import org.junit.Assert;
import org.junit.Test;


public class PLYParserTest {

   private static final String bunnyResource = "/edu/rit/krisher/fileparser/ply/bun_zipper.ply.zip";

   @Test
   public void testParseBunnyHeader() throws IOException {
      final PLYContentDescription content;
      /*
       * Assumes that the ply file is the first entry in the zip...
       */
      final ZipInputStream zis = new ZipInputStream(PLYParserTest.class.getResourceAsStream(bunnyResource));
      try {
         zis.getNextEntry();
         final BufferedReader reader = new BufferedReader(new InputStreamReader(zis, Charset.forName("US-ASCII")));
         try {
            content = new PLYContentDescription(reader);
         } finally {
            reader.close();
         }
      } finally {
         zis.close();
      }

      Assert.assertEquals(PLYFormat.ascii, content.getFormat());
      Assert.assertEquals("1.0", content.getPLYVersion());

      Assert.assertEquals(1, content.getComments().size());
      Assert.assertEquals("zipper output", content.getComments().get(0));

      Assert.assertEquals(2, content.getElements().size());

      final ElementSchema vertexElt = content.getElements().get(0);
      Assert.assertEquals("vertex", vertexElt.name);
      Assert.assertEquals(35947, vertexElt.count);
      Assert.assertEquals(5, vertexElt.getProperties().size());
      Assert.assertEquals("x", vertexElt.getProperties().get(0).name);
      Assert.assertEquals(DataType.float32, vertexElt.getProperties().get(0).valueType);
      Assert.assertEquals(null, vertexElt.getProperties().get(0).listIndexType);
      Assert.assertEquals("y", vertexElt.getProperties().get(1).name);
      Assert.assertEquals(DataType.float32, vertexElt.getProperties().get(1).valueType);
      Assert.assertEquals(null, vertexElt.getProperties().get(1).listIndexType);
      Assert.assertEquals("z", vertexElt.getProperties().get(2).name);
      Assert.assertEquals(DataType.float32, vertexElt.getProperties().get(2).valueType);
      Assert.assertEquals(null, vertexElt.getProperties().get(2).listIndexType);
      Assert.assertEquals("confidence", vertexElt.getProperties().get(3).name);
      Assert.assertEquals(DataType.float32, vertexElt.getProperties().get(3).valueType);
      Assert.assertEquals(null, vertexElt.getProperties().get(3).listIndexType);
      Assert.assertEquals("intensity", vertexElt.getProperties().get(4).name);
      Assert.assertEquals(DataType.float32, vertexElt.getProperties().get(4).valueType);
      Assert.assertEquals(null, vertexElt.getProperties().get(4).listIndexType);

      final ElementSchema facesElt = content.getElements().get(1);
      Assert.assertEquals("face", facesElt.name);
      Assert.assertEquals(69451, facesElt.count);
      Assert.assertEquals(1, facesElt.getProperties().size());
      Assert.assertEquals("vertex_indices", facesElt.getProperties().get(0).name);
      Assert.assertEquals(DataType.int32, facesElt.getProperties().get(0).valueType);
      Assert.assertEquals(DataType.uint8, facesElt.getProperties().get(0).listIndexType);
   }
}
