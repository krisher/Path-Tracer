package edu.rit.krisher.fileparser.ply;



public interface ElementReceiver {

   public static interface ElementAttributeValues {
      public Number getScalarComponent(int attributeIdx);
      public Number[] getVectorComponent(int attributeIdx);

      public void nextElement();
   }

   public void receive(Element element, ElementAttributeValues values);
}
