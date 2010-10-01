package edu.rit.krisher.ui;

import edu.rit.krisher.scene.Camera;
import edu.rit.krisher.scene.Scene;

public class SceneDescription {

   private final String name;
   private final Scene scene;
   private final Camera camera;
   private String detailedDescription;

   public SceneDescription(final String name, final Scene scene, final Camera camera) {
      this.name = name;
      this.scene = scene;
      this.camera = camera;
   }

   public void setDetailedDescription(final String description) {
      this.detailedDescription = description;
   }

   public String getDetailedDescription() {
      return detailedDescription;
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
