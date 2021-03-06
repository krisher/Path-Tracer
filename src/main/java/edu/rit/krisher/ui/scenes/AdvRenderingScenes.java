package edu.rit.krisher.ui.scenes;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import edu.rit.krisher.fileparser.astmbrdf.ASTMBRDFParser;
import edu.rit.krisher.fileparser.ply.PLYParser;
import edu.rit.krisher.scene.Camera;
import edu.rit.krisher.scene.Geometry;
import edu.rit.krisher.scene.Material;
import edu.rit.krisher.scene.Scene;
import edu.rit.krisher.scene.acceleration.KDGeometryContainer;
import edu.rit.krisher.scene.acceleration.KDPartitionStrategy;
import edu.rit.krisher.scene.acceleration.KDSplitMeshGenerator;
import edu.rit.krisher.scene.acceleration.KDTreeMetrics;
import edu.rit.krisher.scene.acceleration.MedianPartitionStrategy;
import edu.rit.krisher.scene.acceleration.SAHPartitionStrategey;
import edu.rit.krisher.scene.camera.DoFCamera;
import edu.rit.krisher.scene.camera.PinholeCamera;
import edu.rit.krisher.scene.geometry.Sphere;
import edu.rit.krisher.scene.geometry.TriangleMesh;
import edu.rit.krisher.scene.light.SphereLight;
import edu.rit.krisher.scene.material.CheckerboardPattern;
import edu.rit.krisher.scene.material.Color;
import edu.rit.krisher.scene.material.CompositeMaterial;
import edu.rit.krisher.scene.material.DiffuseMaterial;
import edu.rit.krisher.scene.material.RefractiveMaterial;
import edu.rit.krisher.scene.material.SpecularMaterial;
import edu.rit.krisher.ui.RTDemo;
import edu.rit.krisher.util.Timer;
import edu.rit.krisher.vecmath.AxisAlignedBoundingBox;
import edu.rit.krisher.vecmath.Quat;
import edu.rit.krisher.vecmath.Transform;
import edu.rit.krisher.vecmath.Vec3;

public final class AdvRenderingScenes {
   private static final String bunnyResource = "/edu/rit/krisher/fileparser/ply/bun_zipper.ply";
   private static final URL bunnyURL = AdvRenderingScenes.class.getResource(bunnyResource);

   static final CheckerboardPattern checkerTexture = new CheckerboardPattern(new Color(0.85, 0.35, 0.35), new Color(0.85, 0.85, 0.35));
   static Material whiteMirror = new SpecularMaterial(Color.white, 100000);
   static Material whiteLambert = new DiffuseMaterial(Color.white);
   static Material blueLambert = new DiffuseMaterial(new Color(0.75, 0.75, 1.0));

   static final RefractiveMaterial blueGreenRefractive = new RefractiveMaterial(1.4, new Color(0.75, 0.75, 1.0), 100000);
   static final CompositeMaterial blueGreenMixedRefractive = new CompositeMaterial();

   static {
      blueGreenMixedRefractive.addMaterial(0.1, blueLambert);
      blueGreenMixedRefractive.addMaterial(0.8, blueGreenRefractive);
      blueGreenMixedRefractive.addMaterial(0.1, new SpecularMaterial(Color.white, 80));
   }

   private AdvRenderingScenes() {
      /*
       * Prevent construction.
       */
   }

