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

package fr.ens.transcriptome.eoulsan.bio.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fr.ens.transcriptome.eoulsan.bio.ReadSequence;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

public class BinaryFastQWriter {

  private static final int CHARACTER_BIT_LENGTH = 7;
  private static final int NUCLEOTIDE_BIT_LENGTH = 3;
  private static final int QUALITY_BIT_LENGTH = 6;

  private MultiBitsCompressor codec = new MultiBitsCompressor();
  private OutputStream os;

  private boolean keepName = true;

  private static class MultiBitsCompressor {

    private static final int BUFFER_LENGTH = 1024;

    private byte[] data = new byte[BUFFER_LENGTH];
    private int bytePos = 0;
    private int bitPos = 0;

    private List<Integer> values = new ArrayList<Integer>();
    private List<Integer> lengths = new ArrayList<Integer>();

    private final void add(final int value, final int bitLength) {

      // Check bit length value
      if (bitLength <= 0 || bitLength > 8)
        throw new InvalidParameterException("Invalid bit length value: "
            + bitLength);

      // Compute maximal value
      final int max = (~0) >>> (4 * 8 - bitLength);

      // check value range
      if (value <= 0 || value > max)
        throw new InvalidParameterException("Invalid value: " + value);

      internalAdd(value, bitLength);
    }

    private final void internalAdd(final int value, final int bitsLength) {

      this.values.add(value);
      this.lengths.add(bitsLength);

      final int shift = bitPos;
      final byte b1 = (byte) (value << shift);

      // TODO test longeur
      this.data[this.bytePos] |= b1;
      this.bitPos += bitsLength;

      if (this.bitPos >= 8) {

        this.bytePos++;
        final byte b2 = (byte) (value >> (8 - shift));
        this.data[this.bytePos] |= b2;
        this.bitPos -= 8;
        // System.out.println(shift);
        // System.out.println(byteToBinary(value));
        // System.out.println(byteToBinary(b1));
        // System.out.println(byteToBinary(b2));
      }

    }

    private void addEndField(final int bitsLength) {

      // Check bits length value
      if (bitsLength <= 0 || bitsLength > 8)
        throw new InvalidParameterException("Invalid bit length value: "
            + bitsLength);

      internalAdd(0, bitsLength);
    }

    private void addEndEntry() {

      int count = 0;
      if (this.data[this.bytePos - 1] == 0)
        count += 1;
      if (this.data[this.bytePos - 2] == 0)
        count += 1;

      this.bytePos += 2 - count;
      this.bitPos = 0;
    }

    private void clear() {

      this.bitPos = 0;
      this.bytePos = 0;
      this.values.clear();
      this.lengths.clear();
      Arrays.fill(this.data, (byte) 0);
    }

    public byte[] toArray() {

      addEndEntry();

      final int len = this.bitPos == 0 ? this.bytePos : this.bytePos + 1;

      byte[] result = new byte[len];
      System.arraycopy(this.data, 0, result, 0, result.length);

      checkResult(result);
      clear();
      return result;
    }

    public String toString() {

      final StringBuilder sb = new StringBuilder();

      final int len = this.bitPos == 0 ? this.bytePos : this.bytePos + 1;

      for (int i = 0; i < len; i++) {
        if (i > 0)
          sb.append(' ');
        sb.append(BinaryFastQWriter.binByteToString(this.data[i]));
      }

      return sb.toString();
    }

    // final MultiBitsDecompressor d = new MultiBitsDecompressor();
    int countOk;

