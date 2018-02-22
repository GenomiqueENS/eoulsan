package fr.ens.biologie.genomique.eoulsan.bio.readsmappers;

/**
 * This class define a abstract implementation of a MapperProvider.
 * @since 2.2
 * @author Laurent Jourdren
 */
public abstract class AbstractMapperProvider implements MapperProvider {

  @Override
  public boolean isIndexGeneratorOnly() {
    return false;
  }

  @Override
  public boolean isCompressedIndex() {
    return false;
  }

  @Override
  public boolean isMultipleInstancesAllowed() {
    return false;
  }

  @Override
  public boolean isSplitsAllowed() {

    return true;
  }

}
