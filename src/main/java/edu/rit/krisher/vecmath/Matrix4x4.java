package edu.rit.krisher.vecmath;

import java.util.Arrays;

public final class Matrix4x4 implements Transform {

   public final double[] matrix = new double[16];

   /**
    * Creates a new 4x4 identity matrix
    */
   public Matrix4x4() {
      matrix[0] = matrix[5] = matrix[10] = matrix[15] = 1;
   }

   /**
    * Creates a new matrix with a copy of the specified components
    * 
    * @param matrix
    *            A non-null 16+ element array (only the first 16 elements are
    *            used).
    */
   public Matrix4x4(final double[] matrix) {
      System.arraycopy(matrix, 0, this.matrix, 0, 16);
   }

   /**
    * Sets the matrix to the 4x4 identity matrix.
    */
   public void resetToIdentity() {
      matrix[1] = matrix[2] = matrix[3] = matrix[4] = matrix[6] = matrix[7] = matrix[8] = matrix[9] = matrix[11] = matrix[12] = matrix[13] = matrix[14] = 0;
      matrix[0] = matrix[5] = matrix[10] = matrix[15] = 1;
   }

   /**
    * Multiplies this matrix by the specified non-null
    * 
    * @param mulByMatrix
    * @return
    */
   public Matrix4x4 mul(final double[] mulByMatrix) {
      return multiply(null, matrix, mulByMatrix);
   }

   public Matrix4x4 mul(final Matrix4x4 mulBy) {
      return multiply(null, matrix, mulBy.matrix);
   }

   public Matrix4x4 mul(final Matrix4x4 _resultM, final double[] mulByMatrix) {
      return multiply(_resultM, matrix, mulByMatrix);
   }

   public void mulBy(final double[] _mulByMatrix) {
      final Matrix4x4 result = mul(_mulByMatrix);
      System.arraycopy(result, 0, matrix, 0, 16);
   }

   public void mulBy(final Matrix4x4 mulBy) {
      final Matrix4x4 result = mul(mulBy);
      System.arraycopy(result.matrix, 0, matrix, 0, 16);
   }

   /**
    * Multiplies the specified vector by this matrix, assuming a value of 1.0
    * for the w component of the vector, and performs the perspective divide to
    * return a 3-component vector.
    */
   @Override
   public Vec3 transformPoint(final Vec3 _target) {
      final double x = _target.x * matrix[0] + _target.y * matrix[1]
                                                                  + _target.z * matrix[2] + matrix[3];
      final double y = _target.x * matrix[4] + _target.y * matrix[5]
                                                                  + _target.z * matrix[6] + matrix[7];
      final double z = _target.x * matrix[8] + _target.y * matrix[9]
                                                                  + _target.z * matrix[10] + matrix[11];
      final double w = matrix[12] + matrix[13] + matrix[14] + matrix[15];

      _target.x = x / w;
      _target.y = y / w;
      _target.z = z / w;
      return _target;
   }

   /**
    * Multiplies the specified vector by this matrix, assuming a homogeneous w
    * component of 0 (so only rotation is applied).
    */
   @Override
   public Vec3 transformVec(final Vec3 _target) {
      final double x = _target.x * matrix[0] + _target.y * matrix[1]
                                                                  + _target.z * matrix[2];
      final double y = _target.x * matrix[4] + _target.y * matrix[5]
                                                                  + _target.z * matrix[6];
      final double z = _target.x * matrix[8] + _target.y * matrix[9]
                                                                  + _target.z * matrix[10];

      _target.x = x;
      _target.y = y;
      _target.z = z;
      return _target;
   }

   /**
    * Computes the multiplicative inverse of this matrix if it is invertible.
    * 
    * @return a new matrix that is the multiplicative inverse of this matrix,
    *         or null if this matrix is singular.
    */
   @Override
   public Matrix4x4 inverted() {
      final double[] inverted = invert(matrix, null);
      return (inverted == null) ? null : new Matrix4x4(inverted);
   }

   /**
    * Creates a 4x4 translation matrix from the specified translation vector
    * 
    * @param x
    * @param y
    * @param z
    * @return
    */
   public static Matrix4x4 translationMatrix(final double x, final double y, final double z) {
      final Matrix4x4 translation = new Matrix4x4();
      translation.matrix[3] = x;
      translation.matrix[7] = y;
      translation.matrix[11] = z;
      return translation;
   }

