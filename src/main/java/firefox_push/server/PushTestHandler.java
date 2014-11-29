package firefox_push.server;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.codec.spdy.DefaultSpdyDataFrame;
import org.jboss.netty.handler.codec.spdy.DefaultSpdySynReplyFrame;
import org.jboss.netty.handler.codec.spdy.DefaultSpdySynStreamFrame;
import org.jboss.netty.handler.codec.spdy.SpdyDataFrame;
import org.jboss.netty.handler.codec.spdy.SpdyHeaders;
import org.jboss.netty.handler.codec.spdy.SpdySynReplyFrame;
import org.jboss.netty.handler.codec.spdy.SpdySynStreamFrame;

public class PushTestHandler extends SimpleChannelUpstreamHandler {

    private static String HTML_BODY_OPEN = "<html><body>";
    private static String HTML_BODY_CLOSE = "</body></html>";

    private static String ROOT_BODY =
            HTML_BODY_OPEN +
            "Firefox Push Test" +
            "<p><a href=\"/iframe-test\">iframe push</a></p>" +
            "<p><a href=\"/object-test\">object push</a></p>" +
            HTML_BODY_CLOSE;

    private static String getIFrameBody(String scheme, String host) {
        return HTML_BODY_OPEN + "<iframe src=\"" +
                scheme + "://" + host + "/hello-world" +
                "\"></iframe>" + HTML_BODY_CLOSE;
    }

    private static String getObjectBody(String scheme, String host) {
        return HTML_BODY_OPEN + "<object type=\"text/html\" data=\"" +
                scheme + "://" + host + "/hello-world" +
                "\"><p>pushed content</p></object>" + HTML_BODY_CLOSE;
    }

    private AtomicInteger pushStreamIds = new AtomicInteger(2);

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        Object message = e.getMessage();

