package edu.rit.krisher.fileparser.ply;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Content definition metadata for a PLY file, as defined by its header. This must be initialized from a provided
 * Reader that represents a PLY file.
 * 
 * @author krisher
 * 
 */
public class PLYContentDescription {
   private static final String propertyDelimiter = "property";
   private static final String formatDelimiter = "format";
   private static final String elementDelimiter = "element";
   private static final String commentDelimiter = "comment";
   private static final String headerEnd = "end_header";
   private static final String headerStart = "ply";
   private static final Pattern scalarPropertyPattern = Pattern.compile("property[ \t]+(\\S+)[ \t]+(\\S+)");
   private static final Pattern listPropertyPattern = Pattern.compile("property[ \t]+list[ \t]+(\\S+)[ \t]+(\\S+)[ \t]+(\\S+)");
   private static final Pattern elementPattern = Pattern.compile("element[ \t]+(.+?)[ \t]+(\\d+)");
   private static final Pattern formatPattern = Pattern.compile("format[ \t]+(.+?)[ \t]+([0-9\\.]+)");

   private PLYFormat format;
   private String versionString;
   private int headerSizeBytes;
   private final List<String> comments = new ArrayList<String>();
   private final List<Element> elements = new ArrayList<Element>();

   /**
    * Initializes the content definition from the specified reader (which must provide access to the beginning of a
    * PLY stream). The reader is left open after the ContentDefinition is initialized, and is positioned to the first
    * location after the header (which should contain element data).
    * 
    * @param reader
    *           A non-null reader that is open, and positioned at the beginning of a PLY stream.
    * @throws IOException
    *            If there is any problem reading from the stream, or if the stream does not contain the expected
    *            data.
    */
   PLYContentDescription(final BufferedReader reader) throws IOException {
      parse(reader);
   }

   /**
    * Accessor for the format of the element content (after the header).
    * 
    * @return Non-null element content.
    */
   public PLYFormat getFormat() {
      return format;
   }

   /**
    * Accessor for the PLY format version string as declared in the header.
    * 
    * @return A non-null String.
    */
   public String getPLYVersion() {
      return versionString;
   }

   /**
    * Accessor for a list of the comments that were encountered in the header, in the order that they appeared.
    * 
    * @return A non-null (but possibly empty) list of comments.
    */
   public List<String> getComments() {
      return Collections.unmodifiableList(comments);
   }

   /**
    * Accessor for the list of element definitions declared in the header. Each element has a list of associated
    * properties that define the content of the elements, as well as the number of each element expected to appear in
    * the content area of the PLY file. The order of the element definitions returned here corresponds to the order
    * that the elements will appear in the content area of the file.
    * 
    * @return A non-null, but possibly empty list of elements (an empty list indicates that the file contains no
    *         content).
    */
   public List<Element> getElements() {
      return Collections.unmodifiableList(elements);
   }

