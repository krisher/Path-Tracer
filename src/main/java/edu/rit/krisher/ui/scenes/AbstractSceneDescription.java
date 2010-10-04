package edu.rit.krisher.ui.scenes;

import edu.rit.krisher.scene.Camera;
import edu.rit.krisher.scene.DefaultScene;
import edu.rit.krisher.scene.EmissiveGeometry;
import edu.rit.krisher.scene.Geometry;
import edu.rit.krisher.scene.Scene;
import edu.rit.krisher.scene.material.Color;

class AbstractSceneDescription<C extends Camera> extends DefaultScene<C> implements Scene {

   boolean init = false;

   public AbstractSceneDescription(final String name, final C camera) {
      super(name, camera);
   }

   @Override
   public C getCamera() {
      checkInit();
      return super.getCamera();
   }

   @Override
   public Geometry[] getGeometry() {
      checkInit();
      return super.getGeometry();
   }

   private final void checkInit() {
      if (!init) {
         initScene();
         init = true;
      }
   }

   @Override
   public EmissiveGeometry[] getLightSources() {
      checkInit();
      return super.getLightSources();
   }

   @Override
   public Color getBackground() {
      checkInit();
      return super.getBackground();
   }

   protected void initScene() {

   }

   @Override
   public String toString() {
      return getName();
   }

}
