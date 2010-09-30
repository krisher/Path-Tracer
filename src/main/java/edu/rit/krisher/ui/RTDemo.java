package edu.rit.krisher.ui;

import java.io.IOException;
import java.util.zip.ZipInputStream;

import javax.swing.SwingUtilities;

import edu.rit.krisher.fileparser.ply.PLYParser;
import edu.rit.krisher.scene.AxisAlignedBoundingBox;
import edu.rit.krisher.scene.Material;
import edu.rit.krisher.scene.Scene;
import edu.rit.krisher.scene.camera.DoFCamera;
import edu.rit.krisher.scene.camera.PinholeCamera;
import edu.rit.krisher.scene.geometry.Box;
import edu.rit.krisher.scene.geometry.Sphere;
import edu.rit.krisher.scene.geometry.TriangleMesh;
import edu.rit.krisher.scene.geometry.acceleration.KDTree;
import edu.rit.krisher.scene.geometry.acceleration.Partitionable;
import edu.rit.krisher.scene.light.SphereLight;
import edu.rit.krisher.scene.material.CheckerboardPattern;
import edu.rit.krisher.scene.material.Color;
import edu.rit.krisher.scene.material.CompositeBRDF;
import edu.rit.krisher.scene.material.LambertBRDF;
import edu.rit.krisher.scene.material.PhongSpecularBRDF;
import edu.rit.krisher.scene.material.RefractiveBRDF;
import edu.rit.krisher.scene.material.RingsPattern;
import edu.rit.krisher.vecmath.Quat;
import edu.rit.krisher.vecmath.Vec3;

public class RTDemo {

   private static final String bunnyResource = "/edu/rit/krisher/fileparser/ply/bun_zipper.ply.zip";
   private static final Color orange = new Color(1, 0.5f, 0.3f);
   private static final Color blue = new Color(0.25, 0.25, 1.0);
   private static final Color green = new Color(0.25, 1.0, 0.4);

   private static final RingsPattern yellowGreenSphereTexture = new RingsPattern(new Color(1, 0.8f, 0), new Color(0.1f, 1, 0.1f));
   private static final CheckerboardPattern yellowRedCheckerTexture = new CheckerboardPattern(new Color(1, 0.25, 0.35), new Color(0.75, 0.75, 0.35));

   private static final CheckerboardPattern checkerTexture = new CheckerboardPattern(new Color(0.85, 0.35, 0.35), new Color(0.85, 0.85, 0.35));

   private static Material whiteLambert = new LambertBRDF(Color.white);
   private static Material blueLambert = new LambertBRDF(blue);
   private static Material greenLambert = new LambertBRDF(green);
   private static Material redLambert = new LambertBRDF(new Color(1, 0.45, 0.5));

   private static Material whiteMirror = new PhongSpecularBRDF(Color.white, 100000);
   private static Material whiteShiny = new PhongSpecularBRDF(Color.white, 30);
   private static Material blueSpecular = new PhongSpecularBRDF(blue, 100);
   private static Material greenSpecular = new PhongSpecularBRDF(green, 100);

   public static final CompositeBRDF mixedWhiteMat = new CompositeBRDF(whiteLambert, 0.5, whiteMirror, 0.5);
   public static final CompositeBRDF mixedOrangeMat = new CompositeBRDF(new LambertBRDF(orange), 0.6, new PhongSpecularBRDF(Color.white, 75), 0.4);
   public static final CompositeBRDF whiteSpecular80 = new CompositeBRDF(whiteLambert, 0.2, new PhongSpecularBRDF(Color.white, 1000), 0.80);

