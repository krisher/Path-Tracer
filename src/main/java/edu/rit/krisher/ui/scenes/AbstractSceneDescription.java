package edu.rit.krisher.ui.scenes;

import edu.rit.krisher.scene.Camera;
import edu.rit.krisher.scene.DefaultScene;
import edu.rit.krisher.scene.EmissiveGeometry;
import edu.rit.krisher.scene.Geometry;
import edu.rit.krisher.scene.Material;
import edu.rit.krisher.scene.Scene;
import edu.rit.krisher.scene.geometry.TriangleMesh;
import edu.rit.krisher.scene.material.Color;
import edu.rit.krisher.scene.material.DiffuseMaterial;
import edu.rit.krisher.vecmath.AxisAlignedBoundingBox;
import edu.rit.krisher.vecmath.Vec3;

class AbstractSceneDescription<C extends Camera> extends DefaultScene<C> implements Scene {

   boolean init = false;

   public AbstractSceneDescription(final String name, final C camera) {
      super(name, camera);
   }

   @Override
   public C getCamera() {
      checkInit();
      return super.getCamera();
   }

   @Override
   public Geometry[] getGeometry() {
      checkInit();
      return super.getGeometry();
   }

   private synchronized final void checkInit() {
      if (!init) {
         initScene();
         init = true;
      }
   }

   @Override
   public EmissiveGeometry[] getLightSources() {
      checkInit();
      return super.getLightSources();
   }

   @Override
   public Color getBackground() {
      checkInit();
      return super.getBackground();
   }

   protected void initScene() {

   }

   @Override
   public String toString() {
      return getName();
   }

   private static final int[] floorBuff = {0,1,2,0,2,3};
   private static final int[] floorAndWallsBuff = { 0, 1, 2, 0, 2, 3, 4, 5, 1, 4, 1, 0, 5, 6, 2, 5, 2, 1, 6, 7, 3,
      6,
      3, 2, 7, 4, 0, 7, 0, 3 /* , 4, 7, 6, 4, 6, 5 */};
   public static TriangleMesh groundPlane(final Material mat, final boolean walls,
         final AxisAlignedBoundingBox sceneBounds) {
      final double xBorder = sceneBounds.xSpan() * 4;
      final double zBorder = sceneBounds.zSpan() * 4;
      final double yBorder = sceneBounds.ySpan() * 2;

      final AxisAlignedBoundingBox expandedAABB = new AxisAlignedBoundingBox(sceneBounds);
      expandedAABB.xyzxyz[0] -= xBorder;
      expandedAABB.xyzxyz[2] -= zBorder;
      expandedAABB.xyzxyz[3] += xBorder;
      expandedAABB.xyzxyz[5] += zBorder;
      expandedAABB.xyzxyz[4] += yBorder;


      final TriangleMesh mesh = new TriangleMesh(expandedAABB.toVertexArrayF(), walls?floorAndWallsBuff:floorBuff);
      if (mat != null) {
         mesh.setMaterial(mat);
      }
      return mesh;
   }

   private static final int[] whiteSections = { 0, 1, 2, 0, 2, 3, // -y

      4, 5, 1, 4, 1, 0, // -z

      4, 7, 6, 4, 6, 5 // +y
   };

   private static final int[] redSection = { 5, 6, 2, 5, 2, 1 };
   private static final int[] greenSection = { 7, 4, 0, 7, 0, 3 };

   public static TriangleMesh[] cornellBox(final AxisAlignedBoundingBox sceneBounds, final Material m1,
         final Material m2, final Material m3) {
      final double paddingDist = Math.max(sceneBounds.xSpan(), Math.max(sceneBounds.zSpan(), sceneBounds.ySpan()));
      final Vec3 center = sceneBounds.centerPt();

      final AxisAlignedBoundingBox expandedAABB = new AxisAlignedBoundingBox(sceneBounds);
      expandedAABB.xyzxyz[0] = center.x - paddingDist;
      expandedAABB.xyzxyz[2] = center.z - paddingDist;
      expandedAABB.xyzxyz[3] = center.x + paddingDist;
      expandedAABB.xyzxyz[5] = center.z + paddingDist;
      expandedAABB.xyzxyz[4] = expandedAABB.xyzxyz[1] + 2.0 * paddingDist;

      final TriangleMesh[] geom = new TriangleMesh[] { new TriangleMesh(expandedAABB.toVertexArrayF(), whiteSections),
            new TriangleMesh(expandedAABB.toVertexArrayF(), redSection),
            new TriangleMesh(expandedAABB.toVertexArrayF(), greenSection) };
      geom[0].setMaterial(m1 == null ? new DiffuseMaterial(new Color(0.73)) : m1);
      geom[1].setMaterial(m2 == null ? new DiffuseMaterial(new Color(0.61, 0.06, 0.06)) : m2);
      geom[2].setMaterial(m3 == null ? new DiffuseMaterial(new Color(0.12, 0.47, 0.1)) : m3);
      return geom;
   }
}
