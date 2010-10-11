package edu.rit.krisher.ui.scenes;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.ZipInputStream;

import edu.rit.krisher.fileparser.ply.PLYParser;
import edu.rit.krisher.scene.AxisAlignedBoundingBox;
import edu.rit.krisher.scene.Camera;
import edu.rit.krisher.scene.Geometry;
import edu.rit.krisher.scene.Material;
import edu.rit.krisher.scene.Scene;
import edu.rit.krisher.scene.camera.DoFCamera;
import edu.rit.krisher.scene.camera.PinholeCamera;
import edu.rit.krisher.scene.geometry.TriangleMesh;
import edu.rit.krisher.scene.geometry.acceleration.KDPartitionStrategy;
import edu.rit.krisher.scene.geometry.acceleration.KDSplitMeshGenerator;
import edu.rit.krisher.scene.geometry.acceleration.KDTree;
import edu.rit.krisher.scene.geometry.acceleration.KDTreeMetrics;
import edu.rit.krisher.scene.geometry.acceleration.MedianPartitionStrategy;
import edu.rit.krisher.scene.geometry.acceleration.SAHPartitionStrategey;
import edu.rit.krisher.scene.geometry.buffer.IndexBuffer;
import edu.rit.krisher.scene.geometry.buffer.Vec3Buffer;
import edu.rit.krisher.scene.geometry.buffer.Vec3fBuffer;
import edu.rit.krisher.scene.light.SphereLight;
import edu.rit.krisher.scene.material.CheckerboardPattern;
import edu.rit.krisher.scene.material.Color;
import edu.rit.krisher.scene.material.CompositeBRDF;
import edu.rit.krisher.scene.material.LambertBRDF;
import edu.rit.krisher.scene.material.PhongSpecularBRDF;
import edu.rit.krisher.scene.material.RefractiveBRDF;
import edu.rit.krisher.ui.RTDemo;
import edu.rit.krisher.util.Timer;
import edu.rit.krisher.vecmath.Vec3;

public final class AdvRenderingScenes {
   private static final String bunnyResource = "/edu/rit/krisher/fileparser/ply/bun_zipper.ply.zip";

   static final CheckerboardPattern checkerTexture = new CheckerboardPattern(new Color(0.85, 0.35, 0.35), new Color(0.85, 0.85, 0.35));
   static Material whiteMirror = new PhongSpecularBRDF(Color.white, 100000);
   static Material whiteLambert = new LambertBRDF(Color.white);
   static Material blueLambert = new LambertBRDF(new Color(0.75, 0.75, 1.0));

   static final RefractiveBRDF blueGreenRefractive = new RefractiveBRDF(1.4, new Color(0.75, 0.75, 1.0), 100000);
   static final CompositeBRDF blueGreenMixedRefractive = new CompositeBRDF();

   static {
      blueGreenMixedRefractive.addMaterial(0.1, blueLambert);
      blueGreenMixedRefractive.addMaterial(0.8, blueGreenRefractive);
      blueGreenMixedRefractive.addMaterial(0.1, new PhongSpecularBRDF(Color.white, 80));
   }

   private AdvRenderingScenes() {
      /*
       * Prevent construction.
       */
   }

   public static Scene[] getScenes() {
      return new Scene[] {
            createScene("Bunny (SAH KDTree)", null, false, new SAHPartitionStrategey(), true, bunnyFactory()),
            createScene("Bunny (Median-Centroid KDTree)", null, false, new MedianPartitionStrategy(25, 2), true, bunnyFactory()),
            createScene("Bunny (No Accel)", null, false, null, true, bunnyFactory()),
            createScene("Bunny SAH KD Tree", null, true, new SAHPartitionStrategey(25), false, createKDVisualization(blueLambert, new SAHPartitionStrategey(15), bunnyFactory())),
            createScene("Bunny Median KD Tree", null, true, new SAHPartitionStrategey(25), false, createKDVisualization(blueGreenMixedRefractive, new MedianPartitionStrategy(15, 2), bunnyFactory())),
            createScene("Bunny (Reflective)", null, false, new SAHPartitionStrategey(), true, bunnyFactory(new CompositeBRDF(blueLambert, 0.6, whiteMirror, 0.4), true)),
            createScene("Bunny (Refractive)", null, true, new SAHPartitionStrategey(), true, bunnyFactory(blueGreenMixedRefractive, true)),
            createScene("Bunny (Ground Reflection)", new CompositeBRDF(new LambertBRDF(Color.white), 0.25, new PhongSpecularBRDF(Color.white, 100000), 0.75), false, new SAHPartitionStrategey(), true, bunnyFactory()),
            createScene("Lucy", null, false, new SAHPartitionStrategey(12), true, plyFactory(new File("/home/krisher/Download/lucy.ply"))),
            createScene("Dragon", null, false, new SAHPartitionStrategey(), true, plyFactory(new File("/home/krisher/Downloads/dragon_vrip.ply"))),
            createScene("Dragon (Normals)", null, false, new SAHPartitionStrategey(), true, plyFactory(new File("/home/krisher/Downloads/dragon_vrip.ply"), new CompositeBRDF(blueLambert, 0.6, whiteMirror, 0.4), true)),
            createScene("Buddha", null, false, new SAHPartitionStrategey(), true, plyFactory(new File("/home/krisher/Download/happy_vrip.ply"))),
            createScene("XYZRGB Dragon", null, false, new SAHPartitionStrategey(), true, plyFactory(new File("/home/krisher/Download/xyzrgb_dragon.ply"))),
            createScene("Thai Statue", null, false, new SAHPartitionStrategey(), true, plyFactory(new File("/home/krisher/Download/xyzrgb_statuette.ply"))) };

   }


