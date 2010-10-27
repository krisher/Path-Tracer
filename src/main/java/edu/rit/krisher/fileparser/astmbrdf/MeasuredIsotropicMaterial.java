/**
 * 
 */
package edu.rit.krisher.fileparser.astmbrdf;

import java.util.Random;

import edu.rit.krisher.raytracer.rays.SampleRay;
import edu.rit.krisher.scene.Material;
import edu.rit.krisher.scene.MaterialInfo;
import edu.rit.krisher.scene.material.Color;
import edu.rit.krisher.vecmath.Vec3;

/**
 *
 */
class MeasuredIsotropicMaterial implements Material {

   final float[] angles;
   final float[] reflectance;

   /**
    * Creates a new Isotropic BRDF material using the specified measurements.
    * <p>
    * Ownership of the passed in array parameters is assumed by this material. Their content may be modified.
    * 
    * @param measurementAngles
    *           The angular sample locations:
    *           <ul>
    *           <li>incident elevation</li> <li>exitant elevation</li> <li>azimuth difference (exitant - incident)</li>
    *           </ul>
    * @param spectralReflectance
    *           The RGB reflectance values for the corresponding measurement.
    */
   public MeasuredIsotropicMaterial(final float[] measurementAngles, final float[] spectralReflectance) {
      this.angles = measurementAngles;
      this.reflectance = spectralReflectance;

      /*
       * Spectrum cleaning.
       * 
       * FIXME: There must be a better way of handling these values, e.g. do our own conversion from CIE XYZ.
       */
      for (int i = 0; i < spectralReflectance.length; ++i) {
         if (spectralReflectance[i] < 0)
            spectralReflectance[i] = 0;
         else if (spectralReflectance[i] > 1)
            spectralReflectance[i] = 1;
      }

      /*
       * Reparameterize the BRDF sample angle table to account for isotropy, reciprocity, and to improve distance
       * calculations for interpolation.
       */
      for (int i = 0; i < measurementAngles.length; i += 3) {
         final float thetaI = measurementAngles[i];
         final float thetaO = measurementAngles[i + 1];
         final float dPhi = measurementAngles[i + 2];

         measurementAngles[i] = (float) (Math.sin(thetaI) * Math.sin(thetaO));
         measurementAngles[i + 1] = (float) Math.abs((((dPhi / Math.PI) + 1.0) % 2.0) - 1.0); // Scale and normalize so
         // dPhi is in [0, 1].
         measurementAngles[i + 2] = (float) (Math.cos(thetaI) * Math.cos(thetaO));
      }
   }

   private static final double sinTheta(final double cosTheta) {
      return Math.sqrt(1.0 - cosTheta * cosTheta);
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

      final double azI = Math.atan2(wIShading.y, wIShading.x);
      final double azO = Math.atan2(wOShading.y, wOShading.x);

      final double dAz = azO - azI;

      final double u = (sinTheta(wIShading.z) * sinTheta(wOShading.z));
      final double v = (float) Math.abs((((dAz / Math.PI) + 1.0) % 2.0) - 1.0); // Scale and normalize so
      final double w = (float) (wIShading.z * wOShading.z);

      // find the closest sample...
      // Note the distance metric is not really good for interpolation...
      int closest = -1;
      double closestDist = Double.POSITIVE_INFINITY;
      for (int i = 0; i < angles.length; i += 3) {
         final double du = angles[i] - u;
         final double dv = angles[i + 1] - v;
         final double dw = angles[i + 2] - w;

         final double distSq = du * du + dv * dv + dw * dw;
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
