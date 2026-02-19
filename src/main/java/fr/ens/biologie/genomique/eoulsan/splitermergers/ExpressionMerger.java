package fr.ens.biologie.genomique.eoulsan.splitermergers;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.EoulsanLogger;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormats;

/**
 * This class define a merger class for expression files.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class ExpressionMerger implements Merger {

  @Override
  public DataFormat getFormat() {

    return DataFormats.EXPRESSION_RESULTS_TSV;
  }

  @Override
  public void configure(final Set<Parameter> conf) throws EoulsanException {

    // The merge does not need any parameter
    for (Parameter p : conf) {
      throw new EoulsanException("Unknown parameter for "
          + getFormat().getName() + " merger: " + p.getName());
    }
  }

  @Override
  public void merge(final Iterator<DataFile> inFileIterator, DataFile outFile)
      throws IOException {

    final Multiset<String> counts = HashMultiset.create();
    final Set<String> emptyCounts = new HashSet<>();

    while (inFileIterator.hasNext()) {

      // Get input file
      final DataFile inFile = inFileIterator.next();

      EoulsanLogger.getLogger()
          .info("Merge " + inFile.getName() + " to " + outFile.getName());

      boolean first = true;

      try (BufferedReader reader =
          new BufferedReader(new InputStreamReader(inFile.open(), UTF_8))) {

        String line = null;

        while ((line = reader.readLine()) != null) {

          // Do no handle header
          if (first) {
            first = false;
            continue;
          }

          final int tabPos = line.indexOf('\t');

          // Do not handle empty or invalid lines
          if (tabPos == -1) {
            continue;
          }

          try {

            final String id = line.substring(0, tabPos).trim();
            final int count = Integer.parseInt(line.substring(tabPos).trim());

            if (count == 0) {
              emptyCounts.add(id);
            }

            counts.add(id, count);

          } catch (NumberFormatException e) {
            // Do not handle parsing errors
          }
        }
      }
    }

    // Write the result file
    try (Writer writer = new OutputStreamWriter(outFile.create(), UTF_8)) {

      writer.write(ExpressionSplitter.EXPRESSION_FILE_HEADER);

      // Write the non empty counts
      for (Multiset.Entry<String> e : counts.entrySet()) {

        final String id = e.getElement();

        // Remove the id from empty counts
        emptyCounts.remove(id);

        // Write the entry
        writer.write(id + '\t' + e.getCount() + '\n');
      }

      // Write the empty counts
      for (String id : emptyCounts) {
        writer.write(id + "\t0\n");
      }
    }

  }

}