   private static final RefractiveBRDF clearRefractive = new RefractiveBRDF(0.95, Color.black, 100000);
   private static final CompositeBRDF clearBlurRefractive = new CompositeBRDF();
   private static final RefractiveBRDF blueGreenRefractive = new RefractiveBRDF(1.4, new Color(0.75, 0.95, 0.95), 100000);
   private static final CompositeBRDF blueGreenMixedRefractive = new CompositeBRDF();
   private static final RefractiveBRDF blueRefractive = new RefractiveBRDF(1.4, new Color(0.55, 0.55, 0.75), 100000);
   private static final RefractiveBRDF redRefractive = new RefractiveBRDF(1.4, new Color(0.95, 0.65, 0.65), 100000);
   private static final CompositeBRDF mixedRefractive = new CompositeBRDF();
   static {
      mixedRefractive.addMaterial(0.05, whiteLambert);
      mixedRefractive.addMaterial(0.8, clearRefractive);
      mixedRefractive.addMaterial(0.01, new PhongSpecularBRDF(Color.white, 80));

      blueGreenMixedRefractive.addMaterial(0.05, blueLambert);
      blueGreenMixedRefractive.addMaterial(0.8, blueGreenRefractive);
      blueGreenMixedRefractive.addMaterial(0.01, new PhongSpecularBRDF(Color.white, 80));

      clearBlurRefractive.addMaterial(0.8, new RefractiveBRDF(2.15, Color.black, 90));
      clearBlurRefractive.addMaterial(0.2, whiteLambert);
   }

   private static final RefractiveBRDF specularClearRefractive = new RefractiveBRDF(0.95, Color.black, 100000);

   /*
    * Camera in a box, where walls are 50% emissive, 50% Lambert diffuse. This
    * should produce white for every pixel, +/- sampling noise.
    */
   private static SceneDescription diffuseTest1() {
      final DoFCamera cam = new DoFCamera();
      final Scene scene = new Scene();
      final CompositeBRDF boxMat = new CompositeBRDF();
      boxMat.addMaterial(0.5, whiteLambert);
      boxMat.addMaterial(0.5, Color.white);
      scene.add(new Box(5, 5, 5, boxMat, new Vec3(0, 0, 0), false));

      cam.setPosition(new Vec3(0, 0, 0));
      cam.setAperture(1 / 100.0);
      cam.setFocalDist(5);

      return new SceneDescription("Diffuse-Emissive Test", scene, cam);
   }

   /*
    * Emissive, indirectly sampled light source touching a Lambert-diffuse
    * sphere. The diffuse sphere should reflect 100% of the light where they
    * touch.
    */
   private static SceneDescription diffuseTest2() {
      final DoFCamera cam = new DoFCamera();
      final Scene scene = new Scene();
      scene.add(new Box(16, 2, 16, whiteLambert, new Vec3(0, -1, 0), false));
      scene.add(new Sphere(new Vec3(0, 1, 0), 1, whiteLambert));
      scene.add(new Sphere(new Vec3(0, 3, 0), 1, Color.white));

      cam.setPosition(new Vec3(0, 2, 8));
      cam.setAperture(1 / 100.0);
      cam.setFocalDist(8);
      return new SceneDescription("Diffuse Test", scene, cam);
   }

   /*
    * Emissive, directly sampled light source touching a Lamber-diffuse sphere.
    * Should produce results identical to diffuseTest2.
    */
   private static SceneDescription diffuseTest3() {
      final DoFCamera cam = new DoFCamera();
      final Scene scene = new Scene();
      scene.add(new Box(16, 2, 16, whiteLambert, new Vec3(0, -1, 0), false));
      scene.add(new Sphere(new Vec3(0, 1, 0), 1, whiteLambert));
      scene.add(new SphereLight(new Vec3(0, 3, 0), 1, Color.white));

      cam.setPosition(new Vec3(0, 2, 8));
      cam.setAperture(1 / 100.0);
      cam.setFocalDist(8);
      return new SceneDescription("Diffuse Test (DL)", scene, cam);
   }

   /*
    * Emissive, indirectly sampled light source touching a near-perfect specular
    * sphere. Light should be reflected 100% in mirror direction, 0% everywhere
    * else.
    */
   private static SceneDescription specularTest1() {
      final DoFCamera cam = new DoFCamera();
      final Scene scene = new Scene();
      scene.add(new Box(16, 2, 16, whiteLambert, new Vec3(0, -1, 0), false));
      scene.add(new Sphere(new Vec3(0, 1, 0), 1, whiteMirror));
      scene.add(new Sphere(new Vec3(0, 3, 0), 1, Color.white));

      cam.setPosition(new Vec3(0, 2, 8));
      cam.setAperture(1 / 100.0);
      cam.setFocalDist(8);
      return new SceneDescription("Specular Test", scene, cam);
   }

