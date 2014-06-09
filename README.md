Timestamp Timer PlugIn
==========================

JMeter plugin for timestamp based delays (new timer component).


------------------------
Installation:
------------------------
Just copy the TimestampPlugin.jar file into the folder lib/ext/ of your JMeter installation directory.
Exemplary test plans require the Standard Set of JMeter Plugins (http://jmeter-plugins.org)

------------------------
About the Timestamp File
------------------------
The timestamp file must contain relative timestamps for request in seconds (seconds since start of load test). 
Each line in the timestamp file must contain one timestamp which is followed by a semicolon (;).
One option for creating timestamp files is the workload modeling tool LIMBO:
http://se2.informatik.uni-wuerzburg.de/mediawiki-se/index.php/Tools#LIMBO:_Load_Intensity_Modeling_Tool


------------------------
Usage
------------------------
Try the provided TestPlans (Standard Set of JMeter Plugins (http://jmeter-plugins.org) required).
Start with: SingleThreadPool-DummySampler.jmx
Find the "Timestamp Timer" element and open a timestamp file (e.g. the provided timestamps.txt).
Run the TestPlan and watch the chart provided by the "jp@gc - Transactions per Second" element.