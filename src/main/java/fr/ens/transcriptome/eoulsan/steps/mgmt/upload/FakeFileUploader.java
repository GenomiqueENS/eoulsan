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

import java.io.IOException;
import java.io.OutputStream;

import fr.ens.transcriptome.eoulsan.datasources.DataSource;

/**
 * This is a fake upload to prevent upload of some type of files.
 * @author Laurent Jourdren
 */
public class FakeFileUploader implements FileUploader {

  private String src;
  private String dest;

  @Override
  public OutputStream createUploadOutputStream() throws IOException {

    return null;
  }

  @Override
  public String getDest() {

    return this.dest;
  }

  @Override
  public String getSrc() {

    return this.src;
  }

  @Override
  public void init(DataSource dataSource, String dest) {
  }

  @Override
  public void prepare() throws IOException {
  }

  @Override
  public void upload() throws IOException {
  }

  //
  // Constructor
  //

  public FakeFileUploader(final String src, final String dest) {

    this.src = src;
    this.dest = dest;
  }

}
