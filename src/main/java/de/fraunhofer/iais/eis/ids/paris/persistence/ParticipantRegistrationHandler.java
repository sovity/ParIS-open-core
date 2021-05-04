package de.fraunhofer.iais.eis.ids.paris.persistence;

import de.fraunhofer.iais.eis.*;
import de.fraunhofer.iais.eis.ids.component.core.RejectMessageException;
import de.fraunhofer.iais.eis.ids.component.core.SecurityTokenProvider;
import de.fraunhofer.iais.eis.ids.component.core.TokenRetrievalException;
import de.fraunhofer.iais.eis.ids.component.core.map.DefaultSuccessMAP;
import de.fraunhofer.iais.eis.ids.connector.commons.broker.SameOriginParticipantMapValidationStrategy;
import de.fraunhofer.iais.eis.ids.connector.commons.messagevalidation.ValidatingMessageHandler;
import de.fraunhofer.iais.eis.ids.connector.commons.participant.map.ParticipantNotificationMAP;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;

/**
 * This class is a message handler for messages about the status of participants,
 * such as ParticipantAvailableMessages, ParticipantUpdateMessages, and ParticipantUnavailableMessages
 */
public class ParticipantRegistrationHandler extends ValidatingMessageHandler<ParticipantNotificationMAP, DefaultSuccessMAP> {
    private final InfrastructureComponent infrastructureComponent;
    private final ParticipantStatusHandler participantStatusHandler;
    private final SecurityTokenProvider securityTokenProvider;
    private final URI responseSenderAgent;

    /**
     * Constructor
     * @param participantStatusHandler The component which then takes care of persisting the changes
     * @param infrastructureComponent The ParIS as infrastructure component, such that appropriate responses can be sent
     * @param securityTokenProvider A security token provider for sending responses with a DAT
     * @param responseSenderAgent The "senderAgent" which should show in automatic response messages
     */
    public ParticipantRegistrationHandler(ParticipantStatusHandler participantStatusHandler, InfrastructureComponent infrastructureComponent, SecurityTokenProvider securityTokenProvider, URI responseSenderAgent) {
        this.infrastructureComponent = infrastructureComponent;
        this.participantStatusHandler = participantStatusHandler;
        this.addMapValidationStrategy(new SameOriginParticipantMapValidationStrategy());
        this.securityTokenProvider = securityTokenProvider;
        this.responseSenderAgent = responseSenderAgent;
    }

    /**
     * This function takes care of an inbound message which can be handled by this class
     * @param messageAndPayload The message to be handled
     * @return MessageProcessedNotification wrapped in a DefaultSuccessMAP, if the message has been processed properly
     * @throws RejectMessageException thrown, if the message could not be processed properly
     */
    @Override
    public DefaultSuccessMAP handleValidated(ParticipantNotificationMAP messageAndPayload) throws RejectMessageException {
        Message msg = messageAndPayload.getMessage();
        try {
            /*
            if (msg instanceof ParticipantAvailableMessage) {
                if (((ParticipantAvailableMessage) msg).getAffectedParticipant() != null) {
                    if (!messageAndPayload.getPayload().isPresent()) {
                        throw new RejectMessageException(RejectionReason.BAD_PARAMETERS, new NullPointerException("The Payload is null, but it must contain the affected Participant."));
                    }
                    this.participantStatusHandler.available(messageAndPayload.getPayload().get());
                } else {
                    throw new RejectMessageException(RejectionReason.BAD_PARAMETERS, new NullPointerException("Affected Participant is missing in the header"));
                }
            }
            else */
            if (msg instanceof ParticipantUpdateMessage) {
                if (((ParticipantUpdateMessage) msg).getAffectedParticipant() != null) {
                    if (messageAndPayload.getPayload().isEmpty()) {
                        throw new RejectMessageException(RejectionReason.BAD_PARAMETERS, new NullPointerException("The Payload is null, but it must contain the affected Participant."));
                    }
                    this.participantStatusHandler.updated(messageAndPayload.getPayload().get());
                } else {
                    throw new RejectMessageException(RejectionReason.BAD_PARAMETERS, new NullPointerException("Affected Participant is missing in the header"));
                }
            } else if (msg instanceof ParticipantUnavailableMessage) {
                if (((ParticipantUnavailableMessage) msg).getAffectedParticipant() != null) {
                    this.participantStatusHandler.unavailable(((ParticipantUnavailableMessage) msg).getAffectedParticipant());
                } else {
                    throw new RejectMessageException(RejectionReason.BAD_PARAMETERS, new NullPointerException("Affected Participant is missing in message header"));
                }
            }
        } catch (Exception e) {
            if (e instanceof RejectMessageException) {
                throw (RejectMessageException) e;
            }
            //For some reason, ConnectExceptions sometimes do not provide an exception message.
            //This causes a NullPointerException and returns an HTTP 500
            e.printStackTrace();
            if (e.getMessage() == null) {
                e = new Exception(e.getClass().getName() + " with empty message.");
            }
            throw new RejectMessageException(RejectionReason.INTERNAL_RECIPIENT_ERROR, e);
        }
        try {
            return new DefaultSuccessMAP(infrastructureComponent.getId(), infrastructureComponent.getOutboundModelVersion(), messageAndPayload.getMessage().getId(), securityTokenProvider.getSecurityTokenAsDAT(), responseSenderAgent);
        }
        catch (TokenRetrievalException e)
        {
            throw new RejectMessageException(RejectionReason.INTERNAL_RECIPIENT_ERROR, e);
        }
    }

    /**
     * This function provides a list of message types which are supported by this class
     * @return List of supported message types
     */
    @Override
    public Collection<Class<? extends Message>> getSupportedMessageTypes() {
        return Arrays.asList(ParticipantUnavailableMessage.class, ParticipantUpdateMessage.class);
    }
}
