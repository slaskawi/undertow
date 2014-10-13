package io.undertow.server.protocol.http;

import io.undertow.UndertowOptions;
import io.undertow.server.HttpServerExchange;
import org.easymock.EasyMockRunner;
import org.easymock.IAnswer;
import org.easymock.Mock;
import org.easymock.MockType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xnio.OptionMap;
import org.xnio.Pool;
import org.xnio.Pooled;
import org.xnio.XnioIoThread;
import org.xnio.XnioWorker;
import org.xnio.conduits.ConduitStreamSourceChannel;
import org.xnio.conduits.StreamSinkConduit;
import org.xnio.conduits.StreamSourceConduit;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

/**
 * Tests Parse timeout for {@link io.undertow.server.protocol.http.HttpReadListener}.
 */
@RunWith(EasyMockRunner.class)
public class ParseTimeoutTestCase {

    @Mock(type = MockType.NICE)
    XnioIoThread mockXNioThred;

    @Mock(type = MockType.NICE)
    XnioWorker mockXNioWorker;

    @Mock(type = MockType.NICE)
    HttpServerConnection mockHttpServerConnection;

    @Mock(type = MockType.NICE)
    HttpRequestParser mockParser;

    @Mock(type = MockType.NICE)
    StreamSourceConduit mockStreamSourceConduit;

    @Mock(type = MockType.NICE)
    StreamSinkConduit mockStreamSinkConduit;

    @Mock(type = MockType.NICE)
    Pool<ByteBuffer> mockConnectionBuffer;

    @Mock(type = MockType.NICE)
    Pooled<ByteBuffer> mockPooledBuffer;

    ByteBuffer buffer = ByteBuffer.allocate(1024);

    /**
     * Tests if {@link HttpServerConnection#close()} method is invoked when {@link
     * UndertowOptions#REQUEST_PARSE_TIMEOUT} is properly defined.
     *
     * <p>Main idea for this test is to keep {@link io.undertow.server.protocol.http
     * .HttpReadListener#handleEventWithNoRunningRequest(org.xnio .conduits.ConduitStreamSourceChannel)} running in
     * the loop (read will always return 1024) and rely on Parse timeout to stop it.</p>
     */
    @Test(timeout = 1000)
    public void testIfParseTimeoutCloseConnection() throws Exception {
        //given
        OptionMap parseTimeoutOption = OptionMap.create(UndertowOptions.REQUEST_PARSE_TIMEOUT,
                10, UndertowOptions.MAX_HEADER_SIZE, Integer.MAX_VALUE);

        expect(mockXNioThred.getWorker()).andReturn(mockXNioWorker).anyTimes();
        expect(mockHttpServerConnection.getUndertowOptions()).andReturn(parseTimeoutOption).anyTimes();
        expect(mockHttpServerConnection.getOriginalSourceConduit()).andReturn(mockStreamSourceConduit).anyTimes();
        expect(mockHttpServerConnection.getOriginalSinkConduit()).andReturn(mockStreamSinkConduit).anyTimes();
        expect(mockHttpServerConnection.getBufferPool()).andReturn(mockConnectionBuffer).anyTimes();
        expect(mockConnectionBuffer.allocate()).andReturn(mockPooledBuffer).anyTimes();
        expect(mockPooledBuffer.getResource()).andReturn(buffer).anyTimes();
        expect(mockStreamSourceConduit.read(anyObject(ByteBuffer.class))).andReturn(1024).anyTimes();

        mockParser.handle(anyObject(ByteBuffer.class), anyObject(ParseState.class),
                anyObject(HttpServerExchange.class));
        expectLastCall().andAnswer(new IAnswer<Void>() {
            public Void answer() throws InterruptedException {
                TimeUnit.MILLISECONDS.sleep(20);
                return null;
            }
        });

        //verification
        mockHttpServerConnection.close();
        expectLastCall().once();

        replay(mockStreamSourceConduit, mockXNioThred, mockHttpServerConnection, mockConnectionBuffer, mockPooledBuffer, mockParser);

        HttpReadListener readListener = new HttpReadListener(mockHttpServerConnection, mockParser, null);
        ConduitStreamSourceChannel event = new ConduitStreamSourceChannel(mockXNioWorker,
                mockStreamSourceConduit);

        //when
        readListener.handleEventWithNoRunningRequest(event);

        //then
        verify(mockHttpServerConnection);
    }
}