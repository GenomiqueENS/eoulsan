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

package fr.ens.transcriptome.eoulsan.util.hadoop;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RunningJob;
import org.apache.hadoop.mapreduce.Job;


/**
 * This class provide utility methods to run map reduce jobs.
 * @since 1.0
 * @author Laurent Jourdren
 */
@SuppressWarnings("deprecation")
public class MapReduceUtils {

  /**
   * Move the content of the result of a map reduce into a new place
   * @param srcDirPath path of the output directory of map reduce
   * @param destPath new path of the result
   * @param overwrite true if must overwrite existing file
   * @param conf Configuration
   * @throws IOException if an error occurs while moving file
   */
  public static boolean moveMapredOutput(final Path srcDirPath,
      final Path destPath, final boolean overwrite, final Configuration conf)
      throws IOException {

    final List<Path> paths =
        PathUtils.listPathsByPrefix(srcDirPath, "part-", conf);

    if (paths == null)
      throw new NullPointerException("The list of output path is null");

    if (paths.size() == 0)
      return false;

    if (paths.size() == 1) {

      final Path p = paths.get(0);
      final FileSystem srcFs = p.getFileSystem(conf);
      final FileSystem destFs = destPath.getFileSystem(conf);

      if (destFs.exists(destPath))
        if (overwrite)
          destFs.delete(destPath, false);
        else
          return false;

      return srcFs.rename(p, destPath)
          && PathUtils.fullyDelete(srcDirPath, conf);
    }

    return PathUtils.concat(
        PathUtils.listPathsByPrefix(srcDirPath, "part-r-", conf), destPath,
        true, overwrite, conf) && PathUtils.fullyDelete(srcDirPath, conf);
  }

  /**
   * Submit a collection of jobs and get running jobs.
   * @param jobconfs Collection of jobs to submit
   * @return a collection of running jobs
   * @throws IOException if an IO error occurs while creating jobs
   */
  public static Collection<RunningJob> submitJobConfs(
      final Collection<JobConf> jobconfs) throws IOException {

    if (jobconfs == null)
      throw new NullPointerException("The list of jobs is null");

    final Set<RunningJob> jobs = new HashSet<RunningJob>();
    JobClient jc = null;

    for (JobConf jconf : jobconfs) {
      if (jconf == null)
        continue;
      if (jc == null)
        jc = new JobClient(jconf);
      jobs.add(jc.submitJob(jconf));
    }

    return jobs;
  }

  /**
   * Submit a collection of jobs and wait the completion of all jobs.
   * @param jobconfs Collection of jobs to submit
   * @param waitTimeInMillis waiting time between 2 checks of the completion of
   *          jobs
   * @param counterGroup the counter group
   * @throws IOException if an IO error occurs while waiting for jobs
   * @throws InterruptedException if an error occurs while waiting for jobs
   * @throws ClassNotFoundException if a class needed for map reduce execution
   *           is not found
   */
  public static HadoopJobsResults submitAndWaitForRunningJobs(
      final Collection<JobConf> jobconfs, final int waitTimeInMillis,
      final String counterGroup) throws IOException, InterruptedException,
      ClassNotFoundException {

    return new OldAPIJobsResults(submitJobConfs(jobconfs), waitTimeInMillis,
        counterGroup);
  }

  /**
   * Submit a collection of jobs and wait the completion of all jobs.
   * @param jobs Collection of jobs to submit
   * @param waitTimeInMillis waiting time between 2 checks of the completion of
   *          jobs
   * @param counterGroup the counter group
   * @throws IOException if an IO error occurs while waiting for jobs
   * @throws InterruptedException if an error occurs while waiting for jobs
   * @throws ClassNotFoundException if a class needed for map reduce execution
   *           is not found
   */
  public static HadoopJobsResults submitAndWaitForJobs(final Collection<Job> jobs,
      final int waitTimeInMillis, final String counterGroup)
      throws IOException, InterruptedException, ClassNotFoundException {

    if (jobs == null)
      throw new NullPointerException("The list of jobs is null");

    for (Job j : jobs)
      j.submit();

    return new NewAPIJobsResults(jobs, waitTimeInMillis, counterGroup);
  }

  //
  // Constructor
  //

  private MapReduceUtils() {
  }

}
