package de.fraunhofer.iais.eis.ids.paris.impl;

import de.fraunhofer.iais.eis.*;
import de.fraunhofer.iais.eis.ids.component.core.SelfDescriptionProvider;
import de.fraunhofer.iais.eis.util.TypedLiteral;
import de.fraunhofer.iais.eis.util.Util;

import java.net.URI;

import static de.fraunhofer.iais.eis.util.Util.asList;

/**
 * This class is used to create a self description document for an IDS Participant Information Service (ParIS)
 * The description contains information about the operator, security related information and a reference to the catalog
 */
public class ParISSelfDescription implements SelfDescriptionProvider {

    private final URI componentId, maintainerId;
    private final String modelVersion;

    /**
     * This is the constructor for the self-description provider of a Participant Information Service
     * @param componentId The ID of this ParIS
     * @param maintainerId The maintainer of this ParIS
     * @param modelVersion The IDS information model version the ParIS is using
     */
    public ParISSelfDescription(URI componentId, URI maintainerId, String modelVersion) {
        this.componentId = componentId;
        this.maintainerId = maintainerId;
        this.modelVersion = modelVersion;
    }

    //TODO: This description should be extended to encompass a description of the available endpoints
    //  We possibly also need to add further fields, e.g. a SHA256 hash of the transport certificate
    @Override
    public InfrastructureComponent getSelfDescription() {
        return new ParISBuilder(componentId)
                ._title_(asList(new TypedLiteral("EIS ParIS", "en")))
                ._description_(asList(new TypedLiteral("A Participant Information Service with a graph persistence layer", "en")))
                ._maintainer_(maintainerId)
                ._curator_(maintainerId)
                ._inboundModelVersion_(Util.asList(modelVersion))
                ._outboundModelVersion_(modelVersion)
                ._securityProfile_(SecurityProfile.BASE_SECURITY_PROFILE)
                ._resourceCatalog_(Util.asList(new ResourceCatalogBuilder()._offeredResource_(Util.asList(new ResourceBuilder().build())).build())) //empty catalog
                ._hasDefaultEndpoint_(new ConnectorEndpointBuilder()._accessURL_(URI.create(componentId.toString() + "infrastructure")).build())
                .build();
    }

}
