package edu.rit.krisher.scene;

import edu.rit.krisher.collections.CopyOnWriteArrayList;
import edu.rit.krisher.scene.material.Color;

public class DefaultScene<C extends Camera> implements Scene {

   private final Color bgColor = new Color(0, 0, 0);

   private final String name;

   protected final C camera;

   private final CopyOnWriteArrayList<Geometry> objects = new CopyOnWriteArrayList<Geometry>(Geometry.class);
   private final CopyOnWriteArrayList<EmissiveGeometry> lights = new CopyOnWriteArrayList<EmissiveGeometry>(EmissiveGeometry.class);

   public DefaultScene(final String name, final C cam) {
      this.name = name;
      this.camera = cam;
   }

   /*
    * (non-Javadoc)
    * 
    * @see edu.rit.krisher.scene.Scene#getName()
    */
   @Override
   public String getName() {
      return name;
   }

   @Override
   public C getCamera() {
      return camera;
   }

   public void add(final Geometry geometry) {
      objects.add(geometry);
      if (geometry instanceof EmissiveGeometry) {
         lights.add((EmissiveGeometry) geometry);
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see edu.rit.krisher.scene.Scene#getGeometry()
    */
   @Override
   public Geometry[] getGeometry() {
      return objects.array;
   }

   /*
    * (non-Javadoc)
    * 
    * @see edu.rit.krisher.scene.Scene#getLightSources()
    */
   @Override
   public EmissiveGeometry[] getLightSources() {
      return lights.array;
   }

   /*
    * (non-Javadoc)
    * 
    * @see edu.rit.krisher.scene.Scene#getBackground()
    */
   @Override
   public Color getBackground() {
      return bgColor;
   }

   public void setBackground(final Color bg) {
      this.bgColor.set(bg);
   }

}