    private void checkResult(final byte[] result) {

      if (result == null)
        return;

      boolean found00 = false;
      for (int i = 0; i < result.length - 1; i++) {

        if (result[i] == 0 && result[i + 1] == 0)
          if (i < result.length - 2) {
            for (int j = 0; j < result.length; j++)
              System.out.print(byteToBinary(result[j]) + ",");
            System.out.println();

            throw new Error("Found 00 in pos "
                + i + "(-" + (result.length - i) + ")  count=" + countOk);
          }
        found00 = true;
      }

      if (!found00) {

        for (int j = 0; j < result.length; j++)
          System.out.print(byteToBinary(result[j]) + ",");
        System.out.println();

        throw new Error("No 00 found at the end of the entry. Count=" + countOk);
      }

      // System.out.println("len="+result.length);
      // for (int i = 0; i < result.length; i++)
      // System.out.print(byteToBinary(result[i]) + ",");
      // System.out.println();

      final MultiBitsDecompressor d = new MultiBitsDecompressor();
      d.setData(result);
      final int len = this.values.size();

      for (int i = 0; i < len; i++) {

        final int val = d.nextValue(this.lengths.get(i));
        if (countOk % 1000 == 0)
          System.out.println("count: " + countOk);
        // System.out.println(this.values.get(i) + "(" +
        // byteToBinary(this.values.get(i))
        // + ") gets " + val + "(" + byteToBinary(val) + " "+
        // Integer.toBinaryString(val) +") i=" + i
        // + " countOk=" + countOk);
        if (val != this.values.get(i)) {
          throw new Error("Error: waiting "
              + this.values.get(i) + "(" + byteToBinary(this.values.get(i))
              + ") gets " + val + "(" + byteToBinary(val) + ") i=" + i
              + " countOk=" + countOk);
        }

      }
      countOk++;
    }

  }

  private static class MultiBitsDecompressor {

    private byte[] data;
    private int bytePos = 0;
    private int bitPos = 0;

    public void setData(final byte[] array) {

      if (array == null)
        throw new NullPointerException("Array is null.");

      this.data = array;
      this.bytePos = 0;
      this.bitPos = 0;
    }

    public int nextValue(final int bitsLength) {

      // Check bit length value
      if (bitsLength <= 0 || bitsLength > 8)
        throw new InvalidParameterException("Invalid bit length value: "
            + bitsLength);

      final byte b1 = this.data[this.bytePos];
      final int i1 = b1 & 0xFF;

      final int posShift = this.bitPos;
      this.bitPos += bitsLength;
      final int cleanShift =
          this.bitPos < 8 ? 4 * 8 - bitsLength : 4
              * 8 - bitsLength + this.bitPos - 8;

      final int r1 = (i1 >>> posShift);
      // System.out.println("r1:" + Integer.toBinaryString(r1));
      final int r2 = (r1 << cleanShift);
      // System.out.println("r2:" + Integer.toBinaryString(r2));
      final int r3 = (r2 >>> cleanShift);
      // System.out.println("r3:" + Integer.toBinaryString(r3));

      // System.out.println("r="+r3);

      int result = r3;
      // System.out.println("result tmp  ="
      // + result + "\t" + Integer.toBinaryString(result));

      if (this.bitPos > 8) {

        final byte b2 = this.data[this.bytePos + 1];
        final int i2 = b2 & 0xFF;

        final int shift2 = 4 * 8 - (this.bitPos - 8);

        final int t1 = (i2 << (shift2));
        // System.out.println("t1:" + Integer.toBinaryString(t1));
        final int t2 = (t1 >>> (shift2));
        // System.out.println("t2:" + Integer.toBinaryString(t2));
        final int t3 = (t2 << (bitsLength - (this.bitPos - 8) + 3 * 8));
        // System.out.println("t3:" + Integer.toBinaryString(t3));
        final int t4 = (t3 >>> (3 * 8));
        // System.out.println("t4:" + Integer.toBinaryString(t4));

        result += t4;
      }

      // + result + "\t" + Integer.toBinaryString(result));
      if (this.bitPos >= 8) {
        this.bitPos -= 8;
        this.bytePos++;
        // TODO test longueur
      }

      return result;
    }

  }

