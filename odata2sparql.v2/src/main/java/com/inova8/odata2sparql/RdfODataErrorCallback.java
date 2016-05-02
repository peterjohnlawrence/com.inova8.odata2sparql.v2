/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 inova8.com and/or its affiliates. All rights reserved.
 *
 * 
 */
package com.inova8.odata2sparql;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.olingo.odata2.api.ep.EntityProvider;
import org.apache.olingo.odata2.api.exception.ODataApplicationException;
import org.apache.olingo.odata2.api.processor.ODataErrorCallback;
import org.apache.olingo.odata2.api.processor.ODataErrorContext;
import org.apache.olingo.odata2.api.processor.ODataResponse;

class RdfODataErrorCallback implements ODataErrorCallback {
	private final Log log = LogFactory.getLog(RdfODataErrorCallback.class);
	  public ODataResponse handleError(ODataErrorContext context) throws ODataApplicationException { 
	    log.error(context.getException().getClass().getName() + ":" + context.getMessage()); 
	        return EntityProvider.writeErrorDocument(context); 
	  }
}
