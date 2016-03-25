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

package fr.ens.biologie.genomique.eoulsan.bio;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;

/**
 * This class allow the easily get fields of Illumina reads ids.
 * @since 1.0
 * @author Laurent Jourdren
 */
public final class IlluminaReadId {

  private static final Pattern PATTERN_1 =
      Pattern.compile("^([a-zA-Z0-9\\-\\_]+):(\\d+):(\\d+):(\\d+):(\\d+)$");

  private static final Pattern PATTERN_2 = Pattern
      .compile("^([a-zA-Z0-9\\-\\_]+):(\\d+):(\\d+):(\\d+):(\\d+)/(\\d)$");

  private static final Pattern PATTERN_1_4 = Pattern.compile(
      "^([a-zA-Z0-9\\-\\_]+):(\\d+):(\\d+):(\\d+):(\\d+)#([0ATGC]+)/(\\d)$");

  private static final Pattern PATTERN_1_8 = Pattern.compile(
      "^([a-zA-Z0-9\\-\\_]+):(\\d+):([a-zA-Z0-9]+):(\\d+):(\\d+):(\\d+):(\\d+) "
          + "(\\d+):([YN]):(\\d+):([NATGC]*)$");

  private static final Pattern PATTERN_3 = Pattern.compile(
      "^([a-zA-Z0-9\\-\\_]+):(\\d+):([a-zA-Z0-9]+):(\\d+):(\\d+):(\\d+):(\\d+) "
          + "(\\d+):([YN]):(\\d+):(\\d)$");

  private static final Pattern PATTERN_SRA = Pattern.compile(
      "^[a-zA-Z0-9\\.]+ ([a-zA-Z0-9\\-\\_]+):(\\d+):([a-zA-Z0-9]+):(\\d+):(\\d+):(\\d+):(\\d+) "
          + ".*$");

  private final Pattern pattern;

  private String instrumentId;
  private int runId;
  private String flowCellId;
  private int flowCellLane;
  private int tileNumberInFlowCellLane;
  private int xClusterCoordinateInTile;
  private int yClusterCoordinateInTile;
  private String sequenceIndex;
  private int pairMember;
  private boolean filtered;
  private int controlNumber;

  //
  // Getters
  //

  /**
   * Get instrument id.
   * @return a String with the instrument id
   */
  public final String getInstrumentId() {
    return this.instrumentId;
  }

  /**
   * Get Run id.
   * @return the run id or -1 if there is no run id
   */
  public final int getRunId() {
    return this.runId;
  }

  /**
   * Get the flow cell id.
   * @return the flow cell id as a string or null if there is no flow cell id
   */
  public final String getFlowCellId() {
    return this.flowCellId;
  }

  /**
   * Get the flowcell lane.
   * @return the flowcell lane
   */
  public final int getFlowCellLane() {
    return this.flowCellLane;
  }

  /**
   * Get the tile number within the flowcell lane.
   * @return the tile number within the flowcell lane
   */
  public final int getTileNumberInFlowCellLane() {
    return this.tileNumberInFlowCellLane;
  }

  /**
   * Get 'x'-coordinate of the cluster within the tile.
   * @return the 'x'-coordinate of the cluster within the tile
   */
  public final int getXClusterCoordinateInTile() {
    return this.xClusterCoordinateInTile;
  }

  /**
   * Get 'y'-coordinate of the cluster within the tile.
   * @return the 'y'-coordinate of the cluster within the tile
   */
  public final int getYClusterCoordinateInTile() {
    return this.yClusterCoordinateInTile;
  }

  /**
   * Get the sequence index for a multiplexed sample.
   * @return the sequence index for a multiplexed sample, "0" for no indexing
   */
  public final String getSequenceIndex() {
    return this.sequenceIndex;
  }

  /**
   * Get the member of a pair.
   * @return the the member of a pair, /1 or /2 (paired-end or mate-pair reads
   *         only)
   */
  public final int getPairMember() {
    return this.pairMember;
  }

  /**
   * Test if the read is filtered.
   * @return true if the read is filtered
   */
  public final boolean isFiltered() {
    return this.filtered;
  }

  /**
   * Get the value of the control number.
   * @return the control number or -1 if there is no control number
   */
  public final int getControlNumber() {
    return this.controlNumber;
  }

  //
  // Test if the fields exists
  //

  /**
   * Test if instrument id field exists.
   * @return true if instrument id field exists
   */
  public final boolean isInstrumentIdField() {
    return true;
  }

  /**
   * Test if the Run id field exists.
   * @return true if the Run id field exists
   */
  public final boolean isRunIdField() {

    if (this.pattern == PATTERN_1_4
        || this.pattern == PATTERN_2 || this.pattern == PATTERN_1) {
      return false;
    }

    return true;
  }

