package edu.rit.krisher.ui.scenes;

import edu.rit.krisher.scene.Camera;
import edu.rit.krisher.scene.DefaultScene;
import edu.rit.krisher.scene.EmissiveGeometry;
import edu.rit.krisher.scene.Geometry;
import edu.rit.krisher.scene.Material;
import edu.rit.krisher.scene.Scene;
import edu.rit.krisher.scene.geometry.TriangleMesh;
import edu.rit.krisher.scene.material.Color;
import edu.rit.krisher.vecmath.AxisAlignedBoundingBox;

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

   private final void checkInit() {
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
}
