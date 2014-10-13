///*
// * JBoss, Home of Professional Open Source.
// * Copyright 2014 Red Hat, Inc., and individual contributors
// * as indicated by the @author tags.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// *  Unless required by applicable law or agreed to in writing, software
// *  distributed under the License is distributed on an "AS IS" BASIS,
// *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// *  See the License for the specific language governing permissions and
// *  limitations under the License.
// */
//
//package io.undertow.server;
//
//import io.undertow.UndertowOptions;
//import io.undertow.testutils.DefaultServer;
//import io.undertow.testutils.HttpClientUtils;
//import io.undertow.testutils.HttpOneOnly;
//import io.undertow.testutils.ProxyIgnore;
//import io.undertow.testutils.TestHttpClient;
//import io.undertow.util.Headers;
//import org.apache.http.HttpResponse;
//import org.apache.http.client.methods.HttpPost;
//import org.apache.http.entity.AbstractHttpEntity;
//import org.apache.http.entity.StringEntity;
//import org.junit.After;
//import org.junit.Before;
//import org.junit.BeforeClass;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.xnio.OptionMap;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
//import java.net.SocketException;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.fail;
//
///**
// * Tests positive and negative scenario for Parse timeout.
// *
// * @author Sebastian Laskawiec
// * @see io.undertow.UndertowOptions#REQUEST_PARSE_TIMEOUT
// */
//@HttpOneOnly
//@ProxyIgnore
//@RunWith(DefaultServer.class)
//public class ParseTimeoutTestCase {
//
//    private TestHttpClient client;
//
//    @BeforeClass
//    public static void setup() {
//        DefaultServer.setRootHandler(new HttpHandler() {
//            @Override
//            public void handleRequest(final HttpServerExchange exchange) throws Exception {
//                exchange.endExchange();
//            }
//        });
//    }
//
//    @Before
//    public void before() {
//        DefaultServer.setUndertowOptions(OptionMap.EMPTY);
//        client = new TestHttpClient();
//    }
//
//    @After
//    public void after() {
//        client.getConnectionManager().shutdown();
//    }
//
//    @Test
//    public void testPassingRequestWithoutParseTimeout() throws Exception {
//        //given
//        HttpPost post = new HttpPost(DefaultServer.getDefaultServerURL() + "/notamatchingpath");
//        post.setEntity(new StringEntity("test"));
//        post.addHeader(Headers.CONNECTION_STRING, "close");
//
//        //when
//        HttpResponse result = client.execute(post);
//        HttpClientUtils.readResponse(result);
//
//        //then
//        assertEquals(200, result.getStatusLine().getStatusCode());
//    }
//
//    /**
//     * Adds a lot of work for {@link io.undertow.server.protocol.http.HttpRequestParser} and make it fail on a very
//     * short timeout (1 ms).
//     */
//    @Test
//    public void testNoResponseOnParseTimeout() throws Exception {
//        //given
//        OptionMap parseTimeoutOption = OptionMap.create(UndertowOptions.REQUEST_PARSE_TIMEOUT, 1);
//        DefaultServer.setUndertowOptions(parseTimeoutOption);
//
//        HttpPost post = new HttpPost(DefaultServer.getDefaultServerURL() + "/path");
//        post.setEntity(new AbstractHttpEntity() {
//
//            @Override
//            public InputStream getContent() throws IOException, IllegalStateException {
//                return null;
//            }
//
//            @Override
//            public void writeTo(final OutputStream outstream) throws IOException {
//                for (int i = 0; i < 10; ++i) {
//                    outstream.write('*');
//                    outstream.flush();
//                    try {
//                        Thread.sleep(1);
//                    } catch (InterruptedException e) {
//                        throw new RuntimeException(e);
//                    }
//                }
//            }
//
//            @Override
//            public boolean isStreaming() {
//                return true;
//            }
//
//            @Override
//            public boolean isRepeatable() {
//                return false;
//            }
//
//            @Override
//            public long getContentLength() {
//                return 10;
//            }
//        });
//
//        //when //then
//        try {
//            HttpClientUtils.readResponse(client.execute(post));
//            fail();
//        } catch (SocketException expected) {
//            assertEquals("Connection reset", expected.getMessage());
//        }
//    }
//
//}
