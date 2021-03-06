package edu.rit.krisher.ui.scenes;

import edu.rit.krisher.scene.DefaultScene;
import edu.rit.krisher.scene.Material;
import edu.rit.krisher.scene.Scene;
import edu.rit.krisher.scene.acceleration.KDGeometryContainer;
import edu.rit.krisher.scene.camera.DoFCamera;
import edu.rit.krisher.scene.geometry.Box;
import edu.rit.krisher.scene.geometry.Sphere;
import edu.rit.krisher.scene.light.SphereLight;
import edu.rit.krisher.scene.material.CheckerboardPattern;
import edu.rit.krisher.scene.material.Color;
import edu.rit.krisher.scene.material.CompositeMaterial;
import edu.rit.krisher.scene.material.DiffuseMaterial;
import edu.rit.krisher.scene.material.SpecularMaterial;
import edu.rit.krisher.scene.material.RefractiveMaterial;
import edu.rit.krisher.scene.material.RingsPattern;
import edu.rit.krisher.vecmath.Quat;
import edu.rit.krisher.vecmath.Vec3;

public class CG2Scenes {

   static final Color orange = new Color(1, 0.5f, 0.3f);
   static final Color blue = new Color(0.25, 0.25, 1.0);
   static final Color green = new Color(0.25, 1.0, 0.4);

   static final RingsPattern yellowGreenSphereTexture = new RingsPattern(new Color(1, 0.8f, 0), new Color(0.1f, 1, 0.1f));
   static final CheckerboardPattern yellowRedCheckerTexture = new CheckerboardPattern(new Color(1, 0.25, 0.35), new Color(0.75, 0.75, 0.35));

   static final CheckerboardPattern checkerTexture = new CheckerboardPattern(new Color(0.85, 0.35, 0.35), new Color(0.85, 0.85, 0.35));

   static Material whiteLambert = new DiffuseMaterial(Color.white);
   static Material blueLambert = new DiffuseMaterial(blue);
   static Material greenLambert = new DiffuseMaterial(green);
   static Material redLambert = new DiffuseMaterial(new Color(1, 0.45, 0.5));

   static Material whiteMirror = new SpecularMaterial(Color.white, 100000);
   static Material whiteShiny = new SpecularMaterial(Color.white, 30);
   static Material blueSpecular = new SpecularMaterial(blue, 100);
   static Material greenSpecular = new SpecularMaterial(green, 100);

   static final CompositeMaterial mixedWhiteMat = new CompositeMaterial(whiteLambert, 0.5, whiteMirror, 0.5);
   static final CompositeMaterial mixedOrangeMat = new CompositeMaterial(new DiffuseMaterial(orange), 0.6, new SpecularMaterial(Color.white, 75), 0.4);
   static final CompositeMaterial whiteSpecular80 = new CompositeMaterial(whiteLambert, 0.2, new SpecularMaterial(Color.white, 1000), 0.80);

   static final RefractiveMaterial clearRefractive = new RefractiveMaterial(0.95, Color.black, 100000);
   static final CompositeMaterial clearBlurRefractive = new CompositeMaterial();
   static final RefractiveMaterial blueGreenRefractive = new RefractiveMaterial(1.4, new Color(0.75, 0.95, 0.95), 100000);
   static final CompositeMaterial blueGreenMixedRefractive = new CompositeMaterial();
   static final RefractiveMaterial blueRefractive = new RefractiveMaterial(1.4, new Color(0.55, 0.55, 0.75), 100000);
   static final RefractiveMaterial redRefractive = new RefractiveMaterial(1.4, new Color(0.95, 0.65, 0.65), 100000);
   static final CompositeMaterial mixedRefractive = new CompositeMaterial();
   static {
      mixedRefractive.addMaterial(0.05, whiteLambert);
      mixedRefractive.addMaterial(0.8, clearRefractive);
      mixedRefractive.addMaterial(0.01, new SpecularMaterial(Color.white, 80));

      blueGreenMixedRefractive.addMaterial(0.05, blueLambert);
      blueGreenMixedRefractive.addMaterial(0.8, blueGreenRefractive);
      blueGreenMixedRefractive.addMaterial(0.01, new SpecularMaterial(Color.white, 80));

      clearBlurRefractive.addMaterial(0.8, new RefractiveMaterial(2.15, Color.black, 90));
      clearBlurRefractive.addMaterial(0.2, whiteLambert);
   }

