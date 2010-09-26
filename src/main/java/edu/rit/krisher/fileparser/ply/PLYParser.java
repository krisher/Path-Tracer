package edu.rit.krisher.fileparser.ply;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple parser implementation for the Stanford PLY model format. This is based on the format description
 * from: <a
 * href="http://people.cs.kuleuven.be/~ares.lagae/libply/ply-0.1/doc/PLY_FILES.txt">http://people.cs.kuleuven.be
 * /~ares.lagae/libply/ply-0.1/doc/PLY_FILES.txt</a>.
 * <p>
 * In its present form, this may only be able to load a limited subset of PLY models, in particular, it is known to work
 * with the Stanford Bunny model.
 * 
 * @author krisher
 * 
 */
public class PLYParser {

   private static final String headerStart = "ply";
   private static final String headerEnd = "end_header";
   private static final String commentDelimiter = "comment";
   private static final String elementDelimiter = "element";
   private static final String formatDelimiter = "format";
   private static final String propertyDelimiter = "property";

   private static final Pattern formatPattern = Pattern.compile("format[ \t]+(.+?)[ \t]+([0-9\\.]+)");
   private static final Pattern elementPattern = Pattern.compile("element[ \t]+(.+?)[ \t]+(\\d+)");
   private static final Pattern listPropertyPattern = Pattern.compile("property[ \t]+list[ \t]+(\\S+)[ \t]+(\\S+)[ \t]+(\\S+)");
   private static final Pattern scalarPropertyPattern = Pattern.compile("property[ \t]+(\\S+)[ \t]+(\\S+)");

   /*
    * Header records :
    * ply
    * format <type> <version>
    * comment ...
    * element <name> <count>
    * - vertex, edge, and face are common element names.
    * property <data-type> <name>
    * - property list <count-type> <list-data-type> <property-name>
    * - property <scalar-type> <name>
    * - property <name> <scalar-type>
    * end_header
    * 
    * Properties define the fields associated with the previously declared element. The set of properties
    * for an element are all specified on a single line (in the ascii format), or within the specified number of bytes
    * (for the binary formats). Property lists allow variable length records, the number of fields in the property list
    * is given as the first value of the field, with the list values immediately following.
    */

   public static enum FORMAT {
      ascii, binary_little_endian, binary_big_endian
   }

   public static enum DATA_TYPE {
      int8 {
         @Override
         public int sizeBytes() {
            return 1;
         }
      },
      uint8 {
         @Override
         public int sizeBytes() {
            return 1;
         }
      },
      int16 {
         @Override
         public int sizeBytes() {
            return 2;
         }
      },
      uint16 {
         @Override
         public int sizeBytes() {
            return 2;
         }
      },
      int32 {
         @Override
         public int sizeBytes() {
            return 4;
         }
      },
      uint32 {
         @Override
         public int sizeBytes() {
            return 4;
         }
      },
      float32 {
         @Override
         public int sizeBytes() {
            return 4;
         }
      },
      float64 {
         @Override
         public int sizeBytes() {
            return 8;
         }
      };

      /**
       * The size in bytes of the data type, as it would be represented in the binary version of the file.
       * 
       * @return
       */
      public abstract int sizeBytes();

      // public abstract Number parseAscii(String asciiRepresentation);
      //
      // public abstract Number parseBinary(byte[] binaryRepresentation);
   }

   static class ElementDefinition {
      /**
       * The name of the element. This can be anything, but common names
       * include 'vertex', 'face', and 'edge'.
       */
      public final String name;
      public final int count;
      private final List<ElementPropertyDefinition> properties = new ArrayList<ElementPropertyDefinition>();

      public ElementDefinition(final String name, final int count) {
         this.name = name;
         this.count = count;
      }

      public List<ElementPropertyDefinition> getProperties() {
         return Collections.unmodifiableList(properties);
      }
   }

   static class ElementPropertyDefinition {
      /**
       * The name of the property
       */
      public final String name;
      /**
       * If non-null, this property is a list property, and this field specifies the data type of the list length
       * value that is encoded in each instance of the property value. If null, then this is a single valued property.
       */
      public final DATA_TYPE listIndexType;
      /**
       * The non-null data type of the property value.
       */
      public final DATA_TYPE valueType;

      public ElementPropertyDefinition(final String name, final DATA_TYPE listIndexType, final DATA_TYPE valueType) {
         super();
         this.name = name;
         this.listIndexType = listIndexType;
         this.valueType = valueType;
         if (this.name == null || this.valueType == null) {
            throw new IllegalArgumentException("Element Property definition must minmally include a non-null property name and value type.");
         }
      }
   }

