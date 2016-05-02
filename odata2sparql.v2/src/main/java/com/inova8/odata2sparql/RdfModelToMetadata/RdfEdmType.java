package com.inova8.odata2sparql.RdfModelToMetadata;

import java.util.HashMap;
import java.util.Map;

import org.apache.olingo.odata2.api.edm.EdmSimpleTypeKind;

import com.inova8.odata2sparql.RdfConstants.RdfConstants;

public class RdfEdmType {
	private static final Map<String, EdmSimpleTypeKind> SIMPLE_TYPE_MAPPING = new HashMap<String, EdmSimpleTypeKind>();

	static {
		SIMPLE_TYPE_MAPPING.put(RdfConstants.RDF_PLAIN_LITERAL, EdmSimpleTypeKind.String);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2000/01/rdf-schema#Literal", EdmSimpleTypeKind.String);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#decimal", EdmSimpleTypeKind.Decimal);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#Literal", EdmSimpleTypeKind.String);
		SIMPLE_TYPE_MAPPING.put(RdfConstants.XSD_STRING, EdmSimpleTypeKind.String);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#boolean", EdmSimpleTypeKind.Boolean);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#float", EdmSimpleTypeKind.Double);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#double", EdmSimpleTypeKind.Double);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#duration", EdmSimpleTypeKind.Int16);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#dateTime", EdmSimpleTypeKind.DateTime);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#time", EdmSimpleTypeKind.Time);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#date", EdmSimpleTypeKind.DateTime);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#gYearMonth", EdmSimpleTypeKind.DateTime);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#gYear", EdmSimpleTypeKind.DateTime);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#gMonthDay", EdmSimpleTypeKind.DateTime);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#gDay", EdmSimpleTypeKind.DateTime);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#gMonth", EdmSimpleTypeKind.DateTime);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#hexBinary", EdmSimpleTypeKind.Binary);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#base64Binary", EdmSimpleTypeKind.Binary);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#anyURI", EdmSimpleTypeKind.String);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#QName", EdmSimpleTypeKind.String);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#NOTATION", EdmSimpleTypeKind.String);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#normalizedString", EdmSimpleTypeKind.String);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#token", EdmSimpleTypeKind.String);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#language", EdmSimpleTypeKind.String);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#IDREFS", EdmSimpleTypeKind.String);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#ENTITIES", EdmSimpleTypeKind.String);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#NMTOKEN", EdmSimpleTypeKind.String);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#Name", EdmSimpleTypeKind.String);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#NCName", EdmSimpleTypeKind.String);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#ID", EdmSimpleTypeKind.String);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#IDREF", EdmSimpleTypeKind.String);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#ENTITY", EdmSimpleTypeKind.String);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#integer", EdmSimpleTypeKind.Int32);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#nonPositiveInteger", EdmSimpleTypeKind.Int32);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#negativeInteger", EdmSimpleTypeKind.Int32);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#long", EdmSimpleTypeKind.Int64);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#int", EdmSimpleTypeKind.Int32);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#short", EdmSimpleTypeKind.Int16);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#byte", EdmSimpleTypeKind.Byte);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#nonNegativeInteger", EdmSimpleTypeKind.Int32);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#unsignedLong", EdmSimpleTypeKind.Int64);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#unsignedInt", EdmSimpleTypeKind.Int32);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#unsignedShort", EdmSimpleTypeKind.Int16);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#unsignedByte", EdmSimpleTypeKind.Byte);
		SIMPLE_TYPE_MAPPING.put("http://www.w3.org/2001/XMLSchema#positiveInteger", EdmSimpleTypeKind.Int32);

	};
	public static EdmSimpleTypeKind getEdmType(String propertyTypeName) {
		if (!SIMPLE_TYPE_MAPPING.containsKey(propertyTypeName))
			//throw new UnsupportedOperationException("TODO implement edmtype conversion for rdf type: " + rdfType);
		return EdmSimpleTypeKind.String;
		return SIMPLE_TYPE_MAPPING.get(propertyTypeName);
	}
}
