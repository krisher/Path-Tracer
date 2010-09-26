package edu.rit.krisher.scene.material;

public class CheckerboardPattern implements Texture {

   private final Color[] colors;

   public CheckerboardPattern(final Color c1, final Color c2) {
      this.colors = new Color[] { c1, c2 };
   }

   @Override
   public Color getColor(final double... coordinate) {
      int colorIdx = 0;
      if (coordinate != null) {
         for (final double coord : coordinate) {
            if (coord < 0)
               colorIdx = ((int) (colorIdx - coord + 1)) % 2;
            else
               colorIdx = ((int) (colorIdx + coord)) % 2;
         }
      }
      return colors[colorIdx];
   }

}
