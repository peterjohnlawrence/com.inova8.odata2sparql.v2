/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/
package com.inova8.odata2sparql.SparqlProcessor;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.olingo.odata2.api.ODataCallback;
import org.apache.olingo.odata2.api.batch.BatchHandler;
import org.apache.olingo.odata2.api.batch.BatchRequestPart;
import org.apache.olingo.odata2.api.batch.BatchResponsePart;
import org.apache.olingo.odata2.api.commons.HttpStatusCodes;
import org.apache.olingo.odata2.api.edm.EdmEntitySet;
import org.apache.olingo.odata2.api.edm.EdmException;
import org.apache.olingo.odata2.api.edm.FullQualifiedName;
import org.apache.olingo.odata2.api.ep.EntityProvider;
import org.apache.olingo.odata2.api.ep.EntityProviderBatchProperties;
import org.apache.olingo.odata2.api.ep.EntityProviderException;
import org.apache.olingo.odata2.api.ep.EntityProviderReadProperties;
import org.apache.olingo.odata2.api.ep.EntityProviderWriteProperties;
import org.apache.olingo.odata2.api.ep.EntityProviderWriteProperties.ODataEntityProviderPropertiesBuilder;
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.apache.olingo.odata2.api.exception.ODataApplicationException;
import org.apache.olingo.odata2.api.exception.ODataBadRequestException;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.exception.ODataNotFoundException;
import org.apache.olingo.odata2.api.exception.ODataNotImplementedException;
import org.apache.olingo.odata2.api.processor.ODataContext;
import org.apache.olingo.odata2.api.processor.ODataProcessor;
import org.apache.olingo.odata2.api.processor.ODataRequest;
import org.apache.olingo.odata2.api.processor.ODataResponse;
import org.apache.olingo.odata2.api.processor.ODataSingleProcessor;
import org.apache.olingo.odata2.api.uri.ExpandSelectTreeNode;
import org.apache.olingo.odata2.api.uri.KeyPredicate;
import org.apache.olingo.odata2.api.uri.NavigationPropertySegment;
import org.apache.olingo.odata2.api.uri.NavigationSegment;
import org.apache.olingo.odata2.api.uri.PathInfo;
import org.apache.olingo.odata2.api.uri.SelectItem;
import org.apache.olingo.odata2.api.uri.UriParser;
import org.apache.olingo.odata2.api.uri.info.DeleteUriInfo;
import org.apache.olingo.odata2.api.uri.info.GetComplexPropertyUriInfo;
import org.apache.olingo.odata2.api.uri.info.GetEntityCountUriInfo;
import org.apache.olingo.odata2.api.uri.info.GetEntityLinkCountUriInfo;
import org.apache.olingo.odata2.api.uri.info.GetEntityLinkUriInfo;
import org.apache.olingo.odata2.api.uri.info.GetEntitySetCountUriInfo;
import org.apache.olingo.odata2.api.uri.info.GetEntitySetLinksCountUriInfo;
import org.apache.olingo.odata2.api.uri.info.GetEntitySetLinksUriInfo;
import org.apache.olingo.odata2.api.uri.info.GetEntitySetUriInfo;
import org.apache.olingo.odata2.api.uri.info.GetEntityUriInfo;
import org.apache.olingo.odata2.api.uri.info.GetFunctionImportUriInfo;
import org.apache.olingo.odata2.api.uri.info.GetMediaResourceUriInfo;
import org.apache.olingo.odata2.api.uri.info.GetMetadataUriInfo;
import org.apache.olingo.odata2.api.uri.info.GetServiceDocumentUriInfo;
import org.apache.olingo.odata2.api.uri.info.GetSimplePropertyUriInfo;
import org.apache.olingo.odata2.api.uri.info.PostUriInfo;
import org.apache.olingo.odata2.api.uri.info.PutMergePatchUriInfo;

import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfConstructQuery;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfLiteral;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfNodeFactory;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfQuerySolution;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfResultSet;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfSelectQuery;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfTripleSet;
import com.inova8.odata2sparql.RdfConnector.openrdf.RdfUpdate;
import com.inova8.odata2sparql.RdfEdmProvider.RdfEdmProvider;
import com.inova8.odata2sparql.RdfModel.RdfEntity;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfEntityType;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfPrefixes;
import com.inova8.odata2sparql.SparqlBuilder.SparqlQueryBuilder;
import com.inova8.odata2sparql.SparqlBuilder.SparqlUpdateInsertBuilder;
import com.inova8.odata2sparql.SparqlStatement.SparqlStatement;

public class SparqlODataSingleProcessor extends ODataSingleProcessor {
	private final Log log = LogFactory.getLog(SparqlODataSingleProcessor.class);
	private /*static*/final RdfEdmProvider rdfEdmProvider;
	private /*static*/final SparqlUpdateInsertBuilder sparqlUpdateInsertBuilder;
	private SparqlQueryBuilder sparqlBuilder;

	SparqlODataSingleProcessor(RdfEdmProvider rdfEdmProvider) {
		this.rdfEdmProvider = rdfEdmProvider;
		this.sparqlUpdateInsertBuilder = new SparqlUpdateInsertBuilder(rdfEdmProvider);
	}
	@Override
	public ODataResponse readEntitySet(final GetEntitySetUriInfo uriInfo, final String contentType)
			throws ODataException {
		RdfEntityType rdfEntityType = null;
		EdmEntitySet edmEntitySet = null;
		this.sparqlBuilder = new SparqlQueryBuilder(rdfEdmProvider.getRdfModel(),rdfEdmProvider.getEdmMetadata(), uriInfo);
		
		//prepareQuery
		SparqlStatement sparqlStatement = null;
		if (uriInfo.getNavigationSegments().size() == 0) {
			edmEntitySet = uriInfo.getStartEntitySet();
			rdfEntityType = rdfEdmProvider.getRdfEntityTypefromEdmEntitySet(edmEntitySet);
		} else if (uriInfo.getNavigationSegments().size() == 1) {
			edmEntitySet = uriInfo.getTargetEntitySet();
			rdfEntityType = rdfEdmProvider.getRdfEntityTypefromEdmEntitySet(edmEntitySet);
		}
		try {
			sparqlStatement = this.sparqlBuilder.prepareConstructSparql();
		} catch (OData2SparqlException e) {
			throw new ODataBadRequestException(ODataBadRequestException.INVALID_REQUEST, e.getMessage());
		}
		SparqlResults rdfResults = null;
		rdfResults = executeQuery(/* edmEntitySet, */rdfEntityType, sparqlStatement, uriInfo.getExpand(),uriInfo.getSelect());
		List<Map<String, Object>> data = rdfResults.getEntitySetResults();

		ExpandSelectTreeNode expandSelectTreeNode = UriParser.createExpandSelectTree(uriInfo.getSelect(), uriInfo.getExpand());

		Map<String, ODataCallback> callbacks = locateCallbacks(expandSelectTreeNode, rdfResults);
		if (data == null) {
			throw new ODataNotFoundException(ODataNotFoundException.ENTITY);
		} else {
			return EntityProvider.writeFeed(contentType, edmEntitySet, data,
					buildEntitySetProperties(expandSelectTreeNode, callbacks).build());
		}
	}

