package edu.rit.krisher.fileparser.ply;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import edu.rit.krisher.fileparser.ply.PLYContentDescription.DataType;
import edu.rit.krisher.fileparser.ply.PLYContentDescription.PLYFormat;

public class PLYParserTest {

   private static final String bunnyResource = "/edu/rit/krisher/fileparser/ply/bun_zipper.ply";

   @Test
   public void readBunnyShouldParseKnownHeader() throws IOException {
      final PLYContentDescription content;
      /*
       * Assumes that the ply file is the first entry in the zip...
       */
      final InputStream zis = new BufferedInputStream(PLYParserTest.class.getResourceAsStream(bunnyResource));
      try {
         content = PLYParser.getPLYContentDescription(zis);
      } finally {
         zis.close();
      }

      Assert.assertEquals(PLYFormat.ascii, content.getFormat());
      Assert.assertEquals("1.0", content.getPLYVersion());

      Assert.assertEquals(1, content.getComments().size());
      Assert.assertEquals("zipper output", content.getComments().get(0));

      Assert.assertEquals(2, content.getElements().size());

      final Element vertexElt = content.getElements().get(0);
      Assert.assertEquals("vertex", vertexElt.name);
      Assert.assertEquals(35947, vertexElt.count);
      final ElementAttribute[] vertexEltProperties = vertexElt.getProperties();
      Assert.assertEquals(5, vertexEltProperties.length);
      Assert.assertEquals("x", vertexEltProperties[0].name);
      Assert.assertEquals(DataType.float32, vertexEltProperties[0].valueType);
      Assert.assertEquals(null, vertexEltProperties[0].listIndexType);
      Assert.assertEquals("y", vertexEltProperties[1].name);
      Assert.assertEquals(DataType.float32, vertexEltProperties[1].valueType);
      Assert.assertEquals(null, vertexEltProperties[1].listIndexType);
      Assert.assertEquals("z", vertexEltProperties[2].name);
      Assert.assertEquals(DataType.float32, vertexEltProperties[2].valueType);
      Assert.assertEquals(null, vertexEltProperties[2].listIndexType);
      Assert.assertEquals("confidence", vertexEltProperties[3].name);
      Assert.assertEquals(DataType.float32, vertexEltProperties[3].valueType);
      Assert.assertEquals(null, vertexEltProperties[3].listIndexType);
      Assert.assertEquals("intensity", vertexEltProperties[4].name);
      Assert.assertEquals(DataType.float32, vertexEltProperties[4].valueType);
      Assert.assertEquals(null, vertexEltProperties[4].listIndexType);

      final Element facesElt = content.getElements().get(1);
      Assert.assertEquals("face", facesElt.name);
      Assert.assertEquals(69451, facesElt.count);
      final ElementAttribute[] faceEltProperties = facesElt.getProperties();
      Assert.assertEquals(1, faceEltProperties.length);
      Assert.assertEquals("vertex_indices", faceEltProperties[0].name);
      Assert.assertEquals(DataType.int32, faceEltProperties[0].valueType);
      Assert.assertEquals(DataType.uint8, faceEltProperties[0].listIndexType);
   }

   @Test
   public void readBunnyVertexData() throws IOException {
      final VertexReceiver receiver = new VertexReceiver();
      final Map<String, ElementReceiver> elementReceivers = new HashMap<String, ElementReceiver>();
      elementReceivers.put("vertex", receiver);
      final InputStream zis = new BufferedInputStream(PLYParserTest.class.getResourceAsStream(bunnyResource));
      try {
         PLYParser.parsePLY(zis, elementReceivers);

      } finally {
         zis.close();
      }
      final float[] buffer = receiver.getBuffer();
      Assert.assertEquals(35947 * 3, buffer.length);
   }

   @Test
   public void readBunnyIndexData() throws IOException {
      final TrianglesIndexBufferReceiver receiver = new TrianglesIndexBufferReceiver();
      final Map<String, ElementReceiver> elementReceivers = new HashMap<String, ElementReceiver>();
      elementReceivers.put("face", receiver);
      final InputStream zis = new BufferedInputStream(PLYParserTest.class.getResourceAsStream(bunnyResource));
      try {
         PLYParser.parsePLY(zis, elementReceivers);

      } finally {
         zis.close();
      }
      final int[] buffer = receiver.getIndexList();
      Assert.assertEquals(69451 * 3, buffer.length);
   }
}
