package firefox_push.server;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

public class Main {

    private static void bind(int port) {
        // Configure the server.
        ServerBootstrap bootstrap = new ServerBootstrap(
                new NioServerSocketChannelFactory(
                        Executors.newCachedThreadPool(),
                        Executors.newCachedThreadPool()));

        // Enable TCP_NODELAY to handle pipelined requests without latency.
        bootstrap.setOption("child.tcpNoDelay", true);

        // Set up the event pipeline factory.
        bootstrap.setPipelineFactory(new ServerPipelineFactory());

        // Bind and start to accept incoming connections.
        bootstrap.bind(new InetSocketAddress(port));
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println(
                    "Usage: " + Main.class.getSimpleName() + " <port>");
            return;
        }
        int port = Integer.parseInt(args[0]);
        bind(port);
    }
}