   /*
    * Emissive, directly sampled light source touching a near-perfect specular
    * sphere. Light should be reflected 100% in mirror direction, 0% everywhere
    * else.
    */
   private static SceneDescription specularTest2() {
      final DoFCamera cam = new DoFCamera();
      final Scene scene = new Scene();
      scene.add(new Box(16, 2, 16, whiteLambert, new Vec3(0, -1, 0), false));
      scene.add(new Sphere(new Vec3(0, 1, 0), 1, whiteMirror));
      scene.add(new SphereLight(new Vec3(0, 3, 0), 1, Color.white));

      cam.setPosition(new Vec3(0, 2, 8));
      cam.setAperture(1 / 100.0);
      cam.setFocalDist(8);
      return new SceneDescription("Specular Test (DL)", scene, cam);
   }

   /*
    * Emissive, indirectly sampled light source touching an imperfect specular
    * sphere. Should show blurry reflection of light source, with near 100%
    * transmittance in mirror direction.
    */
   private static SceneDescription specularTest3() {
      final DoFCamera cam = new DoFCamera();
      final Scene scene = new Scene();
      scene.add(new Box(16, 2, 16, whiteLambert, new Vec3(0, -1, 0), false));
      scene.add(new Sphere(new Vec3(0, 1, 0), 1, whiteShiny));
      scene.add(new SphereLight(new Vec3(0, 3, 0), 1, Color.white));

      cam.setPosition(new Vec3(0, 2, 8));
      cam.setAperture(1 / 100.0);
      cam.setFocalDist(8);
      return new SceneDescription("Shiny Specular Test", scene, cam);
   }

   private static SceneDescription threeBalls() {
      final DoFCamera cam = new DoFCamera();
      final Scene scene = new Scene();
      // root.add(new Box(16, 16, 16, whiteLambert, new Vec3(0, 8, 0)));
      scene.add(new Box(16, 2.5, 16, new LambertBRDF(yellowRedCheckerTexture), new Vec3(0, -1.25, 0), false));
      scene.add(new Sphere(new Vec3(-2, 1, 0), 1, blueLambert));
      scene.add(new Sphere(new Vec3(2, 1, 0), 1, new CompositeBRDF(greenLambert, 0.4, whiteShiny, 0.5)));
      scene.add(new Sphere(new Vec3(0, 1, 0), 1, whiteMirror));
      scene.add(new SphereLight(new Vec3(1, 6, 3), 1.0, new Color(1.0f, 1.0f, 1.0f), 1.0f));

      cam.setPosition(new Vec3(1, 4, 5));
      final Vec3 focusOn = new Vec3(-3, -1.5 + Math.sqrt(2), -1).subtract(cam.getPosition());
      cam.setFocalDist(-focusOn.dot(new Vec3(cam.getPosition()).normalize()));
      cam.setAperture(1 / 22.0);

      final Quat yaw = new Quat(Vec3.yAxis, Math.toRadians(15));
      final Quat pitch = new Quat(Vec3.xAxis, -Math.toRadians(25));
      yaw.multiplyBy(pitch);
      cam.setOrientation(yaw);
      cam.setFOVAngle(56.14);
      return new SceneDescription("Three Balls", scene, cam);

   }

