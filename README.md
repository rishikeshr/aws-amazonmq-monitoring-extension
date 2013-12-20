# AppDynamics ActiveMQ Monitoring Extension

This extension works only with the Java agent.

##Use Case

ActiveMQ is an open source, JMS 1.1 compliant, message-oriented middleware (MOM) from the Apache Software Foundation that provides high-availability, performance, scalability, reliability and security for enterprise messaging. 
The ActiveMQ Monitoring extension collects metrics from an ActiveMQ messaging server and presents them in the AppDynamics Metric Browser. 


##Installation

1. Run 'ant package' from the active-mq-monitoring-extension directory
2. Download the file ActiveMQMonitor.zip located in the 'dist' directory into \<machineagent install dir\>/monitors/
3. Unzip the downloaded file
4. In \<machineagent install dir\>/monitors/ActiveMQMonitor/, open monitor.xml and configure the ActiveMQ parameters. 
	<pre>
	 &lt;argument name="host" is-required="true" default-value="localhost" /&gt;
     &lt;argument name="port" is-required="true" default-value="1099" /&gt;
     &lt;argument name="username" is-required="true" default-value="admin" /&gt;
     &lt;argument name="password" is-required="true" default-value="admin" /&gt;
     &lt;argument name="exclude-queues" is-required="false" default-value=""/&gt;
     &lt;argument name="exclude-topics" is-required="false" default-value=""/&gt; 
     &lt;argument name="exclude-metrics-path" is-required="false" default-value="monitors/ActiveMQMonitor/conf/metrics.xml" /&gt;
</pre>
5. Restart the Machine Agent. 
 
In the AppDynamics Metric Browser, look for: Application Infrastructure Performance  | \<Tier\> | Custom Metrics | ActiveMQ

##Directory Structure

<table><tbody>
<tr>
<th align="left"> File/Folder </th>
<th align="left"> Description </th>
</tr>
<tr>
<td class='confluenceTd'> conf </td>
<td class='confluenceTd'> Contains the monitor.xml, metrics.xml </td>
</tr>
<tr>
<td class='confluenceTd'> lib </td>
<td class='confluenceTd'> Contains third-party project references </td>
</tr>
<tr>
<td class='confluenceTd'> src </td>
<td class='confluenceTd'> Contains source code to the ActiveMQ Monitoring Extension </td>
</tr>
<tr>
<td class='confluenceTd'> dist </td>
<td class='confluenceTd'> Only obtained when using ant. Run 'ant build' to get binaries. Run 'ant package' to get the distributable .zip file </td>
</tr>
<tr>
<td class='confluenceTd'> build.xml </td>
<td class='confluenceTd'> Ant build script to package the project (required only if changing Java code) </td>
</tr>
</tbody>
</table>

##Contributing

Always feel free to fork and contribute any changes directly via [GitHub](https://github.com/Appdynamics/f5-monitoring-extension).

##Community

Find out more in the [AppSphere](http://appsphere.appdynamics.com/t5/eXchange/F5-Monitoring-Extension/idi-p/2063) community.

##Support

For any questions or feature request, please contact [AppDynamics Center of Excellence](mailto:ace-request@appdynamics.com).


