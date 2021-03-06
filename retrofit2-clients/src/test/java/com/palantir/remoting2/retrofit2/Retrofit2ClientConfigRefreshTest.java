/*
 * Copyright 2017 Palantir Technologies, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.remoting2.retrofit2;

import static org.assertj.core.api.Assertions.assertThat;

import com.palantir.remoting2.config.service.ServiceConfiguration;
import com.palantir.remoting2.ext.refresh.Refreshable;
import java.util.Optional;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Rule;
import org.junit.Test;


public final class Retrofit2ClientConfigRefreshTest {

    @Rule
    public final MockWebServer server1 = new MockWebServer();
    @Rule
    public final MockWebServer server2 = new MockWebServer();

    @Test
    public void testConfigRefresh() throws Exception {

        ServiceConfiguration config1 =
                ServiceConfiguration.of("http://localhost:" + server1.getPort(), Optional.empty());
        ServiceConfiguration config2 =
                ServiceConfiguration.of("http://localhost:" + server2.getPort(), Optional.empty());

        Refreshable<ServiceConfiguration> refreshableConfig = Refreshable.of(config1);
        TestService proxy = Retrofit2Client.create(TestService.class, "agent", refreshableConfig);

        // Call 1
        server1.enqueue(new MockResponse().setBody("\"server1\""));
        assertThat(proxy.get().execute().body()).isEqualTo("server1");
        assertThat(server1.getRequestCount()).isEqualTo(1);
        assertThat(server2.getRequestCount()).isEqualTo(0);

        // Call 2
        server1.enqueue(new MockResponse().setBody("\"server1\""));
        assertThat(proxy.get().execute().body()).isEqualTo("server1");
        assertThat(server1.getRequestCount()).isEqualTo(2);
        assertThat(server2.getRequestCount()).isEqualTo(0);

        // Switch config
        refreshableConfig.set(config2);

        // Call 3
        server2.enqueue(new MockResponse().setBody("\"server2\""));
        assertThat(proxy.get().execute().body()).isEqualTo("server2");
        assertThat(server1.getRequestCount()).isEqualTo(2);
        assertThat(server2.getRequestCount()).isEqualTo(1);

        // Call 4
        server2.enqueue(new MockResponse().setBody("\"server2\""));
        assertThat(proxy.get().execute().body()).isEqualTo("server2");
        assertThat(server1.getRequestCount()).isEqualTo(2);
        assertThat(server2.getRequestCount()).isEqualTo(2);
    }
}
