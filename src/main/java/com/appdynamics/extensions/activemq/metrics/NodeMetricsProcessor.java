/*

 *   Copyright 2018. AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.extensions.activemq.metrics;

import com.appdynamics.extensions.activemq.ActiveMQUtil;
import com.appdynamics.extensions.activemq.JMXConnectionAdapter;
import com.appdynamics.extensions.activemq.filters.IncludeFilter;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.metrics.MetricProperties;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.remote.JMXConnector;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.appdynamics.extensions.activemq.Constants.*;

public class NodeMetricsProcessor {
    private static final Logger logger = LoggerFactory.getLogger(NodeMetricsProcessor.class);
    private final MetricKeyFormatter metricKeyFormatter = new MetricKeyFormatter();
    private JMXConnectionAdapter jmxConnectionAdapter;
    private JMXConnector jmxConnector;
    private String serverName;

    public NodeMetricsProcessor(JMXConnectionAdapter jmxConnectionAdapter, JMXConnector jmxConnector, String serverName) {
        this.jmxConnectionAdapter = jmxConnectionAdapter;
        this.jmxConnector = jmxConnector;
        this.serverName = serverName;
    }

    public List<Metric> getNodeMetrics(Map mBean, Map<String, ? > metricsPropertiesMap, String metricPrefix) throws
            MalformedObjectNameException, IOException, IntrospectionException, InstanceNotFoundException,
            ReflectionException {
        List<Metric> nodeMetrics = Lists.newArrayList();
        String configObjectName = ActiveMQUtil.convertToString(mBean.get(OBJECT_NAME), "");
        Set<ObjectInstance> objectInstances = jmxConnectionAdapter.queryMBeans(jmxConnector, ObjectName.getInstance
                (configObjectName));
        for (ObjectInstance instance : objectInstances) {
            List<String> metricNamesDictionary = jmxConnectionAdapter.getReadableAttributeNames(jmxConnector, instance);
            List<String> metricNamesToBeExtracted = applyFilters(mBean, metricNamesDictionary);
            Set<Attribute> attributes = jmxConnectionAdapter.getAttributes(jmxConnector, instance.getObjectName(),
                    metricNamesToBeExtracted.toArray(new String[metricNamesToBeExtracted.size()]));
            collect(metricPrefix, nodeMetrics, attributes, instance, metricsPropertiesMap);
        }
        return nodeMetrics;
    }

    private List<String> applyFilters(Map aConfigMBean, List<String> metricNamesDictionary) throws
            IntrospectionException, ReflectionException, InstanceNotFoundException, IOException {
        Set<String> filteredSet = Sets.newHashSet();
        Map configMetrics = (Map) aConfigMBean.get(METRICS);
        List includeDictionary = (List) configMetrics.get(INCLUDE);
        new IncludeFilter(includeDictionary).applyFilter(filteredSet, metricNamesDictionary);
        return Lists.newArrayList(filteredSet);
    }

    private void collect(String metricPrefix, List<Metric> nodeMetrics, Set<Attribute> attributes, ObjectInstance instance, Map<String, ? > metricPropsPerMetricName) {
        for (Attribute attribute : attributes) {
            try {
                String metricName = attribute.getName();
                if (isCurrentObjectComposite(attribute)) {
                    Set<String> attributesFound = ((CompositeDataSupport) attribute.getValue()).getCompositeType()
                            .keySet();
                    for (String str : attributesFound) {
                        String key = metricName + "." + str;
                        if (metricPropsPerMetricName.containsKey(key)) {
                            Object attributeValue = ((CompositeDataSupport) attribute.getValue()).get(str);
                            setMetricDetails(metricPrefix, key, attributeValue, instance, metricPropsPerMetricName, nodeMetrics);
                        }
                    }
                } else {
                    setMetricDetails(metricPrefix, metricName, attribute.getValue(), instance, (Map)metricPropsPerMetricName,
                            nodeMetrics);
                }
            } catch (Exception e) {
                logger.error("Error collecting value for {} {}", instance.getObjectName(), attribute.getName(), e);
            }
        }
    }

    private void setMetricDetails(String metricPrefix, String attributeName, Object attributeValue, ObjectInstance instance, Map<String, ? > metricPropsPerMetricName, List<Metric> nodeMetrics) {

        Map<String, ?> props = (Map)metricPropsPerMetricName.get(attributeName);
        if (props == null) {
            logger.error("Could not find metric properties for {} ", attributeName);
        }
        String instanceKey = metricKeyFormatter.getInstanceKey(instance);
        String metricPath = Strings.isNullOrEmpty(metricPrefix) ? serverName + "|" + instanceKey + attributeName : metricPrefix + "|"+ serverName + "|" + instanceKey + attributeName;
        Metric current_metric = new Metric(attributeName, attributeValue.toString(), metricPath, props);
        nodeMetrics.add(current_metric);


    }

    private boolean isCurrentObjectComposite(Attribute attribute) {
        return attribute.getValue().getClass().equals(CompositeDataSupport.class);
    }
}
