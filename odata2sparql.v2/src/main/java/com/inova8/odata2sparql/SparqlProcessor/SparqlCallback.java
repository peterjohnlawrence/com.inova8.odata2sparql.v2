/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 inova8.com and/or its affiliates. All rights reserved.
 *
 * 
 */
package com.inova8.odata2sparql.SparqlProcessor;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.olingo.odata2.api.ODataCallback;
import org.apache.olingo.odata2.api.edm.EdmException;
import org.apache.olingo.odata2.api.edm.EdmNavigationProperty;
import org.apache.olingo.odata2.api.ep.EntityProviderException;
import org.apache.olingo.odata2.api.ep.EntityProviderWriteProperties;
import org.apache.olingo.odata2.api.ep.callback.OnWriteEntryContent;
import org.apache.olingo.odata2.api.ep.callback.OnWriteFeedContent;
import org.apache.olingo.odata2.api.ep.callback.WriteEntryCallbackContext;
import org.apache.olingo.odata2.api.ep.callback.WriteEntryCallbackResult;
import org.apache.olingo.odata2.api.ep.callback.WriteFeedCallbackContext;
import org.apache.olingo.odata2.api.ep.callback.WriteFeedCallbackResult;
import org.apache.olingo.odata2.api.exception.ODataApplicationException;
import org.apache.olingo.odata2.api.uri.ExpandSelectTreeNode;

import com.inova8.odata2sparql.RdfModel.RdfEntity;


class SparqlCallback implements OnWriteEntryContent, OnWriteFeedContent {
	private final URI serviceRoot;
	private final SparqlResults sparqlResults;

	SparqlCallback( SparqlResults sparqlResults, URI serviceRoot) {

		this.serviceRoot = serviceRoot;
		this.sparqlResults = sparqlResults;
	}

	public WriteEntryCallbackResult retrieveEntryResult(WriteEntryCallbackContext context)
			throws ODataApplicationException {
		WriteEntryCallbackResult result = new WriteEntryCallbackResult();
		EdmNavigationProperty navigationProperty = context.getNavigationProperty();
		String navigationPropertyName = null;
		String subjectEntity = ((RdfEntity) (context.getEntryData())).getSubject();
		
		@SuppressWarnings("unused")
		Map<String, Object> keys;
		try {
			keys = context.extractKeyFromEntryData();
		} catch (EntityProviderException e) {
			throw new ODataApplicationException("Error extracting keys", null);
		}
	      
	      
		Map<String, Object> data = null;
		try {
			navigationPropertyName=navigationProperty.getName();
			data = sparqlResults.retrieveEntryResultsData(subjectEntity, navigationPropertyName);
		} catch (EdmException e) {

			throw new ODataApplicationException("Error retrieving navproperty data for: " + subjectEntity + "/"
					+ navigationPropertyName, null);
		}
		
		Map<String, ODataCallback> callbacks = locateEntityCallbacks(context.getCurrentExpandSelectTreeNode(), sparqlResults);
		EntityProviderWriteProperties inlineProperties = EntityProviderWriteProperties.serviceRoot(serviceRoot)
				.expandSelectTree(context.getCurrentExpandSelectTreeNode()).callbacks(callbacks).build();
		result.setEntryData(data);
		result.setInlineProperties(inlineProperties);
		return result;
	}

	public WriteFeedCallbackResult retrieveFeedResult(WriteFeedCallbackContext context)
			throws ODataApplicationException {
		WriteFeedCallbackResult result = new WriteFeedCallbackResult();
		EdmNavigationProperty navigationProperty = context.getNavigationProperty();
		String navigationPropertyName = null;
		String subjectEntity = ((RdfEntity) (context.getEntryData())).getSubject();
		
		@SuppressWarnings("unused")
		Map<String, Object> keys;
		try {
			keys = context.extractKeyFromEntryData();
		} catch (EntityProviderException e1) {
			throw new ODataApplicationException("Error extracting keys", null);
		}
      
		List<Map<String, Object>> data = null;
		try {
			navigationPropertyName=navigationProperty.getName();
			data = sparqlResults.retrieveFeedResultData(subjectEntity, navigationPropertyName);
			
		} catch (EdmException e) {
			throw new ODataApplicationException("Error retrieving navproperty data for: " + subjectEntity + "/"
					+ navigationPropertyName, null);
		}
		Map<String, ODataCallback> callbacks = locateEntityCallbacks(context.getCurrentExpandSelectTreeNode(), sparqlResults);
		EntityProviderWriteProperties inlineProperties = EntityProviderWriteProperties.serviceRoot(serviceRoot)
				.expandSelectTree(context.getCurrentExpandSelectTreeNode()).selfLink(context.getSelfLink()).callbacks(callbacks).build();
		result.setFeedData(data);
		result.setInlineProperties(inlineProperties);
		return result;
	}
	//TODO also used by SparqlProcessor but private 
	private Map<String, ODataCallback> locateEntityCallbacks(ExpandSelectTreeNode expandSelectTreeNode, SparqlResults rdfResults)
		{
		Map<String, ODataCallback> callbacks = new HashMap<String, ODataCallback>();
		for (Entry<String, ExpandSelectTreeNode> expandSelectTreeNodeLinksEntry : expandSelectTreeNode.getLinks().entrySet()) {
			String navigationPropertyName = expandSelectTreeNodeLinksEntry.getKey();
			callbacks.put(navigationPropertyName, new SparqlCallback(rdfResults,	this.serviceRoot));
		}
		return callbacks;
	}

}
