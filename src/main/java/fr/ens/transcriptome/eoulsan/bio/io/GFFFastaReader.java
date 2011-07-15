package fr.ens.transcriptome.eoulsan.bio.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;

/**
 * This method define a Fasta reader for fasta section of GFF files.
 * @author Laurent Jourdren
 */
public class GFFFastaReader extends FastaReader {

  private boolean inFastaSection;
  
  
  @Override
  public boolean readEntry() throws IOException, BadBioEntryException {
   
    if (this.inFastaSection)
      return super.readEntry();
    
    String line = null;

    while ((line = this.reader.readLine()) != null) { 
    
      if (line.startsWith("###FASTA")) {
        this.inFastaSection = true;
        return super.readEntry();
      }
    }
    
    return false;
  }

  //
  // Constructors
  //
  
  
  /**
   * Public constructor
   * @param file File to use
   */
  public GFFFastaReader(File file) throws FileNotFoundException {
    super(file);
  }
  
  /**
   * Public constructor
   * @param is InputStream to use
   */
  public GFFFastaReader(final InputStream is) throws FileNotFoundException {
    super(is);
  }

}
