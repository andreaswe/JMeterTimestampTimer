package org.apache.jmeter.timers.timestamp;

import java.beans.PropertyDescriptor;

import org.apache.jmeter.testbeans.BeanInfoSupport;
import org.apache.jmeter.testbeans.gui.FileEditor;

public class TimestampTimerBeanInfo extends BeanInfoSupport {

	public TimestampTimerBeanInfo() {
		super(TimestampTimer.class);
		createPropertyGroup("timestamp_timer", new String[] {"usageMode", "filename" });
		PropertyDescriptor p = property("filename");
		p = property("usageMode", TimestampTimer.Mode.class); //$NON-NLS-1$
        p.setValue(DEFAULT, Integer.valueOf(TimestampTimer.Mode.TimestampsSharedWithinThreadGroup.ordinal()));
        p.setValue(NOT_UNDEFINED, Boolean.TRUE); // must be defined
		
		p = property("filename");
        p.setValue(NOT_EXPRESSION, Boolean.FALSE);
        p.setValue(NOT_UNDEFINED, Boolean.TRUE); // must be defined
        p.setValue(DEFAULT, "");
        p.setPropertyEditorClass(FileEditor.class);
        
        
	}

}
