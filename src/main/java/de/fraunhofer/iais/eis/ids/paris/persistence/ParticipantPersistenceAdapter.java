package de.fraunhofer.iais.eis.ids.paris.persistence;

import de.fraunhofer.iais.eis.Participant;
import de.fraunhofer.iais.eis.ids.component.core.RejectMessageException;
import de.fraunhofer.iais.eis.ids.connector.commons.broker.QueryResultsProvider;

import java.io.IOException;
import java.net.URI;

/**
 * Abstract class which provides the required functions for building a persistence adapter for IDS Participants
 */
public abstract class ParticipantPersistenceAdapter implements ParticipantStatusHandler, QueryResultsProvider {

    /**
     * This function updates an existing participant in the triple store and updates the index correspondingly
     * It should be called when a ParticipantUpdateMessage was received
     * @param participant The updated participant which was announced to the ParIS
     * @throws IOException may be thrown if the connection to the triple store or index fails
     * @throws RejectMessageException may be thrown if, for example, the participant doesn't exist yet or some internal error occurs
     */
    @Override
    public abstract void updated(Participant participant) throws IOException, RejectMessageException;

    /**
     * This function removes an existing participant from the triple store and updates the index correspondingly
     * Note that deletion should not be physical, but rather mark the participant as deleted and treat any queries on the triple store as if this participant didn't exist anymore
     * It should be called when a ParticipantUnavailableMessage was received
     * @param participantUri A URI reference to the participant which is now unavailable
     * @throws IOException may be thrown if the connection to the triple store or index fails
     * @throws RejectMessageException may be thrown if, for example, the participant doesn't exist yet or some internal error occurs
     */
    @Override
    public abstract void unavailable(URI participantUri) throws IOException, RejectMessageException;
}
