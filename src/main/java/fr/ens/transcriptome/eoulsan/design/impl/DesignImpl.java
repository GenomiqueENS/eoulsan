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

package fr.ens.transcriptome.eoulsan.design.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.design.SampleMetadata;
import fr.ens.transcriptome.eoulsan.util.Utils;

public class DesignImpl implements Design {

  private List<String> samplesOrder = new ArrayList<String>();
  private Map<String, Integer> samples = new HashMap<String, Integer>();
  private Map<Integer, String> samplesReverse = new HashMap<Integer, String>();
  private Map<Integer, Integer> ids = new HashMap<Integer, Integer>();

  private Map<Integer, String> sources = new HashMap<Integer, String>();

  private Map<String, Integer> metadataFields = new HashMap<String, Integer>();
  private List<String> metadataOrder = new ArrayList<String>();
  private Map<String, String> metadataData = new HashMap<String, String>();

  private int countSamples;
  private int countLabels;

  private String createkeySampleMetadataField(final String sample,
      final String metadataField) {

    final int slideId = this.samples.get(sample);
    final int fieldId = this.metadataFields.get(metadataField);

    return slideId + "-" + fieldId;
  }

  /**
   * Get the identifier of a sample
   * @param sampleName Sample name
   * @return the sample identifier
   */
  public int getSampleId(final String sampleName) {

    if (sampleName == null)
      throw new NullPointerException("Slide name is null");

    if (!isSample(sampleName))
      throw new EoulsanRuntimeException("The slide doesn't exists");

    final int sampleId = this.samples.get(sampleName);

    final Integer result = this.ids.get(sampleId);

    if (result == null)
      return -1;

    return result;
  }

  /**
   * Get the slide name from the slide id.
   * @param slideId Slide identifier
   * @return the name of the slide or null if not exists
   */
  String getSampleName(final int sampleId) {

    return this.samplesReverse.get(sampleId);
  }

  @Override
  public void addMetadataField(String fieldName) {

    if (fieldName == null)
      throw new EoulsanRuntimeException("The metadata field can't be null");
    if (isMetadataField(fieldName))
      throw new EoulsanRuntimeException(
          "The descriptionLabel name already exists");

    this.metadataFields.put(fieldName, countLabels++);
    this.metadataOrder.add(fieldName);

  }

  @Override
  public void addSample(String sampleName) {

    if (sampleName == null)
      throw new EoulsanRuntimeException("Slide name can't be null");
    if (isSample(sampleName))
      throw new EoulsanRuntimeException("Slide name already exists");

    final int slideId = countSamples++;
    this.samples.put(sampleName, slideId);
    this.samplesReverse.put(slideId, sampleName);
    this.samplesOrder.add(sampleName);

    int id = slideId + 1;
    while (this.ids.containsValue(id))
      id++;
    this.ids.put(slideId, id);
  }