	@Override
	public ODataResponse readEntity(final GetEntityUriInfo uriInfo, final String contentType) throws ODataException {

		RdfEntityType rdfEntityType = null;
		EdmEntitySet edmEntitySet = null;
		this.sparqlBuilder = new SparqlQueryBuilder(rdfEdmProvider.getRdfModel(),rdfEdmProvider.getEdmMetadata(),uriInfo);
		
		//prepareQuery
		SparqlStatement sparqlStatement = null;
		if (uriInfo.getNavigationSegments().size() == 0) {
			edmEntitySet = uriInfo.getStartEntitySet();
			rdfEntityType = rdfEdmProvider.getRdfEntityTypefromEdmEntitySet(edmEntitySet);
			//Test
			//Test 	sparqlStatement = prepareStartEntityQuery(edmEntitySet, rdfEntityType, uriInfo);
		} else if (uriInfo.getNavigationSegments().size() == 1) {
			edmEntitySet = uriInfo.getTargetEntitySet();
			rdfEntityType = rdfEdmProvider.getRdfEntityTypefromEdmEntitySet(edmEntitySet);
			//Test
			//Test sparqlStatement = prepareEntityNavPropertyQuery(edmEntitySet, rdfEntityType, uriInfo);
		}
		try {
			sparqlStatement = this.sparqlBuilder.prepareConstructSparql();
		} catch (OData2SparqlException e) {
			throw new ODataBadRequestException(ODataBadRequestException.INVALID_REQUEST, e.getMessage());
		}
		SparqlResults rdfResults = null;
		rdfResults = executeQuery(rdfEntityType, sparqlStatement, uriInfo.getExpand(), uriInfo.getSelect());
		Map<String, Object> data = rdfResults.getEntityResults();
		if (data == null) {
			throw new ODataNotFoundException(ODataNotFoundException.ENTITY);
		} else {
			ExpandSelectTreeNode expandSelectTreeNode = UriParser.createExpandSelectTree(uriInfo.getSelect(), uriInfo.getExpand());
			Map<String, ODataCallback> callbacks = locateCallbacks(expandSelectTreeNode, rdfResults);
			return EntityProvider.writeEntry(contentType, edmEntitySet, data, buildEntityProperties(expandSelectTreeNode, callbacks)
					.build());
		}
	}
	private ODataEntityProviderPropertiesBuilder buildEntitySetProperties(ExpandSelectTreeNode expandSelectTreeNode,
			Map<String, ODataCallback> callbacks) throws ODataException {
		ODataEntityProviderPropertiesBuilder propertiesBuilder = EntityProviderWriteProperties.serviceRoot(getContext()
				.getPathInfo().getServiceRoot());
		propertiesBuilder.expandSelectTree(expandSelectTreeNode).callbacks(callbacks);
		return propertiesBuilder;
	}
	private Map<String, ODataCallback> locateCallbacks(ExpandSelectTreeNode expandSelectTreeNode,SparqlResults rdfResults)
			throws EdmException, ODataException {
		Map<String, ODataCallback> callbacks = new HashMap<String, ODataCallback>();
		for (Entry<String, ExpandSelectTreeNode> expandSelectTreeNodeLinksEntry : expandSelectTreeNode.getLinks().entrySet()) {
			String navigationPropertyName = expandSelectTreeNodeLinksEntry.getKey();
			callbacks.put(navigationPropertyName, new SparqlCallback(rdfResults,	getContext().getPathInfo().getServiceRoot()));
		}
		return callbacks;
	}
	private ODataEntityProviderPropertiesBuilder buildEntityProperties(ExpandSelectTreeNode expandSelectTreeNode,
			Map<String, ODataCallback> callbacks) throws ODataException {
		ODataEntityProviderPropertiesBuilder propertiesBuilder = EntityProviderWriteProperties.serviceRoot(getContext()
				.getPathInfo().getServiceRoot());
		propertiesBuilder.expandSelectTree(expandSelectTreeNode).callbacks(callbacks);
		return propertiesBuilder;
	}
//	@Deprecated
//	private SparqlStatement prepareEntityNavPropertyQuery(EdmEntitySet entitySet, RdfEntityType entityType,
//			GetEntityUriInfo uriInfo) throws EdmException, ExceptionVisitExpression, ODataApplicationException {
//		FilterExpression filter = uriInfo.getFilter();
//		List<KeyPredicate> keys = uriInfo.getKeyPredicates();
//
//		Integer top;
//		Integer skip;
//
//		top = sparqlODataProvider.getRdfRepository().getDefaultQueryLimit();
//		skip = 0;
//
//		List<RdfAssociation> navProperties = new ArrayList<RdfAssociation>();
//		for (NavigationSegment navigationSegment : uriInfo.getNavigationSegments()) {
//			RdfAssociation navProperty = sparqlODataProvider.getMappedNavigationProperty(new FullQualifiedName(
//					navigationSegment.getNavigationProperty().getRelationship().getNamespace(), navigationSegment
//							.getNavigationProperty().getRelationship().getName()));
//			navProperties.add(navProperty);
//		}
//
//		return sparqlQueryProvider.generateNavPropertyQuery(entityType, keys, filter, navProperties, top, skip);
//	}
//	@Deprecated
//	private SparqlStatement prepareEntitySetNavPropertyQuery(EdmEntitySet entitySet, RdfEntityType entityType,
//			GetEntitySetUriInfo uriInfo) throws EdmException, ExceptionVisitExpression, ODataApplicationException {
//
//		FilterExpression filter = uriInfo.getFilter();
//		List<KeyPredicate> keys = uriInfo.getKeyPredicates();
//
//		Integer top;
//		Integer skip;
//		if (uriInfo.getTop() == null) {
//			top = sparqlODataProvider.getRdfRepository().getDefaultQueryLimit();
//			skip = 0;
//		} else {
//			top = uriInfo.getTop();
//			if (top > sparqlODataProvider.getRdfRepository().getDefaultQueryLimit())
//				top = sparqlODataProvider.getRdfRepository().getDefaultQueryLimit();
//			if (uriInfo.getSkip() == null) {
//				skip = 0;
//			} else {
//				skip = uriInfo.getSkip();
//			}
//		}
//		List<RdfAssociation> navProperties = new ArrayList<RdfAssociation>();
//		for (NavigationSegment navigationSegment : uriInfo.getNavigationSegments()) {
//			RdfAssociation navProperty = sparqlODataProvider.getMappedNavigationProperty(new FullQualifiedName(
//					navigationSegment.getNavigationProperty().getRelationship().getNamespace(), navigationSegment
//							.getNavigationProperty().getRelationship().getName()));
//			navProperties.add(navProperty);
//		}
//
//		return sparqlQueryProvider.generateNavPropertyQuery(entityType, keys, filter, navProperties, top, skip);
//	}
//	@Deprecated
//	private SparqlStatement prepareEntitySetQuery(EdmEntitySet entitySet, RdfEntityType entityType,
//			GetEntitySetUriInfo uriInfo) throws EdmException, ExceptionVisitExpression, ODataApplicationException {
//
//		FilterExpression filter = uriInfo.getFilter();
//		List<SelectItem> select = uriInfo.getSelect();
//		List<KeyPredicate> keys = uriInfo.getKeyPredicates();
//		List<ArrayList<NavigationPropertySegment>> expand = uriInfo.getExpand();
//
//		Integer top;
//		Integer skip;
//		int defaultLimit = sparqlODataProvider.getRdfRepository().getModelRepository().getDefaultQueryLimit();
//		if (uriInfo.getTop() == null) {
//			top = defaultLimit;
//			skip = 0;
//		} else {
//			top = uriInfo.getTop();
//
//			if ((top > defaultLimit) && (defaultLimit != 0))
//				top = defaultLimit;
//			if (uriInfo.getSkip() == null) {
//				skip = 0;
//			} else {
//				skip = uriInfo.getSkip();
//			}
//		}
//		return sparqlQueryProvider.generateEntitiesQuery(entityType, keys, filter, select, top, skip, expand);
//
//	}