   private static SceneDescription niceScene() {
      final DoFCamera cam = new DoFCamera();
      final Scene scene = new Scene();
      scene.add(new Box(16, 16, 16, whiteLambert, new Vec3(0, 8, 0), true));
      scene.add(new Sphere(new Vec3(-2, 1, 0), 1, blueGreenMixedRefractive));
      scene.add(new Sphere(new Vec3(0, 1, 0), 1, clearBlurRefractive));
      scene.add(new Sphere(new Vec3(-1, 1 + Math.sqrt(2), -1), 1, whiteMirror));
      scene.add(new Sphere(new Vec3(0, 1, -2), 1, blueLambert));
      scene.add(new Sphere(new Vec3(-2, 1, -2), 1, greenLambert));

      scene.add(new Box(1.99, 1.99, 1.99, new LambertBRDF(yellowRedCheckerTexture), new Vec3(-3.5, 0.995, 2.75), false));

      scene.add(new SphereLight(new Vec3(1, 6, 3), 1.0, new Color(1.0f, 1.0f, 1.0f), 2.0f));

      cam.setPosition(new Vec3(1.5, 4.5, 7));
      final Vec3 focusOn = new Vec3(-1, 1 + Math.sqrt(2), -1).subtract(cam.getPosition());
      cam.setFocalDist(-focusOn.dot(new Vec3(cam.getPosition()).normalize()));
      cam.setAperture(1 / 8.0);

      final Quat yaw = new Quat(Vec3.yAxis, Math.toRadians(33));
      final Quat pitch = new Quat(Vec3.xAxis, -Math.toRadians(28));
      yaw.multiplyBy(pitch);
      cam.setOrientation(yaw);
      cam.setFOVAngle(56.14);
      return new SceneDescription("Nice Scene", scene, cam);
   }

   private static SceneDescription checkpoint6() {
      final DoFCamera cam = new DoFCamera();
      final Scene scene = new Scene();
      scene.add(new Box(16, 16, 16, whiteLambert, new Vec3(0, 8, 0), true));
      // root.add(new Box(16, 2.5, 16, new CompositeBRDF(new
      // LambertBRDF(checkerTexture), 0.8, whiteShiny, 0.2), new Vec3(0, -1.25,
      // 0)));
      scene.add(new Sphere(new Vec3(1, 1 + Math.sqrt(2), -1), 1, blueRefractive));
      scene.add(new Sphere(new Vec3(2, 1, -2), 1, blueGreenRefractive));
      scene.add(new Sphere(new Vec3(0, 1, 0), 1, blueGreenRefractive));
      scene.add(new Sphere(new Vec3(2, 1, 0), 1, redRefractive));
      scene.add(new Sphere(new Vec3(0, 1, -2), 1, redRefractive));

      scene.add(new SphereLight(new Vec3(4, 6, -1), 0.5, new Color(1.0f, 1.0f, 1.0f), 15.0f));
      scene.add(new SphereLight(new Vec3(-4, 6, -1), 0.5, new Color(1.0f, 1.0f, 1.0f), 1.0f));
      scene.add(new SphereLight(new Vec3(1, 6, -4), 0.5, new Color(1.0f, 1.0f, 1.0f), 1.0f));

      cam.setPosition(new Vec3(1, 6, 5));
      final Vec3 focusOn = new Vec3(0, 2, 0).subtract(cam.getPosition());
      cam.setFocalDist(-focusOn.dot(new Vec3(cam.getPosition()).normalize()));
      cam.setAperture(1 / 22.0);

      // final Quat yaw = new Quat(Vec3.yAxis, Math.toRadians(15));
      final Quat pitch = new Quat(Vec3.xAxis, -Math.toRadians(45));
      // yaw.multiplyBy(pitch);
      cam.setOrientation(pitch);
      cam.setFOVAngle(56.14);
      return new SceneDescription("Checkpoint 6", scene, cam);
   }

