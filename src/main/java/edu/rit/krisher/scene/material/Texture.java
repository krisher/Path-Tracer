package edu.rit.krisher.scene.material;

public interface Texture {

   /**
    * Returns a color based on the specified texture coordinates.
    * 
    * @param coordinate
    * @return
    */
   public Color getColor(double... coordinate);
}