	private SparqlResults executeQuery(RdfEntityType entityType, SparqlStatement sparqlStatement,
			List<ArrayList<NavigationPropertySegment>> expand, List<SelectItem> select) throws EntityProviderException, ODataException {
		RdfConstructQuery rdfQuery = new RdfConstructQuery(rdfEdmProvider.getRdfRepository().getDataRepository(),
				sparqlStatement.getSparql());
		RdfTripleSet results;
		try {
			results = rdfQuery.execConstruct();
		} catch (OData2SparqlException e) {
			log.error(e.getMessage());
			throw new ODataApplicationException(e.getMessage(), null);
		}
		SparqlBaseCommand rdfBaseCommand = new SparqlBaseCommand();
		return rdfBaseCommand.toOEntities(rdfEdmProvider, entityType, results, expand,select);
	}

	private SparqlResults executeLinksQuery(EdmEntitySet entitySet, RdfEntityType entityType,
			SparqlStatement sparqlStatement, List<NavigationSegment> navigationSegments)
			throws EntityProviderException, EdmException, OData2SparqlException, ODataApplicationException {

		RdfConstructQuery rdfQuery = new RdfConstructQuery(rdfEdmProvider.getRdfRepository().getDataRepository(),
				sparqlStatement.getSparql());
		RdfTripleSet results = null;
		try {
			results = rdfQuery.execConstruct();
		} catch (OData2SparqlException e) {
			log.error(e.getMessage());
			throw new ODataApplicationException(e.getMessage(), null);
		} 
		SparqlBaseCommand rdfBaseCommand = new SparqlBaseCommand();

		return rdfBaseCommand.toOLinks(rdfEdmProvider, entityType, results, navigationSegments);
	}

	@Override
	public ODataResponse countEntitySet(GetEntitySetCountUriInfo uriInfo, String contentType) throws ODataException {

		this.sparqlBuilder = new SparqlQueryBuilder(rdfEdmProvider.getRdfModel(),rdfEdmProvider.getEdmMetadata(),uriInfo);
		final SparqlStatement sparqlStatement = this.sparqlBuilder.prepareCountEntitySetSparql();
		try {
			return executeCountQuery(sparqlStatement, contentType);
		} catch (OData2SparqlException e) {
			log.error(e.getMessage());
			throw new ODataApplicationException(e.getMessage(), null);
		}
	}

	private ODataResponse executeCountQuery(SparqlStatement sparqlStatement, final String contentType)
			throws EntityProviderException, ODataException, OData2SparqlException {
		RdfSelectQuery rdfQuery = new RdfSelectQuery(rdfEdmProvider.getRdfRepository().getDataRepository(),
				sparqlStatement.getSparql());
		RdfResultSet results = null;
		try {
			results = rdfQuery.execSelect();
		} catch (OData2SparqlException e) {
			log.error(e.getMessage());
			throw new ODataException(e.getMessage());
		}

		RdfLiteral countLiteral = null;
		while (results.hasNext()) {
			RdfQuerySolution solution = results.next();
			countLiteral = solution.getRdfLiteral("COUNT");
			break; // Only one record, but no reason for more anyway
		}
		if (countLiteral == null) {
			throw new ODataNotFoundException(ODataNotFoundException.ENTITY);
		} else {
			return EntityProvider.writeText(countLiteral.getString());
		}
	}

	@Override
	public ODataResponse createEntity(PostUriInfo uriInfo, InputStream content, String requestContentType,
			String contentType) throws ODataException, EntityProviderException, ODataException {
		if (uriInfo.getNavigationSegments().size() > 0) {
			throw new ODataNotImplementedException();
		}

		//No support for media resources
		if (uriInfo.getStartEntitySet().getEntityType().hasStream()) {
			throw new ODataNotImplementedException();
		}

		EntityProviderReadProperties properties = EntityProviderReadProperties.init().mergeSemantic(false).build();

		ODataEntry entry;
		try {
			entry = EntityProvider.readEntry(requestContentType, uriInfo.getStartEntitySet(), content,
					properties);
			log.info("Content: " + entry.getProperties().toString()); 
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new ODataException(e.getMessage());
		}
		//if something goes wrong in deserialization this is managed via the ExceptionMapper
		//no need for an application to do exception handling here an convert the exceptions in HTTP exceptions

		//Map<String, Object> data = entry.getProperties();
		//now one can use the data to create the entry in the backend ...

		SparqlStatement sparqlStatement = null;
		RdfEntityType entityType = null;
		EdmEntitySet entitySet = null;
		entitySet = uriInfo.getStartEntitySet();
		entityType = rdfEdmProvider.getMappedEntityType(new FullQualifiedName(entitySet.getEntityType()
				.getNamespace(), entitySet.getEntityType().getName()));
		try {
			sparqlStatement = prepareInsertQuery(entitySet, entityType, entry);
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new ODataException(e.getMessage());
		}
		try {
			executeInsert(sparqlStatement);
		} catch (SQLException e) {
			log.error(e.getMessage());
			throw new ODataException(e.getMessage());
		}
		return EntityProvider.writeEntry(contentType, uriInfo.getStartEntitySet(), entry.getProperties(),
				EntityProviderWriteProperties.serviceRoot(getContext().getPathInfo().getServiceRoot()).build());
	}

