package com.inova8.odata2sparql.RdfConnector.openrdf;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.UpdateExecutionException;
import org.eclipse.rdf4j.repository.RepositoryException;

import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfRepository.RdfRoleRepository;

public class RdfUpdate extends RdfQuery{
	private final Log log = LogFactory.getLog(RdfConstructQuery.class);
	private Update updateQuery;
	public RdfUpdate(RdfRoleRepository rdfRoleRepository, String query) {
		super.rdfRoleRepository = rdfRoleRepository;
		super.query = query;
	}
	public void execUpdate() throws OData2SparqlException {
		try {
			super.connection = rdfRoleRepository.getRepository().getConnection();
			log.info( super.query);
			updateQuery = connection.prepareUpdate(QueryLanguage.SPARQL, super.query);
			updateQuery.execute();

		} catch (RepositoryException | MalformedQueryException |UpdateExecutionException e) {
			log.error( e.getMessage());
			throw new OData2SparqlException("RdfUpdate execUpdate failure",e);
		} 
	}
	public void close() {
		try {
			super.connection.close();
		} catch (RepositoryException e) {
			log.warn("Failed to close update connection",e);
		}	
	}	
}
