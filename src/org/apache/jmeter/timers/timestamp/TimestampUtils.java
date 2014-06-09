package org.apache.jmeter.timers.timestamp;


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

/**
 * Utility class for reading timestamp files
 * Timestamp files must contain relative timestamps in seconds. Each line in the timestamp file must
 * contain one timestamp which is followed by a semicolon (;).
 * One option for creating timestamp files is the workload modeling tool LIMBO:
 * @see <a href="http://se2.informatik.uni-wuerzburg.de/mediawiki-se/index.php/Tools#LIMBO:_Load_Intensity_Modeling_Tool">LIMBO Website</a>
 * 
 * Examplary file content for a timestamp file:
 * 0.5;
 * 1;
 * 1,5;
 * 2;
 * 
 * 
 * @author Andreas Weber <andreas.weber4@student.kit.edu>
 */
public class TimestampUtils implements Serializable{

	private static final long serialVersionUID = 1L;

	private static final int SECONDS_TO_MILLISECONDS = 1000;

	/**
	 * Read a timestamp file and adds the timestamps into a list
	 * @param file location of timestamp file
	 * @param cvsSplitBy symbol which ends each line (e.g. semicolon)
	 * @param timestamps list where timestamps (long values which specify timestamp in milliseconds) will be added.
	 * @return true if and only if it was possible to open and parse the timestamp file
	 */
	public static boolean readTimestampFile(String file,
			String cvsSplitBy, List<Long> timestamps) {
		boolean result = false;
		
		BufferedReader br = null;
		String line = "";
		try {
			br = new BufferedReader(new FileReader(file));
			while ((line = br.readLine()) != null) {
				String[] timestampString = line.split(cvsSplitBy);
				long timestamp		= (long) (SECONDS_TO_MILLISECONDS * Double.parseDouble(timestampString[0]));
				timestamps.add(timestamp);
			}
			result = true;
		} catch (FileNotFoundException e) {
			// return quietly
		} catch (IOException e) {
			System.out.println("Error reading file: " + file);
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return result;
	}

}
