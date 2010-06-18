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

package fr.ens.transcriptome.eoulsan.util;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RunningJob;
import org.apache.hadoop.mapred.Counters.Counter;
import org.apache.hadoop.mapred.Counters.Group;

/**
 * This class provide utility methods to run map reduce jobs.
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
      final FileSystem srcFs = PathUtils.getFileSystem(p, conf);
      final FileSystem destFs = PathUtils.getFileSystem(destPath, conf);

      if (destFs.exists(destPath))
        if (overwrite)
          destFs.delete(destPath, false);
        else
          return false;

      return srcFs.rename(p, destPath)
          && PathUtils.fullyDelete(srcDirPath, conf);
    }

    return PathUtils.concat(PathUtils.listPathsByPrefix(srcDirPath, "part-r-",
        conf), destPath, true, overwrite, conf)
        && PathUtils.fullyDelete(srcDirPath, conf);
  }

  /**
   * Submit a collection of jobs and get running jobs.
   * @param jobconfs Collection of jobs to submit
   * @return a collection of running jobs
   * @throws IOException if an IO error occurs while creating jobs
   */
  public static Collection<RunningJob> submitJobs(
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
   * @param jobs Collection of jobs to submit
   * @param waitTimeInMillis waiting time between 2 checks of the completion of
   *          jobs
   * @throws IOException if an IO error occurs while waiting for jobs
   * @throws InterruptedException if an error occurs while waiting for jobs
   * @throws ClassNotFoundException if a class needed for map reduce execution
   *           is not found
   */
  public static String submitAndWaitForRunningJobs(final Collection<JobConf> jobconfs,
      final int waitTimeInMillis, final String counterGroup)
      throws IOException, InterruptedException, ClassNotFoundException {

    return waitForRunningJobs(submitJobs(jobconfs), waitTimeInMillis, counterGroup);
  }

  /**
   * Wait the completion of a collection of jobs.
   * @param jobs Collection of jobs to submit
   * @param waitTimeInMillis waiting time between 2 checks of the completion of
   *          jobs
   * @throws IOException if an IO error occurs while waiting for jobs
   * @throws InterruptedException if an error occurs while waiting for jobs
   * @throws ClassNotFoundException if a class needed for map reduce execution
   *           is not found
   */
  public static String waitForRunningJobs(final Collection<RunningJob> jobs,
      final int waitTimeInMillis, final String counterGroup)
      throws InterruptedException, IOException {

    if (jobs == null)
      throw new NullPointerException("The list of jobs is null");

    final int totalJobs = jobs.size();
    int completedJobs = 0;

    final StringBuilder sb = new StringBuilder();
    final Set<RunningJob> completedJobsSet = new HashSet<RunningJob>();

    while (completedJobs != totalJobs) {

      Thread.sleep(waitTimeInMillis);
      completedJobs = 0;
      for (RunningJob j : jobs) {
        if (j.isComplete()) {
          completedJobs++;

          if (counterGroup != null && !completedJobsSet.contains(j)) {
            sb.append(j.getJobName());
            sb.append('\n');

            final Group g = j.getCounters().getGroup(counterGroup);
            if (g != null) {
              final Iterator<Counter> it = g.iterator();
              while (it.hasNext()) {

                Counter counter = it.next();
                sb.append('\t');
                sb.append(counter.getName());
                sb.append('=');
                sb.append(counter.getValue());
                sb.append('\n');

              }

            }

          }

          completedJobsSet.add(j);
        }

      }
    }
    return sb.toString();
  }


  
  //
  // Constructor
  //

  private MapReduceUtils() {
  }

}
