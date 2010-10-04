package edu.rit.krisher.scene;

import edu.rit.krisher.scene.material.Color;

public interface Scene {

   public String getName();

   public Geometry[] getGeometry();

   public EmissiveGeometry[] getLightSources();

   public Color getBackground();

   public Camera getCamera();
}
