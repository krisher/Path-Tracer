package edu.rit.krisher.fileparser.ply;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.BufferUnderflowException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import edu.rit.krisher.fileparser.ply.ElementReceiver.ElementAttributeValues;
import edu.rit.krisher.fileparser.ply.PLYContentDescription.PLYFormat;
import edu.rit.krisher.scene.geometry.TriangleMesh;
import edu.rit.krisher.scene.geometry.buffer.Vec3fBuffer;
import edu.rit.krisher.util.Timer;

/**
 * Simple parser implementation for the Stanford PLY model format. This is based on the format description from: <a
 * href="http://people.cs.kuleuven.be/~ares.lagae/libply/ply-0.1/doc/PLY_FILES.txt">http://people.cs.kuleuven.be
 * /~ares.lagae/libply/ply-0.1/doc/PLY_FILES.txt</a>.
 * <p>
 * This is quick and dirty in its present form, this may only be able to load a limited subset of PLY models, in
 * particular, it is known to work with the Stanford Bunny, Happy Buddha, and Dragon models
 * (http://graphics.stanford.edu/data/3Dscanrep/).
 * 
 * @author krisher
 * 
 */
public final class PLYParser {

   public static PLYContentDescription getPLYContentDescription(final InputStream stream) throws IOException {
      final BufferedInputStream reader = new BufferedInputStream(stream);
      try {
         return new PLYContentDescription(reader);
      } finally {
         reader.close();
      }
   }

