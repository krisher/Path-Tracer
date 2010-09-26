package edu.rit.krisher.scene.material;

public class RingsPattern implements Texture {

   private final Color[] colors;
   double ringWidth = 1 / 16.0;

   public RingsPattern(final Color c1, final Color c2) {
      this.colors = new Color[] { c1, c2 };
   }

   @Override
   public Color getColor(final double... coordinate) {
      double radius = 0;
      if (coordinate != null) {
         for (final double coord : coordinate) {
            radius += coord * coord;
         }
      }
      return colors[((int) (Math.sqrt(radius) / ringWidth)) % 2];
   }

}
