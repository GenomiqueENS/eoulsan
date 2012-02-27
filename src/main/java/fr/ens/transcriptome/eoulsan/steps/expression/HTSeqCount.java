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

package fr.ens.transcriptome.eoulsan.steps.expression;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.bio.GFFEntry;
import fr.ens.transcriptome.eoulsan.bio.GenomicArray;
import fr.ens.transcriptome.eoulsan.bio.GenomicInterval;
import fr.ens.transcriptome.eoulsan.bio.io.GFFReader;
import fr.ens.transcriptome.eoulsan.util.Utils;

public class HTSeqCount {

  public void countReadsInFeatures(final File samFile, final File gffFile,
      final boolean stranded, final String overlapMode,
      final String FeatureType, final String attributeId, final boolean quiet,
      final int minAvarageQual, final File samOutFile) throws EoulsanException,
      IOException, BadBioEntryException {

    // features = HTSeq.GenomicArrayOfSets( "auto", stranded != "no" )
    final GenomicArray<String> features = new GenomicArray<String>();
    final Map<String, Integer> counts = Utils.newHashMap();

    final GFFReader gffReader = new GFFReader(gffFile);

    for (final GFFEntry gff : gffReader) {

      if (FeatureType.equals(gff.getType())) {

        final String featureId = gff.getAttributeValue(attributeId);
        if (featureId == null)
          throw new EoulsanException("Feature "
              + FeatureType + " does not contains a " + attributeId
              + " attribute");

        if (!stranded && ".".equals(gff.getStrand()))
          throw new EoulsanException(
              "Feature "
                  + FeatureType
                  + " does not have strand information butyou are running htseq-count in stranded mode.");

        // features[ f.iv ] += feature_id
        features.addExon(new GenomicInterval(gff, stranded), featureId);
        counts.put(attributeId, 0);
      }
    }
    gffReader.throwException();

  }
}