   static final RefractiveMaterial specularClearRefractive = new RefractiveMaterial(0.95, Color.black, 100000);

   /*
    * Camera in a box, where walls are 50% emissive, 50% Lambert diffuse. This should produce white for every pixel, +/-
    * sampling noise.
    */
   private static final Scene diffuseTest1 = new AbstractSceneDescription<DoFCamera>("Diffuse-Emissive Test", new DoFCamera()) {

      @Override
      protected void initScene() {
         final CompositeMaterial boxMat = new CompositeMaterial(whiteLambert, 0.5, Color.white, 0.5);
         add(new Box(5, 5, 5, boxMat, new Vec3(0, 0, 0), false));
         camera.setPosition(new Vec3(0, 0, 0));
         camera.setAperture(1 / 100.0);
         camera.setFocalDist(5);
      }
   };

   /*
    * Emissive, indirectly sampled light source touching a Lambert-diffuse sphere. The diffuse sphere should reflect
    * 100% of the light where they touch.
    */
   private static Scene diffuseTest2 = new AbstractSceneDescription<DoFCamera>("Diffuse Test", new DoFCamera()) {
      @Override
      protected void initScene() {
         add(new Box(16, 2, 16, whiteLambert, new Vec3(0, -1, 0), false));
         add(new Sphere(new Vec3(0, 1, 0), 1, whiteLambert));
         add(new Sphere(new Vec3(0, 3, 0), 1, Color.white));

         camera.setPosition(new Vec3(0, 2, 8));
         camera.setAperture(1 / 100.0);
         camera.setFocalDist(8);
      }
   };

   /*
    * Emissive, directly sampled light source touching a Lamber-diffuse sphere. Should produce results identical to
    * diffuseTest2.
    */
   private static Scene diffuseTest3 = new AbstractSceneDescription<DoFCamera>("Diffuse Test (DL)", new DoFCamera()) {
      @Override
      protected void initScene() {
         add(new Box(16, 2, 16, whiteLambert, new Vec3(0, -1, 0), false));
         add(new Sphere(new Vec3(0, 1, 0), 1, whiteLambert));
         add(new SphereLight(new Vec3(0, 3, 0), 1, Color.white));

         camera.setPosition(new Vec3(0, 2, 8));
         camera.setAperture(1 / 100.0);
         camera.setFocalDist(8);
      }
   };

   /*
    * Emissive, indirectly sampled light source touching a near-perfect specular sphere. Light should be reflected 100%
    * in mirror direction, 0% everywhere else.
    */
   private static Scene specularTest1 = new AbstractSceneDescription<DoFCamera>("Specular Test", new DoFCamera()) {
      @Override
      protected void initScene() {
         add(new Box(16, 2, 16, whiteLambert, new Vec3(0, -1, 0), false));
         add(new Sphere(new Vec3(0, 1, 0), 1, whiteMirror));
         add(new Sphere(new Vec3(0, 3, 0), 1, Color.white));

         camera.setPosition(new Vec3(0, 2, 8));
         camera.setAperture(1 / 100.0);
         camera.setFocalDist(8);
      }
   };

   /*
    * Emissive, directly sampled light source touching a near-perfect specular sphere. Light should be reflected 100% in
    * mirror direction, 0% everywhere else.
    */
   private static Scene specularTest2() {
      final DoFCamera cam = new DoFCamera();
      final AbstractSceneDescription<DoFCamera> scene = new AbstractSceneDescription<DoFCamera>("Specular Test (DL)", cam);
      scene.add(new Box(16, 2, 16, whiteLambert, new Vec3(0, -1, 0), false));
      scene.add(new Sphere(new Vec3(0, 1, 0), 1, whiteMirror));
      scene.add(new SphereLight(new Vec3(0, 3, 0), 1, Color.white));

      cam.setPosition(new Vec3(0, 2, 8));
      cam.setAperture(1 / 100.0);
      cam.setFocalDist(8);
      return scene;
   }

