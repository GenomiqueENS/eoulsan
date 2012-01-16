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
    return flowCellId;
  }

  /**
   * Get the lane for the sample.
   * @return Returns the lane
   */
  public int getLane() {
    return lane;
  }

  /**
   * Get the sample id.
   * @return Returns the sampleId
   */
  public String getSampleId() {
    return sampleId;
  }

  /**
   * Get the genome reference for the sample.
   * @return Returns the sampleRef
   */
  public String getSampleRef() {
    return sampleRef;
  }

  /**
   * Get the index sequence for the sample.
   * @return Returns the index
   */
  public String getIndex() {
    return index;
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
    return description;
  }

  /**
   * Test if the sample is a control
   * @return Returns the control
   */
  public boolean isControl() {
    return control;
  }

  /**
   * Get the recipe use to make the sample
   * @return Returns the recipe
   */
  public String getRecipe() {
    return recipe;
  }

  /**
   * Get the operator who has made the sample.
   * @return Returns the operator
   */
  public String getOperator() {
    return operator;
  }

  /**
   * Get the name of the project for the sample
   * @return Returns the sampleProject
   */
  public String getSampleProject() {
    return sampleProject;
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
  public void setDescription(String description) {
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

    return this.getClass().getSimpleName()
        + "{sampleId=" + this.sampleId + ", flowCellId=" + this.flowCellId
        + ", lane=" + this.lane + ", sampleRef=" + this.sampleRef + ", index="
        + this.index + ", description=" + this.description + ", control="
        + this.control + ", recipe=" + this.recipe + ", operator="
        + this.operator + ", sampleProject=" + this.sampleProject + "}";
  }

}
