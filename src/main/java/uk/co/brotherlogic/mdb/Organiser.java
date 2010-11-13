package uk.co.brotherlogic.mdb;

import java.io.File;
import java.util.List;
import uk.co.brotherlogic.mdb.Connect;

/**
 * Main entry for the mdborg application
 * 
 */
public class Organiser {

	/** The base location for all the music files */
	public static final String BASE_LOC = "/usr/share/music/music/";

	/** To avoid magic numbers */
	private static final double MS_IN_A_S = 1000.0;

	/**
	 * Main method
	 * 
	 * @param args
	 *            no arguments
	 */
	public static void main(final String[] args) throws Exception{
		Organiser org = new Organiser();

		long sTime = System.currentTimeMillis();
		org.run();
		Connect.getConnection().commitTrans();
		System.out.println("Complete in "
				+ (System.currentTimeMillis() - sTime) / MS_IN_A_S);
	}

	/** Out file locator */
	private final CDOutLocator locator = new CDOutLocator();

	/** THe processor */
	private final Processor proc = new Processor();

	/**
	 * Method to do all the work
	 */
	public final void run() {
		List<File> cdOutFiles = locator.getLocations();
		for (File f : cdOutFiles) {
			try {
				proc.process(f);
			} catch (Exception e) {
				System.err.println("Error in : " + f);
				e.printStackTrace();
			}
		}
	}
}
