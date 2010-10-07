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

package fr.ens.transcriptome.eoulsan.steps.mgmt.upload;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.apache.hadoop.conf.Configuration;

import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.bio.io.FastQReader;
import fr.ens.transcriptome.eoulsan.datasources.DataSource;

/**
 * This class define an uploader for TFQ files.
 * @author Laurent Jourdren
 */
public class HDFSTfqFileUploader extends HDFSFileUploader {

  private DataSource src;

  @Override
  public void init(final DataSource src, final String dest) {

    super.init(src, dest);
    this.src = src;
  }

  @Override
  public void prepare() throws IOException {

    final BufferedWriter bw =
        new BufferedWriter(new OutputStreamWriter(super
            .createUploadOutputStream()));

    final FastQReader fqr = new FastQReader(this.src.getInputStream());

    try {
      while (fqr.readEntry())
        bw.write(fqr.toTFQ(false));
    } catch (BadBioEntryException e) {

      throw new IOException("Invalid fastq entry in " + this.src);
    }

    bw.close();
    fqr.close();
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param conf Configuration object
   */
  public HDFSTfqFileUploader(final Configuration conf) {

    super(conf);
  }

}
