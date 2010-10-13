package edu.rit.krisher.scene.material;

import java.util.Random;

import edu.rit.krisher.raytracer.rays.SampleRay;
import edu.rit.krisher.scene.Material;
import edu.rit.krisher.scene.MaterialInfo;
import edu.rit.krisher.vecmath.Vec3;

public class Color implements Material, Texture {
   public static final Color white = new Color(1, 1, 1);
   public static final Color black = new Color(0, 0, 0);

   public double r;
   public double g;
   public double b;

   public Color(final double weight) {
      r = g = b = weight;
   }

   public Color(final double d, final double e, final double f) {
      r = d;
      g = e;
      b = f;
   }

   public Color(final Color other) {
      this.r = other.r;
      this.g = other.g;
      this.b = other.b;
   }

   /**
    * Creates and returns a new Color that is the component-wise multiplication
    * of this color and the specified color.
    * 
    * @param other
    *           A non-null Color.
    * @return
    */
   public Color convolvedWith(final Color other) {
      return new Color(r * other.r, g * other.g, b * other.b);
   }

   @Override
   public Color getColor(final double... coordinate) {
      return this;
   }

   @Override
   public void getEmissionColor(final Color emissionOut, final Vec3 responseDirection, final MaterialInfo parameters) {
      emissionOut.set(this);
   }

   @Override
   public void getIrradianceResponse(final Color colorOut, final Vec3 responseDirection, final Vec3 incomingDirection,
         final MaterialInfo parameters) {
      colorOut.set(0, 0, 0);
   }

   public boolean isTranslucent() {
      return false;
   }

   public Color multiply(final Color other) {
      r *= other.r;
      g *= other.g;
      b *= other.b;
      return this;
   }

   public Color multiply(final double scale) {
      r *= scale;
      g *= scale;
      b *= scale;
      return this;
   }

   public Color multiply(final double r, final double g, final double b) {
      this.r *= r;
      this.g *= g;
      this.b *= b;
      return this;
   }

   @Override
   public void sampleInteraction(final SampleRay sampleOut, final Random rng, final Vec3 wIncoming,
         final MaterialInfo parameters) {
      sampleOut.transmissionSpectrum.clear();
      // No lighting response...
   }

   public Color scaleSet(final Color other, final double scale) {
      this.r = other.r * scale;
      this.g = other.g * scale;
      this.b = other.b * scale;
      return this;
   }

   public Color scaleSet(final double r, final double g, final double b, final double scale) {
      this.r = r * scale;
      this.g = g * scale;
      this.b = b * scale;
      return this;
   }

   public Color add(final Color other) {
      this.r += other.r;
      this.g += other.g;
      this.b += other.b;
      return this;
   }

   public Color scaleAdd(final Color other, final double scale) {
      this.r += other.r * scale;
      this.g += other.g * scale;
      this.b += other.b * scale;
      return this;
   }

   public Color add(final double r, final double g, final double b) {
      this.r += r;
      this.g += g;
      this.b += b;
      return this;
   }

   public Color scaleAdd(final double r, final double g, final double b, final double scale) {
      this.r += r * scale;
      this.g += g * scale;
      this.b += b * scale;
      return this;
   }

   public Color set(final Color from) {
      r = from.r;
      g = from.g;
      b = from.b;
      return this;
   }

   public Color set(final Color from, final double scale) {
      r = from.r * scale;
      g = from.g * scale;
      b = from.b * scale;
      return this;
   }

   public Color set(final double r, final double g, final double b) {
      this.r = r;
      this.b = b;
      this.g = g;
      return this;
   }

   public void clear() {
      this.r = this.g = this.b = 0;
   }

   public boolean isZero() {
      return r == 0 && g == 0 && b == 0;
   }

   public boolean isAlmostZero(final double small) {
      return r < small && g < small && b < small;
   }

   @Override
   public String toString() {
      return "[ " + r + ", " + g + ", " + b + " ]";
   }

   public boolean transmitsLight() {
      return false;
   }

   @Override
   public boolean shouldSampleDirectIllumination() {
      return false;
   }
}