   /**
    * Creates a 4x4 rotation matrix from the specified normalized axis of
    * rotation, and (right-handed) angle of rotation.
    * 
    * @param axis
    *            A non-null, normalized vector representing the axis of
    *            rotation.
    * @param angleRads
    *            The angle to rotate by.
    * @return
    */
   public static Matrix4x4 rotationMatrix(final Vec3 axis, final double angleRads) {
      final Matrix4x4 rotation = new Matrix4x4();

      final double sinTheta = Math.sin(angleRads);
      final double cosTheta = Math.cos(angleRads);

      final double xx = axis.x * axis.x;
      final double xy = axis.x * axis.y;
      final double xz = axis.x * axis.z;
      final double yy = axis.y * axis.y;
      final double yz = axis.y * axis.z;
      final double zz = axis.z * axis.z;

      rotation.matrix[0] = xx + (1 - xx) * cosTheta;
      rotation.matrix[1] = xy - xy * cosTheta - axis.z * sinTheta;
      rotation.matrix[2] = xz - xz * cosTheta + axis.y * sinTheta;

      rotation.matrix[4] = xy - xy * cosTheta + axis.z * sinTheta;
      rotation.matrix[5] = yy + (1 - yy) * cosTheta;
      rotation.matrix[6] = yz - yz * cosTheta - axis.x * sinTheta;

      rotation.matrix[8] = xz - xz * cosTheta - axis.y * sinTheta;
      rotation.matrix[9] = yz - yz * cosTheta + axis.x * sinTheta;
      rotation.matrix[10] = zz + (1 - zz) * cosTheta;
      return rotation;
   }

   /**
    * Creates a 4x4 rotation matrix representing a right-handed rotation of
    * angleRads radians about the x axis.
    * 
    * @param angleRads
    *            The angle to rotate by.
    * @return
    */
   public static Matrix4x4 rotationXMatrix(final double angleRads) {
      final Matrix4x4 rotation = new Matrix4x4();

      final double sinTheta = Math.sin(angleRads);
      final double cosTheta = Math.cos(angleRads);

      rotation.matrix[5] = cosTheta;
      rotation.matrix[6] = -sinTheta;

      rotation.matrix[9] = sinTheta;
      rotation.matrix[10] = cosTheta;
      return rotation;
   }

   /**
    * Creates a 4x4 rotation matrix representing a right-handed rotation of
    * angleRads radians about the y axis.
    * 
    * @param angleRads
    *            The angle to rotate by.
    * @return
    */
   public static Matrix4x4 rotationYMatrix(final double angleRads) {
      final Matrix4x4 rotation = new Matrix4x4();

      final double sinTheta = Math.sin(angleRads);
      final double cosTheta = Math.cos(angleRads);

      rotation.matrix[0] = cosTheta;
      rotation.matrix[2] = sinTheta;

      rotation.matrix[8] = -sinTheta;
      rotation.matrix[10] = cosTheta;
      return rotation;
   }

   /**
    * Creates a 4x4 rotation matrix representing a right-handed rotation of
    * angleRads radians about the z axis.
    * 
    * @param angleRads
    *            The angle to rotate by.
    * @return
    */
   public static Matrix4x4 rotationZMatrix(final double angleRads) {
      final Matrix4x4 rotation = new Matrix4x4();

      final double sinTheta = Math.sin(angleRads);
      final double cosTheta = Math.cos(angleRads);

      rotation.matrix[0] = cosTheta;
      rotation.matrix[1] = -sinTheta;

      rotation.matrix[4] = sinTheta;
      rotation.matrix[5] = cosTheta;
      return rotation;
   }

   /**
    * Composites (via multiplication) the specified matrices.
    * 
    * @param components
    *            A sequence of non-null matrices. They are multiplied in the
    *            order as specified, so the last matrix specified is the first
    *            transformation that is applied to a vector.
    * @return
    */
   public static Matrix4x4 composite(final Matrix4x4... components) {
      final Matrix4x4 composite = new Matrix4x4();
      for (final Matrix4x4 component : components) {
         composite.mulBy(component);
      }
      return composite;
   }

