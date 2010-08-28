package uk.co.brotherlogic.mdb;

import java.io.File;
import java.util.List;

/**
 * Main entry for the mdborg application
 * 
 */
public class Organiser {

	/** The base location for all the music files */
	public static final String BASE_LOC = "/usr/share/hancock_multimedia/music/";

	/**
	 * Main method
	 * 
	 * @param args
	 *            no arguments
	 */
	public static void main(final String[] args) {
		Organiser org = new Organiser();

		long sTime = System.currentTimeMillis();
		org.run();
		System.out.println("Complete in "
				+ (System.currentTimeMillis() - sTime) / 1000.0);
	}

	/** Out file locator */
	private final CDOutLocator locator = new CDOutLocator();

	/**
	 * Method to do all the work
	 */
	public final void run() {
		List<File> cdOutFiles = locator.getLocations();
	}
}
