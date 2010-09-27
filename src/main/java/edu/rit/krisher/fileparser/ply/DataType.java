package edu.rit.krisher.fileparser.ply;

/**
 * The various data types that scalar values may be specified in.
 * 
 * @author krisher
 * 
 */
public enum DataType {
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

   // public abstract Number parseAscii(String asciiRepresentation);
   //
   // public abstract Number parseBinary(byte[] binaryRepresentation);
}