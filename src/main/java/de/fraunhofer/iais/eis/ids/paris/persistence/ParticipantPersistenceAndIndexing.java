package de.fraunhofer.iais.eis.ids.paris.persistence;

import de.fraunhofer.iais.eis.Participant;
import de.fraunhofer.iais.eis.RejectionReason;
import de.fraunhofer.iais.eis.ids.component.core.RejectMessageException;
import de.fraunhofer.iais.eis.ids.index.common.persistence.*;
import de.fraunhofer.iais.eis.ids.index.common.persistence.spi.Indexing;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.ARQException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This class takes care of persisting and indexing any changes to participants that are announced to the ParIS
 */
public class ParticipantPersistenceAndIndexing extends ParticipantPersistenceAdapter implements ParticipantQueryHandler {
    private final RepositoryFacade repositoryFacade;
    private Indexing indexing = new NullIndexing();
    private final ParticipantModelCreator participantModelCreator = new ParticipantModelCreator();
    private final Logger logger = LoggerFactory.getLogger(ParticipantPersistenceAndIndexing.class);

    /**
     * Constructor
     * @param repositoryFacade repository (triple store) to which the modifications should be stored
     */
    public ParticipantPersistenceAndIndexing(RepositoryFacade repositoryFacade) {
        this.repositoryFacade = repositoryFacade;
        Date date=new Date();
        Timer timer = new Timer();

        //Regularly recreate the index to keep index and triple store in sync
        //The triple store is considered as single source of truth, so the index is dropped and recreated from the triple store
        timer.schedule(new TimerTask(){
            public void run(){
                refreshIndex();
            }
        },date, 12*60*60*1000); //12*60*60*1000 add 12 hours delay between job executions.
    }

    /**
     * Setter for the indexing method
     * @param indexing indexing to be used
     */
    public void setIndexing(Indexing indexing) {
        this.indexing = indexing;
    }

    /**
     * Setter for the context document URL. Typically extracted from the application.properties
     * @param contextDocumentUrl the context document URL to be used
     */
    public void setContextDocumentUrl(String contextDocumentUrl) {
        participantModelCreator.setContextFetchStrategy(JsonLdContextFetchStrategy.FROM_URL, contextDocumentUrl);
    }

    /**
     * Function to refresh the index. The index is dropped entirely and recreated from the triple store
     * This keeps the index and triple store in sync, while respecting the triple store as single source of truth
     */
    public void refreshIndex() {
        //Recreate the index to delete everything
        try {
            logger.info("Refreshing index.");
            indexing.recreateIndex("registrations");

            //Iterate over all active graphs, i.e. non-passivated and non-deleted graphs
            for (String graph : repositoryFacade.getActiveGraphs()) {
                //Add each connector to the index
                logger.info("Adding participant " + graph + " to index.");
                indexing.add(repositoryFacade.getParticipantFromTripleStore(new URI(graph)));
            }
        }
        catch (ConnectException ignored){} //Prevent startup error
        catch (IOException | URISyntaxException | RejectMessageException e)
        {
            logger.error("Failed to refresh index: ", e);
        }
    }

    /**
     * Function to persist new participants and to index modifications to an existing participant
     * @param participant The updated participant which was announced to the ParIS
     * @throws IOException thrown, if the connection to the repository could not be established
     * @throws RejectMessageException thrown, if the update is not permitted, e.g. because the participant was previously deleted, or if an internal error occurs
     */
    @Override
    public void updated(Participant participant) throws IOException, RejectMessageException {
        if(!repositoryFacade.graphExists(participant.getId().toString()))
        {
            addToTriplestore(participant.toRdf());
            indexing.add(participant);
            return;
        }
        boolean wasActive = repositoryFacade.graphIsActive(participant.getId().toString());
        updateTriplestore(participant.toRdf());
        //We need to reflect the changes in the index.
        //If the connector was passive before, the document was deleted from the index, so we need to recreate it
        if(wasActive) { //Connector exists in index - update it
            try {
                indexing.update(participant);
            }
            catch (Exception e)
            {
                if(e.getMessage().contains("document_missing_exception")) { //Elasticsearch specific check
                    indexing.add(participant);
                }
                else
                {
                    logger.error("Exception caught with message " + e.getMessage());
                    throw new RejectMessageException(RejectionReason.INTERNAL_RECIPIENT_ERROR, e);
                }
            }
        }
        else
        { //Connector does not exist in index - create it
            indexing.add(participant);
        }
    }

    /**
     * Internal function which should only be called from the available function. It applies the changes to the triple store
     * @param selfDescriptionJsonLD String representation of the participant to be added to triple store
     * @throws IOException thrown, if the changes could not be applied to the triple store
     * @throws RejectMessageException thrown, if the changes are illegal, or if an internal error has occurred
     */
    private void addToTriplestore(String selfDescriptionJsonLD) throws IOException, RejectMessageException {
        ParticipantModelCreator.InnerModel result = participantModelCreator.toModel(selfDescriptionJsonLD);
        repositoryFacade.addStatements(result.getModel(), result.getNamedGraph().toString());
    }