   /*
    * Emissive, indirectly sampled light source touching an imperfect specular sphere. Should show blurry reflection of
    * light source, with near 100% transmittance in mirror direction.
    */
   private static Scene specularTest3() {
      final DoFCamera cam = new DoFCamera();
      final DefaultScene scene = new DefaultScene("Shiny Specular Test", cam);
      scene.add(new Box(16, 2, 16, whiteLambert, new Vec3(0, -1, 0), false));
      scene.add(new Sphere(new Vec3(0, 1, 0), 1, whiteShiny));
      scene.add(new SphereLight(new Vec3(0, 3, 0), 1, Color.white));

      cam.setPosition(new Vec3(0, 2, 8));
      cam.setAperture(1 / 100.0);
      cam.setFocalDist(8);
      return scene;
   }

   private static Scene threeBalls() {
      final DoFCamera cam = new DoFCamera();
      final DefaultScene scene = new DefaultScene("Three Balls", cam);
      // root.add(new Box(16, 16, 16, whiteLambert, new Vec3(0, 8, 0)));
      scene.add(new Box(16, 2.5, 16, new DiffuseMaterial(yellowRedCheckerTexture), new Vec3(0, -1.25, 0), false));
      scene.add(new Sphere(new Vec3(-2, 1, 0), 1, blueLambert));
      scene.add(new Sphere(new Vec3(2, 1, 0), 1, new CompositeMaterial(greenLambert, 0.4, whiteShiny, 0.5)));
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
      return scene;

   }

   private static Scene niceScene() {
      final DoFCamera cam = new DoFCamera();
      final AbstractSceneDescription scene = new AbstractSceneDescription("Nice Scene", cam);
      scene.add(new Box(16, 16, 16, whiteLambert, new Vec3(0, 8, 0), true));
      scene.add(new Sphere(new Vec3(-2, 1, 0), 1, blueGreenMixedRefractive));
      scene.add(new Sphere(new Vec3(0, 1, 0), 1, clearBlurRefractive));
      scene.add(new Sphere(new Vec3(-1, 1 + Math.sqrt(2), -1), 1, whiteMirror));
      scene.add(new Sphere(new Vec3(0, 1, -2), 1, blueLambert));
      scene.add(new Sphere(new Vec3(-2, 1, -2), 1, greenLambert));

      scene.add(new Box(1.99, 1.99, 1.99, new DiffuseMaterial(yellowRedCheckerTexture), new Vec3(-3.5, 0.995, 2.75), false));

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
      return scene;
   }

   private static Scene checkpoint6() {
      final DoFCamera cam = new DoFCamera();
      final AbstractSceneDescription scene = new AbstractSceneDescription("Checkpoint 6", cam);
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
      return scene;
   }

   private static Scene whittedScene(final double lightPower) {
      final DoFCamera cam = new DoFCamera();
      final AbstractSceneDescription scene = new AbstractSceneDescription("Whitted Scene (Light = " + lightPower + ")", cam);
      
      final KDGeometryContainer tree = new KDGeometryContainer(
                                     new Sphere(new Vec3(0, 2, 0), 1, mixedRefractive),
                                     new Sphere(new Vec3(-2, 1, -2), 1, whiteSpecular80), new Box(10, 2.5, 16, new CompositeMaterial(new DiffuseMaterial(checkerTexture), 0.9, whiteShiny, 0.1), new Vec3(-2, -1.25, 0), false));
      scene.add(tree);
      scene.add(new SphereLight(new Vec3(3, 6, 3.5), 1.0, new Color(1.0f, 1.0f, 1.0f), lightPower));

      cam.setPosition(new Vec3(0, 2, 5));
      final Vec3 focusOn = new Vec3(0, 2, 0).subtract(cam.getPosition());
      cam.setFocalDist(-focusOn.dot(new Vec3(cam.getPosition()).normalize()));
      cam.setAperture(1 / 8.0);
      cam.setFOVAngle(56.14);
      scene.setBackground(new Color(0.25, 0.25, 0.65));
      return scene;

   }

