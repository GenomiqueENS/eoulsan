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

package fr.ens.transcriptome.eoulsan.actions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.Counters;

import com.google.common.collect.Maps;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.HadoopEoulsanRuntime;
import fr.ens.transcriptome.eoulsan.LocalEoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Settings;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormatConverter;
import fr.ens.transcriptome.eoulsan.io.ProgressCounterOutputStream;
import fr.ens.transcriptome.eoulsan.steps.mgmt.DistDataFileCopy;
import fr.ens.transcriptome.eoulsan.steps.mgmt.upload.DataFileDistCp;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

public class DistCpBenchmark extends AbstractAction {

  @Override
  public String getDescription() {

    return "DistCp benchmark";
  }

  @Override
  public String getName() {

    return "distcpbenchmark";
  }

  @Override
  public boolean isHadoopJarMode() {

    return true;
  }

  @Override
  public boolean isHidden() {

    return true;
  }

  /**
   * Configure the application.
   * @return a Hadoop configuration object
   * @throws EoulsanException if an error occurs while reading settings
   */
  private static Configuration init() throws EoulsanException {

    try {
      // Create and load settings
      final Settings settings = new Settings();

      // Create Hadoop configuration object
      final Configuration conf =
          CommonHadoop.createConfigurationFromSettings(settings);

      // Initialize runtime
      HadoopEoulsanRuntime.newEoulsanRuntime(settings, conf);

      return conf;
    } catch (IOException e) {
      throw new EoulsanException("Error while reading settings: "
          + e.getMessage());
    }
  }

  @Override
  public void action(String[] arguments) {

    try {

      // Initialize The application
      final Configuration conf = init();

      DistDataFileCopy.main(conf);

      if (true)
        return;

      final DataFile src1 =
          new DataFile(
              "ftp://leburon:norubel@idunn.ens.fr/import/mimir06/html/leburon/soap_index_1.zip");
      final DataFile dest1 =
          new DataFile("hdfs://skadi.ens.fr/user/jourdren/soap_index_1.zip");

      copy(src1, dest1, conf);

      final DataFile src2 =
          new DataFile(
              "ftp://leburon:norubel@idunn.ens.fr/import/mimir06/html/leburon/Ant5.fq.bz2");
      final DataFile dest2 =
          new DataFile("hdfs://skadi.ens.fr/user/jourdren/Ant5.tfq");

      copy(src2, dest2, conf);

    } catch (FileNotFoundException e) {

      Common.errorExit(e, "File not found: " + e.getMessage());

    } catch (EoulsanException e) {

      Common.errorExit(e, "Error while executing "
          + Globals.APP_NAME_LOWER_CASE + ": " + e.getMessage());

    } catch (IOException e) {

      Common.errorExit(e, "Error: " + e.getMessage());

    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  private void copy(final DataFile src, final DataFile dest,
      final Configuration conf) throws IOException {

    final Path destPath = new Path(dest.toString());
    final FileSystem fs = destPath.getFileSystem(conf);

    if (fs.isFile(destPath)) {
      fs.delete(destPath, false);
    }

    final Path jobPath = new Path("hdfs://skadi.ens.fr/user/jourdren/toto");

    final DataFileDistCp dscp = new DataFileDistCp(conf, jobPath);

    final Map<DataFile, DataFile> entries = Maps.newHashMap();
    entries.put(src, dest);

    final long startTime = System.currentTimeMillis();
    dscp.copy(entries);
    final long endTime = System.currentTimeMillis();

    final long diff = endTime - startTime;

    System.out.println("Copy of "
        + src.getName() + ", Time: " + diff + "("
        + StringUtils.toTimeHumanReadable(diff) + ")");

    fs.delete(jobPath, true);

  }

  public static void main(String[] args) throws IOException, EoulsanException {

    // Initialize the runtime
    LocalEoulsanRuntime.newEoulsanRuntime(new Settings());

    final File baseDir = new File("/home/jourdren/tmp/distcp");

    DataFile src1 =
        new DataFile(new File(baseDir, "soap_index_1.zip").toString());
    DataFile dest1 =
        new DataFile(new File(baseDir, "new_soap_index_1.zip").toString());

    DataFile src2 = new DataFile(new File(baseDir, "Ant5.fq.bz2").toString());
    DataFile dest2 = new DataFile(new File(baseDir, "Ant5.tfq").toString());

    long time1 = copyStd(src1, dest1);

    System.out.println("Copy std of "
        + src1.getName() + ", Time: " + time1 + "("
        + StringUtils.toTimeHumanReadable(time1) + ")");

    long time3 = copyOpimized(src1, dest1);

    System.out.println("Copy std of "
        + src1.getName() + ", Time: " + time3 + "("
        + StringUtils.toTimeHumanReadable(time3) + ")");

    long time2 = copyStd(src2, dest2);

    System.out.println("Copy std of "
        + src2.getName() + ", Time: " + time2 + "("
        + StringUtils.toTimeHumanReadable(time2) + ")");

    long time4 = copyOpimized(src2, dest2);

    System.out.println("Copy std of "
        + src2.getName() + ", Time: " + time4 + "("
        + StringUtils.toTimeHumanReadable(time4) + ")");

  }

  private static long copyStd(DataFile src, final DataFile dest)
      throws IOException {
    final long startTime = System.currentTimeMillis();
    new DataFormatConverter(src, dest).convert();
    final long endTime = System.currentTimeMillis();

    return endTime - startTime;
  }

  private static long copyOpimized(DataFile src, final DataFile dest)
      throws IOException {
    final Counter counter = new Counter() {

      long counter = 0;

      @Override
      public synchronized void increment(long incr) {
        counter += incr;
        System.out.println("count: " + counter);
      }
    };

    // Add a progress counter to output stream
    final OutputStream os =
        new ProgressCounterOutputStream(dest.create(), counter);

    final long startTime = System.currentTimeMillis();

    // Copy the file
    new DataFormatConverter(src, dest, os).convert();

    final long endTime = System.currentTimeMillis();

    return endTime - startTime;
  }

}
