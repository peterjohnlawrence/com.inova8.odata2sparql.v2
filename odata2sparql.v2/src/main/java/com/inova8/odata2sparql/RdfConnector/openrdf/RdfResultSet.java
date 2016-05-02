/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 inova8.com and/or its affiliates. All rights reserved.
 *
 * 
 */
package com.inova8.odata2sparql.RdfConnector.openrdf;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

import com.inova8.odata2sparql.OData2SparqlException.OData2SparqlException;

/**
 * The Class RdfResultSet. Acts as a wrapper class for the results of a SPARQL query
 */
public class RdfResultSet {
	private final Log log = LogFactory.getLog(RdfTripleSet.class);
    /** The result set. */
    private final TupleQueryResult resultSet;
    private final RepositoryConnection connection;
    
    /**
     * Instantiates a new rdf result set.
     * @param connection 
     *
     * @param resultSet the result set
     */
    RdfResultSet(RepositoryConnection connection, TupleQueryResult resultSet){
    	this.connection = connection;
    	this.resultSet= resultSet;
    }
	
	/**
	 * Checks for next.
	 *
	 * @return true, if successful
	 */
	public boolean hasNext() {
		try {
			return resultSet.hasNext();
		} catch (QueryEvaluationException e) {
			return false;
		}
	}
	
	/**
	 * Next solution.
	 *
	 * @return the rdf query solution
	 * @throws OData2SparqlException 
	 */
	public RdfQuerySolution nextSolution() throws OData2SparqlException {
		BindingSet querySolution;
		try {
			querySolution = resultSet.next();
		} catch (QueryEvaluationException e) {
			throw new OData2SparqlException("RdfResultSet nextSolution failure",e);
		}
		return new RdfQuerySolution(querySolution);
	}
	
	/**
	 * Next.
	 *
	 * @return the rdf query solution
	 * @throws OData2SparqlException 
	 */
	public RdfQuerySolution next() throws OData2SparqlException {
		BindingSet querySolution;
		try {
			querySolution = resultSet.next();
		} catch (QueryEvaluationException e) {
			throw new OData2SparqlException("RdfResultSet next failure",e);
		}
		return new RdfQuerySolution(querySolution);
	}

	public void close()  {
		try {
			resultSet.close();
			connection.close();
		} catch (QueryEvaluationException | RepositoryException e) {
			log.warn("failed to close RdfResultSet");
		}
	}
	protected void finalize() throws Throwable {
		super.finalize();
		this.close();
	}
}