   private static TriangleMesh groundPlane(final Material mat, final boolean walls,
         final AxisAlignedBoundingBox sceneBounds) {
      final double xBorder = sceneBounds.xSpan() * 2;
      final double zBorder = sceneBounds.zSpan() * 2;
      final Vec3Buffer vb = new Vec3fBuffer(walls ? 8 : 4);
      final IndexBuffer ib = new IndexBuffer(walls ? 30 : 6);
      vb.put(sceneBounds.maxXYZ[0] + xBorder, sceneBounds.minXYZ[1], sceneBounds.minXYZ[2] - zBorder);
      vb.put(sceneBounds.minXYZ[0] - xBorder, sceneBounds.minXYZ[1], sceneBounds.minXYZ[2] - zBorder);
      vb.put(sceneBounds.minXYZ[0] - xBorder, sceneBounds.minXYZ[1], sceneBounds.maxXYZ[2] + zBorder);
      vb.put(sceneBounds.maxXYZ[0] + xBorder, sceneBounds.minXYZ[1], sceneBounds.maxXYZ[2] + zBorder);
      ib.put(0).put(1).put(2);
      ib.put(0).put(2).put(3);

      if (walls) {
         final double yBorder = sceneBounds.ySpan() * 2;
         vb.put(sceneBounds.maxXYZ[0] + xBorder, sceneBounds.maxXYZ[1] + yBorder, sceneBounds.minXYZ[2] - zBorder);
         vb.put(sceneBounds.minXYZ[0] - xBorder, sceneBounds.maxXYZ[1] + yBorder, sceneBounds.minXYZ[2] - zBorder);
         vb.put(sceneBounds.minXYZ[0] - xBorder, sceneBounds.maxXYZ[1] + yBorder, sceneBounds.maxXYZ[2] + zBorder);
         vb.put(sceneBounds.maxXYZ[0] + xBorder, sceneBounds.maxXYZ[1] + yBorder, sceneBounds.maxXYZ[2] + zBorder);

         ib.put(4).put(5).put(1);
         ib.put(4).put(1).put(0);

         ib.put(5).put(6).put(2);
         ib.put(5).put(2).put(1);

         ib.put(6).put(7).put(3);
         ib.put(6).put(3).put(2);

         ib.put(7).put(4).put(0);
         ib.put(7).put(0).put(3);
      }

      final TriangleMesh mesh = new TriangleMesh(vb, ib.getIndices());
      if (mat != null) {
         mesh.setMaterial(mat);
      }
      return mesh;
   }

   public static GeometryFactory createKDVisualization(final Material material, final KDPartitionStrategy kdStrategy,
         final GeometryFactory... geomFactories) {
      return new GeometryFactory() {
         @Override
         public Geometry createGeometry() {
            final Geometry[] geometry = new Geometry[geomFactories.length];
            for (int i = 0; i < geomFactories.length; ++i) {
               geometry[i] = geomFactories[i].createGeometry();
            }
            final Timer kdTimer = new Timer("KD-Tree Construction (KD Visualization)").start();
            final KDTree accel = new KDTree(kdStrategy, geometry);
            kdTimer.stop().print(System.out);
            System.out.println(new KDTreeMetrics(accel));
            System.out.println("Generating KDTree visualization mesh...");
            final TriangleMesh kdMesh = KDSplitMeshGenerator.generateLeafNodeMesh(accel);
            kdMesh.setMaterial(material);
            return kdMesh;
         }
      };
   }