  @Override
  public String getMetadata(String sampleName, String fieldName) {

    if (sampleName == null)
      throw new NullPointerException("Sample name can't be null");
    if (fieldName == null)
      throw new NullPointerException("Metadata field name can't be null");

    if (!isSample(sampleName))
      throw new EoulsanRuntimeException("The sample name doesn't exists");
    if (!isMetadataField(fieldName))
      throw new EoulsanRuntimeException(
          "The metadata field name doesn't exists");

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
  public Sample getSample(int index) {

    final String sampleName = this.samplesOrder.get(index);
    if (sampleName == null)
      throw new EoulsanRuntimeException("The slide index doesn't exists");

    return getSample(sampleName);
  }

  @Override
  public Sample getSample(String sampleName) {

    if (sampleName == null)
      throw new EoulsanRuntimeException("The slide name can't be null");

    if (!isSample(sampleName))
      throw new EoulsanRuntimeException("The slide doesn't exists");

    final int slideId = this.samples.get(sampleName);

    return new SampleImpl(this, slideId);
  }

  @Override
  public int getSampleCount() {

    return this.samples.size();
  }

  @Override
  public SampleMetadata getSampleMetadata(String sampleName) {

    if (sampleName == null)
      throw new NullPointerException("Sample name is null");

    if (!isSample(sampleName))
      throw new EoulsanRuntimeException("The sample doesn't exists");

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
  public String getSource(String sampleName) {

    if (sampleName == null)
      throw new NullPointerException("Slide name is null");

    if (!isSample(sampleName))
      throw new EoulsanRuntimeException("The slide doesn't exists");

    final int id = this.samples.get(sampleName);

    return this.sources.get(id);
  }

  @Override
  public String getSourceInfo(String sampleName) {

    if (sampleName == null)
      throw new NullPointerException("Slide name is null");

    if (!isSample(sampleName))
      throw new EoulsanRuntimeException("The slide doesn't exists");

    final int id = this.samples.get(sampleName);

    String ds = this.sources.get(id);

    return ds == null ? "" : new DataFile(ds).getSource();
  }

  @Override
  public boolean isMetadataField(String fieldName) {

    return this.metadataFields.containsKey(fieldName);
  }

  @Override
  public boolean isSample(String sampleName) {

    return this.samples.containsKey(sampleName);
  }

  @Override
  public void removeMetadataField(String fieldName) {

    if (fieldName == null)
      throw new EoulsanRuntimeException(
          "The description field name can't be null");

    if (!isMetadataField(fieldName))
      throw new EoulsanRuntimeException(
          "The description field name doesn't exists");

    // Remove targets
    final String suffix = "-" + this.metadataFields.get(fieldName);

    for (String key : this.metadataData.keySet())
      if (key.endsWith(suffix))
        this.metadataData.remove(key);

    // Remove entry
    this.metadataFields.remove(fieldName);
    this.metadataOrder.remove(fieldName);
  }

  @Override
  public void removeSample(String sampleName) {

    if (sampleName == null)
      throw new EoulsanRuntimeException("Slide name can't be null");

    if (!isSample(sampleName))
      throw new EoulsanRuntimeException("the slide name doesn't exists");

    // Remove descriptions
    final String prefixDescritpion = this.samples.get(sampleName) + "-";

    for (String key : new HashSet<String>(this.metadataData.keySet()))
      if (key.startsWith(prefixDescritpion))
        this.metadataData.remove(key);

    // Remove formats and bioassays
    final int slideId = this.samples.get(sampleName);

    // Remove entry
    this.samples.remove(sampleName);
    this.samplesReverse.remove(slideId);
    this.samplesOrder.remove(sampleName);
  }

  @Override
  public void renameMetadataField(String oldMetadataFieldName,
      String newMetadataFieldName) {

    if (oldMetadataFieldName == null)
      throw new EoulsanRuntimeException("oldName name can't be null");
    if (newMetadataFieldName == null)
      throw new EoulsanRuntimeException("newName name can't be null");

    if (!isMetadataField(oldMetadataFieldName))
      throw new EoulsanRuntimeException("the old label name don't exists");
    if (isMetadataField(newMetadataFieldName))
      throw new EoulsanRuntimeException("the new label name already exists");

    int id = this.metadataFields.get(oldMetadataFieldName);
    this.metadataFields.remove(oldMetadataFieldName);
    this.metadataFields.put(newMetadataFieldName, id);

    final int index =
        Collections.binarySearch(this.metadataOrder, oldMetadataFieldName);
    this.metadataOrder.set(index, newMetadataFieldName);
  }

  @Override
  public void renameSample(String oldSampleName, String newSampleName) {

    if (oldSampleName == null)
      throw new EoulsanRuntimeException("oldName name can't be null");
    if (newSampleName == null)
      throw new EoulsanRuntimeException("newName name can't be null");

    if (!isSample(oldSampleName))
      throw new EoulsanRuntimeException("the old slide name don't exists");
    if (isSample(newSampleName))
      throw new EoulsanRuntimeException("the new slide name already exists");

    int slideId = this.samples.get(oldSampleName);
    this.samples.remove(oldSampleName);
    this.samples.put(newSampleName, slideId);
    this.samplesReverse.put(slideId, newSampleName);

    int index = -1;
    int count = 0;
    for (String s : this.samplesOrder)
      if (s.equals(oldSampleName))
        index = count;
      else
        count++;

    // final int index = Collections.binarySearch(this.slidesOrder,
    // oldName);
    this.samplesOrder.set(index, newSampleName);
  }

  @Override
  public void setMetadata(String sampleName, String fieldName, String value) {

    if (sampleName == null)
      throw new NullPointerException("Slide name can't be null");
    if (fieldName == null)
      throw new NullPointerException("Description field can't be null");
    if (value == null)
      throw new NullPointerException("value name can't be null");

    if (!isSample(sampleName))
      throw new EoulsanRuntimeException("The slide name doesn't exists");
    if (!isMetadataField(fieldName))
      addMetadataField(fieldName);

    this.metadataData.put(createkeySampleMetadataField(sampleName, fieldName),
        value);
  }

  @Override
  public void setSource(String sampleName, String source) {

    if (sampleName == null)
      throw new NullPointerException("Sample name is null");

    if (source == null)
      throw new NullPointerException("Sample source is null");

    if (!isSample(sampleName))
      throw new EoulsanRuntimeException("The sample doesn't exists");

    final int id = this.samples.get(sampleName);

    this.sources.put(id, source);
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
      throw new EoulsanRuntimeException("The sample doesn't exists");

    if (this.ids.containsValue(id))
      throw new EoulsanRuntimeException("The identifier already exists: " + id);

    final int sampleId = this.samples.get(sampleName);

    this.ids.put(sampleId, id);
  }

  @Override
  public int hashCode() {

    return Utils.hashCode(this.samplesOrder, this.samples, this.ids,
        this.sources, this.metadataFields, this.metadataOrder,
        this.metadataData, this.countSamples, this.countLabels);
  }
}
