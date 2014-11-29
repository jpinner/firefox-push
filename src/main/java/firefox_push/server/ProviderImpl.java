package firefox_push.server;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jetty.npn.NextProtoNego;

public class ProviderImpl implements NextProtoNego.ServerProvider {

    @Override
    public void unsupported() {
    }

    @Override
    public List<String> protocols() {
        return Arrays.asList("spdy/3.1");
    }

    @Override
    public void protocolSelected(String protocol) {
    }
}
