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

	private void checkFile(File f, Record r, int trackNumber)
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
				System.err.println("ADJUST: " + f);
				file.setID3Tag(tags);
				file.sync();
			}
		} else {
			System.err.println("SETTING THE TAGS ANEW: " + f);
			ID3V2_3_0Tag tag = new ID3V2_3_0Tag();
			tag.setArtist(r.getFormTrackArtist(trackNumber));
			tag.setAlbum(r.getTitle());
			tag.setTrackNumber(trackNumber);
			tag.setTitle(r.getFormTrackTitle(trackNumber));
			file.setID3Tag(tag);
			file.sync();
		}

	}

    private File moveDirectory(File f, Record r) throws IOException,SQLException {
		File baseDir = f.getParentFile();
		File outDir = new File(Organiser.BASE_LOC + r.getFileAdd());
		outDir.getParentFile().mkdirs();

		String[] procString = new String[] {"mv",baseDir.getAbsolutePath(),outDir.getAbsolutePath()};
		Process p = Runtime.getRuntime().exec(procString);
		BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getErrorStream()));
		String s = "";
		while ((s = stdInput.readLine()) != null) {
		    System.err.println("DIR MOVE: " + s);
		}
		stdInput.close();
		return new File(outDir, "CDout.txt");
	}

	public void process(File f) throws IOException, SQLException {
		// Read the CDOut file
		String[] lines = readLines(f);
		if (lines.length == 1)
			process(f, Integer.parseInt(lines[0]));
		else
			process(f, Integer.parseInt(lines[3]));
	}

	private void process(File f, int number) throws IOException, SQLException {
		Record r = GetRecords.create().getRecord(number);

		//Check that this record still exists
		if (r == null)
		    return;

		String base = r.getFileAdd();
		String aBase = f.getParentFile().getAbsolutePath().substring(
				Organiser.BASE_LOC.length());

		// Check the directory
		if (!base.equals(aBase)) {
			f = moveDirectory(f, r);
		}

		System.out.println(r.getAuthor() + " - " + r.getTitle() + " => " + r.getNumber());
		// Now check the track names
		for (File trackFile : f.getParentFile().listFiles())
			if (trackFile.getName().endsWith(".mp3")) {
				String strFilename = trackFile.getAbsolutePath().substring(
						Organiser.BASE_LOC.length() + r.getFileAdd().length()
								+ 1);
				int tNumber = Integer.parseInt(strFilename.substring(0, 3));
				System.out.println("Track " + tNumber);
				String tRep = r.getTrackRep(tNumber) + ".mp3";

				// See if we need to move the file
				if (!strFilename.equals(tRep)) {
					String[] mover = new String[] {
							"mv",
							Organiser.BASE_LOC + r.getFileAdd()
									+ File.separator + strFilename,
							Organiser.BASE_LOC + r.getFileAdd()
									+ File.separator + tRep };
					Process p = Runtime.getRuntime().exec(mover);
					BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
					while (stdInput.readLine() != null) {
					    //pass
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

	private String[] readLines(File f) {
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