        if (message instanceof SpdySynStreamFrame) {
            SpdySynStreamFrame spdySynStreamFrame = (SpdySynStreamFrame) message;

            int streamId = spdySynStreamFrame.getStreamId();
            String scheme = SpdyHeaders.getScheme(3, spdySynStreamFrame);
            String url = SpdyHeaders.getUrl(3, spdySynStreamFrame);
            String host = SpdyHeaders.getHost(spdySynStreamFrame);
            HttpVersion httpVersion = SpdyHeaders.getVersion(3, spdySynStreamFrame);

            if ("/".equals(url)) {
                // render selection menu for iframe or embed test

                SpdySynReplyFrame spdySynReplyFrame = new DefaultSpdySynReplyFrame(streamId);
                SpdyHeaders.setVersion(3, spdySynReplyFrame, httpVersion);
                SpdyHeaders.setStatus(3, spdySynReplyFrame, HttpResponseStatus.OK);
                Channels.write(ctx.getChannel(), spdySynReplyFrame);

                SpdyDataFrame spdyDataFrame = new DefaultSpdyDataFrame(streamId);
                spdyDataFrame.setLast(true);
                spdyDataFrame.setData(ChannelBuffers.copiedBuffer(ROOT_BODY, StandardCharsets.US_ASCII));
                Channels.write(ctx.getChannel(), spdyDataFrame);
            } else if ("/iframe-test".equals(url)) {
                // render hello-world in <iframe> tag

                String body = getIFrameBody(scheme, host);

                SpdySynReplyFrame spdySynReplyFrame = new DefaultSpdySynReplyFrame(streamId);
                SpdyHeaders.setVersion(3, spdySynReplyFrame, httpVersion);
                SpdyHeaders.setStatus(3, spdySynReplyFrame, HttpResponseStatus.OK);
                Channels.write(ctx.getChannel(), spdySynReplyFrame);

                // Push "/hello-world"
                pushHelloWorld(ctx, spdySynStreamFrame);

                SpdyDataFrame spdyDataFrame = new DefaultSpdyDataFrame(streamId);
                spdyDataFrame.setLast(true);
                spdyDataFrame.setData(ChannelBuffers.copiedBuffer(body, StandardCharsets.US_ASCII));
                Channels.write(ctx.getChannel(), spdyDataFrame);
            } else if ("/object-test".equals(url)) {
                // render hello-world in <object> tag
                String body = getObjectBody(scheme, host);

                SpdySynReplyFrame spdySynReplyFrame = new DefaultSpdySynReplyFrame(streamId);
                SpdyHeaders.setVersion(3, spdySynReplyFrame, httpVersion);
                SpdyHeaders.setStatus(3, spdySynReplyFrame, HttpResponseStatus.OK);
                Channels.write(ctx.getChannel(), spdySynReplyFrame);

                // Push "/hello-world"
                pushHelloWorld(ctx, spdySynStreamFrame);

                SpdyDataFrame spdyDataFrame = new DefaultSpdyDataFrame(streamId);
                spdyDataFrame.setLast(true);
                spdyDataFrame.setData(ChannelBuffers.copiedBuffer(body, StandardCharsets.US_ASCII));
                Channels.write(ctx.getChannel(), spdyDataFrame);
            } else if ("/hello-world".equals(url)) {
                // Why request a pushed resource?
                String body = HTML_BODY_OPEN + "Y U No Push?" + HTML_BODY_CLOSE;

                SpdySynReplyFrame spdySynReplyFrame = new DefaultSpdySynReplyFrame(streamId);
                SpdyHeaders.setVersion(3, spdySynReplyFrame, httpVersion);
                SpdyHeaders.setStatus(3, spdySynReplyFrame, HttpResponseStatus.OK);
                Channels.write(ctx.getChannel(), spdySynReplyFrame);

                SpdyDataFrame spdyDataFrame = new DefaultSpdyDataFrame(streamId);
                spdyDataFrame.setLast(true);
                spdyDataFrame.setData(ChannelBuffers.copiedBuffer(body, StandardCharsets.US_ASCII));
                Channels.write(ctx.getChannel(), spdyDataFrame);
            } else {
                // not found
                SpdySynReplyFrame spdySynReplyFrame = new DefaultSpdySynReplyFrame(streamId);
                SpdyHeaders.setVersion(3, spdySynReplyFrame, httpVersion);
                SpdyHeaders.setStatus(3, spdySynReplyFrame, HttpResponseStatus.NOT_FOUND);
                Channels.write(ctx.getChannel(), spdySynReplyFrame);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        Channels.close(ctx.getChannel());
    }

    private void pushHelloWorld(ChannelHandlerContext ctx, SpdySynStreamFrame spdySynStreamFrame) {
        String scheme = SpdyHeaders.getScheme(3, spdySynStreamFrame);
        String host = SpdyHeaders.getHost(spdySynStreamFrame);
        HttpVersion httpVersion = SpdyHeaders.getVersion(3, spdySynStreamFrame);

        int pushStreamId = pushStreamIds.getAndAdd(2);
        SpdySynStreamFrame pushedSynStreamFrame = new DefaultSpdySynStreamFrame(
                pushStreamId, spdySynStreamFrame.getStreamId(), (byte) 0);
        pushedSynStreamFrame.setUnidirectional(true);
        SpdyHeaders.setScheme(3, pushedSynStreamFrame, scheme);
        SpdyHeaders.setUrl(3, pushedSynStreamFrame, "/hello-world");
        SpdyHeaders.setHost(pushedSynStreamFrame, host);
        SpdyHeaders.setVersion(3, pushedSynStreamFrame, httpVersion);
        SpdyHeaders.setStatus(3, pushedSynStreamFrame, HttpResponseStatus.OK);
        Channels.write(ctx.getChannel(), pushedSynStreamFrame);

        SpdyDataFrame pushedDataFrame = new DefaultSpdyDataFrame(pushStreamId);
        pushedDataFrame.setLast(true);
        pushedDataFrame.setData(ChannelBuffers.copiedBuffer("Hello, World!", StandardCharsets.US_ASCII));
        Channels.write(ctx.getChannel(), pushedDataFrame);
    }
}
