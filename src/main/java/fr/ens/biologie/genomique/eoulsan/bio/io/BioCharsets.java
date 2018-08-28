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

package fr.ens.biologie.genomique.eoulsan.bio.io;

import java.nio.charset.Charset;

/**
 * This class define encoding and charsets for bio file formats.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class BioCharsets {

  /** FASTA encoding. */
  public static final String FASTA_ENCODING = "ISO-8859-1";

  /** FASTQ Charset. */
  public static final Charset FASTA_CHARSET = Charset.forName(FASTA_ENCODING);

  /** FASTQ encoding. */
  public static final String FASTQ_ENCODING = "ISO-8859-1";

  /** FASTQ Charset. */
  public static final Charset FASTQ_CHARSET = Charset.forName(FASTQ_ENCODING);

  /** SAM encoding. */
  public static final String SAM_ENCODING = "ISO-8859-1";

  /** SAM Charset. */
  public static final Charset SAM_CHARSET = Charset.forName(SAM_ENCODING);

  /** GFF encoding. */
  public static final String GFF_ENCODING = "ISO-8859-1";

  /** GFF Charset. */
  public static final Charset GFF_CHARSET = Charset.forName(GFF_ENCODING);

  /** GFF encoding. */
  public static final String BED_ENCODING = "ISO-8859-1";

  /** BED Charset. */
  public static final Charset BED_CHARSET = Charset.forName(BED_ENCODING);

  /** Expression files encoding. */
  public static final String EXPRESSION_ENCODING = "UTF-8";

  /** Expression files Charset. */
  public static final Charset EXPRESSION_CHARSET =
      Charset.forName(EXPRESSION_ENCODING);

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private BioCharsets() {
  }

}
