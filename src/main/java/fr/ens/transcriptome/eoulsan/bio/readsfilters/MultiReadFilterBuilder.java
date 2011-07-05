package fr.ens.transcriptome.eoulsan.bio.readsfilters;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.util.ReporterIncrementer;

/**
 * This builder allow to create a MultiReadFilter object.
 * @author Laurent Jourdren
 */
public class MultiReadFilterBuilder {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private final Map<String, ReadFilter> mapFilters = Maps.newHashMap();
  private final List<ReadFilter> listFilter = Lists.newArrayList();

  private final Map<String, String> mapParameters = Maps.newLinkedHashMap();

  /**
   * Add a parameter to the builder
   * @param key key of the parameter
   * @param value value of the parameter
   * @throws EoulsanException if the filter reference in the key does not exist
   *           or if an error occurs while setting the parameter in the
   *           dedicated filter
   */
  public void addParameter(final String key, final String value)
      throws EoulsanException {

    if (key == null || value == null)
      return;

    // Get first dot position
    final String keyTrimmed = key.trim();
    final int index = keyTrimmed.indexOf('.');

    final String filterName;
    final String filterKey;

    // Get the the filter name and parameter name
    if (index == -1) {
      filterName = keyTrimmed;
      filterKey = null;
    } else {
      filterName = keyTrimmed.substring(0, index);
      filterKey = keyTrimmed.substring(index + 1);
    }

    final ReadFilter filter;

    // Get the filter object, load it if necessary
    if (mapFilters.containsKey(filterName))
      filter = mapFilters.get(filterName);
    else {
      filter = ReadFilterService.getInstance().getReadFilter(filterName);

      if (filter == null)
        throw new EoulsanException("Unable to find "
            + filterName + " read filter.");

      this.mapFilters.put(filterName, filter);
      this.listFilter.add(filter);
    }

    // Set the parameter
    if (filterKey != null) {
      final String valueTrimmed = value.trim();
      filter.setParameter(filterKey, valueTrimmed);
      this.mapParameters.put(filterKey, valueTrimmed);
      LOGGER.info("Set read filter parameter: "
          + filterKey + "=" + valueTrimmed);
    }
  }

  /**
   * Create the final MultiReadFilter.
   * @return a new MultiReadFilter object
   * @throws EoulsanException if an error occurs while initialize one of the
   *           filter
   */
  public ReadFilter getReadFilter() throws EoulsanException {

    for (ReadFilter f : this.listFilter)
      f.init();

    final MultiReadFilter mrf = new MultiReadFilter(this.listFilter);

    return mrf;
  }

  /**
   * Create the final MultiReadFilter.
   * @param incrementer incrementer to use
   * @param counterGroup counter group for the incrementer
   * @return a new MultiReadFilter object
   * @throws EoulsanException if an error occurs while initialize one of the
   *           filter
   */
  public ReadFilter getReadFilter(final ReporterIncrementer incrementer,
      final String counterGroup) throws EoulsanException {

    for (ReadFilter f : this.listFilter)
      f.init();

    final MultiReadFilter mrf =
        new MultiReadFilter(incrementer, counterGroup, this.listFilter);

    return mrf;
  }

  /**
   * Get a map with all the parameters used to create the MultiReadFilter.
   * @return an ordered map object
   */
  public Map<String, String> getParameters() {

    return Collections.unmodifiableMap(this.mapParameters);
  }

}
