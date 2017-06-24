package com.inova8.odata2sparql;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.olingo.odata2.api.ODataCallback;
import org.apache.olingo.odata2.api.ODataService;
import org.apache.olingo.odata2.api.ODataServiceFactory;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.processor.ODataContext;

import com.inova8.odata2sparql.Constants.ODataServiceVersion;
import com.inova8.odata2sparql.Constants.RdfConstants;
import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.SparqlProcessor.SparqlODataSingleProcessor;
import com.inova8.odata2sparql.SparqlProcessor.SparqlODataSingleProcessors;

public class RdfODataServiceFactory extends ODataServiceFactory {
	private final Log log = LogFactory.getLog(RdfODataServiceFactory.class);
	//Refactor	static private final RdfRepositories rdfRepositories = new RdfRepositories();
	//Refactor	static private final RdfEdmProviders rdfEdmProviders = new RdfEdmProviders();
	static private final SparqlODataSingleProcessors sparqlODataSingleProcessors = new SparqlODataSingleProcessors();
	public RdfODataServiceFactory() throws ODataException {
		super();
	}

	@Override
	public ODataService createService(final ODataContext ctx) throws ODataException {
		String odataVersion = ctx.getPathInfo().getPrecedingSegments().get(0).getPath();
		if (odataVersion.equals(RdfConstants.RESET)) {
			String rdfRepositoryID = ctx.getPathInfo().getPrecedingSegments().get(1).getPath();
			//RdfEdmProviders.getRdfRepositories().reset(rdfRepositoryID);
			sparqlODataSingleProcessors.reset(rdfRepositoryID);
			return null;
		} else if (odataVersion.equals(RdfConstants.RELOAD)) {
			String rdfRepositoryID = ctx.getPathInfo().getPrecedingSegments().get(1).getPath();
			//RdfEdmProviders.getRdfRepositories().reload(rdfRepositoryID);
			sparqlODataSingleProcessors.reload(rdfRepositoryID);
			return null;
		} else {
			if (!ODataServiceVersion.validateDataServiceVersion(odataVersion)) {
				log.error("Unsupported Odata version: " + odataVersion);
				throw new ODataException("Unsupported Odata version: " + odataVersion);
			}
			String rdfRepositoryID = ctx.getPathInfo().getPrecedingSegments().get(1).getPath();
			
			SparqlODataSingleProcessor sparqlODataSingleProcessor;
			try {
				log.info(ctx.getHttpMethod() + ": " + ctx.getPathInfo().getRequestUri()); 
				sparqlODataSingleProcessor = sparqlODataSingleProcessors.getSparqlODataSingleProcessor(odataVersion,rdfRepositoryID );
			} catch (OData2SparqlException e) {
				throw new ODataException("Cannot create SparqlODataSingleProcessor for odataVersion:" + odataVersion+" rdfRepositoryId:"+rdfRepositoryID);
			}	
			return createODataSingleProcessorService(sparqlODataSingleProcessor.getRdfEdmProvider(), sparqlODataSingleProcessor);
		}
	}

	@SuppressWarnings("unchecked")
	public <T extends ODataCallback> T getCallback(final Class<T> callbackInterface) {
		T callback;

		if (callbackInterface.isAssignableFrom(RdfODataDebugCallback.class)) {
			callback = (T) new RdfODataDebugCallback();
		} else if (callbackInterface.isAssignableFrom(RdfODataErrorCallback.class)) {
			callback = (T) new RdfODataErrorCallback();
		} else {
			callback = (T) super.getCallback(callbackInterface);
		}

		return callback;
	}
}
