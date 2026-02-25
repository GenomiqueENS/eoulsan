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

import java.nio.charset.Charset;

/**
 * This class define default charsets.
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
public class FileCharsets {

  /** Ascii file encoding. */
  public static final String ASCII_ENCODING = "US-ASCII";

  /** Ascii charset. */
  public static final Charset ASCII_CHARSET = Charset.forName(ASCII_ENCODING);

  /** Latin1 file encoding. */
  public static final String LATIN1_FILE_ENCODING = "ISO-8859-1";

  /** Latin1 charset. */
  public static final Charset LATIN1_CHARSET = Charset.forName(LATIN1_FILE_ENCODING);

  /** UTF-8 file encoding. */
  public static final String UTF8_FILE_ENCODING = "UTF-8";

  /** UTF-8 charset. */
  public static final Charset UTF8_CHARSET = Charset.forName(UTF8_FILE_ENCODING);

  /** The system default charset. */
  public static final String SYSTEM_FILE_ENCODING = System.getProperty("file.encoding");

  /** The system default charset. */
  public static final Charset SYSTEM_CHARSET = Charset.forName(SYSTEM_FILE_ENCODING);

  //
  // Constructor
  //

  /** Private constructor. */
  private FileCharsets() {}
}