  /**
   * Test if the flow cell id field exist.
   * @return true if the flow cell id field exist
   */
  public final boolean isFlowCellIdField() {

    if (this.pattern == PATTERN_1_4
        || this.pattern == PATTERN_2 || this.pattern == PATTERN_1) {
      return false;
    }

    return true;
  }

  /**
   * Test if the flowcell lane field exists.
   * @return true if the flowcell lane field exists
   */
  public final boolean isFlowCellLaneField() {
    return true;
  }

  /**
   * Test if the tile number within the flowcell lane field exists.
   * @return true if the tile number within the flowcell lane field exists
   */
  public final boolean isTileNumberInFlowCellLaneField() {
    return true;
  }

  /**
   * Test if 'x'-coordinate of the cluster within the tile field exists.
   * @return true if the the 'x'-coordinate of the cluster within the tile field
   *         exists
   */
  public final boolean isXClusterCoordinateInTileField() {
    return true;
  }

  /**
   * Test if the 'y'-coordinate of the cluster within the tile exists.
   * @return true if the 'y'-coordinate of the cluster within the tile exists
   */
  public final boolean isYClusterCoordinateInTileField() {
    return true;
  }

  /**
   * Test if the sequence index for a multiplexed sample exist.
   * @return true if the sequence index for a multiplexed sample exist
   */
  public final boolean isSequenceIndexField() {

    if (this.pattern == PATTERN_3
        || this.pattern == PATTERN_2 || this.pattern == PATTERN_1
        || this.pattern == PATTERN_SRA) {
      return false;
    }

    return true;
  }

  /**
   * Test if the member of a pair field exists.
   * @return true if the member of a pair field exists
   */
  public final boolean isPairMemberField() {

    if (this.pattern == PATTERN_1 || this.pattern == PATTERN_SRA) {
      return false;
    }

    return true;
  }

  /**
   * Test if the read filtered field exists.
   * @return true if the read filtered field exist
   */
  public final boolean isFilteredField() {

    if (this.pattern == PATTERN_1_4
        || this.pattern == PATTERN_2 || this.pattern == PATTERN_1
        || this.pattern == PATTERN_SRA) {
      return false;
    }

    return true;
  }

  /**
   * Test if the control number field exists.
   * @return true if the control number field exists
   */
  public final boolean isControlNumberField() {

    if (this.pattern == PATTERN_1_4
        || this.pattern == PATTERN_2 || this.pattern == PATTERN_1
        || this.pattern == PATTERN_SRA) {
      return false;
    }

    return true;
  }

  //
  // Other method
  //

  private static Pattern findPattern(final String readId)
      throws EoulsanException {

    if (PATTERN_1_8.matcher(readId).lookingAt()) {
      return PATTERN_1_8;
    }

    if (PATTERN_3.matcher(readId).lookingAt()) {
      return PATTERN_3;
    }

    if (PATTERN_1_4.matcher(readId).lookingAt()) {
      return PATTERN_1_4;
    }

    if (PATTERN_2.matcher(readId).lookingAt()) {
      return PATTERN_2;
    }

    if (PATTERN_1.matcher(readId).lookingAt()) {
      return PATTERN_1;
    }

    if (PATTERN_SRA.matcher(readId).lookingAt()) {
      return PATTERN_SRA;
    }

    throw new EoulsanException("Invalid illumina id: " + readId);
  }

  /**
   * Parse an Illumina id string in a Sequence object.
   * @param sequence sequence witch name must be parsed
   * @throws EoulsanException if the id is not an Illumina id
   */
  public final void parse(final Sequence sequence) throws EoulsanException {

    if (sequence == null) {
      throw new NullPointerException("The sequence is null");
    }

    parse(sequence.getName());
  }

