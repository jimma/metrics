package org.jboss.ws.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.servlets.MetricsServlet;

public class CXFMetricsServletContextListener extends MetricsServlet.ContextListener {

    public static final MetricRegistry METRIC_REGISTRY = new MetricRegistry();
    protected MetricRegistry getMetricRegistry() {
        return METRIC_REGISTRY;
    }

}
