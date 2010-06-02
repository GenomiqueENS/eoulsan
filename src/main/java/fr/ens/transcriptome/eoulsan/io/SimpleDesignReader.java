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
 * of the �cole Normale Sup�rieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.datasources.DataSource;
import fr.ens.transcriptome.eoulsan.datasources.DataSourceUtils;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.DesignFactory;

/**
 * This class define a design reader for limma design files.
 * @author Laurent Jourdren
 */
public class SimpleDesignReader extends InputStreamDesignReader {

  private static final String SLIDENUMBER_FIELD = "SampleNumber";
  private static final String NAME_FIELD = "Name";
  private static final String FILENAME_FIELD = "FileName";

  private String baseDir = "";

  @Override
  public Design read() throws EoulsanIOException {

    Map<String, List<String>> data = new HashMap<String, List<String>>();
    List<String> fieldnames = new ArrayList<String>();

    try {

      setBufferedReader(new BufferedReader(new InputStreamReader(
          getInputStream(), Globals.DEFAULT_FILE_ENCODING)));

      BufferedReader br = getBufferedReader();
      final String separator = getSeparatorField();
      String line = null;

      boolean firstLine = true;
      // String ref = null;

      while ((line = br.readLine()) != null) {

        final String empty = line.trim();
        if ("".equals(empty) || empty.startsWith("#"))
          continue;

        final String[] fields = line.split(separator);

        if (firstLine) {

          for (int i = 0; i < fields.length; i++) {

            final String field = fields[i].trim();
            data.put(field, new ArrayList<String>());
            fieldnames.add(field);
          }

          firstLine = false;
        } else {

          for (int i = 0; i < fields.length; i++) {

            final String field = fields[i].trim();

            final String fieldName = fieldnames.get(i);

            List<String> l = data.get(fieldName);

            if ((SLIDENUMBER_FIELD.equals(fieldName) || NAME_FIELD
                .equals(fieldName))
                && l.contains(field))
              throw new EoulsanIOException(
                  "Invalid file format: "
                      + "SlideNumber or Name fields can't contains duplicate values");

            l.add(field);

          }

        }

      }
    } catch (IOException e) {

      throw new EoulsanIOException("Error while reading the file");
    }

    try {
      getBufferedReader().close();
    } catch (IOException e) {
      throw new EoulsanIOException("Error while closing the file"
          + e.getMessage());
    }

    if (!data.containsKey(SLIDENUMBER_FIELD))
      throw new EoulsanIOException("Invalid file format: No SlideNumber field");

    if (!data.containsKey(FILENAME_FIELD))
      throw new EoulsanIOException("Invalid file format: No FileName field");

    Design design = DesignFactory.createEmptyDesign();

    List<String> names = data.get(NAME_FIELD);
    List<String> ids = data.get(SLIDENUMBER_FIELD);
    final int count = ids.size();

    for (int i = 0; i < names.size(); i++) {

      final String name = names.get(i);
      design.addSample(name);
      design.getSample(name).setId(Integer.parseInt(ids.get(i)));
    }

    // Set FileName field
    List<String> filenames = data.get(FILENAME_FIELD);
    for (int i = 0; i < count; i++) {

      DataSource source =
          DataSourceUtils.identifyDataSource(this.baseDir, filenames.get(i));
      design.setSource(names.get(i), source.toString());
    }

    for (String fd : fieldnames) {

      if (SLIDENUMBER_FIELD.equals(fd)
          || NAME_FIELD.equals(fd) || FILENAME_FIELD.equals(fd))
        continue;

      design.addMetadataField(fd);
      List<String> descriptions = data.get(fd);

      int k = 0;
      for (String desc : descriptions)
        design.setMetadata(names.get(k++), fd, desc);

    }

    return design;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param file file to read
   * @throws NividicIOException if an error occurs while reading the file or if
   *           the file is null.
   */
  public SimpleDesignReader(final File file) throws EoulsanIOException {

    super(file);
    if (file != null)
      this.baseDir = file.getParent();
  }

  /**
   * Public constructor
   * @param is Input stream to read
   * @throws NividicIOException if the stream is null
   */
  public SimpleDesignReader(final InputStream is) throws EoulsanIOException {

    super(is);
  }

  /**
   * Public constructor
   * @param filename File to read
   * @throws NividicIOException if the stream is null
   * @throws FileNotFoundException if the file doesn't exist
   */
  public SimpleDesignReader(final String filename) throws EoulsanIOException,
      FileNotFoundException {

    this(new FileInputStream(filename));
  }

}
