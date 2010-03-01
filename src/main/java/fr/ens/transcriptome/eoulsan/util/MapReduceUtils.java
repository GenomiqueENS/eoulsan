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
import java.util.List;
import java.util.UUID;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;

public final class MapReduceUtils {

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

    return PathUtils.concat(PathUtils.listPathsByPrefix(srcDirPath, "part-",
        conf), destPath, true, overwrite, conf)
        && PathUtils.fullyDelete(srcDirPath, conf);

    // if (srcDirPath == null)
    // throw new NullPointerException("Source directory is null");
    //
    // if (destPath == null)
    // throw new NullPointerException("Destination path is null");
    //
    // final FileSystem srcFs = PathUtils.getFileSystem(srcDirPath, conf);
    // final FileSystem destFs = PathUtils.getFileSystem(srcDirPath, conf);
    //
    // if (!srcFs.exists(srcDirPath))
    // throw new IOException("Source directory does not exists: " + srcDirPath);
    //
    // if (!srcFs.getFileStatus(srcDirPath).isDir())
    // throw new IOException("Source path is not a directory: " + srcDirPath);
    //
    // if (!overwrite && destFs.exists(destPath))
    // throw new IOException("The destination path already exists: " +
    // destPath);
    //
    // if (destFs.exists(destPath))
    // destFs.delete(destPath, false);
    //
    // final FileStatus[] filesstatus =
    // srcFs.listStatus(srcDirPath, new PathUtils.PrefixPathFilter("part-"));
    //
    // final Path tmpSrcDirPath =
    // PathUtils.createTempPath(srcDirPath, "", "", conf);
    // srcFs.mkdirs(tmpSrcDirPath);
    //
    // if (filesstatus.length == 0)
    // throw new IOException("The source directory is empty: " + srcDirPath);
    //
    // List<Path> paths = PathUtils.listPathsByPrefix(srcDirPath, "part-",
    // conf);
    // for (Path p : paths)
    // srcFs.rename(p, tmpSrcDirPath);
    //
    // PathUtils.copyMerge(srcDirPath, destPath, true, conf);
    //
    // srcFs.delete(srcDirPath, true);
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
  public static void submitandWaitForJobs(final Collection<Job> jobs,
      final int waitTimeInMillis) throws IOException, InterruptedException,
      ClassNotFoundException {

    if (jobs == null)
      throw new NullPointerException("The list of jobs is null");

    for (Job j : jobs)
      j.submit();

    waitForJobs(jobs, waitTimeInMillis);
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
  public static void waitForJobs(final Collection<Job> jobs,
      final int waitTimeInMillis) throws InterruptedException, IOException {

    if (jobs == null)
      throw new NullPointerException("The list of jobs is null");

    final int totalJobs = jobs.size();
    int completedJobs = 0;

    while (completedJobs != totalJobs) {

      Thread.sleep(waitTimeInMillis);
      completedJobs = 0;
      for (Job j : jobs)
        if (j.isComplete())
          completedJobs++;
    }
  }

  //
  // Constructor
  //

  private MapReduceUtils() {
  }

}