   private static Scene dofScene = new AbstractSceneDescription<DoFCamera>("DoF Scene", new DoFCamera()) {
      @Override
      protected void initScene() {
         add(new Box(20, 20, 20, whiteLambert, new Vec3(0, 10, 0), true));

         add(new Sphere(new Vec3(-2 * Math.sqrt(2), 1, 2 * Math.sqrt(2)), 1, redLambert));
         add(new Sphere(new Vec3(-Math.sqrt(2), 1, Math.sqrt(2)), 1, blueGreenMixedRefractive));
         add(new Sphere(new Vec3(0, 1, 0), 1, whiteSpecular80));
         add(new Sphere(new Vec3(Math.sqrt(2), 1, -Math.sqrt(2)), 1, blueGreenMixedRefractive));
         add(new Sphere(new Vec3(2 * Math.sqrt(2), 1, -2 * Math.sqrt(2)), 1, redLambert));

         add(new SphereLight(new Vec3(3, 6, 3.5), 1.0, new Color(1.0f, 1.0f, 1.0f), 10));

         camera.setPosition(new Vec3(0, 1, 8));
         final Vec3 focusOn = new Vec3(0, 1, 0).subtract(camera.getPosition());
         camera.setFocalDist(-focusOn.dot(new Vec3(camera.getPosition()).normalize()));
         camera.setAperture(1 / 2.0);
         camera.setFOVAngle(56.14);

      }
   };

   private static Scene causticScene = new AbstractSceneDescription<DoFCamera>("Caustics Scene", new DoFCamera()) {
      @Override
      protected void initScene() {
         add(new Box(20, 20, 20, whiteLambert, new Vec3(0, 10, 0), true));

         add(new Sphere(new Vec3(0, 2, 0), 1, blueGreenMixedRefractive));
         add(new Box(2, 2, 2, redRefractive, new Vec3(-2, 1, 2), false));
         add(new SphereLight(new Vec3(3, 6, 3.5), 1.0, new Color(1.0f, 1.0f, 1.0f), 5));

         camera.lookAt(new Vec3(-1, 1, 1), 35, -25, 8);
         camera.setAperture(1 / 22.0);
         camera.setFOVAngle(56.14);
      }
   };

   private static Scene multiLightScene = new AbstractSceneDescription<DoFCamera>("Multi Light Scene", new DoFCamera()) {
      @Override
      protected void initScene() {

         add(new Box(20, 20, 20, whiteLambert, new Vec3(0, 10, 0), true));
         add(new Box(1.99, 1.99, 1.99, new DiffuseMaterial(yellowRedCheckerTexture), new Vec3(4, 1, 2), false));

         add(new Sphere(new Vec3(0, 1, 0), 1, whiteShiny));
         add(new Sphere(new Vec3(-1, 1, Math.sqrt(3)), 1, mixedOrangeMat));
         add(new Sphere(new Vec3(1, 1, Math.sqrt(3)), 1, new DiffuseMaterial(new Color(0.25, 0.25, 1.0))));
         add(new SphereLight(new Vec3(-3, 6, 3.5), 1.0, new Color(1.0f, 1.0f, 0.55f), 3));
         add(new SphereLight(new Vec3(3, 6, 0), 1.0, new Color(0.55f, 0.55f, 1.0f), 3));
         add(new SphereLight(new Vec3(1, 7, -3.5), 0.25, new Color(1.0f, 1.0f, 1.0f), 40));

         camera.lookAt(new Vec3(0, 1, 0), 45, -20, 8);
         camera.setAperture(1 / 22.0);
         camera.setFOVAngle(56.14);
      }
   };

   public static Scene[] getScenes() {
      return new Scene[] { whittedScene(75), whittedScene(150), whittedScene(500), dofScene, causticScene,
            multiLightScene, diffuseTest1, diffuseTest2, diffuseTest3, specularTest1, specularTest2(), specularTest3(),
            // threeBalls(), niceScene(), checkpoint6(), projectCP(),
      };
   }
}
