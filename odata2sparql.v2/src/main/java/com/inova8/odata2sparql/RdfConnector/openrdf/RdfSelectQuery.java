package com.inova8.odata2sparql.RdfConnector.openrdf;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.repository.RepositoryException;

import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfRepository.RdfRoleRepository;

public class RdfSelectQuery extends RdfQuery{
	private final Log log = LogFactory.getLog(RdfConstructQuery.class);
	private TupleQuery tupleQuery;
	public RdfSelectQuery(RdfRoleRepository rdfRoleRepository, String query) {
		super.rdfRoleRepository = rdfRoleRepository;
		super.query = query;
	}
	public RdfResultSet execSelect(boolean logQuery) throws OData2SparqlException {
		RdfResultSet rdfResultSet = null;
		try {
			super.connection = rdfRoleRepository.getRepository().getConnection();
			tupleQuery = connection.prepareTupleQuery(QueryLanguage.SPARQL, super.query);
			if( logQuery)log.info( super.query);
			rdfResultSet = new RdfResultSet(connection, tupleQuery.evaluate());
		} catch (RepositoryException | MalformedQueryException | QueryEvaluationException e) {
			log.error( super.query);
			throw new OData2SparqlException("RdfSelectQuery execSelect failure",e);
		}
		return rdfResultSet;
	}
	public RdfResultSet execSelect() throws OData2SparqlException {
		return execSelect(true);
	}	
}
