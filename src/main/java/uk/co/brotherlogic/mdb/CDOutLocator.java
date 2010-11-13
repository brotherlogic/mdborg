package uk.co.brotherlogic.mdb;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * Utility class to locate all the CDOut Files
 * 
 * @author simon
 * 
 */
public class CDOutLocator {

	/**
	 * Method to search the music directory to locate CDOut files
	 * 
	 * @return A list of all the CDOut files
	 */
	public final List<File> getLocations() {
		List<File> cdOutFiles = new LinkedList<File>();

		searchBase(new File(Organiser.BASE_LOC), cdOutFiles);

		return cdOutFiles;
	}

	/**
	 * Searches a base location for CDOut files
	 * 
	 * @param loc
	 *            The location to search from
	 * @param cdOutFiles
	 *            The list to add to
	 */
	private void searchBase(final File loc, final List<File> cdOutFiles) {
		for (File f : loc.listFiles())
			if (f.isDirectory())
				searchBase(f, cdOutFiles);
			else if (f.getName().equals("CDout.txt") && f.getAbsolutePath().contains("Virus"))
				cdOutFiles.add(f);
	}

}
