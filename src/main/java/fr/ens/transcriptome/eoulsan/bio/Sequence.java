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

package fr.ens.transcriptome.eoulsan.bio;

import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;

/**
 * This class define a Sequence.
 * @author Laurent Jourdren
 */
public class Sequence {

  protected int id;
  protected String name;
  protected String sequence;

  //
  // Getters
  //

  /**
   * Get the id of the sequence.
   * @return the id of the sequence
   */
  public final int getId() {
    return this.id;
  }

  /**
   * Set the name of the sequence.
   * @return the name of the sequence
   */
  public final String getName() {
    return this.name;
  }

  /**
   * Get the sequence of the sequence.
   * @return a string with the sequence
   */
  public final String getSequence() {

    return this.sequence;
  }

  //
  // Setters
  //

  /**
   * Set the id of the sequence.
   * @param id id to set
   */
  public void setId(final int id) {
    this.id = id;
  }

  /**
   * Set the name of the sequence.
   * @param name the name to set
   */
  public void setName(final String name) {
    this.name = name;
  }

  /**
   * Set the sequence.
   * @param sequence Sequence to set
   */
  public final void setSequence(final String sequence) {
    this.sequence = sequence;
  }

  //
  // Other methods
  //

  /**
   * Get the length of the read.
   * @return the length of the read
   */
  public int length() {

    if (this.sequence == null) {
      return 0;
    }

    return this.sequence.length();
  }

  /**
   * Get the tm of the sequence.
   * @return the tm of the sequence
   */
  public final float getTm() {

    return getTm(50, 50);
  }

  /**
   * Get the tm of the sequence.
   * @param dnac DNA concentration [nM]
   * @param saltc salt concentration [mM
   * @return the tm temp for the sequence
   */
  public final float getTm(final float dnac, final float saltc) {

    return MeltingTemp.tmstalucDNA(this.sequence, dnac, saltc);
  }

  /**
   * Get the GC percent for the sequence.
   * @return the GC percent for the sequence
   */
  public final float getGCPercent() {

    if (this.sequence == null) {
      return Float.NaN;
    }

    final int len = this.sequence.length();

    int count = 0;

    for (int i = 0; i < len; i++) {

      if (this.sequence.charAt(i) == 'G' || this.sequence.charAt(i) == 'C') {
        count++;
      }
    }

    return (float) count / (float) len;
  }

  /**
   * Set the sequence as the reverse complement.
   */
  public final void reverseComplement() {

    this.sequence = reverseComplement(this.sequence);
  }

  /**
   * Get the sequence as the reverse complement.
   * @param sequence sequence to reverse complement
   * @return the reverse complement sequence
   */
  public static final String reverseComplement(final String sequence) {

    if (sequence == null) {
      return null;
    }

    final int len = sequence.length();
    final StringBuilder sb = new StringBuilder(len);

    for (int i = len - 1; i >= 0; i--) {

      switch (sequence.charAt(i)) {

      case 'A':
        sb.append('T');
        break;
      case 'T':
        sb.append('A');
        break;
      case 'G':
        sb.append('C');
        break;
      case 'C':
        sb.append('G');
        break;
      case 'a':
        sb.append('t');
        break;
      case 't':
        sb.append('t');
        break;
      case 'g':
        sb.append('c');
        break;
      case 'c':
        sb.append('g');
        break;

      default:
        throw new EoulsanRuntimeException("Invalid character in sequence: '"
            + sequence.charAt(i) + "'");
      }

    }
    return sb.toString();
  }

  /**
   * Return the sequence object in string in Fasta format.
   * @return the sequence in Fasta format
   */
  public String toFasta() {

    return '>' + this.name + '\n' + this.sequence + '\n';
  }

  /**
   * Return the sequence object in string in Fasta format with a maximal width
   * for lines.
   * @return the sequence in Fasta format
   */
  public String toFasta(final int width) {

    if (width < 1)
      return toFasta();

    final StringBuilder sb = new StringBuilder();
    sb.append('>');
    sb.append(this.name);
    sb.append('\n');

    final int len = this.sequence.length();
    int pos = 0;

    while (pos < len) {

      final int nextPos = pos + width;
      sb.append(this.sequence.subSequence(pos, nextPos > len ? len : nextPos));
      sb.append('\n');
      pos = nextPos;
    }

    return sb.toString();
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public Sequence() {

    super();
  }

  /**
   * Public constructor.
   * @param id identifier
   * @param name Name of the read
   * @param sequence Sequence of the read
   */
  public Sequence(final int id, final String name, final String sequence) {

    this.id = id;
    this.name = name;
    this.sequence = sequence;
  }

}