   public static GeometryFactory boxes(final AxisAlignedBoundingBox... aabbs) {
      return new GeometryFactory() {
         final Vec3Buffer vertices = new Vec3fBuffer(8 * aabbs.length);
         final IndexBuffer ib = new IndexBuffer(36 * aabbs.length);

         @Override
         public Geometry createGeometry() {
            int idxBase = 0;
            for (final AxisAlignedBoundingBox aabb : aabbs) {
               vertices.put(aabb.maxXYZ[0], aabb.minXYZ[1], aabb.minXYZ[2]);
               vertices.put(aabb.minXYZ[0], aabb.minXYZ[1], aabb.minXYZ[2]);
               vertices.put(aabb.minXYZ[0], aabb.minXYZ[1], aabb.maxXYZ[2]);
               vertices.put(aabb.maxXYZ[0], aabb.minXYZ[1], aabb.maxXYZ[2]);

               vertices.put(aabb.maxXYZ[0], aabb.maxXYZ[1], aabb.minXYZ[2]);
               vertices.put(aabb.minXYZ[0], aabb.maxXYZ[1], aabb.minXYZ[2]);
               vertices.put(aabb.minXYZ[0], aabb.maxXYZ[1], aabb.maxXYZ[2]);
               vertices.put(aabb.maxXYZ[0], aabb.maxXYZ[1], aabb.maxXYZ[2]);

               ib.put(idxBase + 0).put(idxBase + 1).put(idxBase + 2);
               ib.put(idxBase + 0).put(idxBase + 2).put(idxBase + 3);

               ib.put(idxBase + 5).put(idxBase + 4).put(idxBase + 1);
               ib.put(idxBase + 4).put(idxBase + 0).put(idxBase + 1);

               ib.put(idxBase + 5).put(idxBase + 2).put(idxBase + 6);
               ib.put(idxBase + 5).put(idxBase + 1).put(idxBase + 2);

               ib.put(idxBase + 3).put(idxBase + 7).put(idxBase + 6);
               ib.put(idxBase + 3).put(idxBase + 6).put(idxBase + 2);

               ib.put(idxBase + 0).put(idxBase + 4).put(idxBase + 7);
               ib.put(idxBase + 0).put(idxBase + 7).put(idxBase + 3);

               ib.put(idxBase + 4).put(idxBase + 5).put(idxBase + 6);
               ib.put(idxBase + 4).put(idxBase + 6).put(idxBase + 7);

               idxBase += 8;
            }
            return new TriangleMesh(vertices, ib.getIndices());
         }
      };
   }

   public static Scene createScene(final String name, final Material groundMat, final boolean walls,
         final KDPartitionStrategy kdStrategy, final boolean dofCamera, final GeometryFactory... geomFactories) {
      return new AbstractSceneDescription<Camera>(name, (dofCamera) ? new DoFCamera() : new PinholeCamera()) {
         @Override
         protected void initScene() {

            final AxisAlignedBoundingBox geomBounds = new AxisAlignedBoundingBox();
            final Geometry[] geometry = new Geometry[geomFactories.length + 1];
            for (int i = 0; i < geomFactories.length; ++i) {
               geometry[i] = geomFactories[i].createGeometry();
               geomBounds.union(geometry[i].getBounds(-1));
            }
            geometry[geometry.length - 1] = groundPlane(groundMat == null ? new LambertBRDF(new Color(1, 1, 1))
            : groundMat, walls, geomBounds);
            if (kdStrategy != null) {
               final Timer kdTimer = new Timer("KD-Tree Construction (" + name + ")").start();
               final KDTree accel = new KDTree(kdStrategy, geometry);
               kdTimer.stop().print(System.out);
               System.out.println(new KDTreeMetrics(accel));
               add(accel);
            } else {
               for (final Geometry geom : geometry) {
                  add(geom);
               }
            }
            ((PinholeCamera) camera).lookAt(geomBounds.centerPt(), 25, 225, geomBounds.diagonalLength());
            ((PinholeCamera) camera).setFOVAngle(56.14);
            if (dofCamera) {
               ((DoFCamera) camera).setFocalDist(geomBounds.diagonalLength() / 2.0);
               ((DoFCamera) camera).setAperture(1 / 1000.0);
            }
            add(new SphereLight(new Vec3(0, geomBounds.maxXYZ[1] + geomBounds.ySpan(), geomBounds.maxXYZ[2]
                                                                                                         + geomBounds.zSpan()), geomBounds.diagonalLength() * 0.125, new Color(1.0f, 1.0f, 1.0f), 75));
            // add(new PointLight(new Vec3(3, 6, 5), 1.0f, 1.0f, 1.0f, 75));

         }
      };
   }

   public static interface GeometryFactory {
      public Geometry createGeometry();
   }

   private static GeometryFactory plyFactory(final File file) {
      return plyFactory(file, null, false);
   }

   private static GeometryFactory plyFactory(final File file, final Material material, final boolean computeNormals) {
      return new GeometryFactory() {
         @Override
         public Geometry createGeometry() {
            try {
               final TriangleMesh model = PLYParser.parseTriangleMesh(new BufferedInputStream(new FileInputStream(file)), computeNormals);
               if (material != null)
                  model.setMaterial(material);
               return model;
            } catch (final IOException e) {
               e.printStackTrace();
               return null;
            }
         }
      };
   }

   private static GeometryFactory bunnyFactory() {
      return bunnyFactory(new LambertBRDF(Color.white), false);
   }

   private static final GeometryFactory bunnyFactory(final Material mat, final boolean computeNormals) {
      return new GeometryFactory() {
         @Override
         public Geometry createGeometry() {

            ZipInputStream stream = null;
            try {
               stream = new ZipInputStream(RTDemo.class.getResourceAsStream(bunnyResource));
               stream.getNextEntry();
               final TriangleMesh mesh = PLYParser.parseTriangleMesh(stream, computeNormals);
               if (mat != null)
                  mesh.setMaterial(mat);
               return mesh;
            } catch (final IOException ioe) {
               ioe.printStackTrace();
            } finally {
               try {
                  if (stream != null)
                     stream.close();
               } catch (final IOException e) {
                  e.printStackTrace();
               }
            }
            return null;
         }
      };
   }
}
