package de.fraunhofer.iais.eis.ids.paris.persistence;

import de.fraunhofer.iais.eis.Participant;
import de.fraunhofer.iais.eis.ids.component.core.RejectMessageException;

import java.io.IOException;
import java.net.URI;

/**
 * Interface which describes the functionality required to provide an indexing service for participants
 */
public interface ParticipantStatusHandler {
        /**
         * Function to remove a given participant from the indexing and the triple store
         * Note that the participant should never be deleted physically, but only marked as deleted in the triple store and excluded from any search results
         * @param participantUri A URI reference to the participant which is now unavailable
         * @throws IOException may be thrown, if the connection to the triple store could not be established
         * @throws RejectMessageException may be thrown, if the operation is not permitted or due to an internal error
         */
        void unavailable(URI participantUri) throws IOException, RejectMessageException;

        /**
         * Function to persist and index modifications to an existing participant
         * @param participant The updated participant which was announced to the ParIS
         * @throws IOException may be thrown, if the connection to the repository could not be established
         * @throws RejectMessageException may be thrown, if the update is not permitted or if an internal error occurs
         */
        void updated(Participant participant) throws IOException, RejectMessageException;
}
