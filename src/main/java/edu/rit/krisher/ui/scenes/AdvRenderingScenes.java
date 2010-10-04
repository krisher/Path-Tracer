package edu.rit.krisher.ui.scenes;

import java.io.IOException;
import java.util.zip.ZipInputStream;

import edu.rit.krisher.fileparser.ply.PLYParser;
import edu.rit.krisher.scene.AxisAlignedBoundingBox;
import edu.rit.krisher.scene.Material;
import edu.rit.krisher.scene.Scene;
import edu.rit.krisher.scene.camera.DoFCamera;
import edu.rit.krisher.scene.camera.PinholeCamera;
import edu.rit.krisher.scene.geometry.Box;
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

   static final RefractiveBRDF blueGreenRefractive = new RefractiveBRDF(1.4, new Color(0.75, 0.95, 0.95), 100000);
   static final CompositeBRDF blueGreenMixedRefractive = new CompositeBRDF();

   static {
      blueGreenMixedRefractive.addMaterial(0.05, blueLambert);
      blueGreenMixedRefractive.addMaterial(0.8, blueGreenRefractive);
      blueGreenMixedRefractive.addMaterial(0.01, new PhongSpecularBRDF(Color.white, 80));
   }

   private AdvRenderingScenes() {
      /*
       * Prevent construction.
       */
   }

   public static Scene[] getScenes() {
      return new Scene[] { bunnyScene, bunnySceneKDMedian, bunnySceneKDSAH1, bunnySceneKDRef, boxKDTest };
   }

   private static final Scene bunnyScene = new AbstractSceneDescription<PinholeCamera>("Bunny Scene", new PinholeCamera()) {
      @Override
      protected void initScene() {
         add(new Box(10, 1, 16, new CompositeBRDF(new LambertBRDF(checkerTexture), 0.6, whiteMirror, 0.4), new Vec3(-2, -0.5, 0), false));
         final TriangleMesh bunnyMesh = loadBunny();

         add(bunnyMesh);
         final AxisAlignedBoundingBox bounds = bunnyMesh.getBounds();
         camera.lookAt(bounds.centerPt(), 35, 180, bounds.diagonalLength() * 0.8);
         add(new SphereLight(new Vec3(3, 6, 3.5), 1.0, new Color(1.0f, 1.0f, 1.0f), 75));

         camera.setFOVAngle(56.14);
         setBackground(new Color(0.25, 0.25, 0.65));
      }

   };

   private static final Scene bunnySceneKDSAH1 = createKDBunnyScene("Bunny Scene (KD-SAH)", new SAHPartitionStrategey());
   private static final Scene bunnySceneKDMedian = createKDBunnyScene("Bunny Scene (KD-Median)", new MedianPartitionStrategy(25, 2));

   private static final Scene bunnySceneKDRef = new AbstractSceneDescription<DoFCamera>("Bunny KD-Refractive", new DoFCamera()) {
      @Override
      protected void initScene() {
         final TriangleMesh bunnyMesh = loadBunny();

         bunnyMesh.setMaterial(blueGreenMixedRefractive);
         final Timer kdTimer = new Timer("KD-Tree Construction (Bunny Mesh)").start();
         final KDTree accel = new KDTree(new SAHPartitionStrategey(25), bunnyMesh, groundPlane(whiteLambert, true));
         kdTimer.stop().print(System.out);
         System.out.println(new KDTreeMetrics(accel));
         add(accel);
         final AxisAlignedBoundingBox bounds = bunnyMesh.getBounds();
         camera.lookAt(bounds.centerPt(), 25, 180, bounds.diagonalLength());
         camera.setFocalDist(bounds.diagonalLength());
         camera.setAperture(1 / 100.0);

         add(new SphereLight(new Vec3(0, 6, 0), 1.0, new Color(1.0f, 1.0f, 1.0f), 75));

         camera.setFOVAngle(56.14);
         // scene.setBackground(new Color(0.25, 0.25, 0.65));
      }
   };

   private static final Scene boxKDTest = new AbstractSceneDescription<DoFCamera>("Box KD Test", new DoFCamera()) {
      @Override
      protected void initScene() {
         final Timer kdTimer = new Timer("KD-Tree Construction (Box KD Test)").start();
         final KDTree accel = new KDTree(new SAHPartitionStrategey(), groundPlane(whiteLambert, true));
         kdTimer.stop().print(System.out);
         System.out.println(new KDTreeMetrics(accel));
         add(accel);
         final AxisAlignedBoundingBox bounds = accel.getBounds();
         camera.lookAt(bounds.centerPt(), 25, 180, bounds.diagonalLength());
         camera.setFocalDist(bounds.diagonalLength());
         camera.setAperture(1 / 100.0);

         add(new SphereLight(new Vec3(0, 6, 0), 1.0, new Color(1.0f, 1.0f, 1.0f), 75));

         camera.setFOVAngle(56.14);
         // scene.setBackground(new Color(0.25, 0.25, 0.65));
      }
   };

   private static Scene createKDBunnyScene(final String name, final KDPartitionStrategy strategy) {
      return new AbstractSceneDescription<DoFCamera>(name, new DoFCamera()) {
         @Override
         protected void initScene() {
            add(new Box(10, 1, 16, new CompositeBRDF(new LambertBRDF(checkerTexture), 0.2, new PhongSpecularBRDF(Color.white, 1000), 0.8), new Vec3(-2, -0.5, 0), false));
            final TriangleMesh bunnyMesh = loadBunny();
            final Timer kdTimer = new Timer("KD-Tree Construction (" + name + ")").start();
            final KDTree accel = new KDTree(strategy, bunnyMesh);
            kdTimer.stop().print(System.out);
            System.out.println(new KDTreeMetrics(accel));
            add(accel);
            final AxisAlignedBoundingBox bounds = bunnyMesh.getBounds();

            camera.lookAt(bounds.centerPt(), 25, 180, bounds.diagonalLength());
            camera.setFocalDist(bounds.diagonalLength());
            camera.setAperture(1 / 100.0);
            add(new SphereLight(new Vec3(3, 6, 3.5), 1.0, new Color(1.0f, 1.0f, 1.0f), 75));

            camera.setFOVAngle(56.14);
         }
      };
   }

   private static TriangleMesh groundPlane(final Material mat, final boolean walls) {
      final Vec3Buffer vb = new Vec3fBuffer(walls ? 8 : 4);
      final IndexBuffer ib = new IndexBuffer(walls ? 30 : 6);
      vb.put(new Vec3(5, 0, -5));
      vb.put(new Vec3(-5, 0, -5));
      vb.put(new Vec3(-5, 0, 5));
      vb.put(new Vec3(5, 0, 5));
      ib.put(0).put(1).put(2);
      ib.put(0).put(2).put(3);

      if (walls) {
         vb.put(new Vec3(5, 10, -5));
         vb.put(new Vec3(-5, 10, -5));
         vb.put(new Vec3(-5, 10, 5));
         vb.put(new Vec3(5, 10, 5));

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

   private static TriangleMesh loadBunny() {
      ZipInputStream stream = null;
      try {
         stream = new ZipInputStream(RTDemo.class.getResourceAsStream(bunnyResource));
         stream.getNextEntry();
         return PLYParser.parseTriangleMesh(stream);
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
}
