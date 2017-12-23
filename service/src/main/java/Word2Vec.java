import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Map;

public class Word2Vec {

  private final static long ONE_GB = 1024 * 1024 * 1024;

  // Heavily inspired by:
  // https://github.com/medallia/Word2VecJava/blob/85e8ce5715275a2c4b5440f1d62346aa6dcea52e/src/main/java/com/medallia/word2vec/Word2VecModel.java#L120
  public static Map<String, double[]> fromBin(File file) throws IOException {
    if (file.length() > (2 * ONE_GB)) {
      throw new IllegalArgumentException("Model cannot be larger than 2GB");
    }
    final ImmutableMap.Builder<String, double[]> space = ImmutableMap.builder();
    final FileInputStream fis = new FileInputStream(file);
    final FileChannel channel = fis.getChannel();
    MappedByteBuffer buffer =
      channel.map(FileChannel.MapMode.READ_ONLY, 0, Math.min(channel.size(),
            file.length()));
    buffer.order(ByteOrder.LITTLE_ENDIAN);

    StringBuilder sb = new StringBuilder();
    char c = (char) buffer.get();
    while (c != '\n') {
      sb.append(c);
      c = (char) buffer.get();
    }
    String firstLine = sb.toString();
    int index = firstLine.indexOf(' ');
    Preconditions.checkState(index != -1,
        "Expected a space in the first line of file '%s'", firstLine);

    final int vocabSize = Integer.parseInt(firstLine.substring(0, index));
    final int layerSize = Integer.parseInt(firstLine.substring(index + 1));

    for (int i = 0; i < vocabSize; ++i) {
      // Read vocab
      sb.setLength(0);
      c = (char) buffer.get();
      while (c != ' ') {
        // Ignore newlines in front of words (some binary files have newline,
        // some don't)
        if (c != '\n') {
          sb.append(c);
        }
        c = (char) buffer.get();
      }

      // Read vector
      final FloatBuffer floatBuffer = buffer.asFloatBuffer();
      final float[] floats = new float[layerSize];
      floatBuffer.get(floats);
      // We need to convert to doubles because the floats don't have enough
      // precision when we multiply some of them as part of calculating cosine
      // similarity
      final double[] doubles = new double[layerSize];
      for (int j = 0; j < floats.length; ++j) {
        doubles[j] = floats[j];
      }

      space.put(sb.toString(), doubles);

      // Advance the pointer to go past all the floats
      buffer.position(buffer.position() + 4 * layerSize);
    }
    return space.build();
  }

}
