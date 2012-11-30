/*                  Aozan development code 
 * 
 * 
 * 
 */



package fr.ens.transcriptome.eoulsan.bio.readsfilters;

import java.io.IOException;
import java.io.InputStream;

public interface SAMParserLine {

	void parseLine(String inputSAM) throws IOException;
	
	void setup();
	
	void cleanup();
	
}
