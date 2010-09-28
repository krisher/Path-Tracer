package edu.rit.krisher.fileparser.ply;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipInputStream;

import org.junit.Assert;
import org.junit.Test;

import edu.rit.krisher.fileparser.ply.PLYContentDescription.DataType;
import edu.rit.krisher.fileparser.ply.PLYContentDescription.PLYFormat;
import edu.rit.krisher.scene.geometry.buffer.IndexBuffer;
import edu.rit.krisher.scene.geometry.buffer.Vec3fBuffer;

public class PLYParserTest {

   private static final String bunnyResource = "/edu/rit/krisher/fileparser/ply/bun_zipper.ply.zip";

   @Test
   public void readBunnyShouldParseKnownHeader() throws IOException {
      final PLYContentDescription content;
      /*
       * Assumes that the ply file is the first entry in the zip...
       */
      final ZipInputStream zis = new ZipInputStream(PLYParserTest.class.getResourceAsStream(bunnyResource));
      try {
         zis.getNextEntry();
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
      final ZipInputStream zis = new ZipInputStream(PLYParserTest.class.getResourceAsStream(bunnyResource));
      try {
         zis.getNextEntry();

         PLYParser.parsePLY(zis, elementReceivers);

      } finally {
         zis.close();
      }
      final Vec3fBuffer buffer = receiver.getBuffer();
      Assert.assertEquals(35947, buffer.capacity());
      Assert.assertEquals(35947, buffer.limit());
      Assert.assertEquals(0, buffer.position());
   }

   @Test
   public void readBunnyIndexData() throws IOException {
      final TrianglesIndexBufferReceiver receiver = new TrianglesIndexBufferReceiver();
      final Map<String, ElementReceiver> elementReceivers = new HashMap<String, ElementReceiver>();
      elementReceivers.put("face", receiver);
      final ZipInputStream zis = new ZipInputStream(PLYParserTest.class.getResourceAsStream(bunnyResource));
      try {
         zis.getNextEntry();

         PLYParser.parsePLY(zis, elementReceivers);

      } finally {
         zis.close();
      }
      final IndexBuffer buffer = receiver.getBuffer();
      Assert.assertEquals(69451 * 3, buffer.capacity());
      Assert.assertEquals(69451 * 3, buffer.limit());
      Assert.assertEquals(0, buffer.position());
   }
}