  /**
   * Write the current entry.
   * @throws IOException if an error occurs while writing data
   */
  public void write(ReadSequence read) throws IOException {

    if (read == null)
      return;

    addName(read.getName());
    addSequence(read.getSequence());
    addQuality(read.getQuality());

    os.write(this.codec.toArray());
  }

  /**
   * Close the writer.
   * @throws IOException if an error occurs while closing the writer
   */
  public void close() throws IOException {

    this.os.close();
  }

  //
  // Utility methods
  //

  private final void addName(final String name) {

    if (name == null)
      throw new NullPointerException("The sequence name is null.");

    if (!keepName)
      this.codec.add('!' - ' ', CHARACTER_BIT_LENGTH);
    else {

      final int len = name.length();

      for (int i = 0; i < len; i++) {

        char c = name.charAt(i);

        if (c <= ' ' || c >= 127)
          throw new InvalidParameterException(
              "Sequence contains invalid character: " + name.charAt(i));

        this.codec.add(c - ' ', CHARACTER_BIT_LENGTH);
      }
    }
    this.codec.addEndField(CHARACTER_BIT_LENGTH);
  }

  private final void addSequence(final String sequence) {

    if (sequence == null)
      throw new NullPointerException("The sequence is null.");

    final int len = sequence.length();

    for (int i = 0; i < len; i++) {

      switch (sequence.charAt(i)) {

      case 'A':
        this.codec.add(1, NUCLEOTIDE_BIT_LENGTH);
        break;
      case 'T':
        this.codec.add(2, NUCLEOTIDE_BIT_LENGTH);
        break;
      case 'G':
        this.codec.add(3, NUCLEOTIDE_BIT_LENGTH);
        break;
      case 'C':
        this.codec.add(4, NUCLEOTIDE_BIT_LENGTH);
        break;
      case 'N':
        this.codec.add(5, NUCLEOTIDE_BIT_LENGTH);
        break;
      default:
        throw new InvalidParameterException(
            "Sequence contains invalid character: " + sequence.charAt(i));
      }
    }

    this.codec.addEndField(NUCLEOTIDE_BIT_LENGTH);
  }

  private final void addQuality(final String quality) {

    if (quality == null)
      throw new NullPointerException("The quality sequence is null.");

    final int len = quality.length();

    for (int i = 0; i < len; i++) {

      char c = quality.charAt(i);

      if (c < 64 || c >= 127)
        throw new InvalidParameterException(
            "Sequence contains invalid character: " + quality.charAt(i));

      this.codec.add(c - 64 + 1, QUALITY_BIT_LENGTH);
    }
    this.codec.addEndField(QUALITY_BIT_LENGTH);
  }

  //
  // Constructors
  //

  /**
   * Public constructor.
   * @param os OutputStream to use
   */
  public BinaryFastQWriter(final OutputStream os) throws FileNotFoundException {

    if (os == null)
      throw new NullPointerException("The output stream is null");

    this.os = os;
  }

  /**
   * Public constructor.
   * @param outputFile file to use
   */
  public BinaryFastQWriter(final File outputFile) throws FileNotFoundException {

    this.os = FileUtils.createOutputStream(outputFile);
  }

  /**
   * Public constructor.
   * @param outputFilename name of the file to use
   */
  public BinaryFastQWriter(final String outputFilename)
      throws FileNotFoundException {

    this.os = FileUtils.createOutputStream(outputFilename);
  }

  public static String binByteToString(byte b) {

    final StringBuilder sb = new StringBuilder();
    int i = 256; // max number * 2
    while ((i >>= 1) > 0)
      sb.append(((b & i) != 0 ? "1" : "0"));

    return sb.toString();
  }

  private static final String byteToBinary(int i) {
    return byteToBinary((byte) i);
  }

  private static final String byteToBinary(byte b) {

    final StringBuilder sb = new StringBuilder();
    int i = 256; // max number * 2
    while ((i >>= 1) > 0)
      sb.append((b & i) != 0 ? "1" : "0");

    return sb.toString();
  }

}
