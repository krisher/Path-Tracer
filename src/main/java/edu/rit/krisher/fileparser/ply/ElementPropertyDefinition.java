package edu.rit.krisher.fileparser.ply;

/**
 * Definition/Metadata for a property of an element. Each property corresponds to one component (which may have
 * multiple scalar values) of an element instance in the file.
 * 
 * @author krisher
 * 
 */
public class ElementPropertyDefinition {
   /**
    * The name of the property.
    */
   public final String name;
   /**
    * If non-null, this property is a list property, and this field specifies the data type of the list length
    * value that is encoded in each instance of the property value. If null, then this is a single valued property.
    */
   public final DataType listIndexType;
   /**
    * The non-null data type of the property value.
    */
   public final DataType valueType;

   /**
    * Defines a property.
    * 
    * @param name
    *           A non-null name for the property.
    * @param listIndexType
    *           A possibly null data type for the size parameter of the property (for vector valued properties). If
    *           null, then this property is a scalar property.
    * @param valueType
    *           The data type for the property value components.
    */
   ElementPropertyDefinition(final String name, final DataType listIndexType, final DataType valueType) {
      super();
      this.name = name;
      this.listIndexType = listIndexType;
      this.valueType = valueType;
      if (this.name == null || this.valueType == null) {
         throw new IllegalArgumentException("Element Property definition must minmally include a non-null property name and value type.");
      }
   }
}