package edu.rit.krisher.vecmath;

/**
 * Mutable 3D vector class.
 * <p>
 * Instances are NOT thread safe, and generally should not ever be modified in one thread and read in another unless
 * there is some external synchonization.
 * 
 * <p>
 * It would be better for instances of this class to be immutable, in terms of safety, however that can lead to
 * significant performance loss since it will typically require many new instances of Vec3s to be allocated.
 * 
 * @author krisher
 * 
 * 
 */
public final class Vec3 implements Transform {

   public static final double EPSILON = 1e-15;

   public static final Vec3 zero = new Vec3(0, 0, 0);

   public static final Vec3 xAxis = new Vec3(1, 0, 0);

   public static final Vec3 negXAxis = new Vec3(-1, 0, 0);
   public static final Vec3 yAxis = new Vec3(0, 1, 0);
   public static final Vec3 negYAxis = new Vec3(0, -1, 0);
   public static final Vec3 zAxis = new Vec3(0, 0, 1);
   public static final Vec3 negZAxis = new Vec3(0, 0, -1);

   public static final boolean isNear(final double value, final double nearValue) {
      return value + EPSILON > nearValue && value - EPSILON < nearValue;
   }

   public static final boolean isNearZero(final double value) {
      return value < EPSILON && value > -EPSILON;
   }

   public double x;
   public double y;
   public double z;

   public Vec3() {
      /*
       * Initialize to 0 vector.
       */
   }

   public Vec3(final double x, final double y, final double z) {
      this.x = x;
      this.y = y;
      this.z = z;
   }

   public Vec3(final Vec3 copy) {
      x = copy.x;
      y = copy.y;
      z = copy.z;
   }

   public Vec3(final double[] xyz) {
      this.x = xyz[0];
      this.y = xyz[1];
      this.z = xyz[2];
   }

   public final double get(final int idx) {
      return idx == 0 ? x : (idx == 1 ? y : z);
   }

   public double[] get() {
      return new double[] {x, y, z};
   }

   public final Vec3 add(final Vec3 vector) {
      x += vector.x;
      y += vector.y;
      z += vector.z;
      return this;
   }

   public final Vec3 cross(final Vec3 crossBy) {
      final double x = y * crossBy.z - z * crossBy.y;
      final double y = z * crossBy.x - this.x * crossBy.z;
      z = this.x * crossBy.y - this.y * crossBy.x;
      this.y = y;
      this.x = x;
      return this;
   }

   public final Vec3 cross(final double crossByX, final double crossByY, final double crossByZ) {
      final double x = y * crossByZ - z * crossByY;
      final double y = z * crossByX - this.x * crossByZ;
      z = this.x * crossByY - this.y * crossByX;
      this.y = y;
      this.x = x;
      return this;
   }

   public double distance(final Vec3 other) {
      final double dx = other.x - x;
      final double dy = other.y - y;
      final double dz = other.z - z;
      return Math.sqrt(dx * dx + dy * dy + dz * dz);
   }

   public final double dot(final Vec3 vector) {
      return x * vector.x + y * vector.y + z * vector.z;
   }

   @Override
   public Vec3 inverted() {
      return new Vec3(-x, -y, -z);
   }

   public final double length() {
      return Math.sqrt(x * x + y * y + z * z);
   }

   public final double lengthSquared() {
      return x * x + y * y + z * z;
   }

   public Vec3 multiply(final double scale) {
      x *= scale;
      y *= scale;
      z *= scale;
      return this;
   }

   public Vec3 multiply(final Vec3 components) {
      x *= components.x;
      y *= components.y;
      z *= components.z;
      return this;
   }

   public final Vec3 normalize() {
      final double lenSq = x * x + y * y + z * z;
      if (!isNear(lenSq, 1.0)) {
         final double lengthInv = 1. / Math.sqrt(lenSq);
         x *= lengthInv;
         y *= lengthInv;
         z *= lengthInv;
      }
      return this;
   }

   public final Vec3 scaleAdd(final Vec3 vector, final double scale) {
      x += scale * vector.x;
      y += scale * vector.y;
      z += scale * vector.z;
      return this;
   }

   public final Vec3 set(final double x, final double y, final double z) {
      this.x = x;
      this.y = y;
      this.z = z;
      return this;
   }

   public final Vec3 set(final Vec3 from) {
      x = from.x;
      y = from.y;
      z = from.z;
      return this;
   }

   public final Vec3 set(final double[] from) {
      x = from[0];
      y = from[1];
      z = from[2];
      return this;
   }

   public final Vec3 subtract(final Vec3 vector) {
      x -= vector.x;
      y -= vector.y;
      z -= vector.z;
      return this;
   }

   public final Vec3 subtract(final double[] vector) {
      x -= vector[0];
      y -= vector[1];
      z -= vector[2];
      return this;
   }

   public final Vec3 subtract(final double x, final double y, final double z) {
      this.x -= x;
      this.y -= y;
      this.z -= z;
      return this;
   }

   @Override
   public String toString() {
      return "(" + x + ", " + y + ", " + z + ")";
   }

   @Override
   public final Vec3 transformPoint(final Vec3 vec) {
      vec.add(this);
      return vec;
   }

   @Override
   public final Vec3 transformVec(final Vec3 vec) {
      return vec;
   }

   /**
    * Reflects this vector about the specified reflection vector.
    * 
    * @param reflectionVec
    *           A non-null unit length vector.
    * @return this for operation chaining.
    */
   public final Vec3 reflect(final Vec3 reflectionVec) {
      scaleAdd(reflectionVec, -2 * (dot(reflectionVec))).normalize();
      return this;
   }

   public static final double length(final double x, final double y, final double z) {
      return Math.sqrt(x * x + y * y + z * z);
   }

   public static final double dot(final double v1X, final double v1Y, final double v1Z, final double v2X,
         final double v2Y, final double v2Z) {
      return v1X * v2X + v1Y * v2Y + v1Z * v2Z;
   }
}
