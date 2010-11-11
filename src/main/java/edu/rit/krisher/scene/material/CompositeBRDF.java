package edu.rit.krisher.scene.material;

import java.util.Random;

import edu.rit.krisher.collections.CopyOnWriteArrayList;
import edu.rit.krisher.raytracer.rays.IntersectionInfo;
import edu.rit.krisher.raytracer.rays.SampleRay;
import edu.rit.krisher.scene.Material;
import edu.rit.krisher.vecmath.Ray;
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
   public boolean isDiffuse() {
      for (final Material mat : materials.array) {
         if (mat.isDiffuse())
            return true;
      }
      return false;
   }

   @Override
   public void getEmissionColor(final Color emissionOut, final Ray sampleDirection, final IntersectionInfo parameters) {
      double r = 0, g = 0, b = 0;
      for (final Material mat : materials.array) {
         mat.getEmissionColor(emissionOut, sampleDirection, parameters);
         r += emissionOut.r;
         g += emissionOut.g;
         b += emissionOut.b;
      }
      emissionOut.set(r, g, b);
   }

   @Override
   public void evaluateBRDF(final Color radiance, final Vec3 sampleDirection, final Vec3 incidentLightDirection,
         final IntersectionInfo parameters) {
      // FIXME: radiance is an input parameter (as well as output), need to either reset it to original value between
      // each sample, or only sample one material.
      final Material[] mats = materials.array;
      final Double[] P = probabilities.array;
      double r = 0, g = 0, b = 0;
      for (int i = 0; i < mats.length; i++) {
         mats[i].evaluateBRDF(radiance, sampleDirection, incidentLightDirection, parameters);
         r += P[i] * radiance.r;
         g += P[i] * radiance.g;
         b += P[i] * radiance.b;
      }

      radiance.set(r, g, b);
   }

   @Override
   public double sampleBRDF(final SampleRay sampleOut, final Vec3 wIncoming, final IntersectionInfo parameters,
         final Random rng) {

      final double sampleType = rng.nextDouble();
      double cumP = 0;
      final Material[] mats = materials.array;
      final Double[] P = probabilities.array;
      for (int i = 0; i < mats.length; i++) {
         cumP += P[i];
         if (sampleType < cumP) {
            return mats[i].sampleBRDF(sampleOut, wIncoming, parameters, rng);
         }
      }
      sampleOut.throughput.clear();
      return 1;
   }

   public synchronized void addMaterial(final double prob, final Material mat) {
      materials.add(mat);
      probabilities.add(prob);
   }

}
