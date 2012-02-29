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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.bio.GFFEntry;
import fr.ens.transcriptome.eoulsan.bio.GenomicArray;
import fr.ens.transcriptome.eoulsan.bio.GenomicInterval;
import fr.ens.transcriptome.eoulsan.bio.io.GFFReader;
import fr.ens.transcriptome.eoulsan.util.Utils;

public class HTSeqCount {

  public static void countReadsInFeatures(final File samFile,
      final File gffFile, final boolean stranded, final String overlapMode,
      final String featureType, final String attributeId, final boolean quiet,
      final int minAvarageQual, final File samOutFile) throws EoulsanException,
      IOException, BadBioEntryException {

    // features = HTSeq.GenomicArrayOfSets( "auto", stranded != "no" )
    final GenomicArray<String> features = new GenomicArray<String>();
    final Map<String, Integer> counts = Utils.newHashMap();

    final GFFReader gffReader = new GFFReader(gffFile);

    for (final GFFEntry gff : gffReader) {

      if (featureType.equals(gff.getType())) {

        final String featureId = gff.getAttributeValue(attributeId);
        if (featureId == null)
          throw new EoulsanException("Feature "
              + featureType + " does not contains a " + attributeId
              + " attribute");

        if (!stranded && ".".equals(gff.getStrand()))
          throw new EoulsanException(
              "Feature "
                  + featureType
                  + " does not have strand information butyou are running htseq-count in stranded mode.");

        // features[ f.iv ] += feature_id
        features.addExon(new GenomicInterval(gff, stranded), featureId);
        counts.put(attributeId, 0);
      }
    }
    gffReader.throwException();

    // Writer writer = new
    // FileWriter("/home/jourdren/home-net/htseq-count/java.out");
    // writer.write(features.print());
    // System.out.println(features.print());
    // writer.close();

    if (counts.size() == 0)
      throw new EoulsanException("Warning: No features of type '"
          + featureType + "' found.\n");

    int empty = 0;
    int ambiguous = 0;
    int notaligned = 0;
    int lowqual = 0;
    int nonunique = 0;
    int i = 0;

    boolean pairedEnd = false;

    final SAMFileReader inputSam = new SAMFileReader(samFile);

    for (final SAMRecord samRecord : inputSam) {
      i++;

      final List<GenomicInterval> ivSeq = new ArrayList<GenomicInterval>();

      if (!pairedEnd) {

        if (samRecord.getReadUnmappedFlag()) {
          notaligned++;
          // write_to_samout( r, "not_aligned" )
          continue;
        }

        if (samRecord.getIntegerAttribute("NH") > 1) {
          nonunique++;
          // write_to_samout( r, "alignment_not_unique" )
          continue;
        }

        if (samRecord.getMappingQuality() < minAvarageQual) {
          lowqual++;
          // write_to_samout( r, "too_low_aQual" )
          continue;
        }

        if ("reverse".equals(stranded)) {

          // iv_seq = ( co.ref_iv for co in r.cigar if co.type == "M" )
          for (CigarElement ce : samRecord.getCigar().getCigarElements()) {

            if (ce.getOperator() == CigarOperator.M) {
            }
          }

        } else {
          // iv_seq = ( invert_strand( co.ref_iv ) for co in r.cigar if co.type
          // == "M" )
        }

      } else {
        // Paired-end
      }

      final Set<String> fs = new HashSet<String>();
      if (overlapMode.equals("union")) {

        for (final GenomicInterval iv : ivSeq) {

          final String chr = iv.getChromosome();

          if (!features.containsChromosome(chr))
            throw new EoulsanException("Unknown chromosome: " + chr);
          // TODO this is an union: fs = fs.union( fs2 )
          fs.addAll(features.findExons(chr, iv.getStart(), iv.getEnd())
              .values());
        }

      } else {
        // elif overlap_mode =="intersection-strict" or overlap_mode ==
        // "intersection-nonempty":
      }

      switch (fs.size()) {
      case 0:
        empty++;
        // write_to_samout( r, "no_feature" )
        break;

      case 1:
        // write_to_samout( r, list(fs)[0] )
        final String id = fs.iterator().next();
        counts.put(id, counts.get(id) + 1);
        break;

      default:
        ambiguous++;
        // write_to_samout( r, "ambiguous[" + '+'.join( fs ) + "]" )
        break;
      }

    }

    inputSam.close();

    for (Map.Entry<String, Integer> e : counts.entrySet())
      System.out.println(e.getKey() + "\t" + e.getValue());

    System.out.printf("no_feature\t%d", empty);
    System.out.printf("ambiguous\t%d", ambiguous);
    System.out.printf("too_low_aQual\t%d", lowqual);
    System.out.printf("not_aligned\t%d", notaligned);
    System.out.printf("alignment_not_unique\t%d", nonunique);
  }

  public static void main(String[] args) throws EoulsanException, IOException,
      BadBioEntryException {

    final File dir = new File("/home/jourdren/home-net/htseq-count");
    final File samFile = new File(dir, "filtered_mapper_results_1.sam");
    final File gffFile = new File(dir, "mouse.gff");

    System.out.println("start.");
    countReadsInFeatures(samFile, gffFile, false, "union", "gene", "ID", false,
        0, null);
    System.out.println("end.");

  }

}