	private SparqlStatement prepareInsertQuery(EdmEntitySet entitySet, RdfEntityType entityType, ODataEntry entry)
			throws Exception {
		return sparqlUpdateInsertBuilder.generateInsertQuery(entityType, entry);
	}
	private SparqlStatement prepareInsertLinkQuery(EdmEntitySet entitySet, EdmEntitySet targetEntitySet, RdfEntityType entityType, String entityKey, NavigationSegment navigationSegment, List<String> entry)
			throws Exception {
		return sparqlUpdateInsertBuilder.generateInsertLinkQuery(entitySet, targetEntitySet, entityType, entityKey,navigationSegment,entry);
	}
	private void executeInsert(SparqlStatement sparqlStatement) throws EntityProviderException,
			ODataException, SQLException {
		
		RdfUpdate rdfUpdate = new RdfUpdate(rdfEdmProvider.getRdfRepository().getDataRepository(),
				sparqlStatement.getSparql());
		try {
			rdfUpdate.execUpdate();
		} catch (OData2SparqlException e) {
			log.error(e.getMessage());
			throw new EntityProviderException(EntityProviderException.EXCEPTION_OCCURRED, e);
		} finally {
			rdfUpdate.close();
		}				
	}

	@Override
	public ODataResponse existsEntity(GetEntityCountUriInfo uriInfo, String contentType) throws ODataException {
		
		RdfEntityType rdfEntityType = null;
		EdmEntitySet edmEntitySet = null;
		this.sparqlBuilder = new SparqlQueryBuilder(rdfEdmProvider.getRdfModel(),rdfEdmProvider.getEdmMetadata(),uriInfo);
		edmEntitySet = uriInfo.getStartEntitySet();
		rdfEntityType = rdfEdmProvider.getRdfEntityTypefromEdmEntitySet(edmEntitySet);

		final SparqlStatement sparqlStatement = this.sparqlBuilder.prepareExistsEntitySetSparql();
		

		try {
			return executeEntityExistsQuery(edmEntitySet, rdfEntityType, sparqlStatement, contentType);
		} catch (OData2SparqlException e) {
			log.error(e.getMessage());
			throw new ODataException();
		}
	}

	private ODataResponse executeEntityExistsQuery(EdmEntitySet entitySet, RdfEntityType entityType,
			SparqlStatement sparqlStatement, final String contentType) throws EntityProviderException, ODataException,
			OData2SparqlException {

		RdfSelectQuery rdfQuery = new RdfSelectQuery(rdfEdmProvider.getRdfRepository().getDataRepository(),
				sparqlStatement.getSparql());
		RdfResultSet results = null;
		try {
			results = rdfQuery.execSelect();
		} catch (OData2SparqlException e) {
			log.error(e.getMessage());
			throw new EntityProviderException(null);
		} 

		while (results.hasNext()) {
			@SuppressWarnings("unused")
			RdfQuerySolution solution = results.nextSolution();
			results.close();
			return EntityProvider.writeText("true");
		}
		results.close();
		return EntityProvider.writeText("false");
	}

	@Override
	public ODataResponse updateEntity(PutMergePatchUriInfo uriInfo, InputStream content, String requestContentType,
			boolean merge, String contentType) throws ODataException {

		//No support for media resources
		if (uriInfo.getStartEntitySet().getEntityType().hasStream()) {
			throw new ODataNotImplementedException();
		}

		EntityProviderReadProperties properties = EntityProviderReadProperties.init().mergeSemantic(false).build();

		ODataEntry entry = EntityProvider.readEntry(requestContentType, uriInfo.getStartEntitySet(), content,
				properties);
		log.info("Content: " + entry.getProperties().toString()); 
		//if something goes wrong in deserialization this is managed via the ExceptionMapper
		//no need for an application to do exception handling here an convert the exceptions in HTTP exceptions

		//Map<String, Object> data = entry.getProperties();
		//now one can use the data to create the entry in the backend ...

		SparqlStatement sparqlStatement = null;
		RdfEntityType entityType = null;
		EdmEntitySet entitySet = null;
		entitySet = uriInfo.getStartEntitySet();
		entityType = rdfEdmProvider.getMappedEntityType(new FullQualifiedName(entitySet.getEntityType()
				.getNamespace(), entitySet.getEntityType().getName()));
		String entityKey = uriInfo.getKeyPredicates().get(0).getLiteral();
		try {
			sparqlStatement = prepareUpdateQuery(entitySet, entityType, uriInfo.getKeyPredicates(), entry);
		} catch (Exception e) {
			throw new ODataException(e.getMessage());
		}
		try {
			executeUpdate(sparqlStatement);
		} catch (SQLException e) {
			log.error(e.getMessage());
			throw new ODataException(e.getMessage());
		}

		return ODataResponse.status(HttpStatusCodes.NO_CONTENT).build();
	}

	private SparqlStatement prepareUpdateQuery(EdmEntitySet entitySet, RdfEntityType entityType, List<KeyPredicate> entityKeys,
			ODataEntry entry) throws Exception {
		return sparqlUpdateInsertBuilder.generateUpdateQuery(entityType, entityKeys, entry);
	}
	private SparqlStatement prepareUpdateLinkQuery(EdmEntitySet entitySet, EdmEntitySet targetEntitySet, RdfEntityType entityType, String entityKey, String targetEntityKey, NavigationSegment navigationSegment, List<String> entry) throws Exception {
		return sparqlUpdateInsertBuilder.generateUpdateLinkQuery(entitySet, targetEntitySet, entityType, entityKey,  targetEntityKey,navigationSegment, entry);
	}
	private SparqlStatement prepareDeleteLinkQuery(EdmEntitySet entitySet, EdmEntitySet targetEntitySet, RdfEntityType entityType, String entityKey, String targetEntityKey, NavigationSegment navigationSegment) throws Exception {
		return sparqlUpdateInsertBuilder.generateDeleteLinkQuery(entitySet, targetEntitySet, entityType, entityKey,  targetEntityKey,navigationSegment);
	}
	private void executeUpdate(SparqlStatement sparqlStatement) throws EntityProviderException,
			ODataException, SQLException {
		
		RdfUpdate rdfUpdate = new RdfUpdate(rdfEdmProvider.getRdfRepository().getDataRepository(),
				sparqlStatement.getSparql());
		try {
			rdfUpdate.execUpdate();
		} catch (OData2SparqlException e) {
			log.error(e.getMessage());
			throw new EntityProviderException(EntityProviderException.EXCEPTION_OCCURRED, e);
		} finally {
			rdfUpdate.close();
		}		
		
//		try {
//			sparqlStatement.asUpdateStatement(sparqlODataProvider.getRdfRepository().getDataEndpoint());
//		} catch (Exception e) {
//			throw new EntityProviderException(EntityProviderException.EXCEPTION_OCCURRED, e.getCause());
//		}
//		return null;
	}

