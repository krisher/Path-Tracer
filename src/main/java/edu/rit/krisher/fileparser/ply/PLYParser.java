package edu.rit.krisher.fileparser.ply;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import edu.rit.krisher.scene.geometry.buffer.Vec3Buffer;

/**
 * Simple parser implementation for the Stanford PLY model format. This is based on the format description
 * from: <a
 * href="http://people.cs.kuleuven.be/~ares.lagae/libply/ply-0.1/doc/PLY_FILES.txt">http://people.cs.kuleuven.be
 * /~ares.lagae/libply/ply-0.1/doc/PLY_FILES.txt</a>.
 * <p>
 * This is quick and dirty in its present form, this may only be able to load a limited subset of PLY models, in
 * particular, it is known to work with the Stanford Bunny model.
 * 
 * @author krisher
 * 
 */
public final class PLYParser {

   static void parsePLY(final InputStream stream, final Vec3Buffer vertexBuffer) throws IOException {
      final BufferedReader reader = new BufferedReader(new InputStreamReader(stream, Charset.forName("US-ASCII")));
      try {
         final PLYContentDescription content = new PLYContentDescription(reader);
         // TODO: find the element section that specifies vertex data and parse it into the provided buffer.
         // TODO: find the element section that specifies face data and parse it into the (un)provided index buffer.
      } finally {
         reader.close();
      }
   }
}
