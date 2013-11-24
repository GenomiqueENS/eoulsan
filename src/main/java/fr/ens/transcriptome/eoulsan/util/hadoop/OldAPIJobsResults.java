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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.mapred.RunningJob;
import org.apache.hadoop.mapred.Counters.Counter;
import org.apache.hadoop.mapred.Counters.Group;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * This is the version for the old Hadoop API of JobsResults.
 * @since 1.0
 * @author Laurent Jourdren
 */
@SuppressWarnings("deprecation")
public class OldAPIJobsResults extends HadoopJobsResults {

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
  private void waitForRunningJobs(final Collection<RunningJob> jobs,
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

            if (!j.isSuccessful()) {

              addFailedJob(j.getJobName());
              sb.append("\tFAILED\n");

            } else {

              final Group g = j.getCounters().getGroup(counterGroup);
              if (g != null) {

                final Map<String, Long> map = Maps.newHashMap();

                final Iterator<Counter> it = g.iterator();
                while (it.hasNext()) {

                  final Counter counter = it.next();
                  map.put(counter.getName(), counter.getValue());
                }

                final List<String> names = Lists.newArrayList(map.keySet());
                Collections.sort(names);
                for (String name : names) {
                  sb.append('\t');
                  sb.append(name);
                  sb.append('=');
                  sb.append(map.get(name));
                  sb.append('\n');
                }

              }
            }

          }

          completedJobsSet.add(j);
        }

      }
    }

    setLog(sb.toString());
  }

  //
  // Constructor
  //

  /**
   * Wait the completion of a collection of jobs.
   * @param runningJobs Collection of running jobs
   * @param waitTimeInMillis waiting time between 2 checks of the completion of
   *          jobs
   * @param counterGroup the counter group
   * @throws IOException if an IO error occurs while waiting for jobs
   * @throws InterruptedException if an error occurs while waiting for jobs
   * @throws ClassNotFoundException if a class needed for map reduce execution
   *           is not found
   */
  public OldAPIJobsResults(final Collection<RunningJob> runningJobs,
      final int waitTimeInMillis, final String counterGroup)
      throws InterruptedException, IOException {

    waitForRunningJobs(runningJobs, waitTimeInMillis, counterGroup);
  }

}
