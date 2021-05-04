package de.fraunhofer.iais.eis.ids.paris.persistence;

import de.fraunhofer.iais.eis.Participant;
import de.fraunhofer.iais.eis.ids.component.core.RejectMessageException;

import java.net.URI;

/**
 * Interface which describes the functionality required to provide a query endpoint for querying participants
 */
public interface ParticipantQueryHandler {
    /**
     * Function to request all information one may obtain about a certain participant. Access control may apply
     * @param participantUri URI reference to a participant which is requested
     * @return All known information about a participant which is allowed to be announced to the requesting party
     * @throws RejectMessageException may be thrown, if the participant is not known to the ParIS or if an internal error has occurred
     */
    Participant requestParticipant(URI participantUri) throws RejectMessageException;
}
