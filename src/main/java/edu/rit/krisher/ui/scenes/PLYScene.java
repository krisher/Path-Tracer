/**
 * 
 */
package edu.rit.krisher.ui.scenes;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;

import edu.rit.krisher.fileparser.ply.PLYParser;
import edu.rit.krisher.scene.Camera;
import edu.rit.krisher.scene.Geometry;
import edu.rit.krisher.scene.Material;
import edu.rit.krisher.scene.acceleration.KDGeometryContainer;
import edu.rit.krisher.scene.acceleration.KDPartitionStrategy;
import edu.rit.krisher.scene.acceleration.KDTreeMetrics;
import edu.rit.krisher.scene.camera.DoFCamera;
import edu.rit.krisher.scene.camera.PinholeCamera;
import edu.rit.krisher.scene.geometry.TriangleMesh;
import edu.rit.krisher.scene.light.SphereLight;
import edu.rit.krisher.scene.material.Color;
import edu.rit.krisher.util.Timer;
import edu.rit.krisher.vecmath.AxisAlignedBoundingBox;
import edu.rit.krisher.vecmath.Transform;
import edu.rit.krisher.vecmath.Vec3;

/**
 *
 */
public class PLYScene<C extends Camera> extends AbstractSceneDescription<C> {

   private final URL modelURL;
   private final Material modelMaterial;
   private final Material boxMaterial;
   private final KDPartitionStrategy partitionStrategy;
   private final boolean interpolateNormals;
   private final Transform modelTransform;

   public PLYScene(final String name, final C camera, final URL modelFile) {
      this(name, camera, modelFile, null, null, null, false, null);
   }

   public PLYScene(final String name, final C camera, final URL modelFile, final Material modelMat,
         final Material boxMat, final KDPartitionStrategy partitionStrategy, final boolean interpolateNormals,
         final Transform modelTransform) {
      super(name, camera);
      this.modelURL = modelFile;
      this.modelMaterial = modelMat;
      this.boxMaterial = boxMat;
      this.partitionStrategy = partitionStrategy;
      this.interpolateNormals = interpolateNormals;
      this.modelTransform = modelTransform;
   }

   @Override
   protected void initScene() {
      try {
         final TriangleMesh model = PLYParser.parseTriangleMesh(new BufferedInputStream(modelURL.openStream()), interpolateNormals);
         if (modelTransform != null) {
            model.transform(modelTransform);
         }
         if (modelMaterial != null)
            model.setMaterial(modelMaterial);

         final AxisAlignedBoundingBox geomBounds = model.getBounds(Geometry.ALL_PRIMITIVES);

         if (partitionStrategy != null) {
            final Timer kdTimer = new Timer("KD-Tree Construction (" + getName() + ")").start();
            final KDGeometryContainer accel = new KDGeometryContainer(partitionStrategy, model);
            kdTimer.stop().print();
            System.out.println(new KDTreeMetrics(accel));
            add(accel);
         } else {
            add(model);
         }

         final TriangleMesh[] boxGeom = cornellBox(geomBounds, boxMaterial, boxMaterial, boxMaterial);

         for (final TriangleMesh mesh : boxGeom) {
            add(mesh);
         }

         final AxisAlignedBoundingBox sceneBounds = getBounds();
         ((PinholeCamera) camera).lookAt(sceneBounds.centerPt(), 0, 180, geomBounds.diagonalLength() * 1.75);
         ((PinholeCamera) camera).setFOVAngle(56.14);
         if (camera instanceof DoFCamera) {
            ((DoFCamera) camera).setFocalDist(geomBounds.diagonalLength());
            ((DoFCamera) camera).setAperture(1 / 1000.0);
         }
         final Vec3 lightPosition = geomBounds.centerPt();
         lightPosition.y = sceneBounds.xyzxyz[4] - geomBounds.diagonalLength() * 0.0625;
         add(new SphereLight(lightPosition, geomBounds.diagonalLength() * 0.0625, new Color(18.0f, 15.0f, 8.0f).multiply(4.0 * Math.PI)));
      } catch (final IOException e) {
         e.printStackTrace();

      }
   }

}
