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
 * of the Institut de Biologie de l'École normale supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.core;

import java.util.EnumSet;

import fr.ens.biologie.genomique.kenetre.io.CompressionType;

/**
 * This interface define an input port of a step.
 * @since 2.0
 * @author Laurent Jourdren
 */
public interface InputPort extends Port {

  /**
   * Test if the port accept a compressed input format.
   * @return a set with the compression type allowed by the step for the port
   */
  EnumSet<CompressionType> getCompressionsAccepted();

  /**
   * Test if input data of the port is required in the working directory. This
   * method allow to declare the input data that need to be copied in the
   * working directory before starting the step. As an example, it is used to
   * copy files from a local file system to a distributed file system like HDFS.
   * After that mapreduce jobs can be efficiency launched.
   * @return true if the input data need to be copied in the working directory.
   */
  boolean isRequiredInWorkingDirectory();

}
