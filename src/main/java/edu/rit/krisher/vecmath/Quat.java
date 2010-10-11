package edu.rit.krisher.vecmath;


public class Quat implements Transform {

   public double a;

   public double b;

   public double c;

   public double d;

   public Quat() {
      a = 1;
   }

   /**
    * Constructs a quaternion that will perform a rotation of the specified
    * angle about the specified axis. Note that the axis vector should be
    * normalized before passing it into the constructor, this call will not
    * normalize the vector.
    * 
    * @param axis
    *           The axis about which rotation should be performed.
    * @param angle
    *           The angle to rotate by, in radians.
    */
   public Quat(final Vec3 axis, final double angle) {
      final double halfAngle = angle / 2.;
      final double sinHalfAngle = Math.sin(halfAngle);

      a = Math.cos(halfAngle);
      b = sinHalfAngle * axis.x;
      c = sinHalfAngle * axis.y;
      d = sinHalfAngle * axis.z;

   }

   public Quat(final double a, final double b, final double c, final double d) {
      this.a = a;
      this.b = b;
      this.c = c;
      this.d = d;
   }

   public void set(final Vec3 axis, final double angle) {
      final double halfAngle = angle / 2.;
      final double sinHalfAngle = Math.sin(halfAngle);

      a = Math.cos(halfAngle);
      b = sinHalfAngle * axis.x;
      c = sinHalfAngle * axis.y;
      d = sinHalfAngle * axis.z;
   }

   public double getAngle() {
      return 2. * Math.acos(a);
   }

   public Vec3 getAxis() {
      final double vecMagnitude = Math.sqrt(b * b + c * c + d * d);
      if (vecMagnitude == 0)
         return Vec3.zero;
      return new Vec3(b / vecMagnitude, c / vecMagnitude, d / vecMagnitude);
   }

   public double magnitude() {
      return Math.sqrt(a * a + b * b + c * c + d * d);
   }

   public void normalize() {
      final double magnitude = Math.sqrt(a * a + b * b + c * c + d * d);
      if (magnitude == 0) {
         a = 1;
         return;
      }
      a /= magnitude;
      b /= magnitude;
      c /= magnitude;
      d /= magnitude;
   }

   public Quat getConjugate() {
      return new Quat(a, -b, -c, -d);
   }

   public Quat multiply(final Quat _quat) {
      return new Quat(a * _quat.a - b * _quat.b - c * _quat.c - d * _quat.d,
                      a * _quat.b + b * _quat.a + c * _quat.d - d * _quat.c, a
                      * _quat.c + c * _quat.a + d * _quat.b - b * _quat.d, a
                      * _quat.d + d * _quat.a + b * _quat.c - c * _quat.b);
   }

   public void multiplyBy(final Quat _quat) {
      final double w = a * _quat.a - b * _quat.b - c * _quat.c - d * _quat.d;
      final double x = a * _quat.b + b * _quat.a + c * _quat.d - d * _quat.c;
      final double y = a * _quat.c + c * _quat.a + d * _quat.b - b * _quat.d;
      final double z = a * _quat.d + d * _quat.a + b * _quat.c - c * _quat.b;

      this.a = w;
      this.b = x;
      this.c = y;
      this.d = z;
   }

   @Override
   public Vec3 transformVec(final Vec3 _vec) {

      /*
       * First, multiply the vector (with a 4th component == 0) by the
       * conjugate of this Quaternion
       */
      final double w = 0 - _vec.x * -b - _vec.y * -c - _vec.z * -d;
      final double x = 0 + _vec.x * a + _vec.y * -d - _vec.z * -c;
      final double y = 0 + _vec.y * a + _vec.z * -b - _vec.x * -d;
      final double z = 0 + _vec.z * a + _vec.x * -c - _vec.y * -b;

      /*
       * Then multiply the result by this quaternion
       */
      /*
       * The w component should be 0 since we are multiplying by both the
       * Quaternion and its conjugate
       */
      // w = a * _quat.a - b * _quat.b - c * _quat.c - d * _quat.d;
      _vec.x = a * x + b * w + c * z - d * y;
      _vec.y = a * y + c * w + d * x - b * z;
      _vec.z = a * z + d * w + b * y - c * x;
      return _vec;
   }

   @Override
   public Vec3 transformPoint(final Vec3 vec) {
      return transformVec(vec);
   }

   @Override
   public Quat inverted() {
      return getConjugate();
   }
}
