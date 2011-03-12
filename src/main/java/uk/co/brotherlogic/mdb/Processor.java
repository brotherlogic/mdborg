package uk.co.brotherlogic.mdb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.blinkenlights.jid3.ID3Exception;
import org.blinkenlights.jid3.MP3File;
import org.blinkenlights.jid3.v2.ID3V2Tag;
import org.blinkenlights.jid3.v2.ID3V2_3_0Tag;

import uk.co.brotherlogic.mdb.record.GetRecords;
import uk.co.brotherlogic.mdb.record.Record;

/**
 * Module for processing the files
 * 
 * @author simon
 * 
 */
public class Processor {

	/**
	 * Checks a single MP3 file
	 * 
	 * @param f
	 *            THe location of the MP3 file
	 * @param r
	 *            The record to check against
	 * @param trackNumber
	 *            The track number this represents
	 * @throws IOException
	 *             If something goes wrong with reading
	 * @throws ID3Exception
	 *             If something goes wrong with the tags
	 * @throws SQLException
	 *             If something goes wrong with the DB
	 */
	private void checkFile(final File f, final Record r, final int trackNumber)
			throws IOException, ID3Exception, SQLException {

		MP3File file = new MP3File(f);
		file.removeID3V1Tag();

		ID3V2Tag tags = file.getID3V2Tag();
		boolean changed = false;
		if (tags != null) {

			if (tags.getArtist() == null
					|| !tags.getArtist().equals(
							r.getFormTrackArtist(trackNumber))) {
				tags.setArtist(r.getFormTrackArtist(trackNumber));
				changed = true;
			}

			if (tags.getTitle() == null
					|| !tags.getTitle()
							.equals(r.getFormTrackTitle(trackNumber))) {
				tags.setTitle(r.getFormTrackTitle(trackNumber));
				changed = true;
			}

			if (tags.getAlbum() == null
					|| !tags.getAlbum().equals(r.getTitle())) {
				tags.setAlbum(r.getTitle());
				changed = true;
			}

			try {
				if (tags.getTrackNumber() <= 0
						|| tags.getTrackNumber() != trackNumber) {
					tags.setTrackNumber(trackNumber);
					changed = true;
				}
			} catch (ID3Exception e) {
				tags.setTrackNumber(trackNumber);
				changed = true;
			}

			if (changed) {
				System.out.println("Adjusting Tags on: " + f);
				file.setID3Tag(tags);
				file.sync();
			}
		} else {
			System.out.println("No tags found on: " + f);
			ID3V2_3_0Tag tag = new ID3V2_3_0Tag();
			tag.setArtist(r.getFormTrackArtist(trackNumber));
			tag.setAlbum(r.getTitle());
			tag.setTrackNumber(trackNumber);
			tag.setTitle(r.getFormTrackTitle(trackNumber));
			file.setID3Tag(tag);
			file.sync();
		}

	}

	/**
	 * Moves a directory to the correct location
	 * 
	 * @param f
	 *            The location of the CDout file
	 * @param r
	 *            The record to be moved
	 * @return The new location of the CDout file
	 * @throws IOException
	 *             If something goes wrong with the moving process
	 * @throws SQLException
	 *             If something goes wrong with the database
	 */
	private File moveDirectory(final File f, final Record r)
			throws IOException, SQLException {
		File baseDir = f.getParentFile();
		File outDir = new File(Organiser.BASE_LOC + r.getFileAdd());

		if (!outDir.getParentFile().mkdirs())
			System.err.println(outDir.getParentFile() + " already exists!");

		System.out.println("Moving \"" + baseDir.getAbsolutePath() + "\" to \""
				+ outDir.getAbsolutePath() + "\"");
		String[] procString = new String[] { "mv", baseDir.getAbsolutePath(),
				outDir.getAbsolutePath() };
		Process p = Runtime.getRuntime().exec(procString);
		BufferedReader stdInput = new BufferedReader(new InputStreamReader(
				p.getErrorStream()));
		String s = "";
		while ((s = stdInput.readLine()) != null) {
			System.err.println("DIR MOVE: " + s);
		}
		stdInput.close();
		return new File(outDir, "CDout.txt");
	}

	/**
	 * Processes a single CDout file
	 * 
	 * @param f
	 *            The location of the CDout file
	 * @throws IOException
	 *             If something goes wrong with reading/writing
	 * @throws SQLException
	 *             If something goes wrong with the database
	 */
	public final void process(final File f) throws IOException, SQLException {
		// Read the CDOut file
		String[] lines = readLines(f);
		if (lines.length == 1)
			process(f, Integer.parseInt(lines[0]));
		else
			process(f, Integer.parseInt(lines[3]));
	}

	/**
	 * Process a record
	 * 
	 * @param f
	 *            Location of the CDout file
	 * @param number
	 *            the id number of the record
	 * @throws IOException
	 *             If something goes wrong reading/writing
	 * @throws SQLException
	 *             If something goes wrong with the db
	 */
	private void process(final File f, final int number) throws IOException,
			SQLException {
		Record r = GetRecords.create().getRecord(number);

		// Check that this record still exists
		if (r == null)
			return;

		String base = r.getFileAdd();
		String aBase = f.getParentFile().getAbsolutePath()
				.substring(Organiser.BASE_LOC.length());

		// Check the directory
		File newdir = f;
		if (!base.equals(aBase)) {
			newdir = moveDirectory(f, r);
		}

		if (r.getRiploc() == null || (!r.getRiploc().equals(newdir.getParentFile().getAbsolutePath()))) {
			System.err.println("Resetting riploc: from " + r.getRiploc()
					+ " to " + newdir.getParentFile().getAbsolutePath());
			r.setRiploc(newdir.getParentFile().getAbsolutePath());
			r.save();
		}

		// Now check the track names
		for (File trackFile : newdir.getParentFile().listFiles())
			if (trackFile.getName().endsWith(".mp3")) {
				String strFilename = trackFile.getAbsolutePath().substring(
						Organiser.BASE_LOC.length() + r.getFileAdd().length()
								+ 1);
				int tNumber = Integer.parseInt(strFilename.substring(0, 3));
				String tRep = r.getTrackRep(tNumber) + ".mp3";

				// See if we need to move the file
				if (!strFilename.equals(tRep)) {
					System.out.println("Moving \"" + Organiser.BASE_LOC
							+ r.getFileAdd() + File.separator + strFilename
							+ "\" to \"" + Organiser.BASE_LOC + r.getFileAdd()
							+ File.separator + tRep + "\"");
					String[] mover = new String[] {
							"mv",
							Organiser.BASE_LOC + r.getFileAdd()
									+ File.separator + strFilename,
							Organiser.BASE_LOC + r.getFileAdd()
									+ File.separator + tRep };
					Process p = Runtime.getRuntime().exec(mover);
					BufferedReader stdInput = new BufferedReader(
							new InputStreamReader(p.getErrorStream()));
					String s = "";
					while ((s = stdInput.readLine()) != null) {
						System.err.println("MOVE FILE: " + s);
					}
					stdInput.close();
				}

				File checkFile = new File(Organiser.BASE_LOC + r.getFileAdd()
						+ File.separator + tRep);

				try {
					checkFile(checkFile, r, tNumber);
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
	}

	/**
	 * Reads the lines in a file
	 * 
	 * @param f
	 *            File to read
	 * @return an array of the lines
	 */
	private String[] readLines(final File f) {
		List<String> stringList = new LinkedList<String>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(f));
			for (String line = reader.readLine(); line != null; line = reader
					.readLine())
				stringList.add(line.trim());
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return stringList.toArray(new String[1]);
	}
}
