package fr.ens.transcriptome.eoulsan.io.comparator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import fr.ens.transcriptome.eoulsan.util.BloomFilterUtils;

public interface Comparator {

  public String getName();

  public boolean compareFiles(final String pathA, final String pathB)
      throws IOException;

  public boolean compareFiles(final String pathA, final String pathB,
      final boolean useSerialize) throws IOException;

  public boolean compareFiles(final File fileA, final File fileB,
      final boolean useSerialize) throws FileNotFoundException, IOException;

  public boolean compareFiles(final File fileA, final File fileB)
      throws FileNotFoundException, IOException;

  public boolean compareFiles(final InputStream isA, final InputStream isB)
      throws IOException;

  public Collection<String> getExtensions();

  public int getNumberElementsCompared();

}
