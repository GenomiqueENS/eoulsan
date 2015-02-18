package fr.ens.transcriptome.eoulsan.splitermergers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormats;

/**
 * This class define a splitter class for expression files.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class ExpressionSplitter implements Splitter {

  private static final int DEFAULT_SPLIT_MAX_LINES = 10000;
  static final String EXPRESSION_FILE_HEADER = "Id\tCount\n";

  private int splitMaxLines = DEFAULT_SPLIT_MAX_LINES;

  @Override
  public DataFormat getFormat() {

    return DataFormats.EXPRESSION_RESULTS_TSV;
  }

  @Override
  public void configure(final Set<Parameter> conf) throws EoulsanException {

    for (Parameter p : conf) {

      if ("max.lines".equals(p.getName())) {
        this.splitMaxLines = p.getIntValue();

        if (this.splitMaxLines < 1) {
          throw new EoulsanException("Invalid "
              + p.getName() + " parameter value: " + p.getIntValue());
        }

      } else {
        throw new EoulsanException("Unknown parameter for "
            + getFormat().getName() + " splitter: " + p.getName());
      }
    }
  }

  @Override
  public void split(final DataFile inFile,
      final Iterator<DataFile> outFileIterator) throws IOException {

    final int max = this.splitMaxLines;
    int readCount = 0;
    Writer writer = null;

    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(inFile.open()))) {

      String line = null;
      boolean first = true;

      while ((line = reader.readLine()) != null) {

        // Discard header
        if (first) {
          first = false;
          continue;
        }

        if (readCount % max == 0) {

          // Close previous writer
          if (writer != null) {
            writer.close();
          }

          // Create new writer
          writer = new OutputStreamWriter(outFileIterator.next().create());
          writer.write(EXPRESSION_FILE_HEADER);
        }

        writer.write(line + '\n');
        readCount++;
      }

      // Close reader and writer
      reader.close();
      if (writer != null) {
        writer.close();
      }
    }
  }

}
