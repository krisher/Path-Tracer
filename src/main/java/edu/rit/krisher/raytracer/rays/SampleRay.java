package edu.rit.krisher.raytracer.rays;

import edu.rit.krisher.scene.material.Color;
import edu.rit.krisher.vecmath.Ray;
import edu.rit.krisher.vecmath.Vec3;

/**
 * Ray subclass with additional per-ray state.
 * 
 * @author krisher
 * 
 */
public class SampleRay extends Ray {

   /**
    * The spectral distribution for this sample, may be used for reflectance ratios or power.
    */
   public final Color sampleColor;

   public final Color extinction = new Color(0,0,0);
   /**
    * The pixel location that this ray contributes to.
    */
   public double pixelX;
   public double pixelY;
   /**
    * Flag indicating whether light incident via a direct hit on an emissive object should be considered.
    **/
   public boolean emissiveResponse;

   public SampleRay(final double filterWeight) {
      super(new Vec3(), new Vec3());
      sampleColor = new Color(filterWeight, filterWeight, filterWeight);
      emissiveResponse = true;
   }

   public void reset() {
      sampleColor.clear();
      emissiveResponse = true;
      //      extinction.clear();
   }
}
