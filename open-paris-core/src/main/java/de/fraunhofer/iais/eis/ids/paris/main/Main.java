package de.fraunhofer.iais.eis.ids.paris.main;

import de.fraunhofer.iais.eis.ids.component.core.InfomodelFormalException;
import de.fraunhofer.iais.eis.ids.component.core.SelfDescriptionProvider;
import de.fraunhofer.iais.eis.ids.component.interaction.multipart.MultipartComponentInteractor;
import de.fraunhofer.iais.eis.ids.component.protocol.http.server.ComponentInteractorProvider;
import de.fraunhofer.iais.eis.ids.index.common.main.MainTemplate;
import de.fraunhofer.iais.eis.ids.paris.impl.ParISSelfDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.FileInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

/**
 * Entry point to the ParIS
 */
@Configuration
@EnableAutoConfiguration(exclude = SolrAutoConfiguration.class)
@ComponentScan(basePackages = { "de.fraunhofer.iais.eis.ids.component.protocol.http.server"} )

public class Main extends MainTemplate implements ComponentInteractorProvider {

    private final Logger logger = LoggerFactory.getLogger(Main.class);

    //Initializing properties which are not inherited from MainTemplate
    @Value("${sparql.url}")
    private String sparqlEndpointUrl;

    @Value("${infomodel.contextUrl}")
    private String contextDocumentUrl;

    @Value("${jwks.trustedHosts}")
    private Collection<String> trustedJwksHosts;

    @Value("${daps.validateIncoming}")
    private boolean dapsValidateIncoming;

    @Value("${component.responseSenderAgent}")
    private String responseSenderAgent;

    @Value("${infomodel.validateWithShacl}")
    private boolean validateShacl;

    //private RestHighLevelClient elasticsearchClient;

    //Environment allows us to access application.properties
    @Autowired
    private Environment env;

    public Main(FileInputStream javakeystore) {
        super(javakeystore);
    }

    @Override
    public SelfDescriptionProvider createSelfDescriptionProvider() throws URISyntaxException {
        return new ParISSelfDescription(
                new URI(componentUri),
                new URI(componentMaintainer),
                componentModelVersion);
    }

    /**
     * This function is called during startup and takes care of the initialization
     */
    @PostConstruct
    @Override
    public void setUp() {

        componentUri = env.getProperty("component.uri");
        componentMaintainer = env.getProperty("component.maintainer");
        componentCatalogUri = env.getProperty("component.catalogUri");
        componentModelVersion = env.getProperty("component.modelversion");
        sslCertificatePath = env.getProperty("ssl.certificatePath");
        keystorePassword = env.getProperty("keystore.password");
        keystoreAlias = env.getProperty("keystore.alias");
        //componentIdsId = env.getProperty("component.idsid");
        dapsUrl = env.getProperty("daps.url");
        trustAllCerts = Boolean.parseBoolean(env.getProperty("ssl.trustAllCerts"));
        ignoreHostName = Boolean.parseBoolean(env.getProperty("ssl.ignoreHostName"));

        try {
            //Open-Source version of ParIS has no indexing
            multipartComponentInteractor = new AppConfig(createSelfDescriptionProvider())
                    .sparqlEndpointUrl(sparqlEndpointUrl)
                    .contextDocumentUrl(contextDocumentUrl)
                    .securityTokenProvider(createSecurityTokenProvider())
                    .trustedJwksHosts(trustedJwksHosts)
                    .dapsValidateIncoming(dapsValidateIncoming)
                    .responseSenderAgent(new URI(responseSenderAgent))
                    .performShaclValidation(validateShacl)
                    .catalogUri(URI.create(componentCatalogUri))
                    .build();
        }
        catch (URISyntaxException e) {
            throw new InfomodelFormalException(e);
        }
    }

    @PreDestroy
    @Override
    public void shutDown() {

    }

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Override
    public MultipartComponentInteractor getComponentInteractor() {
        return multipartComponentInteractor;
    }
}
