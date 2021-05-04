package de.fraunhofer.iais.eis.ids.paris.main;

import de.fraunhofer.iais.eis.ids.component.core.DefaultComponent;
import de.fraunhofer.iais.eis.ids.component.core.RequestType;
import de.fraunhofer.iais.eis.ids.component.core.SelfDescriptionProvider;
import de.fraunhofer.iais.eis.ids.component.ecosystemintegration.daps.DapsSecurityTokenVerifier;
import de.fraunhofer.iais.eis.ids.component.ecosystemintegration.daps.JWKSFromIssuer;
import de.fraunhofer.iais.eis.ids.component.interaction.multipart.MultipartComponentInteractor;
import de.fraunhofer.iais.eis.ids.component.interaction.validation.ShaclValidator;
import de.fraunhofer.iais.eis.ids.connector.commons.broker.QueryHandler;
import de.fraunhofer.iais.eis.ids.index.common.main.AppConfigTemplate;
import de.fraunhofer.iais.eis.ids.index.common.persistence.*;
import de.fraunhofer.iais.eis.ids.paris.persistence.ParticipantPersistenceAndIndexing;
import de.fraunhofer.iais.eis.ids.paris.persistence.ParticipantRegistrationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


/**
 * This class is used to start up a ParIS with appropriate settings and is only created once from the Main class
 */
public class AppConfig extends AppConfigTemplate {

    Logger logger = LoggerFactory.getLogger(AppConfig.class);

    public AppConfig(SelfDescriptionProvider selfDescriptionProvider) {
        super(selfDescriptionProvider);
    }

    /**
     * This method creates a MultipartComponentInteractor with all settings which were previously configured. Appropriate message handlers are created
     * @return MultipartComponentInteractor with ParIS functionality
     */
    public MultipartComponentInteractor build() {
        //Try to pre-initialize the SHACL validation shapes so that this won't slow us down during message handling
        //TODO: Do this in a separate thread
        if(performShaclValidation) {
            try {
                ShaclValidator.initialize();
            } catch (IOException e) {
                logger.warn("Failed to initialize Shapes for SHACL validation.", e);
            }
        }
        RepositoryFacade repositoryFacade = new RepositoryFacade(sparqlEndpointUrl);

        ParticipantPersistenceAndIndexing participantPersistence = new ParticipantPersistenceAndIndexing(
                repositoryFacade);
        participantPersistence.setIndexing(new NullIndexing());
        if (contextDocumentUrl != null && !contextDocumentUrl.isEmpty()) {
            participantPersistence.setContextDocumentUrl(contextDocumentUrl);
            ConstructQueryResultHandler.contextDocumentUrl = contextDocumentUrl;
        }
        ConstructQueryResultHandler.catalogUri = catalogUri.toString();

        ParticipantRegistrationHandler registrationHandler = new ParticipantRegistrationHandler(participantPersistence, selfDescriptionProvider.getSelfDescription(), securityTokenProvider, responseSenderAgent);

        //TODO: implement ParticipantUnavailableValidationStrategy
        //  Task of this validation: Prevent signing off foreign participants
        //  This validation highly depends on the information provided in a Participant object, which is currently subject to changes
        //registrationHandler.addMapValidationStrategy(new ParticipantUnavailableValidationStrategy());
        QueryHandler queryHandler = new QueryHandler(selfDescriptionProvider.getSelfDescription(), participantPersistence, securityTokenProvider, responseSenderAgent);

        DefaultComponent component = new DefaultComponent(selfDescriptionProvider, securityTokenProvider, responseSenderAgent, false);
        DescriptionProvider descriptionProvider = new DescriptionProvider(selfDescriptionProvider.getSelfDescription(), repositoryFacade, catalogUri);
        DescriptionRequestHandler descriptionHandler = new DescriptionRequestHandler(descriptionProvider, securityTokenProvider, responseSenderAgent);
        component.addMessageHandler(registrationHandler, RequestType.INFRASTRUCTURE);
        component.addMessageHandler(queryHandler, RequestType.INFRASTRUCTURE);
//        component.addMessageHandler(requestHandler, RequestType.INFRASTRUCTURE);
        component.addMessageHandler(descriptionHandler, RequestType.INFRASTRUCTURE);
        //component.setSecurityTokenProvider(securityTokenProvider);

        if (dapsValidateIncoming) {
            logger.info("DAPS validation is enabled. Instantiating a DapsSecurityTokenVerifier.");
            component.setSecurityTokenVerifier(new DapsSecurityTokenVerifier(new JWKSFromIssuer(trustedJwksHosts)));
        }

        return new MultipartComponentInteractor(component, securityTokenProvider, responseSenderAgent, performShaclValidation);
    }

}