   public static void parsePLY(final InputStream stream, final Map<String, ElementReceiver> receivers)
         throws IOException {

      try {
         final PLYContentDescription content = new PLYContentDescription(stream);
         if (content.getFormat() == PLYFormat.ascii) {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(stream, Charset.forName("US-ASCII")));
            for (final Element element : content.getElements()) {
               final ElementReceiver receiver = receivers.get(element.name);
               final ASCIIElementAttributeValues asciiParser = new ASCIIElementAttributeValues(element, reader);
               if (receiver != null) {
                  receiver.receive(element, asciiParser);
               }
               asciiParser.skipToEnd(); // Ensure we have reached the end of this section.
            }
         } else if (content.getFormat() == PLYFormat.binary_big_endian) {

            for (final Element element : content.getElements()) {
               final ElementReceiver receiver = receivers.get(element.name);
               final BinaryAttributeValues binParser = new BinaryAttributeValues(element, stream, true);
               if (receiver != null) {
                  receiver.receive(element, binParser);
               }
               binParser.skipToEnd(); // Ensure we have reached the end of this section.
            }
         } else {
            throw new UnsupportedOperationException("Little-endian format is not supported.");
         }
      } finally {
         stream.close();
      }
   }

   public static TriangleMesh parseTriangleMesh(final File file) throws IOException {
      return parseTriangleMesh(new BufferedInputStream(new FileInputStream(file)), false);
   }

   public static TriangleMesh parseTriangleMesh(final InputStream stream) throws IOException {
      return parseTriangleMesh(stream, false);
   }

   public static TriangleMesh parseTriangleMesh(final InputStream stream, final boolean computeNormals) throws IOException {
      final Map<String, ElementReceiver> elementReceivers = new HashMap<String, ElementReceiver>();
      final VertexReceiver vReceiver = new VertexReceiver();
      elementReceivers.put("vertex", vReceiver);
      final TrianglesIndexBufferReceiver iReceiver = new TrianglesIndexBufferReceiver();
      elementReceivers.put("face", iReceiver);

      final Timer timer = new Timer("Parse PLY").start();
      parsePLY(stream, elementReceivers);
      timer.stop().print(System.out);

      final Vec3fBuffer vertices = vReceiver.getBuffer();
      final int[] indices = iReceiver.getBuffer().getIndices();
      return new TriangleMesh(vertices, (computeNormals) ? TriangleMesh.computeTriangleNormals(vertices, indices)
            : null, indices);
   }

   private static final class ASCIIElementAttributeValues implements ElementAttributeValues {
      private int count = 0;
      private final BufferedReader reader;
      private final Element element;
      private final ElementAttribute[] attributes;
      private String[] elementComponents;
      private final int[] attributeStart;
      private final int[] attributeListSize;
      private final boolean scalarOnly;

      public ASCIIElementAttributeValues(final Element element, final BufferedReader reader) {
         this.reader = reader;
         this.element = element;
         this.attributes = element.getProperties();
         attributeStart = new int[attributes.length];
         attributeListSize = new int[attributes.length];
         boolean scalar = true;
         int attrIdx = 0;
         for (final ElementAttribute attr : attributes) {
            scalar &= attr.listIndexType == null;
            attributeListSize[attrIdx] = 1;
            attributeStart[attrIdx] = attrIdx++;
         }
         this.scalarOnly = scalar;
      }

      @Override
      public Number getScalarComponent(final int attributeIdx) {
         return attributes[attributeIdx].valueType.parseAscii(elementComponents[attributeStart[attributeIdx]]);
      }

      @Override
      public Number[] getVectorComponent(final int attributeIdx) {
         final Number[] values = new Number[attributeListSize[attributeIdx]];
         for (int i = 0; i < values.length; i++) {
            values[i] = attributes[attributeIdx].valueType.parseAscii(elementComponents[attributeStart[attributeIdx]
                  + i]);
         }
         return values;

      }

      @Override
      public void nextElement() {
         if (count < element.count) {
            try {
               final String line = reader.readLine();
               if (line == null)
                  throw new IllegalStateException("Unexpected end of file.");
               this.elementComponents = line.split("\\s");
               if (!scalarOnly) {
                  for (int componentStart = 0, attrIdx = 0; attrIdx < attributes.length; ++attrIdx) {
                     if (attributes[attrIdx].listIndexType == null) {
                        attributeListSize[attrIdx] = -1;
                        attributeStart[attrIdx] = componentStart++;
                     } else {
                        attributeStart[attrIdx] = componentStart + 1;
                        attributeListSize[attrIdx] = attributes[attrIdx].listIndexType.parseAscii(elementComponents[componentStart]).intValue();
                        componentStart += attributeListSize[attrIdx] + 1;
                     }
                  }
               }
               ++count;
            } catch (final IOException ioe) {
               throw new IllegalStateException("Error reading PLY stream.", ioe);
            }
         } else
            throw new BufferUnderflowException();
      }

      void skipToEnd() throws IOException {
         while (count++ < element.count && reader.readLine() != null)
            ;
      }
   }

   private static final class BinaryAttributeValues implements ElementAttributeValues {
      private int count = 0;
      private final DataInputStream stream;
      private final Element element;
      private final ElementAttribute[] attributes;
      private final Number[] scalarValues;
      private final Number[][] listValues;
      private final boolean bigEndian;

      public BinaryAttributeValues(final Element element, final InputStream stream, final boolean bigEndian) {
         this.stream = new DataInputStream(stream);
         this.element = element;
         this.attributes = element.getProperties();
         this.bigEndian = bigEndian;
         scalarValues = new Number[attributes.length];
         listValues = new Number[attributes.length][];
      }

      @Override
      public Number getScalarComponent(final int attributeIdx) {
         return scalarValues[attributeIdx];
      }

      @Override
      public Number[] getVectorComponent(final int attributeIdx) {
         return listValues[attributeIdx];
      }

      @Override
      public void nextElement() {
         if (count < element.count) {
            try {
               for (int attrIdx = 0; attrIdx < attributes.length; ++attrIdx) {
                  if (attributes[attrIdx].listIndexType == null) { // Scalar attribute
                     scalarValues[attrIdx] = attributes[attrIdx].valueType.parseBinary(stream, bigEndian);
                  } else {
                     final Number componentCount = attributes[attrIdx].listIndexType.parseBinary(stream, bigEndian);
                     final Number[] values = new Number[componentCount.intValue()];
                     for (int i = 0; i < values.length; i++) {
                        values[i] = attributes[attrIdx].valueType.parseBinary(stream, bigEndian);
                     }
                     listValues[attrIdx] = values;
                  }
               }
               ++count;
            } catch (final IOException ioe) {
               throw new IllegalStateException("Error reading PLY stream.", ioe);
            }
         } else
            throw new BufferUnderflowException();
      }

      void skipToEnd() {
         while (count < element.count) {
            nextElement();
         }
      }
   }
}
