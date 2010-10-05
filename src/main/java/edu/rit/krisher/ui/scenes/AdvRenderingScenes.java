package edu.rit.krisher.ui.scenes;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipInputStream;

import edu.rit.krisher.fileparser.ply.PLYParser;
import edu.rit.krisher.scene.AxisAlignedBoundingBox;
import edu.rit.krisher.scene.Geometry;
import edu.rit.krisher.scene.Material;
import edu.rit.krisher.scene.Scene;
import edu.rit.krisher.scene.camera.DoFCamera;
import edu.rit.krisher.scene.geometry.TriangleMesh;
import edu.rit.krisher.scene.geometry.acceleration.KDPartitionStrategy;
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
   static Material blueLambert = new LambertBRDF(new Color(0.25, 0.25, 1.0));

   static final RefractiveBRDF blueGreenRefractive = new RefractiveBRDF(1.4, new Color(0.75, 0.75, 1.0), 100000);
   static final CompositeBRDF blueGreenMixedRefractive = new CompositeBRDF();

   static {
      blueGreenMixedRefractive.addMaterial(0.1, blueLambert);
      blueGreenMixedRefractive.addMaterial(0.7, blueGreenRefractive);
      blueGreenMixedRefractive.addMaterial(0.1, new PhongSpecularBRDF(Color.white, 80));
   }

   private AdvRenderingScenes() {
      /*
       * Prevent construction.
       */
   }

   public static Scene[] getScenes() {
      return new Scene[] {
            bunnySceneKDSAH1,
            bunnySceneKDMedian,
            bunnyScene,
            bunnySceneKDRef,
            bunnySceneReflection,
            createScene("Lucy", null, false, new SAHPartitionStrategey(12), plyFactory(new File("/home/krisher/Download/lucy.ply"))) };
   }

   private static final Scene bunnyScene = createScene("Bunny (No Accel)", null, false, null, bunnyFactory());

   private static final Scene bunnySceneKDSAH1 = createScene("Bunny (SAH KDTree)", null, false, new SAHPartitionStrategey(), bunnyFactory());
   private static final Scene bunnySceneKDMedian = createScene("Bunny (Median-Centroid KDTree)", null, false, new MedianPartitionStrategy(25, 2), bunnyFactory());

   private static final Scene bunnySceneKDRef = createScene("Bunny (Refractive)", null, true, new SAHPartitionStrategey(), bunnyFactory(blueGreenMixedRefractive));
   private static final Scene bunnySceneReflection = createScene("Bunny (Ground Reflection)", new CompositeBRDF(new LambertBRDF(Color.white), 0.25, new PhongSpecularBRDF(Color.white, 100000), 0.75), false, new SAHPartitionStrategey(), bunnyFactory());


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

      final TriangleMesh mesh = new TriangleMesh(vb, ib);
      if (mat != null) {
         mesh.setMaterial(mat);
      }
      return mesh;
   }

   public static Scene createScene(final String name, final Material groundMat, final boolean walls,
         final KDPartitionStrategy kdStrategy, final GeometryFactory... geomFactories) {
      return new AbstractSceneDescription<DoFCamera>(name, new DoFCamera()) {
         @Override
         protected void initScene() {

            final AxisAlignedBoundingBox geomBounds = new AxisAlignedBoundingBox();
            final Geometry[] geometry = new Geometry[geomFactories.length + 1];
            for (int i = 0; i < geomFactories.length; ++i) {
               geometry[i] = geomFactories[i].createGeometry();
               geomBounds.union(geometry[i].getBounds());
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
            camera.lookAt(geomBounds.centerPt(), 25, 225, geomBounds.diagonalLength());
            camera.setFocalDist(geomBounds.diagonalLength());
            camera.setAperture(1 / 200.0);
            add(new SphereLight(new Vec3(0, geomBounds.maxXYZ[1] + geomBounds.ySpan(), geomBounds.maxXYZ[2]
                                                                                                         + geomBounds.zSpan()), geomBounds.diagonalLength() * 0.125, new Color(1.0f, 1.0f, 1.0f), 75));
            // add(new PointLight(new Vec3(3, 6, 5), 1.0f, 1.0f, 1.0f, 75));

            camera.setFOVAngle(56.14);
         }
      };
   }

   public static interface GeometryFactory {
      public Geometry createGeometry();
   }

   private static GeometryFactory plyFactory(final File file) {
      return plyFactory(file, null);
   }

   private static GeometryFactory plyFactory(final File file, final Material material) {
      return new GeometryFactory() {
         @Override
         public Geometry createGeometry() {
            try {
               final TriangleMesh model = PLYParser.parseTriangleMesh(file);
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
      return bunnyFactory(new LambertBRDF(Color.white));
   }

   private static final GeometryFactory bunnyFactory(final Material mat) {
      return new GeometryFactory() {
         @Override
         public Geometry createGeometry() {

            ZipInputStream stream = null;
            try {
               stream = new ZipInputStream(RTDemo.class.getResourceAsStream(bunnyResource));
               stream.getNextEntry();
               final TriangleMesh mesh = PLYParser.parseTriangleMesh(stream);
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