	@Override
	public ODataResponse deleteEntity(DeleteUriInfo uriInfo, String contentType) throws ODataException {

		SparqlStatement sparqlStatement;
		EdmEntitySet entitySet = uriInfo.getStartEntitySet();
		RdfEntityType entityType = rdfEdmProvider.getMappedEntityType(new FullQualifiedName(entitySet
				.getEntityType().getNamespace(), entitySet.getEntityType().getName()));
		//TODO allow for multiple keys
		List<KeyPredicate> entityKeys = uriInfo.getKeyPredicates();//.get(0).getLiteral();
		try {
			sparqlStatement = prepareDeleteQuery(entitySet, entityType, entityKeys);
		} catch (Exception e) {
			throw new ODataException(e.getMessage());
		}
		try {
			executeDelete(sparqlStatement);
		} catch (SQLException e) {
			log.error(e.getMessage());
			return ODataResponse.status(HttpStatusCodes.NOT_FOUND).build();
		}
		return ODataResponse.status(HttpStatusCodes.NO_CONTENT).build();
	}

	private SparqlStatement prepareDeleteQuery(EdmEntitySet entitySet, RdfEntityType entityType, List<KeyPredicate> entityKeys)
			throws Exception {
		return sparqlUpdateInsertBuilder.generateDeleteQuery(entityType, entityKeys);
	}

	private void executeDelete(SparqlStatement sparqlStatement) throws EntityProviderException,
			ODataException, SQLException {
		
		
		RdfUpdate rdfUpdate = new RdfUpdate(rdfEdmProvider.getRdfRepository().getDataRepository(),
				sparqlStatement.getSparql());
		try {
			rdfUpdate.execUpdate();
		} catch (OData2SparqlException e) {
			log.error(e.getMessage());
			throw new EntityProviderException(EntityProviderException.EXCEPTION_OCCURRED, e);
		} finally {
			rdfUpdate.close();
		}	
		
//		
//		try {
//			sparqlStatement.asUpdateStatement(sparqlODataProvider.getRdfRepository().getDataEndpoint());
//		} catch (Exception e) {
//			throw new EntityProviderException(EntityProviderException.EXCEPTION_OCCURRED, e.getCause());
//		}
//		return null;
	}

	@Override
	public ODataResponse readMetadata(GetMetadataUriInfo uriInfo, String contentType) throws ODataException {
		return super.readMetadata(uriInfo, contentType);
	}

	@Override
	public List<String> getCustomContentTypes(Class<? extends ODataProcessor> processorFeature) throws ODataException {
		return super.getCustomContentTypes(processorFeature);
	}

	@Override
	public ODataResponse readServiceDocument(GetServiceDocumentUriInfo uriInfo, String contentType)
			throws ODataException {
		return super.readServiceDocument(uriInfo, contentType);
	}

	@Override
	public ODataResponse readEntitySimpleProperty(GetSimplePropertyUriInfo uriInfo, String contentType)
			throws ODataException {
		RdfLiteral value;
		try {
			value = (RdfLiteral) getEntitySimplePropertyValue(uriInfo, contentType);
		} catch (OData2SparqlException e) {
			log.error(e.getMessage());
			throw new ODataException();
		}
		return EntityProvider.writeProperty(contentType, uriInfo.getPropertyPath().get(0), value.getLexicalForm());
	}

	@Override
	public ODataResponse readEntitySimplePropertyValue(GetSimplePropertyUriInfo uriInfo, String contentType)
			throws ODataException {
		RdfLiteral value;
		try {
			value = (RdfLiteral) getEntitySimplePropertyValue(uriInfo, contentType);
		} catch (OData2SparqlException e) {
			log.error(e.getMessage());
			throw new ODataException();
		}
		return EntityProvider.writePropertyValue(uriInfo.getPropertyPath().get(0), value.getLexicalForm());
	}

	private Object getEntitySimplePropertyValue(GetSimplePropertyUriInfo uriInfo, String contentType)
			throws ODataException, OData2SparqlException {
		SparqlStatement sparqlStatement;
		EdmEntitySet entitySet = uriInfo.getStartEntitySet();
		RdfEntityType entityType = rdfEdmProvider.getMappedEntityType(new FullQualifiedName(entitySet
				.getEntityType().getNamespace(), entitySet.getEntityType().getName()));
		String entityKey = uriInfo.getKeyPredicates().get(0).getLiteral();
		String property = uriInfo.getPropertyPath().get(0).getName();
		try {
			sparqlStatement = prepareEntitySimplePropertyQuery(entityType, entityKey, property);
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new ODataException(e.getMessage());
		}
		return executeSimplePropertySelect(sparqlStatement);
	}

	private SparqlStatement prepareEntitySimplePropertyQuery(RdfEntityType entityType, String entityKey, String property)
			throws ODataApplicationException {
		return sparqlUpdateInsertBuilder.generateEntitySimplePropertyQuery(entityType, entityKey, property);
	}

	private RdfLiteral executeSimplePropertySelect(SparqlStatement sparqlStatement) throws EntityProviderException,
			ODataException, OData2SparqlException {
		RdfSelectQuery rdfQuery = new RdfSelectQuery(rdfEdmProvider.getRdfRepository().getDataRepository(),
				sparqlStatement.getSparql());
		RdfResultSet results = null;
		try {
			results = rdfQuery.execSelect();
		} catch (OData2SparqlException e) {
			log.error(e.getMessage());
			throw new EntityProviderException(null);
		}

		RdfLiteral valueLiteral = null;
		while (results.hasNext()) {
			RdfQuerySolution solution = results.next();
			valueLiteral = solution.getRdfLiteral("VALUE");
		}

		return valueLiteral;
	}

	@Override
	public ODataResponse deleteEntitySimplePropertyValue(DeleteUriInfo uriInfo, String contentType)
			throws ODataException {

		SparqlStatement sparqlStatement;
		EdmEntitySet entitySet = uriInfo.getStartEntitySet();
		RdfEntityType entityType = rdfEdmProvider.getMappedEntityType(new FullQualifiedName(entitySet
				.getEntityType().getNamespace(), entitySet.getEntityType().getName()));
		String property = uriInfo.getPropertyPath().get(0).getName();
		try {
			sparqlStatement = prepareEntitySimplePropertyValueQuery(entitySet, entityType,  uriInfo.getKeyPredicates(), property);
		} catch (Exception e) {
			throw new ODataException(e.getMessage());
		}
		try {
			executeDelete(sparqlStatement);
		} catch (SQLException e) {
			log.error(e.getMessage());
			return ODataResponse.status(HttpStatusCodes.NOT_FOUND).build();
		}
		return ODataResponse.status(HttpStatusCodes.NO_CONTENT).build();
	}

	private SparqlStatement prepareEntitySimplePropertyValueQuery(EdmEntitySet entitySet, RdfEntityType entityType,
			List<KeyPredicate> entityKeys, String property) throws Exception {
		return sparqlUpdateInsertBuilder.generateEntitySimplePropertyValueQuery(entityType, entityKeys, property);
	}

