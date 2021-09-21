/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.pulsar.broker.lookup.http.v2;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.testng.Assert.assertEquals;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import org.apache.pulsar.broker.lookup.v2.TopicLookup;
import org.apache.pulsar.common.lookup.data.LookupData;
import org.apache.pulsar.common.naming.TopicName;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTestNg;
import org.glassfish.jersey.test.ServletDeploymentContext;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.testng.annotations.Test;

/**
 * TopicLookup V2 API unit tests.
 */
@Test(groups = "broker")
public class TopicLookupTest extends JerseyTestNg.ContainerPerMethodTest {

    private static final String TOPIC_PATH = "/v2/topic/persistent/public/testns/testtopic";

    private TestableTopicLookup resource;

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new GrizzlyWebTestContainerFactory();
    }

    @Override
    protected DeploymentContext configureDeployment() {
        return ServletDeploymentContext.forServlet(new ServletContainer((ResourceConfig) configure())).build();
    }

    @Override
    protected Application configure() {
        resource = spy(new TestableTopicLookup());
        return new ResourceConfig().register(resource);
    }

    @Test
    public void testListenerNameV2() {
        Response response;
        // verify query param
        response = target(TOPIC_PATH).queryParam("listenerName", "query").request().get();
        assertEquals(response.getStatus(), 200);
        assertEquals(resource.actualListenerName, "query");

        // verify header param
        response = target(TOPIC_PATH).request().header("X-Pulsar-ListenerName", "header").get();
        assertEquals(response.getStatus(), 200);
        assertEquals(resource.actualListenerName, "header");

        // verify that query param supersedes the header param
        response = target(TOPIC_PATH).queryParam("listenerName", "query")
                .request().header("X-Pulsar-ListenerName", "header").get();
        assertEquals(response.getStatus(), 200);
        assertEquals(resource.actualListenerName, "query");
    }

    private static class TestableTopicLookup extends TopicLookup {
        private String actualListenerName;

        @Override
        protected void internalLookupTopicAsync(TopicName topicName, boolean authoritative, AsyncResponse asyncResponse,
                String listenerName) {
            Object o = pulsar();
            this.actualListenerName = listenerName;
            asyncResponse.resume(new LookupData());
        }
    }
}
