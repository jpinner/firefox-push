package firefox_push.server;

import javax.net.ssl.SSLEngine;

import org.eclipse.jetty.npn.NextProtoNego;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.spdy.SpdyFrameCodec;
import org.jboss.netty.handler.codec.spdy.SpdySessionHandler;
import org.jboss.netty.handler.codec.spdy.SpdyVersion;
import org.jboss.netty.handler.ssl.SslHandler;

public class ServerPipelineFactory implements ChannelPipelineFactory {

    public ChannelPipeline getPipeline() throws Exception {
        SSLEngine sslEngine = LocalhostSslContext.getServerContext().createSSLEngine();
        sslEngine.setUseClientMode(false);

        NextProtoNego.put(sslEngine, new ProviderImpl());

        ChannelPipeline pipeline = Channels.pipeline();
        pipeline.addLast("sslHandler", new SslHandler(sslEngine));
        pipeline.addLast("spdyFrameCodec", new SpdyFrameCodec(SpdyVersion.SPDY_3_1));
        pipeline.addLast("spdySessionHandler", new SpdySessionHandler(SpdyVersion.SPDY_3_1, true));
        pipeline.addLast("pushTestHandler", new PushTestHandler());
        return pipeline;
    }
}
