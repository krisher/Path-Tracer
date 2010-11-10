package edu.rit.krisher.scene;

import edu.rit.krisher.scene.material.Color;
import edu.rit.krisher.vecmath.AxisAlignedBoundingBox;

/**
 * Simple scene container consisting of Geometry, a Camera model, and light sources.
 * 
 * @author krisher
 * 
 */
public interface Scene {

   /**
    * Accessor for a descriptive name for this scene.
    * 
    * @return A non-null name.
    */
   public String getName();

   public EmissiveGeometry[] getLightSources();

   public Color getBackground();

   public Camera getCamera();
   
   /**
    * Accessor for a tight fitting axis-aligned bounding box around the geometry.
    * 
    * @return A non-null bounding box.
    */
   public AxisAlignedBoundingBox getBounds();

   /**
    * Accessor for all of the geometry that makes up the scene.
    * 
    * @return A non-null array of geometry.
    */
   public Geometry[] getGeometry();
}