	@Override
	public ODataResponse updateEntitySimplePropertyValue(PutMergePatchUriInfo uriInfo, InputStream content,
			String requestContentType, String contentType) throws ODataException {

		Object entry = EntityProvider.readPropertyValue(uriInfo.getPropertyPath().get(0), content);

		return updateEntitySimplePropertyValue(uriInfo, entry, requestContentType, contentType);
	}

	@Override
	public ODataResponse updateEntitySimpleProperty(PutMergePatchUriInfo uriInfo, InputStream content,
			String requestContentType, String contentType) throws ODataException {
		EntityProviderReadProperties properties = EntityProviderReadProperties.init().mergeSemantic(false).build();
		Map<String, Object> entries = EntityProvider.readProperty(contentType, uriInfo.getPropertyPath().get(0),
				content, properties);

		return updateEntitySimplePropertyValue(uriInfo, entries.get(uriInfo.getPropertyPath().get(0).getName()),
				requestContentType, contentType);
	}

	private ODataResponse updateEntitySimplePropertyValue(PutMergePatchUriInfo uriInfo, Object entry,
			String requestContentType, String contentType) throws ODataException {
		SparqlStatement sparqlStatement;
		EdmEntitySet entitySet = uriInfo.getStartEntitySet();
		RdfEntityType entityType = rdfEdmProvider.getMappedEntityType(new FullQualifiedName(entitySet
				.getEntityType().getNamespace(), entitySet.getEntityType().getName()));
		String entityKey = uriInfo.getKeyPredicates().get(0).getLiteral();
		String property = uriInfo.getPropertyPath().get(0).getName();
		log.info("Content: " + entry.toString()); 
		try {
			sparqlStatement = prepareUpdateEntitySimplePropertyValueQuery(entitySet, entityType, uriInfo.getKeyPredicates(), property,
					entry);
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new ODataException(e.getMessage());
		}
		try {
			executeUpdate(sparqlStatement);
		} catch (SQLException e) {
			return ODataResponse.status(HttpStatusCodes.NOT_FOUND).build();
		}
		return ODataResponse.status(HttpStatusCodes.NO_CONTENT).build();
	}

	private SparqlStatement prepareUpdateEntitySimplePropertyValueQuery(EdmEntitySet entitySet,
			RdfEntityType entityType, List<KeyPredicate>  entityKeys, String property, Object entry) throws ODataApplicationException {
		return sparqlUpdateInsertBuilder.generateUpdateEntitySimplePropertyValueQuery(entityType, entityKeys, property, entry);
	}

	@Override
	public ODataResponse readEntityLinks(GetEntitySetLinksUriInfo uriInfo, String contentType) throws ODataException {
		RdfEntityType entityType = null;
		EdmEntitySet entitySet = null;
		EdmEntitySet targetEntitySet = null;
		NavigationSegment navigationProperty = null;
		String entityKey = uriInfo.getKeyPredicates().get(0).getLiteral();
		String decodedEntityKey = RdfEntity.URLDecodeEntityKey(entityKey);
		SparqlStatement sparqlStatement = null;
		List<Map<String, Object>> data = null;
		SparqlResults rdfResults;
		if (uriInfo.getNavigationSegments().size() == 0) {
			throw new ODataException("No navigation segments defined");
		} else if (uriInfo.getNavigationSegments().size() == 1) {
			entitySet = uriInfo.getStartEntitySet();
			entityType = rdfEdmProvider.getMappedEntityType(new FullQualifiedName(entitySet.getEntityType()
					.getNamespace(), entitySet.getEntityType().getName()));
			navigationProperty = uriInfo.getNavigationSegments().get(0);
			targetEntitySet = uriInfo.getTargetEntitySet();
			sparqlStatement = prepareEntityLinksQuery(entitySet, entityType, navigationProperty, targetEntitySet,
					entityKey, uriInfo);
		}
		try {
			rdfResults = executeLinksQuery(entitySet, entityType, sparqlStatement, uriInfo.getNavigationSegments());
		} catch (OData2SparqlException e) {
			throw new ODataException("Navigation query failed");
		}
		RdfPrefixes rdfPrefixes = rdfEdmProvider.getRdfModel().getRdfPrefixes();
		String qnameEntityKey = rdfPrefixes.entitykeyToQName(decodedEntityKey);
		data = rdfResults.getLinks(qnameEntityKey, navigationProperty.getNavigationProperty().getName());
		if (data == null) {
			throw new ODataNotFoundException(ODataNotFoundException.ENTITY);
		}
		ODataEntityProviderPropertiesBuilder propertiesBuilder = EntityProviderWriteProperties.serviceRoot(getContext()
				.getPathInfo().getServiceRoot());

		return EntityProvider.writeLinks(contentType, targetEntitySet, data, propertiesBuilder.build());

	}

	private SparqlStatement prepareEntityLinksQuery(EdmEntitySet entitySet, RdfEntityType entityType,
			NavigationSegment navigationProperty, EdmEntitySet targetEntitySet, String entityKey,
			GetEntitySetLinksUriInfo uriInfo) throws EdmException, ODataApplicationException {
		return sparqlUpdateInsertBuilder.generateEntityLinksQuery(entityType, navigationProperty, targetEntitySet, entityKey);
	}
	private SparqlStatement prepareEntityLinkQuery(EdmEntitySet entitySet, RdfEntityType entityType,
			NavigationSegment navigationProperty, EdmEntitySet targetEntitySet, String entityKey,
			GetEntityLinkUriInfo  uriInfo) throws EdmException, ODataApplicationException {
		return sparqlUpdateInsertBuilder.generateEntityLinksQuery(entityType, navigationProperty, targetEntitySet, entityKey);
	}
	@Override
	public ODataResponse countEntityLinks(GetEntitySetLinksCountUriInfo uriInfo, String contentType)
			throws ODataException {
		RdfEntityType entityType = null;
		EdmEntitySet entitySet = null;
		EdmEntitySet targetEntitySet = null;
		NavigationSegment navigationProperty = null;
		String entityKey = uriInfo.getKeyPredicates().get(0).getLiteral();
		SparqlStatement sparqlStatement = null;
		if (uriInfo.getNavigationSegments().size() == 0) {
			throw new ODataException("No navigation segments defined");
		} else if (uriInfo.getNavigationSegments().size() == 1) {
			entitySet = uriInfo.getStartEntitySet();
			entityType = rdfEdmProvider.getMappedEntityType(new FullQualifiedName(entitySet.getEntityType()
					.getNamespace(), entitySet.getEntityType().getName()));
			navigationProperty = uriInfo.getNavigationSegments().get(0);
			targetEntitySet = uriInfo.getTargetEntitySet();
			sparqlStatement = prepareEntityLinksCountQuery(entitySet, entityType, navigationProperty, targetEntitySet,
					entityKey, uriInfo);
		}

		try {
			return executeCountQuery(sparqlStatement, contentType);
		} catch (OData2SparqlException e) {
			log.error(e.getMessage());
			throw new ODataException();
		}
	}

