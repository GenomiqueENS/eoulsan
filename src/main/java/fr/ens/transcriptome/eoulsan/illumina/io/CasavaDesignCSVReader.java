/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.illumina.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.illumina.CasavaDesign;
import fr.ens.transcriptome.eoulsan.illumina.CasavaSample;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * This class define a reader for Casava design CSV files.
 * @author Laurent Jourdren
 */
public class CasavaDesignCSVReader implements CasavaDesignReader {

  private BufferedReader reader;
  private final Splitter splitter = Splitter.on(',');

  @Override
  public CasavaDesign read() throws IOException {

    String line = null;
    final CasavaDesign design = new CasavaDesign();
    boolean firstLine = true;

    while ((line = this.reader.readLine()) != null) {

      line = line.trim();
      if ("".equals(line))
        continue;

      if (firstLine) {
        firstLine = false;
        continue;
      }

      line = line.replaceAll("\"", "");

      final List<String> fields = Lists.newArrayList(splitter.split(line));

      if (fields.size() != 10)
        throw new IOException("Invalid number of field ("
            + fields.size() + ") in line : " + line);

      final CasavaSample sample = new CasavaSample();

      sample.setFlowCellId(fields.get(0));
      sample.setLane(parseInt(fields.get(1)));
      sample.setSampleId(fields.get(2));
      sample.setSampleRef(fields.get(3));
      sample.setIndex(fields.get(4));
      sample.setDescription(fields.get(5));
      sample.setControl(Boolean.parseBoolean(fields.get(6)));
      sample.setRecipe(fields.get(7));
      sample.setOperator(fields.get(8));
      sample.setSampleProject(fields.get(9));

      design.addSample(sample);
    }

    reader.close();

    return design;
  }

  //
  // Other methods
  //

  private static final int parseInt(final String s) {

    if (s == null)
      return 0;

    try {
      return Integer.parseInt(s);
    } catch (NumberFormatException e) {
      return 0;
    }

  }

  //
  // Constructors
  //

  /**
   * Public constructor
   * @param is InputStream to use
   */
  public CasavaDesignCSVReader(final InputStream is) {

    if (is == null)
      throw new NullPointerException("InputStream is null");

    this.reader = new BufferedReader(new InputStreamReader(is));
  }

  /**
   * Public constructor
   * @param file File to use
   */
  public CasavaDesignCSVReader(final File file) throws FileNotFoundException {

    if (file == null)
      throw new NullPointerException("File is null");

    if (!file.isFile())
      throw new FileNotFoundException("File not found: "
          + file.getAbsolutePath());

    this.reader = FileUtils.createBufferedReader(file);
  }

}
