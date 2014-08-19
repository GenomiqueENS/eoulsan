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

package fr.ens.transcriptome.eoulsan.bio.readsmappers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

public class MapperProcessDemo {

  //
  // Main method
  //

  private static final String EXEC_PATH =
      "/home/jourdren/workspace/eoulsan/src/main/java/files/linux/i386/bowtie";
  private static final String INDEX_PATH =
      "/home/jourdren/test-small/bowtie_index_1/genome";
  private static final int THREAD_NUMBER = 1;

  public static final void main(String[] args) throws IOException,
      InterruptedException {
    final File f = new File("/home/jourdren/test-small/filtered_reads_1a.fq");

    final Stopwatch sw = new Stopwatch();
    sw.start();

    // Use file
    final MapperProcess mp1 =
        new MapperProcess(new BowtieReadsMapper(), true, false, false) {

          @Override
          protected List<List<String>> createCommandLines() {

            final List<String> cmd = new ArrayList<String>();

            cmd.add(EXEC_PATH);
            cmd.add("--phred64-quals");
            cmd.add("-p");
            cmd.add("" + THREAD_NUMBER);
            cmd.add(INDEX_PATH);
            cmd.add("-q");
            cmd.add(f.getAbsolutePath());
            cmd.add("-S");

            return Collections.singletonList(cmd);
          }
        };

    FileUtils.copy(mp1.getStout(), new FileOutputStream(
        "/home/jourdren/toto1.sam"));
    mp1.waitFor();

    sw.stop();
    System.out.println(sw.elapsedTime(TimeUnit.SECONDS));

    sw.reset();
    sw.start();

    // stdin
    final MapperProcess mp2 =
        new MapperProcess(new BowtieReadsMapper(), false, true, false) {

          @Override
          protected List<List<String>> createCommandLines() {

            final List<String> cmd = new ArrayList<String>();

            cmd.add(EXEC_PATH);
            cmd.add("--phred64-quals");
            cmd.add("-p");
            cmd.add("" + THREAD_NUMBER);
            cmd.add(INDEX_PATH);
            cmd.add("-q");
            cmd.add("-");
            cmd.add("-S");
            return Collections.singletonList(cmd);
          }
        };

    mp2.toFile(new File("/home/jourdren/toto2.sam"));
    FileUtils.copy(new FileInputStream(f), mp2.getStdin());
    mp2.waitFor();

    sw.stop();
    System.out.println(sw.elapsedTime(TimeUnit.SECONDS));

    sw.reset();
    sw.start();

    // tmp input file
    final MapperProcess mp3 =
        new MapperProcess(new BowtieReadsMapper(), false, false, false) {

          @Override
          protected List<List<String>> createCommandLines() {

            final List<String> cmd = new ArrayList<String>();

            cmd.add(EXEC_PATH);
            cmd.add("--phred64-quals");
            cmd.add("-p");
            cmd.add("" + THREAD_NUMBER);
            cmd.add(INDEX_PATH);
            cmd.add("-q");
            cmd.add(getTmpInputFile1().getAbsolutePath());
            cmd.add("-S");
            return Collections.singletonList(cmd);
          }
        };

    FileUtils.copy(new FileInputStream(f), mp3.getStdin());
    mp3.toFile(new File("/home/jourdren/toto3.sam"));
    mp3.waitFor();

    sw.stop();
    System.out.println(sw.elapsedTime(TimeUnit.SECONDS));

  }

}