   private static SceneDescription whittedScene(final double lightPower) {
      final DoFCamera cam = new DoFCamera();
      final Scene scene = new Scene();
      scene.add(new Box(10, 2.5, 16, new CompositeBRDF(new LambertBRDF(checkerTexture), 0.9, whiteShiny, 0.1), new Vec3(-2, -1.25, 0), false));

      scene.add(new Sphere(new Vec3(0, 2, 0), 1, mixedRefractive));

      scene.add(new Sphere(new Vec3(-2, 1, -2), 1, whiteSpecular80));

      scene.add(new SphereLight(new Vec3(3, 6, 3.5), 1.0, new Color(1.0f, 1.0f, 1.0f), lightPower));

      cam.setPosition(new Vec3(0, 2, 5));
      final Vec3 focusOn = new Vec3(0, 2, 0).subtract(cam.getPosition());
      cam.setFocalDist(-focusOn.dot(new Vec3(cam.getPosition()).normalize()));
      cam.setAperture(1 / 8.0);
      cam.setFOVAngle(56.14);
      scene.setBackground(new Color(0.25, 0.25, 0.65));
      return new SceneDescription("Whitted Scene (Light = " + lightPower + ")", scene, cam);

   }

   private static SceneDescription bunnyScene(final double lightPower) {
      final PinholeCamera cam = new PinholeCamera();
      final Scene scene = new Scene();
      scene.add(new Box(10, 1, 16, new CompositeBRDF(new LambertBRDF(checkerTexture), 0.9, whiteShiny, 0.1), new Vec3(-2, -0.5, 0), false));

      ZipInputStream stream = null;
      try {
         stream = new ZipInputStream(RTDemo.class.getResourceAsStream(bunnyResource));
         stream.getNextEntry();
         final TriangleMesh bunnyMesh = PLYParser.parseTriangleMesh(stream);
         scene.add(bunnyMesh);
         final AxisAlignedBoundingBox bounds = bunnyMesh.getBounds();
         cam.lookAt(bounds.center(), 35, 180, bounds.diagonalLength() * 0.8);

      } catch (final IOException ioe) {
         ioe.printStackTrace();
      } finally {
         try {
            stream.close();
         } catch (final IOException e) {
            e.printStackTrace();
         }
      }

      scene.add(new SphereLight(new Vec3(3, 6, 3.5), 1.0, new Color(1.0f, 1.0f, 1.0f), lightPower));

      cam.setFOVAngle(56.14);
      scene.setBackground(new Color(0.25, 0.25, 0.65));
      return new SceneDescription("Bunny Scene (Light = " + lightPower + ")", scene, cam);

   }

   private static SceneDescription bunnySceneKD(final double lightPower) {
      final PinholeCamera cam = new PinholeCamera();
      final Scene scene = new Scene();
      scene.add(new Box(10, 1, 16, new CompositeBRDF(new LambertBRDF(checkerTexture), 0.9, whiteShiny, 0.1), new Vec3(-2, -0.5, 0), false));

      ZipInputStream stream = null;
      try {
         stream = new ZipInputStream(RTDemo.class.getResourceAsStream(bunnyResource));
         stream.getNextEntry();
         final TriangleMesh bunnyMesh = PLYParser.parseTriangleMesh(stream);
         final KDTree accel = new KDTree(new Partitionable[] { bunnyMesh }, 10, 5);
         scene.add(accel);
         final AxisAlignedBoundingBox bounds = bunnyMesh.getBounds();
         cam.lookAt(bounds.center(), 35, 180, bounds.diagonalLength() * 0.8);

      } catch (final IOException ioe) {
         ioe.printStackTrace();
      } finally {
         try {
            stream.close();
         } catch (final IOException e) {
            e.printStackTrace();
         }
      }

      scene.add(new SphereLight(new Vec3(3, 6, 3.5), 1.0, new Color(1.0f, 1.0f, 1.0f), lightPower));

      cam.setFOVAngle(56.14);
      scene.setBackground(new Color(0.25, 0.25, 0.65));
      return new SceneDescription("Bunny Scene KD (Light = " + lightPower + ")", scene, cam);

   }

