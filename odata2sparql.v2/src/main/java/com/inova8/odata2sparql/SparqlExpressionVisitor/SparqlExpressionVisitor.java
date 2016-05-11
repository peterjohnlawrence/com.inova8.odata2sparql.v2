/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 inova8.com and/or its affiliates. All rights reserved.
 *
 * 
 */
package com.inova8.odata2sparql.SparqlExpressionVisitor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.validator.routines.UrlValidator;
import org.apache.olingo.odata2.api.edm.EdmException;
import org.apache.olingo.odata2.api.edm.EdmLiteral;
import org.apache.olingo.odata2.api.edm.EdmNavigationProperty;
import org.apache.olingo.odata2.api.edm.EdmTyped;
import org.apache.olingo.odata2.api.edm.FullQualifiedName;
import org.apache.olingo.odata2.api.uri.expression.BinaryExpression;
import org.apache.olingo.odata2.api.uri.expression.BinaryOperator;
import org.apache.olingo.odata2.api.uri.expression.ExpressionVisitor;
import org.apache.olingo.odata2.api.uri.expression.FilterExpression;
import org.apache.olingo.odata2.api.uri.expression.LiteralExpression;
import org.apache.olingo.odata2.api.uri.expression.MemberExpression;
import org.apache.olingo.odata2.api.uri.expression.MethodExpression;
import org.apache.olingo.odata2.api.uri.expression.MethodOperator;
import org.apache.olingo.odata2.api.uri.expression.OrderByExpression;
import org.apache.olingo.odata2.api.uri.expression.OrderExpression;
import org.apache.olingo.odata2.api.uri.expression.PropertyExpression;
import org.apache.olingo.odata2.api.uri.expression.SortOrder;
import org.apache.olingo.odata2.api.uri.expression.UnaryExpression;
import org.apache.olingo.odata2.api.uri.expression.UnaryOperator;

import com.inova8.odata2sparql.RdfConstants.RdfConstants;
import com.inova8.odata2sparql.RdfModel.RdfEntity;
import com.inova8.odata2sparql.RdfModel.RdfModel;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfAssociation;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfEntityType;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfProperty;
import com.inova8.odata2sparql.RdfModelToMetadata.RdfModelToMetadata;

public class SparqlExpressionVisitor implements ExpressionVisitor {
	private final String SUBJECT_POSTFIX = "_s";
	private String sPath = "";
	private EdmNavigationProperty currentNavigationProperty;
	//Container of properties expressed in filter that need to be explicit selected in SPARQL where clause
	//List<RdfProperty> properties = new ArrayList<RdfProperty>();
	private final HashSet<RdfProperty> properties = new HashSet<RdfProperty>();
	private final HashMap<String, HashSet<RdfProperty>> navigationProperties = new HashMap<String, HashSet<RdfProperty>>();

	private final HashMap<String, NavPropertyPropertyFilter> navPropertyPropertyFilters = new HashMap<String, NavPropertyPropertyFilter>();

	private final UrlValidator urlValidator = new UrlValidator();
	//private final RdfEdmProvider rdfEdmProvider;
	private final RdfModel rdfModel;
	private final RdfModelToMetadata rdfModelToMetadata;
	private final RdfEntityType entityType;
	private String conditionString = "";
	private final Boolean allStatus = false;
	public SparqlExpressionVisitor(RdfModel rdfModel,RdfModelToMetadata rdfModelToMetadata, RdfEntityType entityType) {
		super();
		this.rdfModel = rdfModel;
		this.rdfModelToMetadata = rdfModelToMetadata;
		this.entityType = entityType;
	}
	public boolean isAllStatus() {
		//TODO
		return allStatus;
	}

	public String getConditionString() {
		return conditionString;
	}

	public String getAggregateFilterClause() {
		return this.isAllStatus() ? conditionString : "";
	}

	public HashMap<String, NavPropertyPropertyFilter> getNavPropertyPropertyFilters() {
		return navPropertyPropertyFilters;
	}

	public HashMap<String, HashSet<RdfProperty>> getNavigationProperties() {
		return navigationProperties;
	}

