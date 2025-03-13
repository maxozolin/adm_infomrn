package com.edi.service;

import javax.xml.ws.BindingProvider;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.HttpsURLConnection;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.transports.http.configuration.ProxyServerType;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.crypto.Merlin;
import org.apache.wss4j.common.ext.WSSecurityException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Enumeration;
import java.util.Collections;

// Import the generated classes
import it.sogei.domest.infomrnfp.services.InfoMRNFP;
import it.sogei.domest.infomrnfp.services.InfoMRNFP_Service;
import it.sogei.domest.infomrnfp.Richiesta;
import it.sogei.domest.infomrnfp.ObjectFactory;
import it.sogei.ws.output.Risposta;

/**
 * Client for the InfoMRN web service.
 */
public class MrnInfo {
    private static MrnInfo instance;
    private final InfoMRNFP client;
    private final String endpointUrl;
    private final String p12CertificatePath;
    private final String password;
    private String alias;

    static {
        // Disable SSL certificate validation
        disableSslValidation();
    }

    private MrnInfo(String endpointUrl, String p12CertificatePath, String password) {
        this.endpointUrl = endpointUrl;
        this.p12CertificatePath = p12CertificatePath;
        this.password = password;

        try {
            // Find the alias in the PKCS12 keystore
            this.alias = findAliasFromP12(p12CertificatePath, password);
            System.out.println("Using certificate alias: " + alias);
        } catch (Exception e) {
            throw new RuntimeException("Error finding alias in P12 certificate", e);
        }

        // Create the service and get the port
        InfoMRNFP_Service service = new InfoMRNFP_Service();
        this.client = service.getInfoMRNFPService();

        // Configure the endpoint URL
        BindingProvider bindingProvider = (BindingProvider) client;
        bindingProvider.getRequestContext().put(
                BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                endpointUrl);

        // Configure proxy and disable SSL validation
        configureProxyAndSsl();

        // Configure WS-Security with X.509 certificate
        configureWSSecurityWithX509();
    }

