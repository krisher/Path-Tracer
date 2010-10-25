/**
 * 
 */
package edu.rit.krisher.scene.material;

import java.util.Random;

import edu.rit.krisher.raytracer.rays.SampleRay;
import edu.rit.krisher.scene.Material;
import edu.rit.krisher.scene.MaterialInfo;
import edu.rit.krisher.vecmath.Vec3;

/**
 *
 */
public class MeasuredIsotropicMaterial implements Material {

   final float[] angles;
   final float[] reflectance;

   public MeasuredIsotropicMaterial(final float[] measurementAngles, final float[] spectralReflectance) {
      this.angles = measurementAngles;
      this.reflectance = spectralReflectance;
   }

   /*
    * @see edu.rit.krisher.scene.Material#evaluateBRDF(edu.rit.krisher.scene.material.Color,
    * edu.rit.krisher.vecmath.Vec3, edu.rit.krisher.vecmath.Vec3, edu.rit.krisher.scene.MaterialInfo)
    */
   @Override
   public void evaluateBRDF(final Color colorInOut, final Vec3 wOutgoing, final Vec3 wIncoming,
         final MaterialInfo parameters) {
      // TODO Auto-generated method stub
      final Vec3 sY = new Vec3(parameters.surfaceNormal).cross(parameters.tangentVector);
      final Vec3 wOutgoingN = new Vec3(wOutgoing).multiply(-1);
      final Vec3 wIShading = new Vec3(wIncoming.dot(parameters.tangentVector), wIncoming.dot(sY), wIncoming.dot(parameters.surfaceNormal));
      final Vec3 wOShading = new Vec3(wOutgoingN.dot(parameters.tangentVector), wOutgoingN.dot(sY), wOutgoingN.dot(parameters.surfaceNormal));

      final double elevationI = Math.acos(wIShading.z);
      final double elevationO = Math.acos(wOShading.z);

      final double azI = Math.atan2(wIShading.y, wIShading.x);
      final double azO = Math.atan2(wOShading.y, wOShading.x);

      final double dAz = azO - azI;

      // find the closest sample...
      // Note the distance metric is not really good for interpolation...
      int closest = -1;
      double closestDist = Double.POSITIVE_INFINITY;
      for (int i = 0; i < angles.length; i += 3) {
         final double elIDiff = angles[i] - elevationI;
         final double elODiff = angles[i + 1] - elevationO;
         final double azDiff = angles[i + 2] - dAz;

         final double distSq = elIDiff * elIDiff + elODiff * elODiff + azDiff * azDiff;
         if (distSq < closestDist) {
            closest = i;
            closestDist = distSq;
         }
      }
      if (closest >= 0) {
         colorInOut.multiply(reflectance[closest], reflectance[closest + 1], reflectance[closest + 2]);
      }
   }

   /*
    * @see edu.rit.krisher.scene.Material#getEmissionColor(edu.rit.krisher.scene.material.Color,
    * edu.rit.krisher.vecmath.Vec3, edu.rit.krisher.scene.MaterialInfo)
    */
   @Override
   public void getEmissionColor(final Color emissionOut, final Vec3 sampleDirection, final MaterialInfo parameters) {
      emissionOut.clear();
   }

   /*
    * @see edu.rit.krisher.scene.Material#isDiffuse()
    */
   @Override
   public boolean isDiffuse() {
      return true;
   }

   /*
    * @see edu.rit.krisher.scene.Material#sampleBRDF(edu.rit.krisher.raytracer.rays.SampleRay, java.util.Random,
    * edu.rit.krisher.vecmath.Vec3, edu.rit.krisher.scene.MaterialInfo)
    */
   @Override
   public void sampleBRDF(final SampleRay sampleOut, final Random rng, final Vec3 wIncoming,
         final MaterialInfo parameters) {
      // TODO Auto-generated method stub
      sampleOut.sampleColor.clear();
   }

}
