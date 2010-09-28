package edu.rit.krisher.fileparser.ply;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.BufferUnderflowException;
import java.nio.charset.Charset;
import java.util.Map;

import edu.rit.krisher.fileparser.ply.ElementReceiver.ElementAttributeValues;
import edu.rit.krisher.fileparser.ply.PLYContentDescription.PLYFormat;

/**
 * Simple parser implementation for the Stanford PLY model format. This is based on the format description
 * from: <a
 * href="http://people.cs.kuleuven.be/~ares.lagae/libply/ply-0.1/doc/PLY_FILES.txt">http://people.cs.kuleuven.be
 * /~ares.lagae/libply/ply-0.1/doc/PLY_FILES.txt</a>.
 * <p>
 * This is quick and dirty in its present form, this may only be able to load a limited subset of PLY models, in
 * particular, it is known to work with the Stanford Bunny model.
 * 
 * @author krisher
 * 
 */
public final class PLYParser {

   public static PLYContentDescription getPLYContentDescription(final InputStream stream) throws IOException {
      final BufferedReader reader = new BufferedReader(new InputStreamReader(stream, Charset.forName("US-ASCII")));
      try {
         return new PLYContentDescription(reader);
      } finally {
         reader.close();
      }
   }

   public static void parsePLY(final InputStream stream, final Map<String, ElementReceiver> receivers)
   throws IOException {
      final BufferedReader reader = new BufferedReader(new InputStreamReader(stream, Charset.forName("US-ASCII")));
      try {
         final PLYContentDescription content = new PLYContentDescription(reader);
         if (content.getFormat() != PLYFormat.ascii) {
            throw new UnsupportedOperationException("Only ASCII format is supported.");
         }
         for (final Element element : content.getElements()) {
            final ElementReceiver receiver = receivers.get(element.name);
            final ASCIIElementAttributeValues asciiParser = new ASCIIElementAttributeValues(element, reader);
            if (receiver != null) {
               receiver.receive(element, asciiParser);
            }
            asciiParser.skipToEnd(); // Ensure we have reached the end of this section.
         }
      } finally {
         reader.close();
      }
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
         // TODO Auto-generated method stub
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
         while (count++ < element.count && reader.readLine() != null) {

         }
      }
   }
}