   private static SceneDescription dofScene() {
      final DoFCamera cam = new DoFCamera();
      final Scene scene = new Scene();
      scene.add(new Box(20, 20, 20, whiteLambert, new Vec3(0, 10, 0), true));

      scene.add(new Sphere(new Vec3(-2 * Math.sqrt(2), 1, 2 * Math.sqrt(2)), 1, redLambert));
      scene.add(new Sphere(new Vec3(-Math.sqrt(2), 1, Math.sqrt(2)), 1, blueGreenMixedRefractive));
      scene.add(new Sphere(new Vec3(0, 1, 0), 1, whiteSpecular80));
      scene.add(new Sphere(new Vec3(Math.sqrt(2), 1, -Math.sqrt(2)), 1, blueGreenMixedRefractive));
      scene.add(new Sphere(new Vec3(2 * Math.sqrt(2), 1, -2 * Math.sqrt(2)), 1, redLambert));

      scene.add(new SphereLight(new Vec3(3, 6, 3.5), 1.0, new Color(1.0f, 1.0f, 1.0f), 10));

      cam.setPosition(new Vec3(0, 1, 8));
      final Vec3 focusOn = new Vec3(0, 1, 0).subtract(cam.getPosition());
      cam.setFocalDist(-focusOn.dot(new Vec3(cam.getPosition()).normalize()));
      cam.setAperture(1 / 2.0);
      cam.setFOVAngle(56.14);
      return new SceneDescription("DoF Scene", scene, cam);

   }

   private static SceneDescription causticScene() {
      final DoFCamera cam = new DoFCamera();
      final Scene scene = new Scene();
      scene.add(new Box(20, 20, 20, whiteLambert, new Vec3(0, 10, 0), true));

      scene.add(new Sphere(new Vec3(0, 2, 0), 1, blueGreenMixedRefractive));
      scene.add(new Box(2, 2, 2, redRefractive, new Vec3(-2, 1, 2), false));
      scene.add(new SphereLight(new Vec3(3, 6, 3.5), 1.0, new Color(1.0f, 1.0f, 1.0f), 5));

      cam.lookAt(new Vec3(-1, 1, 1), 35, -25, 8);
      cam.setAperture(1 / 22.0);
      cam.setFOVAngle(56.14);
      return new SceneDescription("Caustics Scene", scene, cam);

   }

   private static SceneDescription multiLightScene() {
      final DoFCamera cam = new DoFCamera();
      final Scene scene = new Scene();
      scene.add(new Box(20, 20, 20, whiteLambert, new Vec3(0, 10, 0), true));
      scene.add(new Box(1.99, 1.99, 1.99, new LambertBRDF(yellowRedCheckerTexture), new Vec3(4, 1, 2), false));

      scene.add(new Sphere(new Vec3(0, 1, 0), 1, whiteShiny));
      scene.add(new Sphere(new Vec3(-1, 1, Math.sqrt(3)), 1, mixedOrangeMat));
      scene.add(new Sphere(new Vec3(1, 1, Math.sqrt(3)), 1, new LambertBRDF(new Color(0.25, 0.25, 1.0))));
      scene.add(new SphereLight(new Vec3(-3, 6, 3.5), 1.0, new Color(1.0f, 1.0f, 0.55f), 3));
      scene.add(new SphereLight(new Vec3(3, 6, 0), 1.0, new Color(0.55f, 0.55f, 1.0f), 3));
      scene.add(new SphereLight(new Vec3(1, 7, -3.5), 0.25, new Color(1.0f, 1.0f, 1.0f), 40));

      cam.lookAt(new Vec3(0, 1, 0), 45, -20, 8);
      cam.setAperture(1 / 22.0);
      cam.setFOVAngle(56.14);
      return new SceneDescription("Multi Light Scene", scene, cam);

   }

   public static void main(final String[] args) {
      final RTFrame frame = new RTFrame();

      frame.setScenes(new SceneDescription[] { bunnyScene(75), bunnySceneKD(75), whittedScene(75), whittedScene(150),
            whittedScene(500),
            dofScene(), causticScene(), multiLightScene(), diffuseTest1(), diffuseTest2(), diffuseTest3(),
            specularTest1(), specularTest2(), specularTest3(),

            // threeBalls(), niceScene(), checkpoint6(), projectCP(),
      });

      SwingUtilities.invokeLater(new Runnable() {
         @Override
         public void run() {

            frame.setVisible(true);
         }
      });
   }
}
