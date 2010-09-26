package edu.rit.krisher.raytracer.rays;

import edu.rit.krisher.scene.material.Color;
import edu.rit.krisher.vecmath.Ray;
import edu.rit.krisher.vecmath.Vec3;

public class SampleRay extends Ray {

   public final Color transmissionSpectrum;
   public final Color extinction = new Color(0,0,0);
   public int pixelX, pixelY;
   public boolean emissiveResponse;

   public SampleRay(final Vec3 origin, final Vec3 direction, final double weight, final int x, final int y) {
      super(origin, direction);
      transmissionSpectrum = new Color(weight, weight, weight);
      emissiveResponse = true;
      pixelX = x;
      pixelY = y;
   }

   public void reset() {
      transmissionSpectrum.clear();
      emissiveResponse = true;
      //      extinction.clear();
   }
}
