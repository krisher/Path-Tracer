package edu.rit.krisher.vecmath;

public interface Transform {

    /**
     * Transforms a vector (assumes a homogeneous component of 0.0).
     * 
     * @param vec
     *            The non-null vector to transform. This vector will be modified
     *            directly, and returned for convenient chaining of operations.
     * @return The vector that was passed in as an argument for operation
     *         chaining.
     */
    public Vec3 transformVec(Vec3 vec);

    /**
     * Transforms a point (assumes a homogeneous component of 1.0).
     * 
     * @param point
     *            The non-null point to transform. This point will be modified
     *            directly, and returned for convenient chaining of operations.
     * @return The point that was passed in as an argument for operation
     *         chaining.
     */
    public Vec3 transformPoint(Vec3 point);

    /**
     * Creates and returns a new Transform that is the inverse of this
     * Transform.
     * 
     * @return A new Transform representing the inverse of this transform.
     */
    public Transform inverted();

}
