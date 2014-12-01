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

package fr.ens.transcriptome.eoulsan.illumina;

/**
 * This class define a Casava Sample
 * @since 1.1
 * @author Laurent Jourdren
 */
public class CasavaSample {

  private String flowCellId;
  private int lane;
  private String sampleId;
  private String sampleRef;
  private String index;
  private String description;
  private boolean control;
  private String recipe;
  private String operator;
  private String sampleProject;

  //
  // Getters
  //

  /**
   * Get the flow cell id for the sample.
   * @return Returns the flowCellId
   */
  public String getFlowCellId() {
    return this.flowCellId;
  }

  /**
   * Get the lane for the sample.
   * @return Returns the lane
   */
  public int getLane() {
    return this.lane;
  }

  /**
   * Get the sample id.
   * @return Returns the sampleId
   */
  public String getSampleId() {
    return this.sampleId;
  }

  /**
   * Get the genome reference for the sample.
   * @return Returns the sampleRef
   */
  public String getSampleRef() {
    return this.sampleRef;
  }

  /**
   * Get the index sequence for the sample.
   * @return Returns the index
   */
  public String getIndex() {
    return this.index;
  }

  /**
   * Test if the sample is indexed.
   * @return true if the sample is indexed
   */
  public boolean isIndex() {

    return this.index != null && !"".equals(this.index.trim());
  }

  /**
   * Get the description of the sample.
   * @return Returns the description
   */
  public String getDescription() {
    return this.description;
  }

  /**
   * Test if the sample is a control
   * @return Returns the control
   */
  public boolean isControl() {
    return this.control;
  }

  /**
   * Get the recipe use to make the sample
   * @return Returns the recipe
   */
  public String getRecipe() {
    return this.recipe;
  }

  /**
   * Get the operator who has made the sample.
   * @return Returns the operator
   */
  public String getOperator() {
    return this.operator;
  }

  /**
   * Get the name of the project for the sample
   * @return Returns the sampleProject
   */
  public String getSampleProject() {
    return this.sampleProject;
  }

  //
  // Setters
  //

  /**
   * Set the flow cell id for the sample.
   * @param flowCellId The flowCellId to set
   */
  public void setFlowCellId(final String flowCellId) {
    this.flowCellId = flowCellId;
  }

  /**
   * Set the lane of the sample
   * @param lane The lane to set
   */
  public void setLane(final int lane) {
    this.lane = lane;
  }

  /**
   * Set the sample id for the sample.
   * @param sampleId The sampleId to set
   */
  public void setSampleId(final String sampleId) {
    this.sampleId = sampleId;
  }

  /**
   * Set the genome reference for the sample.
   * @param sampleRef The sampleRef to set
   */
  public void setSampleRef(final String sampleRef) {
    this.sampleRef = sampleRef;
  }

  /**
   * Set the index sequence for the sample
   * @param index The index to set
   */
  public void setIndex(final String index) {
    this.index = index;
  }

  /**
   * @param description The description to set
   */
  public void setDescription(final String description) {
    this.description = description;
  }

  /**
   * Set if the sample is a control.
   * @param control The control to set
   */
  public void setControl(final boolean control) {
    this.control = control;
  }

  /**
   * Set the recipe used to make the sample
   * @param recipe The recipe to set
   */
  public void setRecipe(final String recipe) {
    this.recipe = recipe;
  }

  /**
   * Set the operator who has made the sample.
   * @param operator The operator to set
   */
  public void setOperator(final String operator) {
    this.operator = operator;
  }

  /**
   * Set the name of the project for the sample.
   * @param sampleProject The sampleProject to set
   */
  public void setSampleProject(final String sampleProject) {
    this.sampleProject = sampleProject;
  }

  //
  // Other methods
  //

  public String getDemultiplexedFilenamePrefix(final int readNumber) {

    final StringBuilder sb = new StringBuilder();

    sb.append(getSampleId());
    sb.append('_');
    sb.append(getIndex() == null || "".equals(getIndex().trim())
        ? "NoIndex" : getIndex());
    sb.append('_');
    sb.append("L00");
    sb.append(getLane());
    sb.append('_');
    sb.append("R");
    sb.append(readNumber);
    sb.append('_');

    return sb.toString();
  }

  public String getNotDemultiplexedFilenamePrefix(final int readNumber) {

    final StringBuilder sb = new StringBuilder();

    sb.append("lane");
    sb.append(getLane());
    sb.append('_');
    sb.append("Undetermined");
    sb.append('_');
    sb.append("L00");
    sb.append(getLane());
    sb.append('_');
    sb.append("R");
    sb.append(readNumber);
    sb.append('_');

    return sb.toString();
  }

  //
  // Object methods
  //

  @Override
  public String toString() {

    return CasavaSample.class.getName()
        + "{sampleId=" + this.sampleId + ", flowCellId=" + this.flowCellId
        + ", lane=" + this.lane + ", sampleRef=" + this.sampleRef + ", index="
        + this.index + ", description=" + this.description + ", control="
        + this.control + ", recipe=" + this.recipe + ", operator="
        + this.operator + ", sampleProject=" + this.sampleProject + "}";
  }

}
