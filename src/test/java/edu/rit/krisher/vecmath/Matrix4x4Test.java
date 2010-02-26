package edu.rit.krisher.vecmath;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class Matrix4x4Test {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    final Vec3 yAxis = new Vec3(0, 1, 0);
    final Vec3 xAxis = new Vec3(1, 0, 0);

    @Test
    public void testTransform() {
        final Matrix4x4 rotX = Matrix4x4.rotationXMatrix(Math.toRadians(90.0));
        assertVecEquals(new Vec3(0, 0, 1), rotX.transformPoint(new Vec3(yAxis)));

        /*
         * Translation should be applied...
         */
        final Matrix4x4 transRot = Matrix4x4.translationMatrix(0, 10, 0);
        final Matrix4x4 rotThenTrans = transRot.mul(rotX);
        assertVecEquals(new Vec3(0, 10, 1), rotThenTrans.transformPoint(new Vec3(
                yAxis)));

        final Matrix4x4 transThenRot = rotX.mul(transRot);
        assertVecEquals(new Vec3(0, 0, 11), transThenRot.transformPoint(new Vec3(
                yAxis)));

        final Matrix4x4 rotY = Matrix4x4.rotationYMatrix(Math.toRadians(90.0));
        assertVecEquals(new Vec3(0, 0, -1), rotY.transformPoint(new Vec3(xAxis)));
    }

    @Test
    public void testRotate() {
        final Matrix4x4 rotX = Matrix4x4.rotationXMatrix(Math.toRadians(90.0));
        final Vec3 yAxis = new Vec3(0, 1, 0);
        assertVecEquals(new Vec3(0, 0, 1), rotX.transformVec(new Vec3(yAxis)));

        /*
         * Translation should not be applied...
         */
        final Matrix4x4 transRot = Matrix4x4.translationMatrix(10, 10, 10);
        transRot.mulBy(rotX);
        assertVecEquals(new Vec3(0, 0, 1), transRot.transformVec(new Vec3(yAxis)));
    }

    @Test
    public void testInverse() {
        final Matrix4x4 rotX = Matrix4x4.rotationXMatrix(Math.toRadians(45));
        final Matrix4x4 inverse = rotX.inverted();

        assertMatrixEquals(Matrix4x4.rotationXMatrix(Math.toRadians(-45)),
                inverse);
        assertMatrixEquals(new Matrix4x4(), rotX.mul(inverse));

        final Matrix4x4 translate = Matrix4x4.translationMatrix(10, -2, 1);
        rotX.mulBy(translate);
        assertMatrixEquals(new Matrix4x4(), rotX.mul(rotX.inverted()));
    }

    private static void assertMatrixEquals(Matrix4x4 expected, Matrix4x4 actual) {
        for (int i = 0; i < 16; i++) {
            assertEquals(expected.matrix[i], actual.matrix[i], 0.0000000001);
        }
    }

    private static void assertVecEquals(Vec3 expected, Vec3 actual) {
        assertEquals("Bad X", expected.x, actual.x, 0.0000000001);
        assertEquals("Bad Y", expected.y, actual.y, 0.0000000001);
        assertEquals("Bad Z", expected.z, actual.z, 0.0000000001);
    }

}
