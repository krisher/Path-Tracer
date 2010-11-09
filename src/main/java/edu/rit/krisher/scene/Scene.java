package edu.rit.krisher.scene;

import edu.rit.krisher.scene.material.Color;

/**
 * Simple scene container consisting of Geometry, a Camera model, and light sources.
 * 
 * @author krisher
 * 
 */
public interface Scene {

   public String getName();

   public Geometry[] getGeometry();

   public EmissiveGeometry[] getLightSources();

   public Color getBackground();

   public Camera getCamera();
}
