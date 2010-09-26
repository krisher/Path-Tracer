package edu.rit.krisher.scene.material;

import java.util.Random;

import edu.rit.krisher.collections.CopyOnWriteArrayList;
import edu.rit.krisher.raytracer.rays.SampleRay;
import edu.rit.krisher.scene.Material;
import edu.rit.krisher.vecmath.Vec3;

/**
 * BxDF function that represents a composite of other BxDF functions, each with
 * a specified probability.
 * 
 * @author krisher
 * 
 */
public class CompositeBRDF implements Material, Cloneable {

   private final CopyOnWriteArrayList<Material> materials = new CopyOnWriteArrayList<Material>(Material.class);
   private final CopyOnWriteArrayList<Double> probabilities = new CopyOnWriteArrayList<Double>(Double.class);

   public CompositeBRDF() {
   }

   public CompositeBRDF(final Material diffuse, final double kd, final Material spec, final double ks) {
      addMaterial(kd, diffuse);
      addMaterial(ks, spec);
   }

   @Override
   public boolean shouldSampleDirectIllumination() {
      for (final Material mat : materials.array) {
         if (mat.shouldSampleDirectIllumination())
            return true;
      }
      return false;
   }

   @Override
   public void getEmissionColor(final Color emissionOut, final Vec3 sampleDirection, final Vec3 surfaceNormal, final double... materialCoords) {
      double r = 0, g = 0, b = 0;
      for (final Material mat : materials.array) {
         mat.getEmissionColor(emissionOut, sampleDirection, surfaceNormal, materialCoords);
         r += emissionOut.r;
         g += emissionOut.g;
         b += emissionOut.b;
      }
      emissionOut.set(r, g, b);
   }

   @Override
   public void getDirectIlluminationTransport(final Color radiance, final Vec3 sampleDirection, final Random rng,
         final Vec3 incidentLightDirection, final Vec3 surfaceNormal, final double... materialCoords) {

      // final double rand = rng.nextDouble();
      // if (rand < kd) {
      final Material[] mats = materials.array;
      final Double[] P = probabilities.array;
      double r = 0, g = 0, b = 0;
      for (int i = 0; i < mats.length; i++) {
         mats[i].getDirectIlluminationTransport(radiance, sampleDirection, rng, incidentLightDirection, surfaceNormal,
                                                materialCoords);
         r += P[i] * radiance.r;
         g += P[i] * radiance.g;
         b += P[i] * radiance.b;
      }

      radiance.set(r, g, b);
   }

   @Override
   public void sampleIrradiance(final SampleRay sampleOut, final Random rng, final Vec3 radianceSampleDirection, final Vec3 surfaceNormal,
         final double... materialCoords) {

      final double sampleType = rng.nextDouble();
      double cumP = 0;
      final Material[] mats = materials.array;
      final Double[] P = probabilities.array;
      for (int i = 0; i < mats.length; i++) {
         cumP += P[i];
         if (sampleType < cumP) {
            mats[i].sampleIrradiance(sampleOut, rng, radianceSampleDirection, surfaceNormal, materialCoords);
            return;
         }
      }

   }

   public synchronized void addMaterial(final double prob, final Material mat) {
      materials.add(mat);
      probabilities.add(prob);
   }

}