   public static Scene[] getScenes() {
      return new Scene[] {
            new PLYScene<Camera>("Bunny (SAH KDTree)", new PinholeCamera(), bunnyURL, null, null, new SAHPartitionStrategey(), true, null),
            new PLYScene<Camera>("Bunny (Median KDTree)", new PinholeCamera(), bunnyURL, null, null, new MedianPartitionStrategy(25, 2), true, null),
            new PLYScene<Camera>("Bunny (No KDTree)", new PinholeCamera(), bunnyURL, null, null, null, true, null),
            createScene("Bunny SAH KD Tree", null, true, new SAHPartitionStrategey(25), false, createKDVisualization(blueLambert, new SAHPartitionStrategey(15), bunnyFactory())),
            createScene("Bunny Median KD Tree", null, true, new SAHPartitionStrategey(25), false, createKDVisualization(blueGreenMixedRefractive, new MedianPartitionStrategy(15, 2), bunnyFactory())),

            new PLYScene<Camera>("Bunny (Reflective)", new PinholeCamera(), bunnyURL, new CompositeMaterial(blueLambert, 0.25, whiteMirror, 0.7), null, new SAHPartitionStrategey(), true, null),
            new PLYScene<Camera>("Bunny (Refractive)", new PinholeCamera(), bunnyURL, blueGreenMixedRefractive, null, new SAHPartitionStrategey(), true, null),
            new PLYScene<Camera>("Bunny (Ground Reflection)", new PinholeCamera(), bunnyURL, blueLambert, new SpecularMaterial(Color.white, 10000), new SAHPartitionStrategey(), true, null),
            new PLYScene<Camera>("Bunny (Krylon Blue)", new PinholeCamera(), bunnyURL, ASTMBRDFParser.getKrylonBlue(), null, new SAHPartitionStrategey(), true, null),

            createScene("Sphere Diffuse", new DiffuseMaterial(new Color(1)), true, new SAHPartitionStrategey(), true, sphereFactory(new Vec3(0, 1, 0), 0.5, new DiffuseMaterial(new Color(1)))),
            createScene("Sphere Reflective", new DiffuseMaterial(new Color(1)), true, new SAHPartitionStrategey(), true, sphereFactory(new Vec3(0, 1, 0), 0.5, new SpecularMaterial(new Color(0.5,1,0.5), 100000))),
            createScene("Sphere Refractive", new DiffuseMaterial(new Color(1)), true, new SAHPartitionStrategey(), true, sphereFactory(new Vec3(0, 1, 0), 0.5, blueGreenMixedRefractive)),
            createScene("Spheres Measured BRDFs", new DiffuseMaterial(new Color(0.75)), true, new SAHPartitionStrategey(), true, sphereFactory(new Vec3(0, 1, 0), 0.5, ASTMBRDFParser.getMystique()), sphereFactory(new Vec3(-1, 1, 0), 0.5, ASTMBRDFParser.getKrylonBlue())),

            createSceneMultiTree("Lucy", null, false, new SAHPartitionStrategey(25), true, plyFactory(new File("/home/krisher/Download/lucy.ply"), null, false, new Quat(new Vec3(0, 0, 1), Math.PI).multiply(new Quat(new Vec3(1, 0, 0), Math.PI / 2.0)))),
            createSceneMultiTree("Female (Reflective)", null, false, new SAHPartitionStrategey(), true, plyFactory(new File("/home/krisher/Download/female01.ply"), new CompositeMaterial(blueLambert, 0.6, whiteMirror, 0.4), true, new Quat(new Vec3(1, 0, 0), -Math.PI / 2.0))),
            createScene("Teapot", null, false, new SAHPartitionStrategey(), true, plyFactory(new File("/home/krisher/Download/teapot.ply"), null, true)),
            createScene("Dragon", null, false, new SAHPartitionStrategey(), true, plyFactory(new File("/home/krisher/Downloads/dragon_vrip.ply"))),
            createScene("Dragon (Normals)", null, false, new SAHPartitionStrategey(), true, plyFactory(new File("/home/krisher/Downloads/dragon_vrip.ply"), new CompositeMaterial(blueLambert, 0.6, whiteMirror, 0.4), true, null)),
            createScene("Buddha", null, false, new SAHPartitionStrategey(), true, plyFactory(new File("/home/krisher/Download/happy_vrip.ply"))),
            createScene("XYZRGB Dragon", null, false, new SAHPartitionStrategey(), true, plyFactory(new File("/home/krisher/Download/xyzrgb_dragon.ply"))),
            createScene("Thai Statue", null, false, new SAHPartitionStrategey(), true, plyFactory(new File("/home/krisher/Download/xyzrgb_statuette.ply"))) };

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
            final KDGeometryContainer accel = new KDGeometryContainer(kdStrategy, geometry);
            kdTimer.stop().print();
            System.out.println(new KDTreeMetrics(accel));
            System.out.println("Generating KDTree visualization mesh...");
            final TriangleMesh kdMesh = KDSplitMeshGenerator.generateLeafNodeMesh(accel);
            kdMesh.setMaterial(material);
            return kdMesh;
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
            geometry[geometry.length - 1] = groundPlane(groundMat == null ? new DiffuseMaterial(new Color(1, 1, 1))
            : groundMat, walls, geomBounds);
            System.out.println("Scene bounds: " + geometry[geometry.length - 1].getBounds(-1));
            if (kdStrategy != null) {
               final Timer kdTimer = new Timer("KD-Tree Construction (" + name + ")").start();
               final KDGeometryContainer accel = new KDGeometryContainer(kdStrategy, geometry);
               kdTimer.stop().print();
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
            add(new SphereLight(new Vec3(0, geomBounds.xyzxyz[4] + geomBounds.ySpan(), geomBounds.xyzxyz[5]
                                                                                                         + geomBounds.zSpan()), geomBounds.diagonalLength() * 0.125, new Color(1.0f, 1.0f, 1.0f), 75));
            // add(new PointLight(new Vec3(0, geomBounds.xyzxyz[4] + geomBounds.ySpan(), geomBounds.xyzxyz[5]
            // + geomBounds.zSpan()), 1.0f, 1.0f, 1.0f, 75));
         }
      };
   }

   public static Scene createSceneMultiTree(final String name, final Material groundMat, final boolean walls,
         final KDPartitionStrategy kdStrategy, final boolean dofCamera, final GeometryFactory... geomFactories) {
      return new AbstractSceneDescription<Camera>(name, (dofCamera) ? new DoFCamera() : new PinholeCamera()) {
         @Override
         protected void initScene() {

            final AxisAlignedBoundingBox geomBounds = new AxisAlignedBoundingBox();
            final Geometry[] geometry = new Geometry[geomFactories.length];
            for (int i = 0; i < geomFactories.length; ++i) {
               geometry[i] = geomFactories[i].createGeometry();
               geomBounds.union(geometry[i].getBounds(-1));

               final Timer kdTimer = new Timer("KD-Tree Construction (" + name + ")").start();
               final KDGeometryContainer accel = new KDGeometryContainer(kdStrategy, geometry[i]);
               kdTimer.stop().print();
               System.out.println(new KDTreeMetrics(accel));
               add(accel);
            }
            add(groundPlane(groundMat == null ? new DiffuseMaterial(new Color(1, 1, 1)) : groundMat, walls, geomBounds));
            ((PinholeCamera) camera).lookAt(geomBounds.centerPt(), 25, 225, geomBounds.diagonalLength());
            ((PinholeCamera) camera).setFOVAngle(56.14);
            if (dofCamera) {
               ((DoFCamera) camera).setFocalDist(geomBounds.diagonalLength() / 2.0);
               ((DoFCamera) camera).setAperture(1 / 1000.0);
            }
            //            add(new SphereLight(new Vec3(geomBounds.xyzxyz[0] - geomBounds.xSpan(), geomBounds.xyzxyz[4]
            //                  + geomBounds.ySpan(), geomBounds.xyzxyz[5] + geomBounds.zSpan() * 2.0), geomBounds.diagonalLength() * 0.125, new Color(1.0f, 1.0f, 1.0f), 75));
            add(new SphereLight(new Vec3(0, geomBounds.xyzxyz[4] + geomBounds.ySpan(), geomBounds.xyzxyz[5]
                                                                                                         + geomBounds.zSpan()), geomBounds.diagonalLength() * 0.125, new Color(1.0f, 1.0f, 1.0f), 75));
            // add(new PointLight(new Vec3(3, 6, 5), 1.0f, 1.0f, 1.0f, 75));

         }
      };
   }

   public static interface GeometryFactory {
      public Geometry createGeometry();
   }

   private static GeometryFactory plyFactory(final File file) {
      return plyFactory(file, null, false, null);
   }

   private static GeometryFactory plyFactory(final File file, final Material material, final boolean computeNormals) {
      return plyFactory(file, material, computeNormals, null);
   }

   private static GeometryFactory plyFactory(final File file, final Material material, final boolean computeNormals,
         final Transform vertTransform) {
      return new GeometryFactory() {
         @Override
         public Geometry createGeometry() {
            try {
               final TriangleMesh model = PLYParser.parseTriangleMesh(new BufferedInputStream(new FileInputStream(file)), computeNormals);
               if (vertTransform != null) {
                  model.transform(vertTransform);
               }
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
      return bunnyFactory(new DiffuseMaterial(Color.white), false);
   }

   private static final GeometryFactory bunnyFactory(final Material mat, final boolean computeNormals) {
      return new GeometryFactory() {
         @Override
         public Geometry createGeometry() {

            InputStream stream = null;
            try {
               stream = new BufferedInputStream(RTDemo.class.getResourceAsStream(bunnyResource));
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

   private static final GeometryFactory sphereFactory(final Vec3 position, final double radius, final Material material) {
      return new GeometryFactory() {

         @Override
         public Geometry createGeometry() {
            return new Sphere(position, radius, material);
         }
      };
   }
}
