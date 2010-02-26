package edu.rit.krisher.ui;

import edu.rit.krisher.scene.Camera;
import edu.rit.krisher.scene.Scene;

public class SceneDescription {

   private final String name;
   private final Scene scene;
   private final Camera camera;

   public SceneDescription(String name, Scene scene, Camera camera) {
      this.name = name;
      this.scene = scene;
      this.camera = camera;
   }

   public Camera getCamera() {
      return camera;
   }

   public Scene getScene() {
      return scene;
   }

   @Override
   public String toString() {
      return name;
   }

}
