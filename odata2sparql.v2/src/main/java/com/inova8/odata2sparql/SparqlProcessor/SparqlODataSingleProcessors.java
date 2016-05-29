package com.inova8.odata2sparql.SparqlProcessor;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProvider;
import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProviders;

public class SparqlODataSingleProcessors {
	private final Log log = LogFactory.getLog(SparqlODataSingleProcessors.class);

	private final Map<String, SparqlODataSingleProcessor> sparqlODataSingleProcessors = new HashMap<String, SparqlODataSingleProcessor>();
	static private final RdfEdmProviders rdfEdmProviders = new RdfEdmProviders();

	public SparqlODataSingleProcessors() {
		super();
	}

	public SparqlODataSingleProcessor getSparqlODataSingleProcessor(String odataVersion, String rdfRepositoryID)
			throws OData2SparqlException {
		SparqlODataSingleProcessor sparqlODataSingleProcessor = sparqlODataSingleProcessors.get(odataVersion
				+ rdfRepositoryID);
		if (sparqlODataSingleProcessor == null) {
			RdfEdmProvider rdfEdmProvider;
			try {
				rdfEdmProvider = rdfEdmProviders.getRdfEdmProvider(odataVersion, rdfRepositoryID);
			} catch (OData2SparqlException e) {
				log.error("Error getting rdfEdmProvider version: " + odataVersion +" for rdfRepositoryID:"+rdfRepositoryID);
				throw new OData2SparqlException("Error getting rdfEdmProvider version: " + odataVersion +" for rdfRepositoryID:"+rdfRepositoryID);
			}

			sparqlODataSingleProcessor = new SparqlODataSingleProcessor(rdfEdmProvider);
			sparqlODataSingleProcessors.put(odataVersion + rdfRepositoryID, sparqlODataSingleProcessor);
		}
		return sparqlODataSingleProcessor;
	}

	public void reset(String rdfRepositoryID) {
		// TODO Auto-generated method stub
		RdfEdmProviders.getRdfRepositories().reset(rdfRepositoryID);
		
	}

	public void reload(String rdfRepositoryID) {
		// TODO Auto-generated method stub
		RdfEdmProviders.getRdfRepositories().reload(rdfRepositoryID);
	}
}