   private void parse(final BufferedReader reader) throws IOException {
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
      if (!PLYContentDescription.headerStart.equals(line)) {
         throw new IOException("The provided stream does not appear to contain PLY data, expected \""
                               + PLYContentDescription.headerStart + "\", but found \"" + line + "\"");
      }

      Element eltDef = null;
      /*
       * Next comes the format definition, followed by any number of element defintions.
       */
      while (!PLYContentDescription.headerEnd.equals((line = reader.readLine()))) {
         if (line == null) {
            /*
             * Premature end of file.
             */
            throw new IOException("Premature end of stream while parsing PLY header.");
         }
         /*
          * Line can be a comment, element, or property definition.
          */
         if (line.startsWith(PLYContentDescription.commentDelimiter)) {
            comments.add(line.substring(PLYContentDescription.commentDelimiter.length()).trim());
         } else if (line.startsWith(PLYContentDescription.elementDelimiter)) {
            final Matcher elementMatcher = PLYContentDescription.elementPattern.matcher(line);
            if (!elementMatcher.matches()) {
               throw new IOException("Invalid element declaration in PLY header: " + line + ".");
            }
            eltDef = new Element(elementMatcher.group(1), Integer.parseInt(elementMatcher.group(2)));
            elements.add(eltDef);
         } else if (line.startsWith(PLYContentDescription.propertyDelimiter)) {
            if (eltDef == null)
               throw new IOException("Unexpected property declaration in PLY header, no element has been defined yet: "
                                     + line + ".");
            Matcher propertyMatcher = PLYContentDescription.listPropertyPattern.matcher(line);
            final String propName;
            final DataType propType;
            final DataType listSizeType;
            if (propertyMatcher.matches()) {
               listSizeType = DataType.parseType(propertyMatcher.group(1));
               propType = DataType.parseType(propertyMatcher.group(2));
               propName = propertyMatcher.group(3);
            } else {
               propertyMatcher = PLYContentDescription.scalarPropertyPattern.matcher(line);
               if (!propertyMatcher.matches())
                  throw new IOException("Invalid property declaration in PLY header: " + line + ".");
               listSizeType = null;
               propType = DataType.parseType(propertyMatcher.group(1));
               propName = propertyMatcher.group(2);
            }
            eltDef.addProperty(new ElementAttribute(propName, listSizeType, propType));
         } else if (line.startsWith(PLYContentDescription.formatDelimiter)) {
            final Matcher formatMatcher = PLYContentDescription.formatPattern.matcher(line);
            if (!formatMatcher.matches()) {
               throw new IOException("Invalid format declaration in PLY header: " + line + ".");
            }
            this.format = PLYFormat.valueOf(formatMatcher.group(1));
            this.versionString = formatMatcher.group(2);
         } else {
            throw new IOException("Unexpected delimiter in PLY header: " + line + ".");
         }
      }
   }

   /**
    * Format of the PLY file content, ascii or binary.
    */
   public static enum PLYFormat {
      /**
       * Format for ascii representation of element data.
       */
      ascii,
      /**
       * Format for binary representation of element data.
       */
      binary_little_endian,
      /**
       * Format for binary representation of element data.
       */
      binary_big_endian
   }

   /**
    * The various data types that scalar values may be specified in.
    * 
    * @author krisher
    * 
    */
   public static enum DataType {
      /**
       * 8bit unsigned int (short in Java).
       */
      uint8 {
         @Override
         public int getSizeBytes() {
            return 1;
         }

         @Override
         public boolean isSigned() {
            return false;
         }

         @Override
         public Number parseAscii(final String asciiRepresentation) {
            return Short.parseShort(asciiRepresentation);
         }
      },
      /**
       * 32bit signed int (int in Java).
       */
      int32 {
         @Override
         public int getSizeBytes() {
            return 4;
         }

         @Override
         public boolean isSigned() {
            return true;
         }

         @Override
         public Number parseAscii(final String asciiRepresentation) {
            return Integer.parseInt(asciiRepresentation);
         }
      },
      /**
       * 32bit float (float in Java).
       */
      float32 {
         @Override
         public int getSizeBytes() {
            return 4;
         }

         @Override
         public boolean isSigned() {
            return true;
         }

         @Override
         public Number parseAscii(final String asciiRepresentation) {
            return Float.parseFloat(asciiRepresentation);
         }
      };

      /**
       * The size in bytes of the data type, as it would be represented in the binary version of the file.
       * 
       * @return The size in bytes of the data type as it would be represented in the binary version of the file.
       */
      public abstract int getSizeBytes();

      /**
       * Accessor to determine whether the data type represents signed values (<code>true</code>), or unsigned (
       * <code>false</code>).
       * 
       * @return true or false...
       */
      public abstract boolean isSigned();

      /**
       * Accessor for the DataType constant representing the specified named type (as specified in a PLY header).
       * 
       * @param name
       *           A name.
       * @return The DataType corresponding to the specified name.
       * @throws IllegalArgumentException
       *            if the specified name does not match a known data type.
       */
      public static final DataType parseType(final String name) throws IllegalArgumentException {
         if ("float".equals(name)) {
            return float32;
         } else if ("int".equals(name)) {
            return int32;
         } else if ("uchar".equals(name)) {
            return uint8;
         } else {
            throw new IllegalArgumentException("Unsupported data type: " + name);
         }
      }

      /**
       * Parses the data value from the specified text representation.
       * 
       * @param asciiRepresentation
       *           The string representing the value to parse.
       * @return the numeric value of the specified string.
       */
      public abstract Number parseAscii(String asciiRepresentation);
      //
      // public abstract Number parseBinary(byte[] binaryRepresentation);
   }
}