	public String getNavigationPropertySubjects() {
		String navigationPropertySubject = "";
		for (Entry<String, HashSet<RdfProperty>> navigationPropertyEntry : navigationProperties.entrySet()) {
			if (entityType.isOperation()) {

			} else {//navigationPropertyEntry.getValue().
				navigationPropertySubject += "?" + navigationPropertyEntry.getKey() + "_s ";
			}
		}
		return navigationPropertySubject;
	}

	public String getPropertyClause() {
		String propertyClause = "";
		String key = entityType.entityTypeName;
		for (RdfProperty property : properties) {
			if (entityType.isOperation()) {

			} else {
				if (property.getIsKey()) {
					if (properties.size() > 1) {
						//prefix predicate with key
						propertyClause += "BIND( ?" + key + "_s as ?" + key+ property.propertyName + RdfConstants.PROPERTY_POSTFIX + ").";
					} else {
						//prefix predicate with key
						propertyClause += "?" + key + "_s <" + property.getPropertyURI() + "> ?"
								+key+ property.propertyName + RdfConstants.PROPERTY_POSTFIX + " .";
					}
				} else {
					//prefix predicate with key
					propertyClause += "?" + key + "_s <" + property.getPropertyURI() + "> ?"
							+key+ property.propertyName + RdfConstants.PROPERTY_POSTFIX + " .";
				}
			}
		}
		for (Entry<String, HashSet<RdfProperty>> navigationPropertyEntry : navigationProperties.entrySet()) {
			if (entityType.isOperation()) {

			} else {//navigationPropertyEntry.getValue().
				for (RdfProperty rdfProperty : navigationPropertyEntry.getValue()) {
					if (rdfProperty.getEDMPropertyName().equals(RdfConstants.SUBJECT)) {
						//TODO need to add the navigation property to this subject rather than <rdf:subject>
					} else {
						propertyClause += "?" + navigationPropertyEntry.getKey() + "_s <"
								+ rdfProperty.getPropertyURI() + "> ?" + navigationPropertyEntry.getKey()
								+ rdfProperty.propertyName + RdfConstants.PROPERTY_POSTFIX + " .";
					}
				}
			}
		}
		return propertyClause;

	}

	public String getFilterClause() {
		if (!allStatus) {
			if (conditionString != "")
				return "FILTER(" + conditionString + ")";
		}
		return "";
	}



	@Override
	public Object visitFilterExpression(FilterExpression filterExpression, String expressionString, Object expression) {
		conditionString = (String) expression;
		if (expression == "") {
			return "";
		} else {
			return "FILTER(" + expression + ")";
		}
	}

	@Override
	public Object visitBinary(BinaryExpression binaryExpression, BinaryOperator operator, Object leftSide,
			Object rightSide) {
		String sparqlOperator = "";
		switch (operator) {
		case EQ:
			sparqlOperator = "=";
			break;
		case NE:
			sparqlOperator = "!=";
			break;
		case OR:
			sparqlOperator = "||";
			break;
		case AND:
			sparqlOperator = "&&";
			break;
		case GE:
			sparqlOperator = ">=";
			break;
		case GT:
			sparqlOperator = ">";
			break;
		case LE:
			sparqlOperator = "<=";
			break;
		case LT:
			sparqlOperator = "<";
			break;
		default:
			//Other operators are not supported for SQL Statements
			throw new UnsupportedOperationException("Unsupported operator: " + operator.toUriLiteral());
		}
		//return the binary statement
		return "(" + leftSide + " " + sparqlOperator + " " + rightSide + ")";
	}

	@Override
	public Object visitOrderByExpression(OrderByExpression orderByExpression, String expressionString,
			List<Object> orders) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitOrder(OrderExpression orderExpression, Object filterResult, SortOrder sortOrder) {
		return null;
	}

