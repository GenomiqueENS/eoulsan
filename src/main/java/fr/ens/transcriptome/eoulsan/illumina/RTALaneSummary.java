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

package fr.ens.transcriptome.eoulsan.illumina;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class RTALaneSummary {

  private final double densityRatio;

  private int lane = 1;
  private int tileCount = 32;
  private int clustersRaw = 2473686;
  private double clustersRawSD = 212239.4;
  private int clustersPF = 2379034;
  private double clustersPFSD = 197679.6;
  private double prcPFClusters = 96.2;
  private double prcPFClustersSD = 0.30;
  private double phasing = 0.421;
  private double prephasing = 0.224;
  private int calledCyclesMin = 50;
  private int calledCyclesMax = 50;
  private double prcAlign = 99.03;
  private double prcAlignSD = 0.004;
  private double errRatePhiX = 0.15;
  private double errRatePhiXSD = 0.008;
  private double errRate35 = 0.11;
  private double errRate35SD = 0.003;
  private double errRate75 = 0.00;
  private double errRate75SD = 0.000;
  private double errRate100 = 0.00;
  private double errRate100SD = 0.000;
  private int firstCycleIntPF = 9110;
  private double firstCycleIntPFSD = 363.9;
  private double prcIntensityAfter20CyclesPF = 77.5;
  private double prcIntensityAfter20CyclesPFSD = 0.87;

  //
  // Getters
  //

  /**
   * @return Returns the lane
   */
  public int getLane() {
    return lane;
  }

  /**
   * @return Returns the tileCount
   */
  public int getTileCount() {
    return tileCount;
  }

  /**
   * @return Returns the clustersRaw
   */
  public int getClustersRaw() {
    return clustersRaw;
  }

  /**
   * @return Returns the intClustersRawSD
   */
  public double getClustersRawSD() {
    return clustersRawSD;
  }

  /**
   * @return Returns the clustersPF
   */
  public int getClustersPF() {
    return clustersPF;
  }

  /**
   * @return Returns the clustersPFSD
   */
  public double getClustersPFSD() {
    return clustersPFSD;
  }

  /**
   * @return Returns the prcPFClusters
   */
  public double getPrcPFClusters() {
    return prcPFClusters;
  }

  /**
   * @return Returns the prcPFClustersSD
   */
  public double getPrcPFClustersSD() {
    return prcPFClustersSD;
  }

  /**
   * @return Returns the phasing
   */
  public double getPhasing() {
    return phasing;
  }

  /**
   * @return Returns the prephasing
   */
  public double getPrephasing() {
    return prephasing;
  }

  /**
   * @return Returns the calledCyclesMin
   */
  public int getCalledCyclesMin() {
    return calledCyclesMin;
  }

  /**
   * @return Returns the calledCyclesMax
   */
  public int getCalledCyclesMax() {
    return calledCyclesMax;
  }

  /**
   * @return Returns the prcAlign
   */
  public double getPrcAlign() {
    return prcAlign;
  }

  /**
   * @return Returns the prcAlignSD
   */
  public double getPrcAlignSD() {
    return prcAlignSD;
  }

  /**
   * @return Returns the errRatePhiX
   */
  public double getErrRatePhiX() {
    return errRatePhiX;
  }

  /**
   * @return Returns the errRatePhiXSD
   */
  public double getErrRatePhiXSD() {
    return errRatePhiXSD;
  }

  /**
   * @return Returns the errRate35
   */
  public double getErrRate35() {
    return errRate35;
  }

  /**
   * @return Returns the errRate35SD
   */
  public double getErrRate35SD() {
    return errRate35SD;
  }

  /**
   * @return Returns the errRate75
   */
  public double getErrRate75() {
    return errRate75;
  }

  /**
   * @return Returns the errRate75SD
   */
  public double getErrRate75SD() {
    return errRate75SD;
  }

  /**
   * @return Returns the errRate100
   */
  public double getErrRate100() {
    return errRate100;
  }

  /**
   * @return Returns the errRate100SD
   */
  public double getErrRate100SD() {
    return errRate100SD;
  }

  /**
   * @return Returns the firstCycleIntPF
   */
  public int getFirstCycleIntPF() {
    return firstCycleIntPF;
  }

  /**
   * @return Returns the firstCycleIntPFSD
   */
  public double getFirstCycleIntPFSD() {
    return firstCycleIntPFSD;
  }

  /**
   * @return Returns the prcIntensityAfter20CyclesPF
   */
  public double getPrcIntensityAfter20CyclesPF() {
    return prcIntensityAfter20CyclesPF;
  }

  /**
   * @return Returns the prcIntensityAfter20CyclesPFSD
   */
  public double getPrcIntensityAfter20CyclesPFSD() {
    return prcIntensityAfter20CyclesPFSD;
  }

  //
  // Parsing methods
  //

  public void parse(final Element e) {

    final NamedNodeMap map = e.getAttributes();

    for (int i = 0; i < map.getLength(); i++) {

      final Node attribute = map.item(i);
      final String key = attribute.getNodeName();
      final String value = e.getAttribute(key);

      if ("key".equals(key))
        this.lane = Integer.parseInt(value);
      else if ("TileCount".equals(key))
        this.tileCount = Integer.parseInt(value);
      else if ("ClustersRaw".equals(key))
        this.clustersRaw = Integer.parseInt(value);
      else if ("ClustersRawSD".equals(key))
        this.clustersRawSD = Double.parseDouble(value);
      else if ("ClustersPF".equals(key))
        this.clustersPF = Integer.parseInt(value);
      else if ("ClustersPFSD".equals(key))
        this.clustersPFSD = Double.parseDouble(value);
      else if ("PrcPFClusters".equals(key))
        this.prcPFClusters = Double.parseDouble(value);
      else if ("PrcPFClustersSD".equals(key))
        this.prcPFClustersSD = Double.parseDouble(value);
      else if ("Phasing".equals(key))
        this.phasing = Double.parseDouble(value);
      else if ("Prephasing".equals(key))
        this.prephasing = Double.parseDouble(value);
      else if ("CalledCyclesMin".equals(key))
        this.calledCyclesMin = Integer.parseInt(value);
      else if ("CalledCyclesMax".equals(key))
        this.calledCyclesMax = Integer.parseInt(value);
      else if ("PrcAlign".equals(key))
        this.prcAlign = Double.parseDouble(value);
      else if ("PrcAlignSD".equals(key))
        this.prcAlignSD = Double.parseDouble(value);
      else if ("ErrRatePhiX".equals(key))
        this.errRatePhiX = Double.parseDouble(value);
      else if ("ErrRatePhiXSD".equals(key))
        this.errRatePhiXSD = Double.parseDouble(value);
      else if ("ErrRate35".equals(key))
        this.errRate35 = Double.parseDouble(value);
      else if ("ErrRate35SD".equals(key))
        this.errRate35SD = Double.parseDouble(value);
      else if ("ErrRate75".equals(key))
        this.errRate75 = Double.parseDouble(value);
      else if ("ErrRate75SD".equals(key))
        this.errRate75SD = Double.parseDouble(value);
      else if ("ErrRate100".equals(key))
        this.errRate100 = Double.parseDouble(value);
      else if ("ErrRate100SD".equals(key))
        this.errRate100SD = Double.parseDouble(value);
      else if ("FirstCycleIntPF".equals(key))
        this.firstCycleIntPF = Integer.parseInt(value);
      else if ("FirstCycleIntPFSD".equals(key))
        this.firstCycleIntPFSD = Double.parseDouble(value);
      else if ("PrcIntensityAfter20CyclesPF".equals(key))
        this.prcIntensityAfter20CyclesPF = Double.parseDouble(value);
      else if ("PrcIntensityAfter20CyclesPFSD".equals(key))
        this.prcIntensityAfter20CyclesPFSD = Double.parseDouble(value);
    }

  }

  //
  // Object methods
  //

  public String toHeaderString() {

    return "Lane\tTiles\tClu.Dens. (#/mm<sup>2</sup>)\t"
        + "% PF Clusters\tClusters PF (#/mm<sup>2</sup>)\t% Phas./Preph.\t"
        + "Cycles Err Rated\t% Aligned\t% Error Rate\t"
        + "% Error Rate 35 cycle\t% Error Rate 75 cycle\t% Error Rate 100 cycle\t1st Cycle Int\t"
        + "% Intensity Cycle 20";
  }

  public String toString() {

    return String
        .format(
            "%d\t%d\t%.0fK +/- %.1fK\t%.1f +/- %.2f\t%.1f +/- %.2f\t%.3f / %.3f\t%d%s\t%.2f +/- %.3f\t"
                + "%.2f +/- %.3f\t%.2f +/- %.3f\t%.2f +/- %.3f\t%.2f +/- %.3f\t"
                + "%d +/- %.1f\t%.1f +/- %.2f", this.lane, this.tileCount,
            this.clustersRaw * this.densityRatio / 1000.0, this.clustersRawSD
                * this.densityRatio / 1000, this.prcPFClusters,
            this.prcPFClustersSD, this.clustersPF * this.densityRatio / 1000.0,
            this.clustersPFSD * this.densityRatio / 1000.0, this.phasing,
            this.prephasing, this.calledCyclesMin,
            this.calledCyclesMax > this.calledCyclesMin ? "-"
                + this.calledCyclesMax : "", this.prcAlign, this.prcAlignSD,
            this.errRatePhiX, this.errRatePhiXSD, this.errRate35,
            this.errRate35SD, this.errRate75, this.errRate75SD,
            this.errRate100, this.errRate100, this.firstCycleIntPF,
            this.firstCycleIntPFSD, this.prcIntensityAfter20CyclesPF,
            this.prcIntensityAfter20CyclesPFSD);
  }

  //
  // Constructor
  //

  public RTALaneSummary(double densityRatio) {
    this.densityRatio = densityRatio;
  }

}
