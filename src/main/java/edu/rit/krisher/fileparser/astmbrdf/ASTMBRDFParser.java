/**
 * 
 */
package edu.rit.krisher.fileparser.astmbrdf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import edu.rit.krisher.scene.material.MeasuredIsotropicMaterial;

/**
 * Parser for the ASTM RGB BRDF format as used by <a
 * href="http://www.graphics.cornell.edu/online/measurements/reflectance/"
 * >http://www.graphics.cornell.edu/online/measurements/reflectance/</a>.
 * 
 * <p>
 * This is not a complete ASTM parser, it works with the Cornell data, and has not been tested with or even designed to
 * handle anything else.
 */
public class ASTMBRDFParser {

   public static MeasuredIsotropicMaterial parseRGBBRDF(final File file) throws IOException {
      return parseRGBBRDF(new FileInputStream(file));
   }

   public static MeasuredIsotropicMaterial parseRGBBRDF(final InputStream stream) throws IOException {
      final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

      String line;
      int numPoints = 0;
      /*
       * Read header section until "VARS" declaration line
       */
      while ((line = reader.readLine()) != null) {
         if (line.startsWith("NUM_POINTS")) {
            numPoints = Integer.parseInt(line.substring("NUM_POINTS".length() + 1));
         } else if (line.startsWith("VARS")) {
            line = line.substring("VARS".length() + 1);
            /*
             * Can only parse files with specific vars section...
             */
            if (!"theta_i,phi_i,theta_s,phi_s,R,G,B".equals(line)) {
               System.err.println("Possibly unsupported VARS section in ASTM BRDF data.");
            }
            break;
         }
      }
      if (numPoints <= 0) {
         throw new IllegalStateException("Expected NUM_POINTS declaration before VARS declaration.");
      }

      final float[] thetaIThetaSDeltaPhi = new float[numPoints * 3];
      final float[] rgb = new float[numPoints * 3];

      int sampleOffs = 0;
      while ((line = reader.readLine()) != null) {
         if (line.length() == 0)
            continue;
         final String[] vars = line.split(",");

         thetaIThetaSDeltaPhi[sampleOffs] = Float.parseFloat(vars[0]);
         thetaIThetaSDeltaPhi[sampleOffs + 1] = Float.parseFloat(vars[2]);
         thetaIThetaSDeltaPhi[sampleOffs + 2] = Float.parseFloat(vars[3]) - Float.parseFloat(vars[1]);

         rgb[sampleOffs] = Float.parseFloat(vars[4]);
         rgb[sampleOffs + 1] = Float.parseFloat(vars[5]);
         rgb[sampleOffs + 2] = Float.parseFloat(vars[6]);
         
         if (rgb[sampleOffs] < 0) rgb[sampleOffs] = 0;
         if (rgb[sampleOffs+1] < 0) rgb[sampleOffs+1] = 0;
         if (rgb[sampleOffs+2] < 0) rgb[sampleOffs+2] = 0;

         sampleOffs += 3;
      }

      return new MeasuredIsotropicMaterial(thetaIThetaSDeltaPhi, rgb);
   }
   
   public static MeasuredIsotropicMaterial getKrylonBlue() {
      try {
         return ASTMBRDFParser.parseRGBBRDF(ASTMBRDFParser.class.getResourceAsStream("krylon_blue_RGB.astm"));
      } catch (final IOException e) {
         e.printStackTrace();
         return null;
      }
   }
   //
   // public static void main(final String[] args) {
   // try {
   // parseRGBBRDF(new File("/home/krisher/Downloads/mystique_RGB.astm"));
   // } catch (final IOException e) {
   // e.printStackTrace();
   // }
   // }
}