   private static final Matrix4x4 multiply(Matrix4x4 resultM, final double[] matrix,
         final double[] mulByMatrix) {
      if (resultM == null)
         resultM = new Matrix4x4();
      final double[] result = resultM.matrix;

      result[0] = matrix[0] * mulByMatrix[0] + matrix[1] * mulByMatrix[4]
                                                                       + matrix[2] * mulByMatrix[8] + matrix[3] * mulByMatrix[12];
      result[1] = matrix[0] * mulByMatrix[1] + matrix[1] * mulByMatrix[5]
                                                                       + matrix[2] * mulByMatrix[9] + matrix[3] * mulByMatrix[13];
      result[2] = matrix[0] * mulByMatrix[2] + matrix[1] * mulByMatrix[6]
                                                                       + matrix[2] * mulByMatrix[10] + matrix[3] * mulByMatrix[14];
      result[3] = matrix[0] * mulByMatrix[3] + matrix[1] * mulByMatrix[7]
                                                                       + matrix[2] * mulByMatrix[11] + matrix[3] * mulByMatrix[15];

      result[4] = matrix[4] * mulByMatrix[0] + matrix[5] * mulByMatrix[4]
                                                                       + matrix[6] * mulByMatrix[8] + matrix[7] * mulByMatrix[12];
      result[5] = matrix[4] * mulByMatrix[1] + matrix[5] * mulByMatrix[5]
                                                                       + matrix[6] * mulByMatrix[9] + matrix[7] * mulByMatrix[13];
      result[6] = matrix[4] * mulByMatrix[2] + matrix[5] * mulByMatrix[6]
                                                                       + matrix[6] * mulByMatrix[10] + matrix[7] * mulByMatrix[14];
      result[7] = matrix[4] * mulByMatrix[3] + matrix[5] * mulByMatrix[7]
                                                                       + matrix[6] * mulByMatrix[11] + matrix[7] * mulByMatrix[15];

      result[8] = matrix[8] * mulByMatrix[0] + matrix[9] * mulByMatrix[4]
                                                                       + matrix[10] * mulByMatrix[8] + matrix[11] * mulByMatrix[12];
      result[9] = matrix[8] * mulByMatrix[1] + matrix[9] * mulByMatrix[5]
                                                                       + matrix[10] * mulByMatrix[9] + matrix[11] * mulByMatrix[13];
      result[10] = matrix[8] * mulByMatrix[2] + matrix[9] * mulByMatrix[6]
                                                                        + matrix[10] * mulByMatrix[10] + matrix[11] * mulByMatrix[14];
      result[11] = matrix[8] * mulByMatrix[3] + matrix[9] * mulByMatrix[7]
                                                                        + matrix[10] * mulByMatrix[11] + matrix[11] * mulByMatrix[15];

      result[12] = matrix[12] * mulByMatrix[0] + matrix[13] * mulByMatrix[4]
                                                                          + matrix[14] * mulByMatrix[8] + matrix[15] * mulByMatrix[12];
      result[13] = matrix[12] * mulByMatrix[1] + matrix[13] * mulByMatrix[5]
                                                                          + matrix[14] * mulByMatrix[9] + matrix[15] * mulByMatrix[13];
      result[14] = matrix[12] * mulByMatrix[2] + matrix[13] * mulByMatrix[6]
                                                                          + matrix[14] * mulByMatrix[10] + matrix[15] * mulByMatrix[14];
      result[15] = matrix[12] * mulByMatrix[3] + matrix[13] * mulByMatrix[7]
                                                                          + matrix[14] * mulByMatrix[11] + matrix[15] * mulByMatrix[15];

      return resultM;
   }

   /**
    * Computes the inverse of the matrix using Gauss-Jordan elimination.
    * 
    * @param matrix
    *            The non-null 4x4 matrix to invert.
    * @param result
    *            A possibly null 16+ element array to store the result in.
    * @return The resulting inverted matrix, or null if the matrix is
    *         non-invertible.
    */
   private static final double[] invert(final double[] matrix, double[] result) {
      if (result == null)
         result = new double[16];
      /*
       * Populate the result matrix with the identity
       */
      result[0] = result[5] = result[10] = result[15] = 1;
      result[1] = result[2] = result[3] = result[4] = result[6] = result[7] = result[8] = result[9] = result[11] = result[12] = result[13] = result[14] = 0;

      /*
       * Copy the original matrix into a temporary version so we can
       * manipulate it.
       */
      final double[] temp = Arrays.copyOf(matrix, 16);

      /*
       * Gauss-Jordan elimination with pivoting based on the largest element
       */
      double t;
      for (int i = 0; i < 4; i++) {
         /*
          * find the largest element in the column to use as the pivot
          */
         int swap = i;
         for (int j = i + 1; j < 4; j++) {
            if (Math.abs(temp[j * 4 + i]) > Math.abs(temp[i * 4 + i])) {
               swap = j;
            }
         }

         if (swap != i) {
            /*
             * Swap rows if necessary
             */
            for (int k = 0; k < 4; k++) {
               t = temp[i * 4 + k];
               temp[i * 4 + k] = temp[swap * 4 + k];
               temp[swap * 4 + k] = t;

               t = result[i * 4 + k];
               result[i * 4 + k] = result[swap * 4 + k];
               result[swap * 4 + k] = t;
            }
         }
         /*
          * If a diagonal element of the matrix is zero, the matrix is
          * non-invertible.
          */
         if (temp[i * 4 + i] == 0) {
            return null;
         }

         /*
          * Gaussian elimination...
          */
         t = temp[i * 4 + i];
         for (int k = 0; k < 4; k++) {
            temp[i * 4 + k] /= t;
            result[i * 4 + k] /= t;
         }
         for (int j = 0; j < 4; j++) {
            if (j != i) {
               t = temp[j * 4 + i];
               for (int k = 0; k < 4; k++) {
                  temp[j * 4 + k] -= temp[i * 4 + k] * t;
                  result[j * 4 + k] -= result[i * 4 + k] * t;
               }
            }
         }
      }
      return result;
   }

}
