package firefox_push.server;

import java.security.KeyStore;
import java.security.Security;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

public class LocalhostSslContext {

    private static final String PROTOCOL = "TLS";
    private static final SSLContext SERVER_CONTEXT;

    static {
        String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
        if (algorithm == null) {
            algorithm = "SunX509";
        }

        SSLContext serverContext = null;
        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(LocalhostKeyStore.asInputStream(),
                    LocalhostKeyStore.getKeyStorePassword());

            // Set up key manager factory to use our key store
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
            kmf.init(ks, LocalhostKeyStore.getCertificatePassword());

            // Initialize the SSLContext to work with our key managers.
            serverContext = SSLContext.getInstance(PROTOCOL);
            serverContext.init(kmf.getKeyManagers(), null, null);
        } catch (Exception e) {
            throw new Error(
                    "Failed to initialize the server-side SSLContext", e);
        }

        SERVER_CONTEXT = serverContext;
    }

    public static SSLContext getServerContext() {
        return SERVER_CONTEXT;
    }

    private LocalhostSslContext() {
        // Unused
    }
}
