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
    * The pixel location that this ray contributes to.
    */
   public double pixelX;
   public double pixelY;

   /**
    * The spectral power distribution for this sample, may be used for reflectance ratios or power.
    */
   public final Color throughput;

   /**
    * The extinction value of the current material that the ray is traversing.
    */
   public final Color extinction = new Color(0,0,0);
   
   /**
    * Flag indicating whether light incident via a direct hit on an emissive object should be considered.
    **/
   public boolean emissiveResponse = true;
   
   public final IntersectionInfo intersection = new IntersectionInfo();

   public SampleRay(final double filterWeight) {
      super(new Vec3(), new Vec3());
      throughput = new Color(filterWeight, filterWeight, filterWeight);
   }

   public void reset() {
      throughput.clear();
      emissiveResponse = true;
   }
}
