/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.jboss.ws.metrics;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContextListener;
import javax.xml.ws.Endpoint;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.message.Message;
import org.apache.cxf.metrics.MetricsFeature;
import org.apache.cxf.metrics.codahale.CodahaleMetricsProvider;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.throttling.ThrottleResponse;
import org.apache.cxf.throttling.ThrottlingFeature;
import org.apache.cxf.throttling.ThrottlingManager;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;

import com.codahale.metrics.MetricRegistry;

public class ServletServer {
    public static void main(String args[]) throws Exception {        
        String busFactory = System.getProperty(BusFactory.BUS_FACTORY_PROPERTY_NAME);
        System.setProperty(BusFactory.BUS_FACTORY_PROPERTY_NAME, "org.apache.cxf.bus.CXFBusFactory");
        try {
        	Server httpServer = new Server(8888);
            ContextHandlerCollection contexts = new ContextHandlerCollection();
            httpServer.setHandler(contexts);

            ServletContextHandler root = new ServletContextHandler(contexts, "/",
                                                                   ServletContextHandler.SESSIONS);
            Bus bus = BusFactory.getDefaultBus(true);
            CXFServlet cxf = new CXFServlet();
            cxf.setBus(bus);
            
            
            ServletContextListener metricListener = new CXFMetricsServletContextListener ();
            root.addEventListener(metricListener);
            
            
            CXFHealthCheckServletContextListener healthListener = new CXFHealthCheckServletContextListener();
            root.addEventListener(healthListener);
            
            
            com.codahale.metrics.servlets.AdminServlet adminServlet = new  com.codahale.metrics.servlets.AdminServlet();
            
            org.eclipse.jetty.servlet.ServletHolder metricServlet = new org.eclipse.jetty.servlet.ServletHolder(adminServlet);
            metricServlet.setName("metrics");
            metricServlet.setForcedPath("metrics");
            
            root.addServlet(metricServlet, "/metrics/*");
            
            
            org.eclipse.jetty.servlet.ServletHolder servlet = new org.eclipse.jetty.servlet.ServletHolder(cxf);
            servlet.setName("soap");
            servlet.setForcedPath("soap");
            root.addServlet(servlet, "/soap/*");
            
            

            httpServer.start();
            BusFactory.setDefaultBus(bus);
            
            
            
            
            Map<String, Customer> customers = new HashMap<>();
            customers.put("Premium", new Customer.PremiumCustomer("Premium"));
            customers.put("Regular", new Customer.RegularCustomer("Regular"));
            customers.put("Trial", new Customer.TrialCustomer("Trial"));
            
            Map<String, Object> properties = new HashMap<>();
            properties.put("bus.jmx.usePlatformMBeanServer", Boolean.TRUE);
            properties.put("bus.jmx.enabled", Boolean.TRUE);
            MetricRegistry registry = CXFMetricsServletContextListener.METRIC_REGISTRY;            
            healthListener.getHealthCheckRegistry().register("JMSHealth", new FakeJMSHealthCheck());
            
            CodahaleMetricsProvider.setupJMXReporter(bus, registry);
            bus.setExtension(registry, MetricRegistry.class);        
            
            ThrottlingManager manager = new ThrottlingManager() {
                public ThrottleResponse getThrottleResponse(String phase, Message m) {
                    ThrottleResponse r = new ThrottleResponse();
                    if (m.get("THROTTLED") != null) {
                        return null;
                    }
                    m.put("THROTTLED", true);
                    Customer c = m.getExchange().get(Customer.class);
                    c.throttle(r);
                    return r;
                }

                public List<String> getDecisionPhases() {
                    return Collections.singletonList(Phase.PRE_STREAM);
                }

            };
            bus.getInInterceptors().add(new CustomerMetricsInterceptor(registry, customers));
            
            Object implementor = new GreeterImpl();
            Endpoint.publish("/Greeter", implementor, 
                             new MetricsFeature(),
                             new ThrottlingFeature(manager));


        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            // clean up the system properties
            if (busFactory != null) {
                System.setProperty(BusFactory.BUS_FACTORY_PROPERTY_NAME, busFactory);
            } else {
                System.clearProperty(BusFactory.BUS_FACTORY_PROPERTY_NAME);
            }
        }
        
        
    }
}
