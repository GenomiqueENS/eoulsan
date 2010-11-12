package fr.ens.transcriptome.eoulsan.bio;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.ens.transcriptome.eoulsan.EoulsanException;

/**
 * This class allow the easily get fields of Illimina reads ids.
 * @author Laurent Jourdren
 */
public class IlluminaReadId {

  private static final Pattern PATTERN = Pattern
      .compile("^([a-zA-Z0-9\\-]+):(\\d+):(\\d+):(\\d+):(\\d+)#(\\d+)/(\\d)$");

  // HWUSI-EAS100R:6:73:941:1973#0/1

  private String instrumentId;
  private int flowCellLane;
  private int tileNumberInFlowCellLane;
  private int xClusterCoordinateInTile;
  private int yClusterCoordinateInTile;
  private int multiplexedSample;
  private int pairMember;

  /**
   * Get instrument id.
   * @return a String with the instrument id
   */
  public String getInstrumentId() {
    return instrumentId;
  }

  /**
   * Get the flowcell lane.
   * @return the flowcell lane
   */
  public int getFlowCellLane() {
    return flowCellLane;
  }

  /**
   * Get the tile number within the flowcell lane.
   * @return the tile number within the flowcell lane
   */
  public int getTileNumberInFlowCellLane() {
    return tileNumberInFlowCellLane;
  }

  /**
   * Get 'x'-coordinate of the cluster within the tile.
   * @return the 'x'-coordinate of the cluster within the tile
   */
  public int getXClusterCoordinateInTile() {
    return xClusterCoordinateInTile;
  }

  /**
   * Get 'y'-coordinate of the cluster within the tile.
   * @return the 'y'-coordinate of the cluster within the tile
   */
  public int getYClusterCoordinateInTile() {
    return yClusterCoordinateInTile;
  }

  /**
   * Get index number for a multiplexed sample.
   * @return index number for a multiplexed sample, 0 for no indexing
   */
  public int getMultiplexedSample() {
    return multiplexedSample;
  }

  /**
   * Get the member of a pair.
   * @return the the member of a pair, /1 or /2 (paired-end or mate-pair reads
   *         only)
   */
  public int getPairMember() {
    return pairMember;
  }

  //
  // Other method
  //

  /**
   * Parse an Illumina id string
   * @param s String with the Illumina id
   * @throws EoulsanException if the id is not an Illumina id
   */
  public void parse(final String s) throws EoulsanException {

    if (s == null)
      throw new NullPointerException("The string to parse is null");

    Matcher m = PATTERN.matcher(s.trim());

    if (!m.lookingAt())
      throw new EoulsanException("Invalid illumina id: " + s);

    this.instrumentId = m.group(1);
    this.flowCellLane = Integer.parseInt(m.group(2));
    this.tileNumberInFlowCellLane = Integer.parseInt(m.group(3));
    this.xClusterCoordinateInTile = Integer.parseInt(m.group(4));
    this.yClusterCoordinateInTile = Integer.parseInt(m.group(5));
    this.multiplexedSample = Integer.parseInt(m.group(6));
    this.pairMember = Integer.parseInt(m.group(7));
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param s String with Illumina id to parse
   * @throws EoulsanException if the id is not an Illumina id
   */
  public IlluminaReadId(final String s) throws EoulsanException {

    parse(s);
  }

}
