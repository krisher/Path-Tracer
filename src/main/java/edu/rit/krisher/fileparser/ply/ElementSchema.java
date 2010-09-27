package edu.rit.krisher.fileparser.ply;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Definition/Metadata for a single content element type in the PLY file (defined in the header).
 * 
 * @author krisher
 * 
 */
public class ElementSchema {
   /**
    * The name of the element. This can be anything, but common names
    * include 'vertex', 'face', and 'edge'.
    */
   public final String name;
   /**
    * The number of elements of this type in the file.
    */
   public final int count;
   /**
    * The properties that make up the element.
    */
   private final List<Column> properties = new ArrayList<Column>();

   /**
    * Creates a new ElementDefinition with the specified element name, and number of elements.
    * 
    * @param name
    *           The non-null element name.
    * @param count
    *           The number of elements of this type that appear in the file.
    */
   ElementSchema(final String name, final int count) {
      this.name = name;
      this.count = count;
   }

   /**
    * The list of properties that make up an element of this type. The order of the elements
    * in the returned list corresponds to the order of the elements in the file.
    * 
    * @return A non-null list of properties.
    */
   public List<Column> getProperties() {
      return Collections.unmodifiableList(properties);
   }

   void addProperty(final Column prop) {
      properties.add(prop);
   }
}