	private SparqlStatement prepareEntityLinksCountQuery(EdmEntitySet entitySet, RdfEntityType entityType,
			NavigationSegment navigationProperty, EdmEntitySet targetEntitySet, String entityKey,
			GetEntitySetLinksCountUriInfo uriInfo) throws EdmException, ODataApplicationException {
		return sparqlUpdateInsertBuilder.generateEntityLinksCountQuery(entityType, navigationProperty, targetEntitySet,
				entityKey);
	}
	private SparqlStatement prepareEntityLinkCountQuery(EdmEntitySet entitySet, RdfEntityType entityType,
			NavigationSegment navigationProperty, EdmEntitySet targetEntitySet, String entityKey,
			GetEntityLinkCountUriInfo uriInfo) throws EdmException, ODataApplicationException {
		return sparqlUpdateInsertBuilder.generateEntityLinksCountQuery(entityType, navigationProperty, targetEntitySet,
				entityKey);
	}
	@Override
	public ODataResponse executeFunctionImport(GetFunctionImportUriInfo uriInfo, String contentType)
			throws ODataException {
		// TODO uriType=URI10
		return super.executeFunctionImport(uriInfo, contentType);
	}

	@Override
	public ODataResponse executeFunctionImportValue(GetFunctionImportUriInfo uriInfo, String contentType)
			throws ODataException {
		// TODO Auto-generated method stub
		return super.executeFunctionImportValue(uriInfo, contentType);
	}

	@Override
	public ODataResponse createEntityLink(PostUriInfo uriInfo, InputStream content, String requestContentType,
			String contentType) throws ODataException {
		if (uriInfo.getNavigationSegments().size() > 1) {
			throw new ODataNotImplementedException();
		}

		//No support for media resources
		if (uriInfo.getStartEntitySet().getEntityType().hasStream()) {
			throw new ODataNotImplementedException();
		}

		EntityProviderReadProperties properties = EntityProviderReadProperties.init().mergeSemantic(false).build();

		List<String> entry;
		try {
			entry = EntityProvider.readLinks(requestContentType, uriInfo.getStartEntitySet(), content);
			log.info("Content: " + entry.toString()); 
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new ODataException(e.getMessage());
		}

		//Map<String, Object> data = entry.getProperties();
		//now one can use the data to create the entry in the backend ...

		SparqlStatement sparqlStatement = null;
		RdfEntityType entityType = null;
		EdmEntitySet entitySet = null;
		entitySet = uriInfo.getStartEntitySet();
		EdmEntitySet targetEntitySet = uriInfo.getTargetEntitySet();
		KeyPredicate keyPredicate = uriInfo.getKeyPredicates().get(0);
		String entityKey = keyPredicate.getLiteral();
		entityType = rdfEdmProvider.getMappedEntityType(new FullQualifiedName(entitySet.getEntityType()
				.getNamespace(), entitySet.getEntityType().getName()));
		try {
			sparqlStatement = prepareInsertLinkQuery(entitySet, targetEntitySet, entityType, entityKey, uriInfo.getNavigationSegments().get(0), entry);
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new ODataException(e.getMessage());
		}
		try {
			executeInsert(sparqlStatement);
		} catch (SQLException e) {
			log.error(e.getMessage());
			throw new ODataException(e.getMessage());
		}

		return ODataResponse.status(HttpStatusCodes.NO_CONTENT).build();
	}

	@Override
	public ODataResponse readEntityLink(GetEntityLinkUriInfo uriInfo, String contentType) throws ODataException {
		RdfEntityType entityType = null;
		EdmEntitySet entitySet = null;
		EdmEntitySet targetEntitySet = null;
		NavigationSegment navigationProperty = null;
		String entityKey = uriInfo.getKeyPredicates().get(0).getLiteral();
		String decodedEntityKey = RdfEntity.URLDecodeEntityKey(entityKey);
		SparqlStatement sparqlStatement = null;
		List<Map<String, Object>> data = null;
		SparqlResults rdfResults;
		if (uriInfo.getNavigationSegments().size() == 0) {
			throw new ODataException("No navigation segments defined");
		} else if (uriInfo.getNavigationSegments().size() == 1) {
			entitySet = uriInfo.getStartEntitySet();
			entityType = rdfEdmProvider.getMappedEntityType(new FullQualifiedName(entitySet.getEntityType()
					.getNamespace(), entitySet.getEntityType().getName()));
			navigationProperty = uriInfo.getNavigationSegments().get(0);
			targetEntitySet = uriInfo.getTargetEntitySet();
			sparqlStatement = prepareEntityLinkQuery(entitySet, entityType, navigationProperty, targetEntitySet,
					entityKey, uriInfo);
		}
		try {
			rdfResults = executeLinksQuery(entitySet, entityType, sparqlStatement, uriInfo.getNavigationSegments());
		} catch (OData2SparqlException e) {
			throw new ODataException("Navigation query failed");
		}
		RdfPrefixes rdfPrefixes = rdfEdmProvider.getRdfModel().getRdfPrefixes();
		String qnameEntityKey = rdfPrefixes.entitykeyToQName(decodedEntityKey);
		data = rdfResults.getLinks(qnameEntityKey, navigationProperty.getNavigationProperty().getName());
		if (data == null) {
			throw new ODataNotFoundException(ODataNotFoundException.ENTITY);
		}
		ODataEntityProviderPropertiesBuilder propertiesBuilder = EntityProviderWriteProperties.serviceRoot(getContext()
				.getPathInfo().getServiceRoot());

		return EntityProvider.writeLinks(contentType, targetEntitySet, data, propertiesBuilder.build());
	}

	@Override
	public ODataResponse existsEntityLink(GetEntityLinkCountUriInfo uriInfo, String contentType) throws ODataException {
		RdfEntityType entityType = null;
		EdmEntitySet entitySet = null;
		EdmEntitySet targetEntitySet = null;
		NavigationSegment navigationProperty = null;
		String entityKey = uriInfo.getKeyPredicates().get(0).getLiteral();
		SparqlStatement sparqlStatement = null;
		if (uriInfo.getNavigationSegments().size() == 0) {
			throw new ODataException("No navigation segments defined");
		} else if (uriInfo.getNavigationSegments().size() == 1) {
			entitySet = uriInfo.getStartEntitySet();
			entityType = rdfEdmProvider.getMappedEntityType(new FullQualifiedName(entitySet.getEntityType()
					.getNamespace(), entitySet.getEntityType().getName()));
			navigationProperty = uriInfo.getNavigationSegments().get(0);
			targetEntitySet = uriInfo.getTargetEntitySet();
			sparqlStatement = prepareEntityLinkCountQuery(entitySet, entityType, navigationProperty, targetEntitySet,
					entityKey, uriInfo);
		}

		try {
			return executeCountQuery(sparqlStatement, contentType);
		} catch (OData2SparqlException e) {
			log.error(e.getMessage());
			throw new ODataException();
		}
	}