  /**
   * Parse an Illumina id string.
   * @param readId String with the Illumina id
   * @throws EoulsanException if the id is not an Illumina id
   */
  public final void parse(final String readId) throws EoulsanException {

    if (readId == null) {
      throw new NullPointerException("The string to parse is null");
    }

    final Matcher matcher = this.pattern.matcher(readId.trim());
    if (!matcher.lookingAt()) {
      throw new EoulsanException("Invalid illumina id: " + readId);
    }

    if (this.pattern == PATTERN_1_8) {

      this.instrumentId = matcher.group(1);
      this.runId = Integer.parseInt(matcher.group(2));
      this.flowCellId = matcher.group(3);
      this.flowCellLane = Integer.parseInt(matcher.group(4));
      this.tileNumberInFlowCellLane = Integer.parseInt(matcher.group(5));
      this.xClusterCoordinateInTile = Integer.parseInt(matcher.group(6));
      this.yClusterCoordinateInTile = Integer.parseInt(matcher.group(7));
      this.pairMember = Integer.parseInt(matcher.group(8));
      this.filtered = matcher.group(9).charAt(0) == 'Y';
      this.controlNumber = Integer.parseInt(matcher.group(10));
      this.sequenceIndex = matcher.group(11);

      return;
    } else if (this.pattern == PATTERN_3) {

      this.instrumentId = matcher.group(1);
      this.runId = Integer.parseInt(matcher.group(2));
      this.flowCellId = matcher.group(3);
      this.flowCellLane = Integer.parseInt(matcher.group(4));
      this.tileNumberInFlowCellLane = Integer.parseInt(matcher.group(5));
      this.xClusterCoordinateInTile = Integer.parseInt(matcher.group(6));
      this.yClusterCoordinateInTile = Integer.parseInt(matcher.group(7));
      this.pairMember = Integer.parseInt(matcher.group(8));
      this.filtered = matcher.group(9).charAt(0) == 'Y';
      this.controlNumber = Integer.parseInt(matcher.group(10));
      this.sequenceIndex = "0";

      return;
    } else if (this.pattern == PATTERN_1_4) {

      this.instrumentId = matcher.group(1);
      this.runId = -1;
      this.flowCellId = null;
      this.flowCellLane = Integer.parseInt(matcher.group(2));
      this.tileNumberInFlowCellLane = Integer.parseInt(matcher.group(3));
      this.xClusterCoordinateInTile = Integer.parseInt(matcher.group(4));
      this.yClusterCoordinateInTile = Integer.parseInt(matcher.group(5));
      this.sequenceIndex = matcher.group(6);
      this.pairMember = Integer.parseInt(matcher.group(7));
      this.filtered = false;
      this.controlNumber = -1;

      return;
    } else if (this.pattern == PATTERN_2) {

      this.instrumentId = matcher.group(1);
      this.runId = -1;
      this.flowCellId = null;
      this.flowCellLane = Integer.parseInt(matcher.group(2));
      this.tileNumberInFlowCellLane = Integer.parseInt(matcher.group(3));
      this.xClusterCoordinateInTile = Integer.parseInt(matcher.group(4));
      this.yClusterCoordinateInTile = Integer.parseInt(matcher.group(5));
      this.sequenceIndex = "0";
      this.pairMember = Integer.parseInt(matcher.group(6));
      this.filtered = false;
      this.controlNumber = -1;

      return;
    } else if (this.pattern == PATTERN_SRA) {

      this.instrumentId = matcher.group(1);
      this.runId = Integer.parseInt(matcher.group(2));
      this.flowCellId = matcher.group(3);
      this.flowCellLane = Integer.parseInt(matcher.group(4));
      this.tileNumberInFlowCellLane = Integer.parseInt(matcher.group(5));
      this.xClusterCoordinateInTile = Integer.parseInt(matcher.group(6));
      this.yClusterCoordinateInTile = Integer.parseInt(matcher.group(7));
      this.sequenceIndex = "0";
      this.pairMember = -1;
      this.filtered = false;
      this.controlNumber = -1;

      return;
    }

    // PATTERN_1
    this.instrumentId = matcher.group(1);
    this.runId = -1;
    this.flowCellId = null;
    this.flowCellLane = Integer.parseInt(matcher.group(2));
    this.tileNumberInFlowCellLane = Integer.parseInt(matcher.group(3));
    this.xClusterCoordinateInTile = Integer.parseInt(matcher.group(4));
    this.yClusterCoordinateInTile = Integer.parseInt(matcher.group(5));
    this.sequenceIndex = "0";
    this.pairMember = -1;
    this.filtered = false;
    this.controlNumber = -1;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param readId String with Illumina id to parse
   * @throws EoulsanException if the id is not an Illumina id
   */
  public IlluminaReadId(final String readId) throws EoulsanException {

    if (readId == null) {
      throw new NullPointerException("The string to parse is null");
    }

    this.pattern = findPattern(readId);
    parse(readId);
  }

  /**
   * Public constructor.
   * @param sequence sequence witch name must be parsed
   * @throws EoulsanException if the id is not an Illumina id
   */
  public IlluminaReadId(final Sequence sequence) throws EoulsanException {

    if (sequence == null) {
      throw new NullPointerException("The sequence is null");
    }

    final String readId = sequence.getName();

    if (readId == null) {
      throw new NullPointerException("The string to parse is null");
    }

    this.pattern = findPattern(readId);
    parse(readId);
  }

}