	@Override
	public Object visitLiteral(LiteralExpression literal, EdmLiteral edmLiteral) {

		String expandedKey = this.rdfModel.getRdfPrefixes().expandPrefix(RdfEntity.URLDecodeEntityKey(edmLiteral.getLiteral()));

		if (urlValidator.isValid(expandedKey)) {
			return "<" + expandedKey + ">";
		} else {
			switch (edmLiteral.getType().toString()) {
			case "Null":
				return "null";
			case "Edm.Time":
				return "\"" + edmLiteral.getLiteral().toString() + "\"^^xsd:time";
			case "Edm.DateTime":
				return "\"" + edmLiteral.getLiteral().toString() + "\"^^xsd:dateTime";
			case "Edm.DateTimeOffset":
			case "Edm.String":
				return "\"" + edmLiteral.getLiteral().toString() + "\"";
			case "Edm.Guid":
				return "guid\"" + edmLiteral.getLiteral().toString() + "\"";
			case "Edm.Binary":
				return "X\"" + edmLiteral.getLiteral().toString() + "\"";
			default:
				return edmLiteral.getLiteral().toString();
			}
		}
	}

	@Override
	public Object visitMethod(MethodExpression methodExpression, MethodOperator method, List<Object> parameters) {
		String sparqlmethod = "";
		switch (method) {
		case ENDSWITH:
			sparqlmethod = "STRENDS(" + parameters.get(1) + "," + parameters.get(0) + ")";
			break;
		case INDEXOF:
			sparqlmethod = "";
			break;
		case STARTSWITH:
			sparqlmethod = "STRSTARTS(" + parameters.get(1) + "," + parameters.get(0) + ")";
			break;
		case TOLOWER:
			sparqlmethod = "LCASE(" + parameters.get(0) + ")";
			break;
		case TOUPPER:
			sparqlmethod = "UCASE(" + parameters.get(0) + ")";
			break;
		case TRIM:
			sparqlmethod = "";
			break;
		case SUBSTRING:
			if (parameters.size() > 2) {
				sparqlmethod = "substr(" + parameters.get(1) + "," + parameters.get(0) + "," + parameters.get(2) + ")";
			} else {
				sparqlmethod = "substr(" + parameters.get(1) + "," + parameters.get(0) + ")";
			}
			break;
		case SUBSTRINGOF:
			sparqlmethod = "regex(" + parameters.get(1) + "," + parameters.get(0) + ", \"i\")";
			//TODO replacing with contains sparqlmethod = "contains(" + parameters.get(1) + "," + parameters.get(0) + ")";
			break;
		case CONCAT:
			sparqlmethod = "concat(" + parameters.get(0) + "," + parameters.get(1) + ")";
			break;
		case LENGTH:
			sparqlmethod = "STRLEN(" + parameters.get(0) + ")";
			break;
		case YEAR:
			sparqlmethod = "year(" + parameters.get(0) + ")";
			break;
		case MONTH:
			sparqlmethod = "month(" + parameters.get(0) + ")";
			break;
		case DAY:
			sparqlmethod = "day(" + parameters.get(0) + ")";
			break;
		case HOUR:
			sparqlmethod = "hours(" + parameters.get(0) + ")";
			break;
		case MINUTE:
			sparqlmethod = "minutes(" + parameters.get(0) + ")";
			break;
		case SECOND:
			sparqlmethod = "seconds(" + parameters.get(0) + ")";
			break;
		case ROUND:
			sparqlmethod = "round(" + parameters.get(0) + ")";
			break;
		case FLOOR:
			sparqlmethod = "floor(" + parameters.get(0) + ")";
			break;
		case CEILING:
			sparqlmethod = "ceil(" + parameters.get(0) + ")";
			break;
		default:
			throw new UnsupportedOperationException("Unsupported method: " + method.toUriLiteral());
		}
		return sparqlmethod;
	}

	@Override
	public Object visitMember(MemberExpression memberExpression, Object path, Object property) {

		return property;
	}