	@Override
	public ODataResponse updateEntityLink(PutMergePatchUriInfo uriInfo, InputStream content, String requestContentType,
			String contentType) throws ODataException {
		//No support for media resources
		if (uriInfo.getStartEntitySet().getEntityType().hasStream()) {
			throw new ODataNotImplementedException();
		}
		
		List<String> entry;
		try {
			entry = EntityProvider.readLinks(requestContentType, uriInfo.getStartEntitySet(), content);
			log.info("Content: " + entry.toString()); 
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new ODataException(e.getMessage());
		}
		SparqlStatement sparqlStatement = null;
		RdfEntityType entityType = null;
		EdmEntitySet entitySet = uriInfo.getStartEntitySet();
		EdmEntitySet targetEntitySet = uriInfo.getTargetEntitySet();
		KeyPredicate keyPredicate = uriInfo.getKeyPredicates().get(0);
		String entityKey = keyPredicate.getLiteral();
		KeyPredicate targetKeyPredicate = uriInfo.getTargetKeyPredicates().get(0);
		String targetEntityKey = targetKeyPredicate.getLiteral();
		entityType = rdfEdmProvider.getMappedEntityType(new FullQualifiedName(entitySet.getEntityType()
				.getNamespace(), entitySet.getEntityType().getName()));
		try {
			sparqlStatement = prepareUpdateLinkQuery(entitySet, targetEntitySet, entityType, entityKey, targetEntityKey, uriInfo.getNavigationSegments().get(0), entry);
		} catch (Exception e) {
			throw new ODataException(e.getMessage());
		}
		try {
			executeUpdate(sparqlStatement);
		} catch (SQLException e) {
			log.error(e.getMessage());
			throw new ODataException(e.getMessage());
		}

		return ODataResponse.status(HttpStatusCodes.NO_CONTENT).build();
	}

	@Override
	public ODataResponse deleteEntityLink(DeleteUriInfo uriInfo, String contentType) throws ODataException {
		//No support for media resources
		if (uriInfo.getStartEntitySet().getEntityType().hasStream()) {
			throw new ODataNotImplementedException();
		}
		
		SparqlStatement sparqlStatement = null;
		RdfEntityType entityType = null;
		EdmEntitySet entitySet = uriInfo.getStartEntitySet();
		EdmEntitySet targetEntitySet = uriInfo.getTargetEntitySet();
		KeyPredicate keyPredicate = uriInfo.getKeyPredicates().get(0);
		String entityKey = keyPredicate.getLiteral();
		KeyPredicate targetKeyPredicate=null;
		String targetEntityKey=null;	
		if(uriInfo.getTargetKeyPredicates().size()!=0){
			targetKeyPredicate = uriInfo.getTargetKeyPredicates().get(0);
			targetEntityKey = targetKeyPredicate.getLiteral();			
		}

		entityType = rdfEdmProvider.getMappedEntityType(new FullQualifiedName(entitySet.getEntityType()
				.getNamespace(), entitySet.getEntityType().getName()));
		try {
			sparqlStatement = prepareDeleteLinkQuery(entitySet, targetEntitySet, entityType, entityKey, targetEntityKey, uriInfo.getNavigationSegments().get(0));
		} catch (Exception e) {
			throw new ODataException(e.getMessage());
		}
		try {
			executeUpdate(sparqlStatement);
		} catch (SQLException e) {
			log.error(e.getMessage());
			throw new ODataException(e.getMessage());
		}
		return ODataResponse.status(HttpStatusCodes.NO_CONTENT).build();
	}

	@Override
	public ODataResponse executeBatch(BatchHandler handler, String contentType, InputStream content)
			throws ODataException {
		  List<BatchResponsePart> batchResponseParts = new ArrayList<BatchResponsePart>();
		  PathInfo pathInfo = getContext().getPathInfo();
		  EntityProviderBatchProperties batchProperties = EntityProviderBatchProperties.init().pathInfo(pathInfo).build();
		  List<BatchRequestPart> batchParts = EntityProvider.parseBatchRequest(contentType, content, batchProperties);
		  for (BatchRequestPart batchPart : batchParts) {
		    batchResponseParts.add(handler.handleBatchPart(batchPart));
		  }
		  return EntityProvider.writeBatchResponse(batchResponseParts);
	}

	@Override
	public BatchResponsePart executeChangeSet(BatchHandler handler, List<ODataRequest> requests) throws ODataException {
	    List<ODataResponse> responses = new ArrayList<ODataResponse>();
	    for (ODataRequest request : requests) {
	      ODataResponse response = handler.handleRequest(request);
	      if (response.getStatus().getStatusCode() >= HttpStatusCodes.BAD_REQUEST.getStatusCode()) {
	        // Rollback
	        List<ODataResponse> errorResponses = new ArrayList<ODataResponse>(1);
	        errorResponses.add(response);
	        return BatchResponsePart.responses(errorResponses).changeSet(false).build();
	      }
	      responses.add(response);
	    }
	    return BatchResponsePart.responses(responses).changeSet(true).build();
	}
	
	//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	//The following are nice to have services

	@Override
	public void setContext(ODataContext context) {
		// TODO Auto-generated method stub
		super.setContext(context);
	}

	@Override
	public ODataContext getContext() {
		// TODO Auto-generated method stub
		return super.getContext();
	}
	@Override
	public ODataResponse readEntityComplexProperty(GetComplexPropertyUriInfo uriInfo, String contentType)
			throws ODataException {
		// TODO Auto-generated method stub
		return super.readEntityComplexProperty(uriInfo, contentType);
	}

	@Override
	public ODataResponse updateEntityComplexProperty(PutMergePatchUriInfo uriInfo, InputStream content,
			String requestContentType, boolean merge, String contentType) throws ODataException {
		// TODO Auto-generated method stub
		return super.updateEntityComplexProperty(uriInfo, content, requestContentType, merge, contentType);
	}

	@Override
	public ODataResponse readEntityMedia(GetMediaResourceUriInfo uriInfo, String contentType) throws ODataException {
		// TODO Auto-generated method stub
		return super.readEntityMedia(uriInfo, contentType);
	}

	@Override
	public ODataResponse updateEntityMedia(PutMergePatchUriInfo uriInfo, InputStream content,
			String requestContentType, String contentType) throws ODataException {
		// TODO Auto-generated method stub
		return super.updateEntityMedia(uriInfo, content, requestContentType, contentType);
	}

	@Override
	public ODataResponse deleteEntityMedia(DeleteUriInfo uriInfo, String contentType) throws ODataException {
		// TODO Auto-generated method stub
		return super.deleteEntityMedia(uriInfo, contentType);
	}

	public RdfEdmProvider getRdfEdmProvider() {
		return this.rdfEdmProvider;
	}

}
