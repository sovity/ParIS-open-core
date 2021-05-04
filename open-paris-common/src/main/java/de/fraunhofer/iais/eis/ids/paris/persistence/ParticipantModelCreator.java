package de.fraunhofer.iais.eis.ids.paris.persistence;


import de.fraunhofer.iais.eis.ids.index.common.persistence.INFOMODEL;
import de.fraunhofer.iais.eis.ids.index.common.persistence.ModelCreator;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;

/**
 * This class is an extension to the ModelCreator class, providing functionality to determine whether an RDF triple defines a Participant
 */
public class ParticipantModelCreator extends ModelCreator {

    /**
     * This function determines whether a statement defines a Participant
     * @param statement The statement to be tested
     * @return true, if it defines a Participant, otherwise false
     */
    @Override
    public boolean subjectIsInstanceInnerModel(Statement statement) {
        return statement.getPredicate().equals(RDF.type) &&
                (statement.getObject().equals(INFOMODEL.PARTICIPANT));
    }

}