    /**
     * Internal function which should only be called from the updated function. It applies the changes to the triple store
     * @param selfDescriptionJsonLD String representation of the participant which needs to be updated
     * @throws IOException thrown, if the changes could not be applied to the triple store
     */
    private void updateTriplestore(String selfDescriptionJsonLD) throws IOException, RejectMessageException {
        ParticipantModelCreator.InnerModel result = participantModelCreator.toModel(selfDescriptionJsonLD);
        repositoryFacade.replaceStatements(result.getModel(), result.getNamedGraph().toString());
    }

    /**
     * Function to mark a given Participant as deleted in the triple store, and delete the Participant from the index
     * @param participant A URI reference to the participant which is now unavailable
     * @throws IOException if the connection to the triple store could not be established
     * @throws RejectMessageException if the operation is not permitted, e.g. because one is trying to delete a Participant which was previously deleted or due to an internal error
     */
    @Override
    public void unavailable(URI participant) throws IOException, RejectMessageException {
        if(!repositoryFacade.graphExists(participant.toString()))
        {
            throw new RejectMessageException(RejectionReason.NOT_FOUND, new NullPointerException("The participant you are attempting to delete was not found."));
        }
        removeFromTriplestore(participant);
        indexing.delete(participant);
    }

    /**
     * Internal function which should only be called from the unavailable function. It applies the changes to the triple store
     * @param participant URI of the participant to be removed from triple store
     * @throws RejectMessageException thrown, if the changes are illegal, or if an internal error has occurred
     */
    private void removeFromTriplestore(URI participant) throws RejectMessageException {
        if(repositoryFacade.graphExists(participant.toString()))
        {
            repositoryFacade.changePassivationOfGraph(participant.toString(), false);
        }
        else
        {
            throw new RejectMessageException(RejectionReason.NOT_FOUND, new NullPointerException("The participant you are trying to delete was not found"));
        }
    }

    /**
     * Utility function to evaluate a given query (in a re-formulated way, respecting passivation and hiding underlying structure of named graphs)
     * @param queryString Query to be evaluated
     * @return Query result in String format
     * @throws RejectMessageException, if the query is illegal or if the index is empty
     */
    @Override
    public String getResults(String queryString) throws RejectMessageException {
        return new GenericQueryEvaluator(repositoryFacade).getResults(queryString);
    }

    /**
     * Utility function to obtain an IDS Participant object from the triple store
     * @param participantUri URI reference to a participant which is requested
     * @return IDS Participant object for the requested participant URI
     * @throws RejectMessageException if the requested participant could not be found, or if an internal error has occurred
     */
    @Override
    public Participant requestParticipant(URI participantUri) throws RejectMessageException {
        StringBuilder queryString = new StringBuilder();
        queryString.append("PREFIX ids: <https://w3id.org/idsa/core/> ")
                .append("CONSTRUCT { ?s ?p ?o . ?o ?p2 ?o2 . ?o2 ?p3 ?o3 . ?o3 ?p4 ?o4 . ?o4 ?p5 ?o5 . } "); //TODO: use a loop instead
        repositoryFacade.getActiveGraphs().forEach(graph -> queryString.append("FROM NAMED <").append(graph).append("> "));
        queryString.append("WHERE { ")
                //.append("BIND(<").append(participantUri.toString()).append("> AS ?s) . ") //Use passed URI as entry point
                .append("GRAPH ?g {") //Select from non-default graph
                .append(" ?s a ids:Participant ; ") //Make sure it's a participant
                .append("?p ?o . ") //Grab all direct neighbours
                .append("OPTIONAL { ?o ?p2 ?o2 .  ") //If exists, grab neighbours 1 hop away, such as security profile properties
                .append("OPTIONAL { ?o2 ?p3 ?o3 .  ") //If exists, grab neighbours 2 hops away
                .append("OPTIONAL { ?o3 ?p4 ?o4 .  ") //...
                .append("OPTIONAL { ?o4 ?p5 ?o5 . } } } } } }");

        ParameterizedSparqlString parameterizedSparqlString = new ParameterizedSparqlString(queryString.toString());
        parameterizedSparqlString.setIri("s", participantUri.toString());

        try {
            Model result = repositoryFacade.constructQuery(parameterizedSparqlString.toString());
            if (result.isEmpty()) {
                logger.info("Participant could not be found - result is empty.");
                //Result is empty, throw exception. This will result in a RejectionMessage being sent
                throw new RejectMessageException(RejectionReason.NOT_FOUND, new NullPointerException("The requested participant could not be found."));
            }
            logger.info("Participant was retrieved successfully from triple store.");

            return ConstructQueryResultHandler.GraphQueryResultToParticipant(result);
        }
        catch (ARQException e)
        {
            logger.warn("Potential SPARQL injection attack detected.", e);
            throw new RejectMessageException(RejectionReason.MALFORMED_MESSAGE);
        }
    }

}