   static class ContentDefinition {
      private FORMAT format;
      private String versionString;
      private final List<String> comments = new ArrayList<String>();
      private final List<ElementDefinition> elements = new ArrayList<ElementDefinition>();

      public FORMAT getFormat() {
         return format;
      }

      public String getPLYVersion() {
         return versionString;
      }

      public List<String> getComments() {
         return Collections.unmodifiableList(comments);
      }

      public List<ElementDefinition> getElements() {
         return Collections.unmodifiableList(elements);
      }

      public void parse(final BufferedReader reader) throws IOException {
         /*
          * The file begins with an ascii header section, regardless of the content format. Note that the format
          * specifies ascii data, but who knows whether this is actually followed in the implementation.
          * 
          * First line of the file must be the header-start line.
          */

         /*
          * Read a '\r' terminated line. Note that this will catch \n or \r\n terminated lines as well which would not
          * be part of the spec, but this would only be a problem if someone used the \n character in an element or
          * property name.
          */
         String line = reader.readLine();
         if (!headerStart.equals(line)) {
            throw new IOException("The provided stream does not appear to contain PLY data, expected \"" + headerStart
                                  + "\", but found \"" + line + "\"");
         }

         ElementDefinition eltDef = null;
         /*
          * Next comes the format definition, followed by any number of element defintions.
          */
         while (!headerEnd.equals((line = reader.readLine()))) {
            if (line == null) {
               /*
                * Premature end of file.
                */
               throw new IOException("Premature end of stream while parsing PLY header.");
            }
            /*
             * Line can be a comment, element, or property definition.
             */
            if (line.startsWith(commentDelimiter)) {
               comments.add(line.substring(commentDelimiter.length()).trim());
            } else if (line.startsWith(elementDelimiter)) {
               final Matcher elementMatcher = elementPattern.matcher(line);
               if (!elementMatcher.matches()) {
                  throw new IOException("Invalid element declaration in PLY header: " + line + ".");
               }
               eltDef = new ElementDefinition(elementMatcher.group(1), Integer.parseInt(elementMatcher.group(2)));
               elements.add(eltDef);
            } else if (line.startsWith(propertyDelimiter)) {
               if (eltDef == null)
                  throw new IOException("Unexpected property declaration in PLY header, no element has been defined yet: "
                                        + line + ".");
               Matcher propertyMatcher = listPropertyPattern.matcher(line);
               final String propName;
               final DATA_TYPE propType;
               final DATA_TYPE listSizeType;
               if (propertyMatcher.matches()) {
                  listSizeType = parseType(propertyMatcher.group(1));
                  propType = parseType(propertyMatcher.group(2));
                  propName = propertyMatcher.group(3);
               } else {
                  propertyMatcher = scalarPropertyPattern.matcher(line);
                  if (!propertyMatcher.matches())
                     throw new IOException("Invalid property declaration in PLY header: " + line + ".");
                  listSizeType = null;
                  propType = parseType(propertyMatcher.group(1));
                  propName = propertyMatcher.group(2);
               }
               eltDef.properties.add(new ElementPropertyDefinition(propName, listSizeType, propType));
            } else if (line.startsWith(formatDelimiter)) {
               final Matcher formatMatcher = formatPattern.matcher(line);
               if (!formatMatcher.matches()) {
                  throw new IOException("Invalid format declaration in PLY header: " + line + ".");
               }
               this.format = FORMAT.valueOf(formatMatcher.group(1));
               this.versionString = formatMatcher.group(2);
            } else {
               throw new IOException("Unexpected delimiter in PLY header: " + line + ".");
            }
         }
      }
   }

   static final DATA_TYPE parseType(final String name) {
      if ("float".equals(name)) {
         return DATA_TYPE.float32;
      } else if ("int".equals(name)) {
         return DATA_TYPE.int32;
      } else if ("uchar".equals(name)) {
         return DATA_TYPE.uint8;
      } else {
         throw new IllegalArgumentException("Unsupported data type: " + name);
      }
   }

   static void parsePLY(final InputStream stream) throws IOException {
      final BufferedReader reader = new BufferedReader(new InputStreamReader(stream, Charset.forName("US-ASCII")));
      try {
         final ContentDefinition content = new ContentDefinition();
         content.parse(reader);
      } finally {
         reader.close();
      }
   }
}
