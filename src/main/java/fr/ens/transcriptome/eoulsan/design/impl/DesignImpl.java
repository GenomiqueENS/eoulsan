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

package fr.ens.transcriptome.eoulsan.design.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.design.SampleMetadata;
import fr.ens.transcriptome.eoulsan.util.Utils;

/**
 * This class implements the <code>Design</code> interface.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class DesignImpl implements Design {

  private List<String> samplesOrder = new ArrayList<String>();
  private Map<String, Integer> samples = new HashMap<String, Integer>();
  private Map<Integer, String> samplesReverse = new HashMap<Integer, String>();
  private Map<Integer, Integer> ids = new HashMap<Integer, Integer>();

  private Map<String, Integer> metadataFields = new HashMap<String, Integer>();
  private List<String> metadataOrder = new ArrayList<String>();
  private Map<String, String> metadataData = new HashMap<String, String>();

  private int countSamples;
  private int countLabels;

  private String createkeySampleMetadataField(final String sample,
      final String metadataField) {

    final int sampleId = this.samples.get(sample);
    final int fieldId = this.metadataFields.get(metadataField);

    return sampleId + "-" + fieldId;
  }

  /**
   * Get the identifier of a sample
   * @param sampleName Sample name
   * @return the sample identifier
   */
  public int getSampleId(final String sampleName) {

    if (sampleName == null)
      throw new NullPointerException("Sample name is null");

    if (!isSample(sampleName))
      throw new EoulsanRuntimeException("The sample doesn't exists: "
          + sampleName);

    final int sampleId = this.samples.get(sampleName);

    final Integer result = this.ids.get(sampleId);

    if (result == null)
      return -1;

    return result;
  }

  /**
   * Get the sample name from the sample id.
   * @param sampleId Sample identifier
   * @return the name of the sample or null if not exists
   */
  String getSampleName(final int sampleId) {

    return this.samplesReverse.get(sampleId);
  }

  @Override
  public void addMetadataField(final String fieldName) {

    if (fieldName == null)
      throw new EoulsanRuntimeException("The metadata field can't be null");
    if (isMetadataField(fieldName))
      throw new EoulsanRuntimeException(
          "The descriptionLabel name already exists: " + fieldName);

    this.metadataFields.put(fieldName, countLabels++);
    this.metadataOrder.add(fieldName);

  }

  @Override
  public void addSample(final String sampleName) {

    if (sampleName == null)
      throw new EoulsanRuntimeException("Sample name name can't be null");
    if (isSample(sampleName))
      throw new EoulsanRuntimeException("Sample name already exists: "
          + sampleName);

    final int sampleId = countSamples++;
    this.samples.put(sampleName, sampleId);
    this.samplesReverse.put(sampleId, sampleName);
    this.samplesOrder.add(sampleName);

    int id = sampleId + 1;
    while (this.ids.containsValue(id))
      id++;
    this.ids.put(sampleId, id);
  }

  @Override
  public String getMetadata(final String sampleName, final String fieldName) {

    if (sampleName == null)
      throw new NullPointerException("Sample name can't be null");
    if (fieldName == null)
      throw new NullPointerException("Metadata field name can't be null");

    if (!isSample(sampleName))
      throw new EoulsanRuntimeException("The sample name doesn't exists: "
          + sampleName);
    if (!isMetadataField(fieldName))
      throw new EoulsanRuntimeException(
          "The metadata field name doesn't exists: " + fieldName);

    return this.metadataData.get(createkeySampleMetadataField(sampleName,
        fieldName));
  }

  @Override
  public int getMetadataFieldCount() {

    return this.metadataOrder.size();
  }

  @Override
  public List<String> getMetadataFieldsNames() {

    return Collections.unmodifiableList(this.metadataOrder);
  }

  @Override
  public Sample getSample(final int index) {

    final String sampleName = this.samplesOrder.get(index);
    if (sampleName == null)
      throw new EoulsanRuntimeException("The sample index doesn't exists: "
          + index);

    return getSample(sampleName);
  }

  @Override
  public Sample getSample(final String sampleName) {

    if (sampleName == null)
      throw new EoulsanRuntimeException("The sample name can't be null");

    if (!isSample(sampleName))
      throw new EoulsanRuntimeException("The sample doesn't exists: "
          + sampleName);

    final int sampleId = this.samples.get(sampleName);

    return new SampleImpl(this, sampleId);
  }

  @Override
  public int getSampleCount() {

    return this.samples.size();
  }

  @Override
  public SampleMetadata getSampleMetadata(final String sampleName) {

    if (sampleName == null)
      throw new NullPointerException("Sample name is null");

    if (!isSample(sampleName))
      throw new EoulsanRuntimeException("The sample doesn't exists: "
          + sampleName);

    final int id = this.samples.get(sampleName);

    return new SampleMetadataImpl(this, id);

  }

  @Override
  public List<Sample> getSamples() {

    final List<Sample> result = new ArrayList<Sample>();

    final List<String> names = getSamplesNames();

    for (String n : names)
      result.add(getSample(n));

    return Collections.unmodifiableList(result);
  }

  @Override
  public List<String> getSamplesNames() {

    return Collections.unmodifiableList(this.samplesOrder);
  }

  @Override
  public boolean isMetadataField(String fieldName) {

    return this.metadataFields.containsKey(fieldName);
  }

  @Override
  public boolean isSample(final String sampleName) {

    return this.samples.containsKey(sampleName);
  }

  @Override
  public void removeMetadataField(final String fieldName) {

    if (fieldName == null)
      throw new EoulsanRuntimeException(
          "The description field name can't be null");

    if (!isMetadataField(fieldName))
      throw new EoulsanRuntimeException(
          "The description field name doesn't exists");

    // Remove targets
    final String suffix = "-" + this.metadataFields.get(fieldName);

    for (String key : new HashSet<String>(this.metadataData.keySet()))
      if (key.endsWith(suffix))
        this.metadataData.remove(key);

    // Remove entry
    this.metadataFields.remove(fieldName);
    this.metadataOrder.remove(fieldName);
  }

  @Override
  public void removeSample(final String sampleName) {

    if (sampleName == null)
      throw new EoulsanRuntimeException("Sample name can't be null");

    if (!isSample(sampleName))
      throw new EoulsanRuntimeException("the sample name doesn't exists: "
          + sampleName);

    // Remove descriptions
    final String prefixDescritpion = this.samples.get(sampleName) + "-";

    for (String key : new HashSet<String>(this.metadataData.keySet()))
      if (key.startsWith(prefixDescritpion))
        this.metadataData.remove(key);

    // Remove formats and bioassays
    final int sampleId = this.samples.get(sampleName);

    // Remove entry
    this.samples.remove(sampleName);
    this.samplesReverse.remove(sampleId);
    this.samplesOrder.remove(sampleName);
  }

  @Override
  public void renameMetadataField(final String oldMetadataFieldName,
      final String newMetadataFieldName) {

    if (oldMetadataFieldName == null)
      throw new EoulsanRuntimeException("oldName name can't be null");
    if (newMetadataFieldName == null)
      throw new EoulsanRuntimeException("newName name can't be null");

    if (!isMetadataField(oldMetadataFieldName))
      throw new EoulsanRuntimeException("the old label name don't exists: "
          + oldMetadataFieldName);
    if (isMetadataField(newMetadataFieldName))
      throw new EoulsanRuntimeException("the new label name already exists: "
          + newMetadataFieldName);

    int id = this.metadataFields.get(oldMetadataFieldName);
    this.metadataFields.remove(oldMetadataFieldName);
    this.metadataFields.put(newMetadataFieldName, id);

    final int index =
        Collections.binarySearch(this.metadataOrder, oldMetadataFieldName);
    this.metadataOrder.set(index, newMetadataFieldName);
  }

  @Override
  public void renameSample(final String oldSampleName,
      final String newSampleName) {

    if (oldSampleName == null)
      throw new EoulsanRuntimeException("oldName name can't be null");
    if (newSampleName == null)
      throw new EoulsanRuntimeException("newName name can't be null");

    if (!isSample(oldSampleName))
      throw new EoulsanRuntimeException("the old sample name don't exists");
    if (isSample(newSampleName))
      throw new EoulsanRuntimeException("the new sample name already exists");

    int sampleId = this.samples.get(oldSampleName);
    this.samples.remove(oldSampleName);
    this.samples.put(newSampleName, sampleId);
    this.samplesReverse.put(sampleId, newSampleName);

    int index = -1;
    int count = 0;
    for (String s : this.samplesOrder)
      if (s.equals(oldSampleName))
        index = count;
      else
        count++;

    // final int index = Collections.binarySearch(this.samplesOrder,
    // oldName);
    this.samplesOrder.set(index, newSampleName);
  }

  @Override
  public void setMetadata(final String sampleName, final String fieldName,
      final String value) {

    if (sampleName == null)
      throw new NullPointerException("Sample name can't be null");
    if (fieldName == null)
      throw new NullPointerException("Description field can't be null");
    if (value == null)
      throw new NullPointerException("value name can't be null");

    if (!isSample(sampleName))
      throw new EoulsanRuntimeException("The sample name doesn't exists");
    if (!isMetadataField(fieldName))
      addMetadataField(fieldName);

    this.metadataData.put(createkeySampleMetadataField(sampleName, fieldName),
        value);
  }

  /**
   * Set the identifier of a sample
   * @param sampleName Name of the sample
   * @param id identifier of the sample to set
   */
  public void setSampleId(final String sampleName, final int id) {

    if (sampleName == null)
      throw new NullPointerException("Sample name is null");

    if (id <= 0)
      throw new NullPointerException("Sample source is lower or equals to 0");

    if (!isSample(sampleName))
      throw new EoulsanRuntimeException("The sample doesn't exists: "
          + sampleName);

    if (this.ids.containsValue(id))
      throw new EoulsanRuntimeException("The identifier already exists: " + id);

    final int sampleId = this.samples.get(sampleName);

    this.ids.put(sampleId, id);
  }

  @Override
  public int hashCode() {

    return Utils.hashCode(this.samplesOrder, this.samples, this.ids,
        this.metadataFields, this.metadataOrder, this.metadataData,
        this.countSamples, this.countLabels);
  }
}
