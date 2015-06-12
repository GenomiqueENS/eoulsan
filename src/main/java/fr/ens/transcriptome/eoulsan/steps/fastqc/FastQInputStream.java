package fr.ens.transcriptome.eoulsan.steps.fastqc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import uk.ac.babraham.FastQC.FastQCConfig;
import uk.ac.babraham.FastQC.Sequence.FastQFile;
import uk.ac.babraham.FastQC.Sequence.SequenceFormatException;
import fr.ens.transcriptome.eoulsan.EoulsanException;

public class FastQInputStream extends FastQFile {

  @SuppressWarnings("unused")
  private long fileSize;
  @SuppressWarnings("unused")
  private String name;
  @SuppressWarnings("unused")
  private boolean casavaMode;
  @SuppressWarnings("unused")
  private boolean nofilter;

  FastQInputStream(final InputStream is, final String filename)
      throws SequenceFormatException, IOException, EoulsanException {

    super(null, null);

    fileSize = Long.MAX_VALUE;

    name = filename;

    if (FastQCConfig.getInstance().casava) {
      casavaMode = true;
      if (FastQCConfig.getInstance().nofilter) {
        nofilter = true;
      }
    }

    // readNext();
    // Call private method

    Method setBufferMethod;
    Method readNextMethod;
    try {
      // Extract method
      setBufferMethod =
          FastQFile.class.getDeclaredMethod("setBuffer", new Class<?>[] {});

      // Invoke method, not parameter useful
      setBufferMethod.invoke(this, new Object[] {new BufferedReader(
          new InputStreamReader(is))});

      // Extract method by name
      readNextMethod =
          FastQFile.class.getDeclaredMethod("readNext", new Class<?>[] {});

      // Change accessible, initial private
      readNextMethod.setAccessible(true);

      // Invoke method, not parameter useful
      readNextMethod.invoke(this, new Object[] {});

    } catch (NoSuchMethodException | SecurityException | IllegalAccessException
        | IllegalArgumentException | InvocationTargetException e) {

      throw new EoulsanException(
          "Step FastQC: fail to invoke FastQCFile.readNext() method by reflection.",
          e);
    }

  }
}
