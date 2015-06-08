package fr.ens.transcriptome.eoulsan.steps.fastqc;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import htsjdk.samtools.SAMFileReader;
import htsjdk.samtools.SAMRecordIterator;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import uk.ac.babraham.FastQC.Sequence.BAMFile;
import uk.ac.babraham.FastQC.Sequence.SequenceFormatException;

@SuppressWarnings("deprecation")
public class SamInputStream extends BAMFile {

  private SAMFileReader br;
  @SuppressWarnings("unused")
  private String name;
  @SuppressWarnings("unused")
  private boolean onlyMapped;
  @SuppressWarnings("unused")
  private long fileSize;
  @SuppressWarnings("unused")
  private SAMRecordIterator it;

  SamInputStream(final InputStream is, final String filename)
      throws SequenceFormatException, IOException, EoulsanException {

    super(null, true);

    fileSize = Long.MAX_VALUE;
    name = filename;
    this.onlyMapped = true;

    // SAMFileReader
    // .setDefaultValidationStringency(SAMFileReader.ValidationStringency.SILENT);

    br = new SAMFileReader(is);

    it = br.iterator();

    // readNext();
    // Call private method

    Method readNextMethod;
    try {
      // Extract method by name
      readNextMethod =
          BAMFile.class.getDeclaredMethod("readNext", new Class<?>[] {});

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
