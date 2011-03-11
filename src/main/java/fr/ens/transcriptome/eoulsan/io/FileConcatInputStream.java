/*
 *                  Eoulsan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License version 2.1 or
 * later and CeCILL-C. This should be distributed with the code.
 * If you do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/lgpl-2.1.txt
 *      http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.txt
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/eoulsan
 *
 */

package fr.ens.transcriptome.eoulsan.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import com.google.common.base.Preconditions;

/**
 * This class define an InputStream that concatenate files in an InputStream.
 * @author Laurent Jourdren
 */
public class FileConcatInputStream extends AbstractConcatInputStream {

  private final Iterator<File> it;

  @Override
  protected boolean hasNextInputStream() {

    return it.hasNext();
  }

  @Override
  protected InputStream nextInputStream() throws IOException {

    return new FileInputStream(it.next());
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param files files to concatenate in the InputStream.
   */
  public FileConcatInputStream(final List<File> files) {

    Preconditions.checkNotNull(files, "files is null");

    this.it = files.iterator();
  }

}
