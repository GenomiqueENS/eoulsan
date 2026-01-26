package fr.ens.biologie.genomique.eoulsan.splitermergers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormats;

/**
 * This class define a splitter class for expression files.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class ExpressionSplitter implements Splitter {

  private static final int DEFAULT_SPLIT_MAX_ENTRIES = 10000;
  static final String EXPRESSION_FILE_HEADER = "Id\tCount\n";

  private int splitMaxEntries = DEFAULT_SPLIT_MAX_ENTRIES;

  @Override
  public DataFormat getFormat() {

    return DataFormats.EXPRESSION_RESULTS_TSV;
  }

  @Override
  public void configure(final Set<Parameter> conf) throws EoulsanException {

    for (Parameter p : conf) {

      switch (p.getName()) {

      case "max.entries":
        this.splitMaxEntries = p.getIntValueGreaterOrEqualsTo(1);
        break;

      default:
        throw new EoulsanException("Unknown parameter for "
            + getFormat().getName() + " splitter: " + p.getName());
      }

    }
  }

  @Override
  public void split(final DataFile inFile,
      final Iterator<DataFile> outFileIterator) throws IOException {

    final int max = this.splitMaxEntries;
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

      // Close the writer
      if (writer != null) {
        writer.close();
      }
    }
  }

}
