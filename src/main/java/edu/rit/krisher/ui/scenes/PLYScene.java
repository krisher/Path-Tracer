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
import edu.rit.krisher.scene.material.DiffuseMaterial;
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

         if (boxMaterial != null)
            add(groundPlane(boxMaterial == null ? new DiffuseMaterial(new Color(1, 1, 1)) : boxMaterial, true, geomBounds));
         else {
            final TriangleMesh[] boxGeom = cornellBox(geomBounds);
            for (final TriangleMesh mesh : boxGeom) {
               add(mesh);
            }
         }
         ((PinholeCamera) camera).lookAt(geomBounds.centerPt(), 25, 180, geomBounds.diagonalLength() * 2);
         ((PinholeCamera) camera).setFOVAngle(56.14);
         if (camera instanceof DoFCamera) {
            ((DoFCamera) camera).setFocalDist(geomBounds.diagonalLength());
            ((DoFCamera) camera).setAperture(1 / 1000.0);
         }
         add(new SphereLight(new Vec3(0, geomBounds.xyzxyz[4] + geomBounds.ySpan(), geomBounds.xyzxyz[5]
                                                                                                      + geomBounds.zSpan()), geomBounds.diagonalLength() * 0.125, new Color(1.0f, 1.0f, 1.0f), 75));
      } catch (final IOException e) {
         e.printStackTrace();

      }
   }

   private static final int[] whiteSections = { 0, 1, 2, 0, 2, 3, // -y

      4, 5, 1, 4, 1, 0, // -z

      // 6, 7, 3, 6, 3, 2, //+z

         // 5, 6, 2, 5, 2, 1, // -x

         // 7, 4, 0, 7, 0, 3, // +x

      4, 7, 6, 4, 6, 5 // +y
   };

   private static final int[] redSection = { 5, 6, 2, 5, 2, 1 };
   private static final int[] greenSection = { 7, 4, 0, 7, 0, 3 };

   public static TriangleMesh[] cornellBox(final AxisAlignedBoundingBox sceneBounds) {
      final double paddingDist = 2 * Math.max(sceneBounds.xSpan(), Math.max(sceneBounds.zSpan(), sceneBounds.ySpan()));
      final double zBorder = paddingDist;
      final double yBorder = paddingDist;

      final AxisAlignedBoundingBox expandedAABB = new AxisAlignedBoundingBox(sceneBounds);
      expandedAABB.xyzxyz[0] -= paddingDist;
      expandedAABB.xyzxyz[2] -= zBorder;
      expandedAABB.xyzxyz[3] += paddingDist;
      expandedAABB.xyzxyz[5] += zBorder;
      expandedAABB.xyzxyz[4] += yBorder;

      final TriangleMesh[] geom = new TriangleMesh[] { new TriangleMesh(expandedAABB.toVertexArrayF(), whiteSections),
            new TriangleMesh(expandedAABB.toVertexArrayF(), redSection),
            new TriangleMesh(expandedAABB.toVertexArrayF(), greenSection) };
      geom[0].setMaterial(new DiffuseMaterial(new Color(0.7)));
      geom[1].setMaterial(new DiffuseMaterial(new Color(0.65, 0.06, 0.06)));
      geom[2].setMaterial(new DiffuseMaterial(new Color(0.12, 0.48, 0.1)));
      return geom;
   }
}
