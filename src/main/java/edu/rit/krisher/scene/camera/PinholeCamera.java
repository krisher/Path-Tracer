package edu.rit.krisher.scene.camera;

import java.util.Random;

import edu.rit.krisher.raytracer.rays.SampleRay;
import edu.rit.krisher.scene.Camera;
import edu.rit.krisher.vecmath.Quat;
import edu.rit.krisher.vecmath.Vec3;

public class PinholeCamera implements Camera {

   /**
    * The position of the camera.
    */
   protected Vec3 position = new Vec3(0, 0, 0);

   /**
    * The orientation of the camera relative to its 'home' orientation with the view vector pointing down the -z axis,
    * and up pointing toward +y.
    */
   protected Quat orientation = new Quat();

   /***
    * The field of view, in degrees
    */
   protected double fieldOfView = 40.0;
   /**
    * The distance to the imaging plane for direction of generated rays, based on the assumption that x/y values range
    * from -1 to 1.
    */
   protected double rayZDist = 1.0 / Math.tan(Math.toRadians(getFOVAngle()) / 2.0);

   /*
    * (non-Javadoc)
    * 
    * @see edu.rit.krisher.scene.Camera#getPosition()
    */
   public Vec3 getPosition() {
      return position;
   }

   public void setPosition(final Vec3 position) {
      this.position = position;
   }

   /*
    * (non-Javadoc)
    * 
    * @see edu.rit.krisher.scene.Camera#getOrientation()
    */
   public Quat getOrientation() {
      return orientation;
   }

   public void setOrientation(final Quat orientation) {
      this.orientation = orientation;
   }

   /**
    * Sets the horizontal field of view angle, in degrees.
    * 
    * @param FOVDegrees
    */
   public void setFOVAngle(final double FOVDegrees) {
      this.fieldOfView = FOVDegrees;
      rayZDist = 1.0 / Math.tan(Math.toRadians(getFOVAngle()) / 2.0);
   }

   public double getFOVAngle() {
      return fieldOfView;
   }

   /*
    * (non-Javadoc)
    * 
    * @see edu.rit.krisher.scene.Camera#generateRays(double, double, double)
    */
   @Override
   public void sample(final SampleRay[] rayOut, final int imageWidth, final int imageHeight, final Random rng) {
      for (final SampleRay ray : rayOut) {
         final double x = ray.pixelX * 2.0 / imageWidth - 1.0;
         final double y = ray.pixelY * 2.0 / imageHeight - 1.0;
         ray.direction.set(x, y, -rayZDist).normalize();
         orientation.transformVec(ray.direction);
         ray.origin.set(position);
      }
   }

   /**
    * Configures the camera's position and orientation so that it is looking toward the specified target location from
    * the specified distance away. The azimuth describes the camera's rotation about the y axis (with a 0 value placing
    * the camera above the -z axis, and rotation proceding clockwise -- like a compass heading). Elevation is a rotation
    * about the transformed x axis, with positive values placing the camera above the y == target.y plane looking down.
    * 
    * @param target
    * @param elevationDeg
    * @param azimuthDeg
    * @param distance
    */
   public void lookAt(final Vec3 target, final double elevationDeg, final double azimuthDeg, final double distance) {
      /*
       * Initialize a quaternion to the appropriate rotations about the y (azimuth) and x (elevation) axes.
       */
      final double azRads = -Math.toRadians(azimuthDeg) + Math.PI;
      final double elRads = -Math.toRadians(elevationDeg);
      final double sinHalfPan;
      final double cosHalfPan;
      if (azRads == 0) {
         sinHalfPan = 0;
         cosHalfPan = 1;
      } else {
         sinHalfPan = Math.sin(azRads / 2.);
         cosHalfPan = Math.cos(azRads / 2.);
      }

      final double sinHalfTilt = Math.sin(elRads / 2.);
      final double cosHalfTilt = Math.cos(elRads / 2.);
      orientation.a = cosHalfPan * cosHalfTilt;
      orientation.b = cosHalfPan * sinHalfTilt;
      orientation.c = sinHalfPan * cosHalfTilt;
      orientation.d = -sinHalfPan * sinHalfTilt;
      orientation.normalize();

      /*
       * Transform the position so it is distance away from target.
       */
      position.set(0, 0, distance);
      orientation.transformPoint(position);
      target.transformPoint(position);
   }
}
