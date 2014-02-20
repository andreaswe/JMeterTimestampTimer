package org.apache.jmeter.timers.timestamp;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.jmeter.testbeans.TestBean;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.timers.Timer;

/**
 * This class implements a Timestamp Timer. It can be used to delay
 * execution of samples based on a timestamp file. The timestamp file must
 * contain relative timestamps in seconds. Each line in the timestamp file must
 * contain one timestamp which is followed by a semicolon (;).
 * One option for creating timestamp files is the workload modeling tool LIMBO:
 * @see <a href="http://www.descartes-research.net/Tools#LIMBO:_Load_Intensity_Modeling_Tool">LIMBO Website</a>
 * 
 * Examplary file content for a timestamp file:
 * 0.5;
 * 1;
 * 1,5;
 * 2;
 * 
 * Using this timestamp file the timer will delay the samples 
 * ... during first execution for 0.5 seconds
 * ... during second execution for about 0.5 seconds (so that the sample is executed 1 second after start of load test)
 * ... during third execution for about 0.5 seconds (so that the sample is executed 1.5 second after start of load test)
 * ... and so on
 * 
 * Its possible to configure 2 different usage modes (important when Timer is placed in Thread Group with more than 1 threads):
 *  - "Every thread in thread group uses all timestamps"
 *  - "Timestamps are shared by threads in threadgroup"
 *  
 * When the first option is used every thread will delay its execution at the same points of time.
 * The second option allows "sharing" of timestamps between the thread. When the thread group contains
 * 2 Thread for example, the first thread will just use timestamps 1,3,5,7,... whereas the second thread
 * will use the timestamps 2,4,6,8,...
 *  
 * @author Andreas Weber <andreas.weber4@student.kit.edu>
 */
public class TimestampTimer extends AbstractTestElement implements Timer, TestStateListener, TestBean {


	private static final long serialVersionUID = 240L;

	/**
	 * This enum defines the timer usage modes used by the TimestampTimer.
	 */
	public enum Mode {
		AllThreadsUseAllTimestamps("calcMode.1"),
		TimestampsSharedWithinThreadGroup("calcMode.2");

		private final String propertyName; // The property name to be used to look up the display string

		Mode(String name) {
			this.propertyName = name;
		}

		@Override
		public String toString() {
			return propertyName;
		}
	}

	/**
	 * usage mode: does every thread use all timestamps or are they shared between threads in thread group (default)
	 */
	private Mode 			usageMode = Mode.TimestampsSharedWithinThreadGroup;
	private String 			timestampFile = "";
	private Iterator<Long> 	currentTimestampIterator;
	boolean  				timestampsAvailable = false;
	long 					startTime;
	long 					currentTimestamp;
	long 					position = 0;
	private boolean 		nextTimestampAvailable = false;
	private List<Long> 		timestampMillis = new LinkedList<Long>();
    private boolean 		started = false;
	
	

	@Override
	public long delay() {
		long delay = 0;
		int numberOfThreads = JMeterContextService.getContext().getThreadGroup().getNumberOfThreads();
		// is a new timestamp available? if not set delay to 0
		if (nextTimestampAvailable)
		{
			long plannedStart = startTime + currentTimestamp;
			long now = System.currentTimeMillis();
			if (plannedStart > now) {
				delay = plannedStart - now; 
			}
			//System.out.println("Delay: " + (plannedStart - now));
		}
		

		switch (usageMode) {
		case TimestampsSharedWithinThreadGroup:
			goToNextTimestamp(numberOfThreads);
			break;
		case AllThreadsUseAllTimestamps:
			goToNextTimestamp(1);
			break;
		}		
		return delay;
	}


	public TimestampTimer() {
		super();
		System.out.println("new");
	}

	/**
	 * Initializes Timer. This means:
	 * - Timestamp file is read
	 * - Start time is saved
	 * - First timestamp which is intended to be used by the thread is selected
	 */
	private void readTimestampFile() {
		System.out.println("readFile");
		timestampsAvailable = TimestampUtils.readTimestampFile(timestampFile, ";", timestampMillis);
		if (timestampsAvailable) {
			System.out.println("success");
		}
		System.out.println("readOK " + (timestampMillis != null));
	}


	/**
	 * Jumps forward in timestamp file as specified by goForward
	 * @param goForward number of timestamps to jump forward
	 */
	private void goToNextTimestamp(int goForward) {
		while (goForward > 0 && currentTimestampIterator.hasNext())
		{
			currentTimestamp = currentTimestampIterator.next();
			goForward --;
			position++;
		}
		nextTimestampAvailable = (goForward == 0);
	}

	public int getUsageMode() {
		return usageMode.ordinal();
	}

	public void setUsageMode(int mode) {
		this.usageMode = Mode.values()[mode];
	}
	
	public String getFilename() {
		return timestampFile;
	}
	
	public void setFilename(String filename) {
		if (filename != this.timestampFile)
		{
			this.timestampFile = filename;
			readTimestampFile();
		}
	}


	@Override
	public void testStarted() {
		started = true;
		startTime = System.currentTimeMillis();
	}


	@Override
	public void testStarted(String host) {
		testStarted();
	}


	@Override
	public void testEnded() {
	}


	@Override
	public void testEnded(String host) {
	}
	
	/**
	 * Initilization method for clones (called in clone())
	 */
	private void initTimerClone() {
		if (started)
		{
			selectFirstTimestampForThread();
		}
	}

	/**
	 * 
	 */
	private void selectFirstTimestampForThread() {
		if (timestampsAvailable) {
			currentTimestampIterator = timestampMillis.iterator();
			// threadNum 0-indexed
			int threadNum = JMeterContextService.getContext().getThreadNum();
			switch (usageMode) {
			case TimestampsSharedWithinThreadGroup:
				goToNextTimestamp(threadNum + 1);
				break;
			case AllThreadsUseAllTimestamps:
				goToNextTimestamp(1);
				break;
			}
		}
	}
	
	/**
	 * Customized clone method to pass parameters which where already configured to clones
	 * Triggers initTimerClone() method before returning
	 */
	@Override
	public Object clone() {
		TimestampTimer clone = (TimestampTimer)super.clone();
		clone.usageMode = usageMode;
		clone.timestampFile = timestampFile;
		clone.startTime = this.startTime;
		clone.timestampMillis = timestampMillis;
		clone.started = started;
		clone.timestampsAvailable = timestampsAvailable;
		clone.initTimerClone();
		return clone;
	}
}
