package com.inova8.odata2sparql.RdfConnector.openrdf;

import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;


public class RdfNodeFactory {
	private final static ValueFactory valueFactory = new ValueFactoryImpl();
	public static RdfNode createURI(String uri) {
		RdfNode rdfNode = new RdfNode(valueFactory.createURI( uri));
		return rdfNode;
	}

	public static RdfNode createLiteral(String literal) {
		RdfNode rdfNode = new RdfNode(valueFactory.createLiteral( literal));
		return rdfNode;
	}

}
