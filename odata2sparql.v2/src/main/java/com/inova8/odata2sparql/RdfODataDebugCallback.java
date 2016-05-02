/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 inova8.com and/or its affiliates. All rights reserved.
 *
 * 
 */
package com.inova8.odata2sparql;

import org.apache.olingo.odata2.api.ODataDebugCallback;

class RdfODataDebugCallback implements ODataDebugCallback {
	  public boolean isDebugEnabled() {  
	    boolean isDebug = true; // true|configuration|user role check
	    return isDebug; 
	  }
}
