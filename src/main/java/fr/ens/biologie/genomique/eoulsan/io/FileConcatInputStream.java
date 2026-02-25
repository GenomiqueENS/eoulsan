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
 * of the Institut de Biologie de l'École normale supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.io;

import static java.util.Objects.requireNonNull;

import fr.ens.biologie.genomique.kenetre.io.FileUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

/**
 * This class define an InputStream that concatenate files in an InputStream.
 *
 * @since 1.0
 * @author Laurent Jourdren
 */
public class FileConcatInputStream extends AbstractConcatInputStream {

  private final Iterator<File> it;

  @Override
  protected boolean hasNextInputStream() {

    return this.it.hasNext();
  }

  @Override
  protected InputStream nextInputStream() throws IOException {

    return FileUtils.createInputStream(this.it.next());
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   *
   * @param files files to concatenate in the InputStream.
   */
  public FileConcatInputStream(final List<File> files) {

    requireNonNull(files, "files is null");

    this.it = files.iterator();
  }
}
