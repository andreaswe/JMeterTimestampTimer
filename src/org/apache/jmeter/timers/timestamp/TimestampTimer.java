package org.apache.jmeter.timers.timestamp;

import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.jmeter.testbeans.TestBean;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterVariables;
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
	private static final int SAFETY_DELAY = 1000;
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
	
	private class Timestamp {
		public long id;
		public long timestamp;
		public Timestamp(long id, long timestamp) {
			super();
			this.id = id;
			this.timestamp = timestamp;
		}
	}

	/**
	 * usage mode: does every thread use all timestamps or are they shared between threads in thread group (default)
	 */
	private Mode 						usageMode = Mode.TimestampsSharedWithinThreadGroup;
	private String 						timestampFile = "";
	long 								startTime;
	private BlockingQueue<Timestamp> 	timestampList = new LinkedBlockingQueue<Timestamp>();
	private boolean 					started = false;
	private long						lastTimestamp = 0;



	@Override
	public long delay() {
		long delay = 0;
		Timestamp currentTimestamp = null;
		try {
			currentTimestamp = timestampList.poll(10,
					TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {}
		
		JMeterContext context = getThreadContext();
		JMeterVariables vars = context.getVariables();
		long plannedStart;
		if (currentTimestamp != null) {
			plannedStart = startTime + currentTimestamp.timestamp;
			vars.put("TIMESTAMPTIMER_ID", Long.toString(currentTimestamp.id));
		} else  {
			// No timestamp left, delay thread until end of test.
			plannedStart = startTime + lastTimestamp + SAFETY_DELAY;
			vars.put("TIMESTAMPTIMER_ID", Long.toString(0));
			// Stops thread
			// Be careful, the next sampler will be executed anyway!
			// Thus the timer should always be embedded in an dummy sampler (if not every thread will produce one overhead sample)
			context.getThread().stop();
		}
		long now = System.currentTimeMillis();
		if (plannedStart > now) {
			delay = plannedStart - now; 
		}

		return delay;
	}


	public TimestampTimer() {
		super();
	}


	private void readTimestampFile() {
		LinkedList<Long> timestamps = new LinkedList<Long>();

		boolean timestampsAvailable = TimestampUtils.readTimestampFile(timestampFile, ";", timestamps);
		if (timestampsAvailable)
		{
			timestampList.clear();
			lastTimestamp = 0;
			int numberOfTimestamps = timestamps.size();
			long count = 0; 
			if (numberOfTimestamps > 0)
			{
				lastTimestamp = timestamps.get(numberOfTimestamps-1);
				for (long time : timestamps)
				{
					count++;
					timestampList.add(new Timestamp(count, time));
				}
			}
		}
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
		if (filename != this.timestampFile && !started)
		{
			this.timestampFile = filename;
			readTimestampFile();
		}
	}


	@Override
	public void testStarted() {
		started = true;
		JMeterContext context = getThreadContext();
		JMeterVariables vars = context.getVariables();
		String startTimeString = vars.get("TIMESTAMPTIMER_START");
		if (startTimeString != null)
		{
			startTime = Long.parseLong(startTimeString);
		} else {
			startTime = System.currentTimeMillis();
			vars.put("TIMESTAMPTIMER_START", Long.toString(startTime));
		}
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
	 * Customized clone method to pass parameters which where already configured to clones
	 */
	@Override
	public Object clone() {
		TimestampTimer clone = (TimestampTimer)super.clone();
		clone.usageMode = usageMode;
		clone.timestampFile = timestampFile;
		clone.startTime = startTime;
		switch (usageMode) {
		case TimestampsSharedWithinThreadGroup:
			clone.timestampList = timestampList;
			break;
		case AllThreadsUseAllTimestamps:
			clone.timestampList = new LinkedBlockingQueue<Timestamp>(timestampList);
			break;
		}
		clone.started = started;
		clone.lastTimestamp = lastTimestamp;
		return clone;
	}
}
