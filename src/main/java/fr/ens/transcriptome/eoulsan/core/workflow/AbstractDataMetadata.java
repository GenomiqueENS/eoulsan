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

package fr.ens.transcriptome.eoulsan.core.workflow;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;

import fr.ens.transcriptome.eoulsan.bio.FastqFormat;
import fr.ens.transcriptome.eoulsan.data.DataMetadata;

/**
 * This class define an abstract class for metadata of data objects.
 * @since 2.0
 * @author Laurent Jourdren
 */
abstract class AbstractDataMetadata implements DataMetadata, Serializable {

  private static final long serialVersionUID = -6869029697238897073L;

  @Override
  public boolean isPairedEnd() {

    if (!containsKey(PAIRED_END_KEY)) {
      return false;
    }

    return Boolean.parseBoolean(get(PAIRED_END_KEY));
  }

  @Override
  public void setPairedEnd(boolean pairedEnd) {

    set(PAIRED_END_KEY, Boolean.toString(pairedEnd));
  }

  @Override
  public FastqFormat getFastqFormat() {

    return getFastqFormat(FastqFormat.FASTQ_SANGER);
  }

  @Override
  public FastqFormat getFastqFormat(FastqFormat defaultValue) {

    if (!containsKey(FASTQ_FORMAT_KEY)) {
      return defaultValue;
    }

    final FastqFormat result =
        FastqFormat.getFormatFromName(get(FASTQ_FORMAT_KEY));

    return result == null ? defaultValue : result;
  }

  @Override
  public void setFastqFormat(FastqFormat fastqFormat) {

    checkNotNull(fastqFormat, "fastqFormat argument cannot be null");

    set(FASTQ_FORMAT_KEY, fastqFormat.getName());
  }

  @Override
  public String getSampleName() {

    return get(SAMPLE_NAME_KEY);
  }

  @Override
  public void setSampleName(String sampleName) {

    checkNotNull(sampleName, "sampleName argument cannot be null");

    set(FASTQ_FORMAT_KEY, sampleName);
  }

  @Override
  public int getSampleId() {

    if (!containsKey(SAMPLE_ID_KEY)) {
      return -1;
    }

    try {
      return Integer.parseInt(get(SAMPLE_ID_KEY));
    } catch (NumberFormatException e) {
      return -1;
    }

  }

  @Override
  public void setSampleId(int sampleId) {

    checkArgument(sampleId > 0, "sampleId argument must be greater than 0: "
        + sampleId);

    set(SAMPLE_ID_KEY, Integer.toString(sampleId));
  }

}
