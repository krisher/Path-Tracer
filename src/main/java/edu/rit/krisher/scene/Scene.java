package edu.rit.krisher.scene;

import edu.rit.krisher.collections.CopyOnWriteArrayList;
import edu.rit.krisher.scene.material.Color;

public class Scene {

   private final Color bgColor = new Color(0, 0, 0);

   private final CopyOnWriteArrayList<Geometry> objects = new CopyOnWriteArrayList<Geometry>(Geometry.class);
   private final CopyOnWriteArrayList<EmissiveGeometry> lights = new CopyOnWriteArrayList<EmissiveGeometry>(
         EmissiveGeometry.class);

   public void add(final Geometry geometry) {
      objects.add(geometry);
      if (geometry instanceof EmissiveGeometry) {
         lights.add((EmissiveGeometry) geometry);
      }
   }

   public Geometry[] getGeometry() {
      return objects.array;
   }

   public EmissiveGeometry[] getLightSources() {
      return lights.array;
   }

   public Color getBackground() {
      return bgColor;
   }

   public void setBackground(final Color bg) {
      this.bgColor.set(bg);
   }

}
