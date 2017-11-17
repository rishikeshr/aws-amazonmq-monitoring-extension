package com.appdynamics.extensions.activemq;

import com.appdynamics.extensions.metrics.*;

import com.appdynamics.extensions.AMonitorTaskRunnable;
import com.appdynamics.extensions.activemq.metrics.*;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.metrics.MetricProperties;
import com.appdynamics.extensions.metrics.MetricPropertiesBuilder;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MalformedObjectNameException;
import javax.management.remote.JMXConnector;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import com.google.common.collect.Maps;

import static com.appdynamics.extensions.activemq.Constants.DISPLAY_NAME;
import static com.appdynamics.extensions.activemq.Constants.INCLUDE;
import static com.appdynamics.extensions.activemq.Constants.METRICS;


public class ActiveMQMonitorTask implements AMonitorTaskRunnable {
    private Boolean status = true;

    private static final Logger logger = LoggerFactory.getLogger(ActiveMQMonitorTask.class);
    private static final BigDecimal ERROR_VALUE = BigDecimal.ZERO;
    private static final BigDecimal SUCCESS_VALUE = BigDecimal.ONE;
    private static final String METRICS_COLLECTION_SUCCESSFUL = "Metrics Collection Successful";
    private String metricPrefix;
    private MetricWriteHelper metricWriter;
    private Map server;
    private JMXConnectionAdapter jmxConnectionAdapter;
    private List<Map> configMBeans;
    private String serverName;

    public void run() {
        serverName = ActiveMQUtil.convertToString(server.get(DISPLAY_NAME), "");
        MetricPrinter metricPrinter = new MetricPrinter(metricPrefix, serverName, metricWriter);

        try {
            logger.debug("ActiveMQ monitoring task initiated for server {}", serverName);
            BigDecimal status = populateAndPrintStats();

//            metricPrinter.printMetric(metricPrinter.formMetricPath(METRICS_COLLECTION_SUCCESSFUL), status,
//                    MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
//                    MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
        } catch (Exception e) {
            logger.error("Error in ActiveMQ Monitoring Task for Server {}", serverName, e);
//            metricPrinter.printMetric(metricPrinter.formMetricPath(METRICS_COLLECTION_SUCCESSFUL), BigDecimal.ZERO,
//                    MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION, MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
//                    MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
            status = false;

        } finally {
            logger.debug("ActiveMQ Monitoring Task Complete. Total number of metrics reported = {}", metricPrinter
                    .getTotalMetricsReported());
        }
    }

    private BigDecimal populateAndPrintStats() throws IOException {
        JMXConnector jmxConnector = null;

        try {
            jmxConnector = jmxConnectionAdapter.open();
            logger.debug("JMX Connection is now open");

            for (Map mBean : configMBeans) {
                String configObjName = ActiveMQUtil.convertToString(mBean.get("objectName"), "");
                logger.debug("Processing mBeam {} from the config file", configObjName);

                try {
//                    Map<String, MetricProperties> metricProperties = propertiesBuilder.build(mBean);

                    Map<String, MetricProperties> metricProperties = getMapOfProperties(mBean);
//                    MetricPropertiesBuilder propertiesBuilder = new MetricPropertiesBuilder(mBean,configObjName);



//                    MetricProperties metricProperties = new MetricProperties();
//                    metricProperties = propertiesBuilder.buildMetricProperties();
                    NodeMetricsProcessor nodeMetricsProcessor = new NodeMetricsProcessor(jmxConnectionAdapter, jmxConnector);
                    List<Metric> nodeMetrics = nodeMetricsProcessor.getNodeMetrics(mBean, metricProperties, metricPrefix);

                    if (nodeMetrics.size() > 0) {
                        metricWriter.transformAndPrintMetrics(nodeMetrics);

//                        metricPrinter.reportNodeMetrics(nodeMetrics);
                    }
                } catch (MalformedObjectNameException e) {
                    logger.error("Illegal Object Name {} " + configObjName, e);
                } catch (Exception e) {
                    logger.error("Error fetching JMX metrics for {} and mBeam = {}", serverName, configObjName, e);
                }
            }
        } finally {
            try {
                jmxConnectionAdapter.close(jmxConnector);
                logger.debug("JMX connection is closed.");
            } catch (IOException e) {
                logger.error("Unable to close the JMX connection.");
                return ERROR_VALUE;
            }
        }
        return SUCCESS_VALUE;
    }

    private Map<String, MetricProperties> getMapOfProperties(Map mBean){

        Map<String, MetricProperties> metricPropsMap = Maps.newHashMap();
        if (mBean == null || mBean.isEmpty()) {
            return metricPropsMap;
        }

        Map configMetrics = (Map) mBean.get(METRICS);
        List includeMetrics = (List) configMetrics.get(INCLUDE);

        if(includeMetrics != null){
            for (Object metad : includeMetrics){
                Map localMetaData = (Map)metad;
                Map.Entry entry = (Map.Entry) localMetaData.entrySet().iterator().next();
                String metricName = entry.getKey().toString();
                String alias = entry.getValue().toString();
//                DefaultMetricProperties defaultMetricProperties = new DefaultMetricProperties(metricName);
                MetricProperties props = new DefaultMetricProperties(metricName);
                props.setAlias(alias,metricName);
                setProps(mBean, props); //global level
                setProps(localMetaData, props); //local level
                metricPropsMap.put(metricName, props);

            }
        }


    return metricPropsMap;
    }

    private void setProps (Map metadata, MetricProperties props) {
//        if (metadata.get("metricType") != null) {
//            props.setAggregationFields(metadata.get("metricType").toString());
//        }
        if (metadata.get("multiplier") != null) {
            props.setMultiplier(metadata.get("multiplier").toString());
        }
        if (metadata.get("convert") != null) {
            props.setConversionValues((Map) metadata.get("convert"));
        }
        if (metadata.get("delta") != null) {
            props.setDelta(metadata.get("delta").toString());
        }
        if (metadata.get("clusterRollUpType") != null) {
            props.setDelta(metadata.get("clusterRollUpType").toString());
        }
        if (metadata.get("timeRollUpType") != null) {
            props.setDelta(metadata.get("timeRollUpType").toString());
        }
        if (metadata.get("aggregationType") != null) {
            props.setAggregationType(metadata.get("aggregationType").toString());
        }
    }

    public void onTaskComplete() {
        logger.debug("Task Complete");
        if (status == true) {
            metricWriter.printMetric(metricPrefix + "|" + (String) server.get("displayName"), "1", "AVERAGE", "AVERAGE", "INDIVIDUAL");
        } else {
            metricWriter.printMetric(metricPrefix + "|" + (String) server.get("displayName"), "0", "AVERAGE", "AVERAGE", "INDIVIDUAL");
        }
    }


    public static class Builder {
        private ActiveMQMonitorTask task = new ActiveMQMonitorTask();

        Builder metricPrefix(String metricPrefix) {
            task.metricPrefix = metricPrefix;
            return this;
        }

        Builder metricWriter(MetricWriteHelper metricWriter) {
            task.metricWriter = metricWriter;
            return this;
        }

        Builder server(Map server) {
            task.server = server;
            return this;
        }

        Builder jmxConnectionAdapter(JMXConnectionAdapter adapter) {
            task.jmxConnectionAdapter = adapter;
            return this;
        }

        Builder mbeans(List<Map> mBeans) {
            task.configMBeans = mBeans;
            return this;
        }

        ActiveMQMonitorTask build() {
            return task;
        }
    }
}