    /**
     * Disables SSL certificate validation globally
     */
    private static void disableSslValidation() {
        try {
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };

            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Disable hostname verification
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

            System.out.println("SSL certificate validation disabled");
        } catch (Exception e) {
            throw new RuntimeException("Could not disable SSL validation", e);
        }
    }

    /**
     * Configures proxy settings and SSL for the client
     */
    private void configureProxyAndSsl() {

        boolean ENABLE_PROXY = false;

        Client clientProxy = ClientProxy.getClient(client);
        HTTPConduit conduit = (HTTPConduit) clientProxy.getConduit();

        // Configure HTTP client policy
        HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();
        if (ENABLE_PROXY) {

            // Set proxy server
            httpClientPolicy.setProxyServer("127.0.0.1");
            httpClientPolicy.setProxyServerPort(8080);
            httpClientPolicy.setProxyServerType(ProxyServerType.HTTP);
            // Set connection timeouts
            httpClientPolicy.setConnectionTimeout(36000);
            httpClientPolicy.setReceiveTimeout(32000);
            httpClientPolicy.setAllowChunking(false);

            // Add proxy authentication if needed
            // httpClientPolicy.setProxyServerUsername("username");
            // httpClientPolicy.setProxyServerPassword("password");
        }

        // Set additional HTTP headers if needed
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("User-Agent", Collections.singletonList("EDI Service Client"));
        BindingProvider bp = (BindingProvider) client;
        bp.getRequestContext().put(org.apache.cxf.message.Message.PROTOCOL_HEADERS, headers);

        conduit.setClient(httpClientPolicy);

        // Disable SSL certificate validation for this specific client
        conduit.setTlsClientParameters(new org.apache.cxf.configuration.jsse.TLSClientParameters() {
            {
                setDisableCNCheck(true);
                setTrustManagers(new TrustManager[] {
                        new X509TrustManager() {
                            public X509Certificate[] getAcceptedIssuers() {
                                return null;
                            }

                            public void checkClientTrusted(X509Certificate[] certs, String authType) {
                            }

                            public void checkServerTrusted(X509Certificate[] certs, String authType) {
                            }
                        }
                });
            }
        });

        // Set system properties for proxy
        if (ENABLE_PROXY) {
            System.setProperty("http.proxyHost", "127.0.0.1");
            System.setProperty("http.proxyPort", "8080");
            System.setProperty("https.proxyHost", "127.0.0.1");
            System.setProperty("https.proxyPort", "8080");
            System.out.println("Proxy configured to 127.0.0.1:8080");
        }
        System.out.println("Connection timeout: " + httpClientPolicy.getConnectionTimeout() + "ms");
        System.out.println("Receive timeout: " + httpClientPolicy.getReceiveTimeout() + "ms");

    }

    public static synchronized MrnInfo getInstance() {
        if (instance == null) {
            throw new IllegalStateException("MrnInfo not initialized. Call initialize() first.");
        }
        return instance;
    }

    public static synchronized void initialize(String endpointUrl, String p12CertificatePath, String password) {
        if (instance == null) {
            instance = new MrnInfo(endpointUrl, p12CertificatePath, password);
        }
    }

    private String findAliasFromP12(String p12Path, String password) throws Exception {
        KeyStore keystore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(p12Path)) {
            keystore.load(fis, password.toCharArray());

            // Get the first alias in the keystore
            Enumeration<String> aliases = keystore.aliases();
            if (aliases.hasMoreElements()) {
                return aliases.nextElement();
            } else {
                throw new Exception("No aliases found in the P12 certificate");
            }
        }
    }

    private void configureWSSecurityWithX509() {
        try {
            Client clientProxy = ClientProxy.getClient(client);
            Map<String, Object> outProps = new HashMap<>();

            // Set up the configuration for WS-Security
            outProps.put(WSHandlerConstants.ACTION, WSHandlerConstants.SIGNATURE);
            outProps.put(WSHandlerConstants.SIG_PROP_FILE, "crypto.properties");
            outProps.put(WSHandlerConstants.USER, alias);
            outProps.put(WSHandlerConstants.PW_CALLBACK_CLASS, PasswordCallback.class.getName());
            outProps.put(WSHandlerConstants.SIG_KEY_ID, "DirectReference");
            outProps.put(WSHandlerConstants.SIG_ALGO, "http://www.w3.org/2000/09/xmldsig#rsa-sha1");
            outProps.put(WSHandlerConstants.SIG_DIGEST_ALGO, "http://www.w3.org/2000/09/xmldsig#sha1");

            // Create crypto.properties file dynamically
            createCryptoProperties();

            WSS4JOutInterceptor wssOut = new WSS4JOutInterceptor(outProps);
            clientProxy.getOutInterceptors().add(wssOut);
        } catch (Exception e) {
            throw new RuntimeException("Error configuring WS-Security with X.509", e);
        }
    }

    private void createCryptoProperties() throws IOException {
        Properties props = new Properties();
        props.put("org.apache.wss4j.crypto.provider", "org.apache.wss4j.common.crypto.Merlin");
        props.put("org.apache.wss4j.crypto.merlin.keystore.type", "PKCS12");
        props.put("org.apache.wss4j.crypto.merlin.keystore.password", password);
        props.put("org.apache.wss4j.crypto.merlin.keystore.alias", alias);
        props.put("org.apache.wss4j.crypto.merlin.keystore.file", p12CertificatePath);

        // Save properties to a file
        File propsFile = new File("crypto.properties");
        try (FileOutputStream fos = new FileOutputStream(propsFile)) {
            props.store(fos, "Crypto Configuration");
        } catch (IOException e) {
            throw new RuntimeException("Error creating crypto.properties file", e);
        }
    }

    public Risposta process(Richiesta request) {
        try {
            return client.process(request);
        } catch (Exception e) {
            throw new RuntimeException("Error processing MRN request", e);
        }
    }

    // Helper method to create a new request
    public static Richiesta createRequest(String dichiarante, byte[] xmlData) {
        ObjectFactory factory = new ObjectFactory();
        Richiesta request = factory.createRichiesta();

        // Set the service ID as required by the schema
        request.setServiceId("infoMrn");

        // Create and set the data part
        Richiesta.Data data = factory.createRichiestaData();
        data.setDichiarante(dichiarante);
        data.setXml(xmlData);
        request.setData(data);

        return request;
    }
}