	@Override
	public Object visitProperty(PropertyExpression propertyExpression, String uriLiteral, EdmTyped edmProperty) {
		RdfProperty rdfProperty;
		try {
			if (entityType.isOperation()) {
				return "?" + entityType.findProperty(edmProperty.getName()).varName;

			} else {
				if (edmProperty instanceof EdmNavigationProperty) {
					if (sPath.isEmpty()) {
						sPath = entityType.entityTypeName;
					}
					//Need to create path
					currentNavigationProperty = (EdmNavigationProperty) edmProperty;
					sPath += edmProperty.getName();
					putNavPropertyPropertyFilter(sPath,null,null,null);

					return sPath;
				} else if (RdfConstants.SUBJECT.equals(edmProperty.getName())) {
					//TODO still need to add full path to disambiguate
					//rdfProperty = entityType.findProperty(edmProperty.getName());
					//properties.add(rdfProperty);
					if (!sPath.isEmpty()) {
						rdfProperty = entityType.findProperty(edmProperty.getName());
						if (navigationProperties.containsKey(sPath)) {
							navigationProperties.get(sPath).add(rdfProperty);
						} else {
							HashSet<RdfProperty> properties = new HashSet<RdfProperty>();
							properties.add(rdfProperty);
							navigationProperties.put(sPath, properties);
						}
						//putNavPropertyPropertyFilter(sPath,null,rdfProperty,null); 
						String visitProperty = "?" + sPath + SUBJECT_POSTFIX;
						sPath = "";
						return visitProperty;
					} else {
						sPath = "";
						return "?" + entityType.entityTypeName + SUBJECT_POSTFIX;
					}
				} else {
					//TODO will a property really be unique throughout namespace
					if (!sPath.isEmpty()) {
						rdfProperty = this.rdfModelToMetadata.getMappedProperty(new FullQualifiedName(currentNavigationProperty
								.getRelationship().getNamespace(), edmProperty.getName()));
						if (navigationProperties.containsKey(sPath)) {
							navigationProperties.get(sPath).add(rdfProperty);
						} else {
							HashSet<RdfProperty> properties = new HashSet<RdfProperty>();
							properties.add(rdfProperty);
							navigationProperties.put(sPath, properties);
						}
						putNavPropertyPropertyFilter(sPath,null,rdfProperty,null); 
					} else {
						properties.add(entityType.findProperty(edmProperty.getName()));
						putNavPropertyPropertyFilter(entityType.entityTypeName,null,entityType.findProperty(edmProperty.getName()),null); 
					}
					//If sPath="" then the root path so should use entityTypeName instead
					String visitProperty;
					if(sPath.equals("")){
						visitProperty = "?" + entityType.getEDMEntityTypeName() + uriLiteral + RdfConstants.PROPERTY_POSTFIX;
					}else{
						visitProperty = "?" + sPath + uriLiteral + RdfConstants.PROPERTY_POSTFIX;
					}
					sPath = "";
					return visitProperty;
				}
			}
		} catch (EdmException e) {
			throw new UnsupportedOperationException("Unrecognized property" + uriLiteral);
		}
	}

	private void putNavPropertyPropertyFilter(String sPath, RdfAssociation navProperty, RdfProperty property,
			String filter) {
		NavPropertyPropertyFilter navPropertyPropertyFilter;
		if (!(navPropertyPropertyFilters.containsKey(sPath))) {
			navPropertyPropertyFilter = new NavPropertyPropertyFilter();
			navPropertyPropertyFilters.put(sPath, navPropertyPropertyFilter);
		} else {
			navPropertyPropertyFilter = navPropertyPropertyFilters.get(sPath);
		}
		HashMap<String, PropertyFilter> propertyFilters = navPropertyPropertyFilter.getPropertyFilters();
		PropertyFilter propertyFilter;
		if (property != null) {
			if (!propertyFilters.containsKey(property.propertyName)) {
				propertyFilter = new PropertyFilter(property);
				propertyFilters.put(property.propertyName, propertyFilter);
			} else {
				propertyFilter = propertyFilters.get(property.propertyName);
			}
			if (filter != null &&  !filter.isEmpty())
				propertyFilter.getFilters().add(filter);
		}
	}

	@Override
	public Object visitUnary(UnaryExpression unaryExpression, UnaryOperator operator, Object operand) {
		String sparqlunary = "";
		switch (operator) {
		case MINUS:
			sparqlunary = "-";
			break;
		case NOT:
			sparqlunary = "!";
			break;
		default:
			throw new UnsupportedOperationException("Unsupported unary: " + operator.toUriLiteral());
		}
		return sparqlunary;
	}
}
