package edu.rit.krisher.scene.geometry.buffer;

/**
 * Base interface for a generic data buffer. Sub-interfaces or implementation classes must add the appropriate methods
 * to put and get the data elements of the buffer.
 * <p>
 * The API for Buffer is derived from java.nio.Buffer, and methods here have the same semantics as those defined in the
 * nio class.
 * 
 * @author krisher
 * 
 */
public interface Buffer {

   /**
    * Accessor for the limit of the buffer, the position/index one past the end of the buffer content area.
    * 
    * @return The limit.
    */
   public int limit();

   /**
    * Specifies a new limit for the buffer.
    * 
    * @param newLimit
    *           A new limit, must be >= 0 and <= capacity().
    * @return This buffer for fluent operation chaining.
    */
   public Buffer limit(int newLimit);

   /**
    * Accessor for the capacity of the buffer, in terms of whatever data type is held within.
    * 
    * @return The capacity, which is always >= 0.
    */
   public int capacity();

   /**
    * Accessor for the current position of the buffer, as a 0-based index in terms of data elements.
    * 
    * @return The position, always >= 0 and <= capacity.
    */
   public int position();

   /**
    * Clears the buffer, resetting the position to 0 and the limit to the capacity.
    * 
    * @return This buffer for fluent operation chaining.
    */
   public Buffer clear();

   /**
    * Flips the buffer, setting the limit to the current position, and the position to 0.
    * 
    * @return This buffer for fluent operation chaining.
    */
   public Buffer flip();

   /**
    * Changes the current position of the buffer to the specified value.
    * 
    * @param newIndex
    *           A position, which must be >= 0 and <= limit.
    * @return This buffer for fluent operation chaining.
    */
   public Buffer position(int newIndex);

}