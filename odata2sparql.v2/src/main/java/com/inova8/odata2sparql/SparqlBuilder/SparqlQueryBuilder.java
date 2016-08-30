package com.inova8.odata2sparql.SparqlBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.olingo.odata2.api.edm.EdmEntitySet;
import org.apache.olingo.odata2.api.edm.EdmException;
import org.apache.olingo.odata2.api.edm.EdmNavigationProperty;
import org.apache.olingo.odata2.api.exception.ODataApplicationException;
import org.apache.olingo.odata2.api.uri.ExpandSelectTreeNode;
import org.apache.olingo.odata2.api.uri.KeyPredicate;
import org.apache.olingo.odata2.api.uri.NavigationPropertySegment;
import org.apache.olingo.odata2.api.uri.NavigationSegment;
import org.apache.olingo.odata2.api.uri.SelectItem;
import org.apache.olingo.odata2.api.uri.UriInfo;
import org.apache.olingo.odata2.api.uri.UriParser;
import org.apache.olingo.odata2.api.uri.expression.ExceptionVisitExpression;
import org.apache.olingo.odata2.api.uri.expression.FilterExpression;
import org.apache.olingo.odata2.api.uri.info.GetEntityCountUriInfo;
import org.apache.olingo.odata2.api.uri.info.GetEntitySetCountUriInfo;
import org.apache.olingo.odata2.api.uri.info.GetEntitySetUriInfo;
import org.apache.olingo.odata2.api.uri.info.GetEntityUriInfo;

import com.inova8.odata2sparql.Constants.RdfConstants;
import com.inova8.odata2sparql.Exception.OData2SparqlException;
import com.inova8.odata2sparql.RdfModel.RdfEntity;
import com.inova8.odata2sparql.RdfModel.RdfModel;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfAssociation;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfEntityType;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfPrimaryKey;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfProperty;
import com.inova8.odata2sparql.RdfModelToMetadata.RdfModelToMetadata;
import com.inova8.odata2sparql.SparqlExpressionVisitor.NavPropertyPropertyFilter;
import com.inova8.odata2sparql.SparqlExpressionVisitor.PropertyFilter;
import com.inova8.odata2sparql.SparqlExpressionVisitor.SparqlExpressionVisitor;
import com.inova8.odata2sparql.SparqlStatement.SparqlStatement;
import com.inova8.odata2sparql.uri.UriType;

//	Query Pseudocode
//	================
//	
//	ResourcePath
//	------------
//	
//	Defined by 
//		entitySetUriInfo.getNavigationSegments() and entitySetUriInfo.getTargetEntitySet()
//	or
//		entitySetUriInfo.getStartEntitySet()
//		
//		
//	/entitySet()
//	
//		?resource a [rdfs:subClassOf* :entitySet]
//	
//	/entitySet(:id)
//	
//		VALUES(?resource){(:id0)}
//	
//	/entitySet(:id)/navProp1
//	
//		?id :navProp1  ?resource
//		
//	/entitySet(:id)/navProp1(:id1)
//	
//		?id :navProp1  :id1 .
//		
//	/entitySet(:id){/navPropN(:idN)}*/{navProp}?
//	
//		
//		CONSTRUCT{
//	      ?resource a ?resourceType
//			?resource ?resource_p ?resource_o
//			...#select constructs
//			...#expand constructs
//		} 
//		WHERE {
//			OPTIONAL{ #select=*
//				?resource ?resource_p ?resource_o .
//			}
//			{
//				SELECT #select=*
//					?resource
//	
//				/entitySet()
//				?resource a [rdfs:subClassOf* :entitySet]
//				
//				/entitySet(:id)/navProp1
//				?id :navProp1  ?resource	
//				
//				/entitySet(:id)
//				VALUES(?resource){(:id)}
//	
//				/entitySet(:id)/navProp1(:id1)
//				?id :navProp1  :id1 . #validate relationship
//				VALUES(?resource){(:id1)}
//				
//				/entitySet(:id){/navPropN(:idN)}*/navProp
//				?id :navProp1  :id1 . #validate relationships
//				:id1 :navProp2  :id2 .
//				:id2 :navProp3  :id3 . 
//				...
//				:idN :navProp  ?resource
//				
//			}
//		}
//		
//		
//	Expand
//	------
//	
//	$expand=np1/np2/np3/, np4...
//		CONSTRUCT{
//			...#type construct
//			...#path constructs
//			...#select constructs
//			?resource	:np1	?resource_np1 .
//			?resource_np1 :np2 ?resource_np1_np2 .
//			?resource_np1_np2 :np3 ?resource_np1_np2_np3 .
//			?resource	:np4	?resource_np4 .
//			...
//		} 
//		WHERE {
//			...#select clauses
//			SELECT ?resource ?resource_np1 ?resource_np1_np2 ?resource_np1_np2_np3 ?resource_np4 
//			{
//				...
//				OPTIONAL{
//					?resource	:np1	?resource_np1 .
//					OPTIONAL{
//						?resource_np1 :np2 ?resource_np1_np2 .
//						OPTIONAL{
//							?resource_np1_np2 :np3 ?resource_np1_np2_np3 .
//							...
//						}
//					}
//				}
//				OPTIONAL{
//					?resource	:np4	?resource_np4 .
//				}	
//				SELECT ?resource
//				{
//				...#path clauses
//				}
//			}
//		}
//		
//	Note
//		If no filter conditions on properties within path then path is optional, otherwise not
//		An inverse property swotches subject and object position:
//		
//		$expand=np1/ip2/np3/...
//	
//			CONSTRUCT{
//				...
//				...#path constructs
//				...#select constructs
//				?resource	:np1	?resource_np1 .
//				?resource_np1_ip2 :ip2 ?resource_np1 .
//				?resource_np1_ip2 :np3 ?resource_np1_ip2_np3 
//				...
//			} 
//			WHERE {
//				...#select clauses
//				SELECT ?resource ?resource_np1 ?resource_np1_ip2 ?resource_np1_ip2_np3 
//				{
//					...
//					...#path clauses
//					...
//					OPTIONAL{	#/np1/
//						?resource	:np1	?resource_np1 .
//						OPTIONAL{	#/np1/np2/
//							?resource_np1_ip2 :ip2 ?resource_np1 .
//							OPTIONAL{	#/np1/ip2/np3/
//								?resource_np1_ip2 :np3 ?resource_np1_ip2_np3 .
//								...
//							}
//						}
//					}
//					SELECT ?resource
//					{
//					...#path clauses
//					}		
//				}
//			}
//		
//	Select
//	------
//	Note
//		Selected values must already appear in path
//		
//	$select=dpa, np1/dpb, np1/np2/dpc, ...
//	
//		CONSTRUCT{
//			...
//			...#expand constructs
//			?resource	?resource_p   ?resource_o .
//			?resource_np1	?resource_np1_p ?resource_np1_o  .
//			?resource_np1_np2 ?resource_np1_np2_p ?resource_np1_np2_o .	
//			...
//		} 
//		WHERE {	#/
//			OPTIONAL {
//				?resource ?resource_p ?resource_o .
//				VALUES(?resource_p){(:dpa)}
//			}	
//			OPTIONAL { ?resource :np1 ?resource_np1 . 
//			|| based on if path has filter associated
//			{	#/np1/
//				OPTIONAL {
//					?resource_np1 ?resource_np1_p ?resource_np1_o .
//					VALUES(?resource_np1_p){(:dpb)}
//				}
//				OPTIONAL { ?resource_np1 :np2 ?resource_np1_np2 . 
//				|| based on if path has filter associated
//				{	#/np1/np2/
//					OPTIONAL {
//						?resource_np1_np2 ?resource_np1_np2_p ?resource_np1_np2_o .
//						VALUES(?resource_np1_np2_p){(:dpc)}
//					}
//					...
//				}
//			}
//			{
//				SELECT ?resource ?resource_np1 ?resource_np1_np2  
//				...#path clauses
//				...#expand clauses
//			}
//		}
//	
//	Filter
//	------
//	Note
//		Filtered values must already appear in path
//		
//	$filter=condition({npN/}*dpN)
//		
//		CONSTRUCT{
//			...
//			...#expand constructs
//			...#select constructs
//			...
//		} WHERE 
//		{
//			...
//			...#select clauses
//			...
//			{	SELECT ?resource ?resource_np1 ?resource_np1_ip2 ?resource_np1_ip2_np3 
//				WHERE {
//					...
//					...#path clauses
//					...
//					{	#filter=condition(dp)
//						?resource :dp ?resource_dp_o .
//						FILTER(condition(?resource_sp_o))			
//					}
//					{	#/np1/
//						?resource	:np1	?resource_np1 .
//						{	#filter=condition(np1/dp1)
//							?resource_np1 :dp1 ?resource_dp1_o .
//							FILTER(condition(?resource_dp1_o))					
//						}
//						{	#/np1/np2/
//							?resource_np1 :np2 ?resource_np1_np2  .
//							{	#filter=condition(np1/np2/dp2)
//								?resource_np1_np2 :dp2 ?resource_np1_np2_dp2_o.
//								FILTER(condition(?resource_np1_np2_dp2_o))					
//							}
//							{	#/np1/ip2/np3/
//								?resource_np1_ip2 :np3 ?resource_np1_ip2_np3 .
//								...
//							}
//						}
//					}
//					SELECT DISTINCT
//						?resource
//					WHERE {
//						...#path clauses
//						{	#filter=condition(dp)
//							?resource :dp ?resource_dp_o .
//							FILTER(condition(?resource_sp_o))			
//						}
//						{	#/np1/
//							?resource	:np1	?resource_np1 .
//							{	#filter=condition(np1/dp1)
//								?resource_np1 :dp1 ?resource_dp1_o .
//								FILTER(condition(?resource_dp1_o))					
//							}
//							{	#/np1/np2/
//								?resource_np1 :np2 ?resource_np1_np2  .
//								{	#filter=condition(np1/np2/dp2)
//									?resource_np1_np2 :dp2 ?resource_np1_np2_dp2_o.
//									FILTER(condition(?resource_np1_np2_dp2_o))					
//								}
//								{	#/np1/ip2/np3/
//									?resource_np1_ip2 :np3 ?resource_np1_ip2_np3 .
//									...
//								}
//							}
//						}				
//					}	GROUP BY ?resource LIMIT $top		
//				}
//			}
//		}

public class SparqlQueryBuilder {
	private final Log log = LogFactory.getLog(SparqlQueryBuilder.class);
	private final RdfModel rdfModel;
	private final RdfModelToMetadata rdfModelToMetadata;
	
	private final UriType uriType;
	private final UriInfo uriInfo;
	
	
	private RdfEntityType rdfEntityType = null;
	private RdfEntityType rdfTargetEntityType = null;
	private EdmEntitySet edmEntitySet = null;
	private EdmEntitySet edmTargetEntitySet = null;
	private ExpandSelectTreeNode expandSelectTreeNode;
	private HashMap<String, RdfAssociation> expandSelectNavPropertyMap;
	private SparqlExpressionVisitor filterClause;
	private HashMap<String, HashSet<String>> selectPropertyMap;

	private static final boolean DEBUG = true;

	public SparqlQueryBuilder( RdfModel rdfModel, RdfModelToMetadata rdfModelToMetadata, GetEntitySetUriInfo entitySetUriInfo) throws EdmException, ExceptionVisitExpression, ODataApplicationException {
		super();
		this.rdfModel = rdfModel;
		this.rdfModelToMetadata = rdfModelToMetadata;
		this.uriInfo = (UriInfo) entitySetUriInfo;
		if (entitySetUriInfo.getNavigationSegments().size() > 0) {
			this.uriType = UriType.URI6B;
		} else {
			this.uriType = UriType.URI1;
		}
		//Prepare what is required to create the SPARQL
		prepareBuilder();}

	public SparqlQueryBuilder( RdfModel rdfModel, RdfModelToMetadata rdfModelToMetadata, GetEntityUriInfo entityUriInfo) throws EdmException, ExceptionVisitExpression, ODataApplicationException {
		super();
		this.rdfModel = rdfModel;
		this.rdfModelToMetadata = rdfModelToMetadata;
		this.uriInfo = (UriInfo) entityUriInfo;
		if (entityUriInfo.getNavigationSegments().size() > 0) {
			this.uriType = UriType.URI6A;
		} else {
			this.uriType = UriType.URI2;
		}
		//Prepare what is required to create the SPARQL
		prepareBuilder();
	}

	public SparqlQueryBuilder( RdfModel rdfModel, RdfModelToMetadata rdfModelToMetadata,GetEntitySetCountUriInfo entitySetCountUriInfo) throws EdmException, ExceptionVisitExpression, ODataApplicationException {
		super();
		this.rdfModel = rdfModel;
		this.rdfModelToMetadata = rdfModelToMetadata;
		this.uriInfo = (UriInfo) entitySetCountUriInfo;
		this.uriType = UriType.URI15;
		//Prepare what is required to create the SPARQL
		prepareBuilder();
	}

	public SparqlQueryBuilder( RdfModel rdfModel, RdfModelToMetadata rdfModelToMetadata, GetEntityCountUriInfo entityCountUriInfo) throws EdmException, ExceptionVisitExpression, ODataApplicationException {
		super();
		this.rdfModel = rdfModel;
		this.rdfModelToMetadata = rdfModelToMetadata;
		this.uriInfo = (UriInfo) entityCountUriInfo;
		this.uriType = UriType.URI16;
		//Prepare what is required to create the SPARQL
		prepareBuilder();
	}

	private void prepareBuilder() throws EdmException, ExceptionVisitExpression, ODataApplicationException {
		//Prepare what is required to create the SPARQL
		switch (this.uriType) {
		case URI1: {
			edmEntitySet = this.uriInfo.getStartEntitySet();
			edmTargetEntitySet = uriInfo.getTargetEntitySet();
			rdfEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(edmEntitySet);
			rdfTargetEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(edmTargetEntitySet);
			expandSelectTreeNode = UriParser.createExpandSelectTree(uriInfo.getSelect(), uriInfo.getExpand());
			expandSelectNavPropertyMap = createExpandSelectNavPropertyMap(uriInfo.getSelect(), uriInfo.getExpand());
			filterClause = filterClause(uriInfo.getFilter(), rdfEntityType);
			selectPropertyMap = createSelectPropertyMap(uriInfo.getSelect());
		}
			break;
		case URI2: {
			edmEntitySet = this.uriInfo.getStartEntitySet();
			edmTargetEntitySet = uriInfo.getTargetEntitySet();
			rdfEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(edmEntitySet);
			rdfTargetEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(edmTargetEntitySet);
			expandSelectTreeNode = UriParser.createExpandSelectTree(uriInfo.getSelect(), uriInfo.getExpand());
			expandSelectNavPropertyMap = createExpandSelectNavPropertyMap(uriInfo.getSelect(), uriInfo.getExpand());
			filterClause = filterClause(uriInfo.getFilter(), rdfEntityType);
			selectPropertyMap = createSelectPropertyMap(uriInfo.getSelect());
		}
			break;
		case URI6A: {
			edmEntitySet = this.uriInfo.getStartEntitySet();
			edmTargetEntitySet = uriInfo.getTargetEntitySet();
			rdfEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(edmEntitySet);
			rdfTargetEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(edmTargetEntitySet);
			expandSelectTreeNode = UriParser.createExpandSelectTree(uriInfo.getSelect(), uriInfo.getExpand());
			expandSelectNavPropertyMap = createExpandSelectNavPropertyMap(uriInfo.getSelect(), uriInfo.getExpand());
			filterClause = filterClause(uriInfo.getFilter(), rdfEntityType);
			selectPropertyMap = createSelectPropertyMap(uriInfo.getSelect());
		}
			break;
		case URI6B: {
			edmEntitySet = this.uriInfo.getStartEntitySet();
			edmTargetEntitySet = uriInfo.getTargetEntitySet();
			rdfEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(edmEntitySet);
			rdfTargetEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(edmTargetEntitySet);
			expandSelectTreeNode = UriParser.createExpandSelectTree(uriInfo.getSelect(), uriInfo.getExpand());
			expandSelectNavPropertyMap = createExpandSelectNavPropertyMap(uriInfo.getSelect(), uriInfo.getExpand());
			filterClause = filterClause(uriInfo.getFilter(), rdfTargetEntityType);
			selectPropertyMap = createSelectPropertyMap(uriInfo.getSelect());
		}
			break;
		case URI7B: {
			//TO TEST
			edmEntitySet = this.uriInfo.getStartEntitySet();
			edmTargetEntitySet = uriInfo.getTargetEntitySet();
			rdfEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(edmEntitySet);
			rdfTargetEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(edmTargetEntitySet);
			expandSelectTreeNode = UriParser.createExpandSelectTree(uriInfo.getSelect(), uriInfo.getExpand());
			expandSelectNavPropertyMap = createExpandSelectNavPropertyMap(uriInfo.getSelect(), uriInfo.getExpand());
			filterClause = filterClause(uriInfo.getFilter(), rdfTargetEntityType);//rdfEntityType);
			selectPropertyMap = createSelectPropertyMap(uriInfo.getSelect());
		}
			break;
		case URI15: {
			edmEntitySet = this.uriInfo.getStartEntitySet();
			edmTargetEntitySet = uriInfo.getTargetEntitySet();
			rdfEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(edmEntitySet);
			rdfTargetEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(edmTargetEntitySet);
			expandSelectTreeNode = UriParser.createExpandSelectTree(uriInfo.getSelect(), uriInfo.getExpand());
			expandSelectNavPropertyMap = createExpandSelectNavPropertyMap(uriInfo.getSelect(), uriInfo.getExpand());
			filterClause = filterClause(uriInfo.getFilter(), rdfTargetEntityType);
			selectPropertyMap = createSelectPropertyMap(uriInfo.getSelect());
		}
			break;
		case URI16: {
			//To be tested
			edmEntitySet = this.uriInfo.getStartEntitySet();
			edmTargetEntitySet = uriInfo.getTargetEntitySet();
			rdfEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(edmEntitySet);
			rdfTargetEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(edmTargetEntitySet);
			expandSelectTreeNode = UriParser.createExpandSelectTree(uriInfo.getSelect(), uriInfo.getExpand());
			expandSelectNavPropertyMap = createExpandSelectNavPropertyMap(uriInfo.getSelect(), uriInfo.getExpand());
			filterClause = filterClause(uriInfo.getFilter(), rdfTargetEntityType);
			selectPropertyMap = createSelectPropertyMap(uriInfo.getSelect());
		}
			break;
		default:
		}
	}

	public SparqlStatement prepareConstructSparql() throws EdmException, ExceptionVisitExpression,
			ODataApplicationException, OData2SparqlException {


		StringBuilder prepareConstruct = new StringBuilder("");
		prepareConstruct.append(construct());
		prepareConstruct.append("WHERE {\n");
		prepareConstruct.append(where());
		prepareConstruct.append("}");
		prepareConstruct.append(defaultLimitClause());
		return new SparqlStatement(prepareConstruct.toString());
	}

	public SparqlStatement prepareCountEntitySetSparql() throws ExceptionVisitExpression, ODataApplicationException,
			EdmException {

		StringBuilder prepareCountEntitySet = new StringBuilder("");
		prepareCountEntitySet.append("\t").append("SELECT ");
		prepareCountEntitySet.append("(COUNT(DISTINCT ?" + edmTargetEntitySet.getEntityType().getName() + "_s")
				.append(expandSelectTreeNodeVariables(rdfTargetEntityType.entityTypeName, this.expandSelectTreeNode))
				.append(") AS ?COUNT)").append("\n");
		prepareCountEntitySet.append(selectExpandWhere(""));
		return new SparqlStatement(prepareCountEntitySet.toString());
	}

	public SparqlStatement prepareExistsEntitySetSparql() throws ExceptionVisitExpression, ODataApplicationException,
			EdmException {

		StringBuilder prepareCountEntitySet = new StringBuilder("");
		prepareCountEntitySet.append("\t").append("SELECT ");
		prepareCountEntitySet.append("(BOUND(?" + edmTargetEntitySet.getEntityType().getName() + "_s")
				.append(expandSelectTreeNodeVariables(rdfTargetEntityType.entityTypeName, this.expandSelectTreeNode))
				.append(") AS ?EXISTS)").append("\n");
		prepareCountEntitySet.append(selectExpandWhere("")).append("LIMIT 1");
		return new SparqlStatement(prepareCountEntitySet.toString());
	}

	private SparqlExpressionVisitor filterClause(FilterExpression filter, RdfEntityType entityType)
			throws ExceptionVisitExpression, ODataApplicationException {
		SparqlExpressionVisitor sparqlExpressionVisitor = new SparqlExpressionVisitor( rdfModel, rdfModelToMetadata, entityType);
		if (filter != null) {
			@SuppressWarnings("unused")
			String filterClause = (String) filter.accept(sparqlExpressionVisitor);
		}
		return sparqlExpressionVisitor;
	}

	private StringBuilder construct() throws EdmException {
		StringBuilder construct = new StringBuilder("CONSTRUCT {\n");
		construct.append(targetEntityIdentifier( edmTargetEntitySet.getEntityType().getName(), "\t"));
		if (this.rdfTargetEntityType.isOperation()) {
			construct.append(constructOperation(rdfTargetEntityType, ""));
		} else {
			construct.append(constructType(rdfTargetEntityType, edmTargetEntitySet.getEntityType().getName(), "\t"));
			construct.append(constructPath());
		}
		construct.append(constructExpandSelect());
		construct.append("}\n");
		return construct;
	}
	private StringBuilder targetEntityIdentifier(String key, String indent) throws EdmException {
		StringBuilder targetEntityIdentifier = new StringBuilder();
		if (DEBUG)
			targetEntityIdentifier.append(indent).append("#targetEntityIdentifier\n");
		String type = rdfEntityType.getIRI();
		targetEntityIdentifier.append(indent).append("?" + key + "_s <" + RdfConstants.TARGETENTITY +"> true .\n");
		return targetEntityIdentifier;
	}
	
	private StringBuilder constructType(RdfEntityType rdfEntityType, String key, String indent) throws EdmException {
		StringBuilder constructType = new StringBuilder();
		if (DEBUG)
			constructType.append(indent).append("#constructType\n");
		String type = rdfEntityType.getIRI();
		constructType.append(indent).append("?" + key + "_s a <" + type + "> .\n");
		return constructType;
	}

	private StringBuilder constructOperation(RdfEntityType rdfOperationType, String indent) throws EdmException {
		StringBuilder constructOperation = new StringBuilder();
		if (DEBUG)
			constructOperation.append(indent).append("#constructOperation\n");
		String type = rdfOperationType.getIRI();
		constructOperation.append(indent + "\t").append("[ a <" + type + "> ;\n");
		for (RdfProperty property : rdfOperationType.getProperties()) {
			constructOperation.append(indent + "\t\t").append(
					" <" + property.getPropertyURI() + "> ?" + property.varName + " ;\n");
		}
		constructOperation.replace(constructOperation.length() - 2, constructOperation.length() - 1, "] .");
		return constructOperation;
	}

	private StringBuilder constructPath() throws EdmException {
		StringBuilder constructPath = new StringBuilder();
		if (DEBUG)
			constructPath.append("\t#constructPath\n");
		String key = edmTargetEntitySet.getEntityType().getName();
		constructPath.append("\t").append("?" + key + "_s ?" + key + "_p ?" + key + "_o .\n");
		return constructPath;
	}

	private StringBuilder constructExpandSelect() throws EdmException {
		StringBuilder constructExpandSelect = new StringBuilder();
		if (DEBUG)
			constructExpandSelect.append("\t#constructExpandSelect\n");
		constructExpandSelect.append(expandSelectTreeNodeConstruct(rdfTargetEntityType.entityTypeName,
				this.expandSelectTreeNode, "\t"));
		return constructExpandSelect;
	}

	private StringBuilder where() throws EdmException, OData2SparqlException {
		StringBuilder where = new StringBuilder();
		if (this.rdfTargetEntityType.isOperation()) {
			where.append(clausesOperationProperties(this.rdfTargetEntityType));
		} else {
			where.append(clausesPathProperties());
		}
		where.append(clausesExpandSelect());
		if (this.rdfEntityType.isOperation()) {
			where.append(selectOperation());
		} else if (this.rdfTargetEntityType.isOperation()) {
			where.append(selectOperation());
		} else {
			where.append(selectExpand());
		}
		return where;
	}

	private StringBuilder clausesPathProperties() throws EdmException {
		StringBuilder clausesPathProperties = new StringBuilder();
		if (DEBUG)
			clausesPathProperties.append("\t#clausesPathProperties\n");
		clausesPathProperties.append(clausesSelect(edmTargetEntitySet.getEntityType().getName(), edmTargetEntitySet
				.getEntityType().getName(), "\t"));
		return clausesPathProperties;
	}

	private StringBuilder clausesOperationProperties(RdfEntityType rdfOperationType) throws EdmException,
			OData2SparqlException {
		StringBuilder clausesOperationProperties = new StringBuilder();
		if (DEBUG)
			clausesOperationProperties.append("\t#clausesOperationProperties\n");
		clausesOperationProperties.append("\t{\n").append(preprocessOperationQuery(rdfOperationType)).append("\t}\n");
		return clausesOperationProperties;
	}

	private String preprocessOperationQuery(RdfEntityType rdfOperationType) throws EdmException, OData2SparqlException {
		Map<String, String> queryOptions = uriInfo.getCustomQueryOptions();
		String queryText = rdfOperationType.queryText;
		for (Entry<String, com.inova8.odata2sparql.RdfModel.RdfModel.FunctionImportParameter> functionImportParameterEntry : rdfOperationType.getFunctionImportParameters().entrySet()) {
			com.inova8.odata2sparql.RdfModel.RdfModel.FunctionImportParameter functionImportParameter = functionImportParameterEntry.getValue();
			if (queryOptions.containsKey(functionImportParameter.getName())) {
				String parameterValue = queryOptions.get(functionImportParameter.getName());
				//				switch (functionImportParameter.getValue().getType()) {
				//				correctly format for non-strings such as dates
				//				case Binary:
				//				case Boolean:
				//				case Byte:
				//				case DateTime:
				//				case DateTimeOffset:
				//				case Decimal:
				//				case Double:
				//				case Guid:
				//				case Int16:
				//				case Int32:
				//				case Int64:
				//				case SByte:
				//				case Single:
				//				case String:
				//				case Time:
				//				case Null:
				//				default:
				//					break;
				//				}
				queryText = queryText.replaceAll("\\?" + functionImportParameter.getName(), parameterValue);
			} else {
				if (!functionImportParameter.isNullable())
					throw new OData2SparqlException(
							"FunctionImport cannot be called without values for non-nullable parameters");
			}
		}
		return queryText;
	}

	private StringBuilder clausesExpandSelect() throws EdmException, OData2SparqlException {
		StringBuilder clausesExpandSelect = new StringBuilder();
		if (DEBUG)
			clausesExpandSelect.append("\t#clausesExpandSelect\n");
		if (!this.expandSelectNavPropertyMap.isEmpty()) {
			clausesExpandSelect.append(expandSelectTreeNodeWhere(rdfTargetEntityType.entityTypeName,
					this.expandSelectTreeNode, "\t"));
		}
		return clausesExpandSelect;
	}

	private StringBuilder selectOperation() throws EdmException {
		StringBuilder selectOperation = new StringBuilder();
		if (DEBUG)
			selectOperation.append("\t#selectOperation\n");
		selectOperation.append(clausesPath_KeyPredicateValues("\t"));
		return selectOperation;
	}

	private StringBuilder selectExpand() throws EdmException {
		StringBuilder selectExpand = new StringBuilder();
		if (DEBUG)
			selectExpand.append("\t#selectExpand\n");
		selectExpand.append("\t").append("{\tSELECT\n");
		selectExpand.append("\t\t\t").append("?" + edmTargetEntitySet.getEntityType().getName() + "_s")
				.append(expandSelectTreeNodeVariables(rdfTargetEntityType.entityTypeName, this.expandSelectTreeNode))
				.append("\n");
		selectExpand.append(selectExpandWhere("\t\t"));
		selectExpand.append("\t").append("}\n");
		return selectExpand;
	}

	private StringBuilder selectExpandWhere(String indent) throws EdmException {
		StringBuilder selectExpandWhere = new StringBuilder();

		selectExpandWhere.append(indent).append("WHERE {\n");
		selectExpandWhere.append(filter(indent + "\t"));
		switch (uriType) {
		case URI1:
			//nothing required for any entitySet query
			break;
		case URI2:
		case URI6A:
		case URI6B:
		case URI15:
		case URI16:
			selectExpandWhere.append(clausesPath(indent + "\t"));
			break;
		default:
			selectExpandWhere.append("#Unhandled URIType: " + this.uriType + "\n");
		}
		selectExpandWhere.append(clausesFilter(indent + "\t"));
		selectExpandWhere.append(clausesExpandFilter(indent + "\t"));
		switch (uriType) {
		case URI1:
			selectExpandWhere.append(selectPath());
			break;
		case URI2:
		case URI6A:
		case URI6B:
		case URI15:
		case URI16:
			//nothing required for any entity query
			break;
		default:
			selectExpandWhere.append("#Unhandled URIType: " + this.uriType + "\n");
		}
		selectExpandWhere.append(indent).append("}\n");

		return selectExpandWhere;
	}

	private StringBuilder filter(String indent) {
		StringBuilder filter = new StringBuilder().append(indent);
		if (DEBUG)
			filter.append("#filter\n");
		if (!filterClause.getFilterClause().isEmpty())
			filter.append(indent).append(filterClause.getFilterClause()).append("\n");
		return filter;
	}

	private StringBuilder exists(String indent) {
		StringBuilder exists = new StringBuilder();
		if (DEBUG)
			exists.append(indent).append("#exists\n");
		exists.append(indent);
		exists.append("{{?" + rdfEntityType.entityTypeName + "_s ?p1 ?o1 } UNION {?s1 ?" + rdfEntityType.entityTypeName
				+ "_s ?o1} UNION {?s3 ?p3 ?" + rdfEntityType.entityTypeName + "_s} }\n");
		return exists;
	}

	private StringBuilder clausesPath(String indent) throws EdmException {
		StringBuilder clausesPath = new StringBuilder().append(indent);
		if (DEBUG)
			clausesPath.append("#clausesPath\n");
		switch (this.uriType) {
		case URI1: {
			clausesPath.append(clausesPath_URI1(indent));
			break;
		}
		case URI2: {
			clausesPath.append(clausesPath_URI2(indent));
			break;
		}
		case URI6A: {
			clausesPath.append(clausesPath_URI2(indent));
			break;
		}
		case URI6B: {
			clausesPath.append(clausesPath_URI1(indent));
			break;
		}
		case URI15: {
			clausesPath.append(clausesPath_URI1(indent));
			break;
		}
		case URI16: {
			clausesPath.append(clausesPath_URI2(indent));
			clausesPath.append(exists(indent));
			break;
		}
		default:
			clausesPath.append("#Unhandled URIType: " + this.uriType + "\n");
			break;
		}
		return clausesPath;
	}

	private StringBuilder clausesPath_URI1(String indent) throws EdmException {
		StringBuilder clausesPath = new StringBuilder();
		if (uriInfo.getNavigationSegments().size() > 0) {
			clausesPath.append(clausesPathNavigation(indent, uriInfo.getNavigationSegments(),
					uriInfo.getKeyPredicates()));
		} else {
			clausesPath.append(indent).append(
					"?" + rdfEntityType.entityTypeName
							+ "_s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?class .\n");
			clausesPath.append(indent).append(
					"?class (<http://www.w3.org/2000/01/rdf-schema#subClassOf>)* <" + rdfEntityType.getIRI() + "> .\n");

			// Workaround for Virtuoso that sometimes misinterprets subClassOf*
			//			clausesPath.append(indent).append(
			//					"?" + rdfEntityType.entityTypeName + "_s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <"
			//							+ rdfEntityType.getURI() + "> .\n");
		}
		return clausesPath;
	}

	private StringBuilder clausesPath_URI2(String indent) throws EdmException {
		StringBuilder clausesPath = new StringBuilder();
		if (uriInfo.getNavigationSegments().size() > 0) {
			clausesPath.append(clausesPathNavigation(indent, uriInfo.getNavigationSegments(),
					uriInfo.getKeyPredicates()));
		} else {
			clausesPath.append(clausesPath_KeyPredicateValues(indent));
		}
		return clausesPath;
	}

	private StringBuilder clausesPath_KeyPredicateValues(String indent) throws EdmException {
		StringBuilder clausesPath_KeyPredicateValues = new StringBuilder();
		String key = "";
		if (rdfEntityType.isOperation()) {
			if (uriInfo.getNavigationSegments().isEmpty()) {
				for (RdfPrimaryKey primaryKey : rdfEntityType.getPrimaryKeys()) {
					key = key + "?" + primaryKey.getPrimaryKeyName() + " ";
				}
			} else {
				if (uriInfo.getNavigationSegments().size() > 1) {
					log.error("Too many navigation properties for operation:"
							+ uriInfo.getNavigationSegments().toString());
				} else {
					RdfAssociation navProperty = rdfEntityType.findNavigationProperty(uriInfo.getNavigationSegments()
							.get(0).getNavigationProperty().getName());
					String keyPredicate = navProperty.getVarName();
					key = rdfTargetEntityType.entityTypeName;
					clausesPath_KeyPredicateValues.append(indent).append("VALUES(?" + key + "_s)");
					if (uriInfo.getKeyPredicates() != null && !uriInfo.getKeyPredicates().isEmpty()) {
						for (KeyPredicate entityKey : uriInfo.getKeyPredicates()) {
							if (entityKey.getProperty().getName().equals(keyPredicate)) {
								String decodedEntityKey = RdfEntity.URLDecodeEntityKey(entityKey.getLiteral());
								String expandedKey = rdfModel.getRdfPrefixes()
										.expandPrefix(decodedEntityKey);
								clausesPath_KeyPredicateValues.append("{(<" + expandedKey + ">)}");
							}
						}
					}
					return clausesPath_KeyPredicateValues;
				}
			}
		} else if (rdfTargetEntityType.isOperation()) {
			if (uriInfo.getNavigationSegments().size() > 1) {
				log.error("Too many navigation properties for operation:" + uriInfo.getNavigationSegments().toString());
			} else {
				RdfAssociation navProperty = rdfEntityType.findNavigationProperty(uriInfo.getNavigationSegments()
						.get(0).getNavigationProperty().getName());
				if (navProperty != null) {
					key = "?"
							+ rdfTargetEntityType.findNavigationProperty(
									navProperty.getInversePropertyOf().getLocalName()).getVarName();
				} else {
					log.error("Failed to locate operation navigation property:"
							+ uriInfo.getNavigationSegments().get(0).getNavigationProperty().getName());
				}
			}
		} else {
			key = "?" + rdfEntityType.entityTypeName + "_s";
		}

		if (uriInfo.getKeyPredicates() != null && !uriInfo.getKeyPredicates().isEmpty()) {
			clausesPath_KeyPredicateValues.append(indent).append("VALUES(" + key + ") {(");
			for (KeyPredicate entityKey : uriInfo.getKeyPredicates()) {
				String decodedEntityKey = RdfEntity.URLDecodeEntityKey(entityKey.getLiteral());
				String expandedKey = rdfModel.getRdfPrefixes().expandPrefix(decodedEntityKey);
				clausesPath_KeyPredicateValues.append("<" + expandedKey + ">");
			}
			clausesPath_KeyPredicateValues.append(")}\n");
		}
		return clausesPath_KeyPredicateValues;
	}

	private StringBuilder clausesPathNavigation(String indent, List<NavigationSegment> navigationSegments,
			List<KeyPredicate> entityKeys) throws EdmException {
		StringBuilder clausesPathNavigation = new StringBuilder();

		String path = edmTargetEntitySet.getEntityType().getName();
		boolean isFirstSegment = true;
		Integer lastIndex = navigationSegments.size();
		Integer index = 0;
		String pathVariable = "";
		String targetVariable = "";
		for (NavigationSegment navigationSegment : navigationSegments) {
			index++;
			EdmNavigationProperty predicate = navigationSegment.getNavigationProperty();
//			RdfAssociation navProperty = rdfModelToMetadata.getMappedNavigationProperty(new FullQualifiedName(
//					navigationSegment.getNavigationProperty().getRelationship().getNamespace(), navigationSegment
//							.getNavigationProperty().getRelationship().getName()));
			RdfAssociation navProperty = rdfModelToMetadata.getMappedNavigationProperty(
					navigationSegment.getNavigationProperty().getRelationship());
			if (isFirstSegment) {
				// Not possible to have more than one key field is it?
				for (KeyPredicate entityKey : entityKeys) {
					String decodedEntityKey = RdfEntity.URLDecodeEntityKey(entityKey.getLiteral());
					String expandedKey = rdfModel.getRdfPrefixes()
							.expandPrefix(decodedEntityKey);
					pathVariable = "<" + expandedKey + ">";
				}
			} else {
				pathVariable = "?" + path + "_s";
			}
			if (index.equals(lastIndex)) {
				targetVariable = "?" + edmTargetEntitySet.getEntityType().getName() + "_s";
			} else {
				targetVariable = "?" + path + navProperty.getAssociationName() + "_s";
			}
			if (navProperty.IsInverse()) {
				clausesPathNavigation.append(indent).append(
						targetVariable + " <" + navProperty.getInversePropertyOf().getIRI() + "> " + pathVariable
								+ " .\n");
			} else {
				clausesPathNavigation.append(indent).append(
						pathVariable + " <" + navProperty.getAssociationIRI() + "> " + targetVariable + " .\n");
			}
			path += predicate.getName();
			isFirstSegment = false;

		}
		return clausesPathNavigation;
	}

	private StringBuilder clausesExpandFilter(String indent) {
		StringBuilder clausesExpandFilter = new StringBuilder().append(indent);
		if (DEBUG)
			clausesExpandFilter.append("#clausesExpandFilter\n");
		clausesExpandFilter.append(expandSelectTreeNodeFilter(rdfEntityType.entityTypeName, this.expandSelectTreeNode,
				indent));
		return clausesExpandFilter;
	}

	private StringBuilder selectPath() throws EdmException {
		StringBuilder selectPath = new StringBuilder();

		if (DEBUG)
			selectPath.append("\t\t\t#selectPath\n");
		selectPath.append("\t\t\t").append("{\tSELECT DISTINCT\n");
		selectPath.append("\t\t\t\t\t").append("?" + edmTargetEntitySet.getEntityType().getName() + "_s\n");
		selectPath.append("\t\t\t\t").append("WHERE {\n");
		selectPath.append(filter("\t\t\t\t\t"));
		selectPath.append(clausesPath("\t\t\t\t\t"));
		selectPath.append(clausesFilter("\t\t\t\t\t"));
		selectPath.append(clausesExpandFilter("\t\t\t\t\t"));
		selectPath.append("\t\t\t\t").append("} GROUP BY ?" + edmTargetEntitySet.getEntityType().getName() + "_s")
				.append(limitClause()).append("\n");
		selectPath.append("\t\t\t").append("}\n");
		return selectPath;
	}

	private StringBuilder clausesFilter(String indent) {
		StringBuilder clausesFilter = new StringBuilder().append(indent);
		if (DEBUG)
			clausesFilter.append("#clausesFilter\n");
		if (this.filterClause.getNavPropertyPropertyFilters() != null
				&& this.filterClause.getNavPropertyPropertyFilters().get(rdfTargetEntityType.entityTypeName) != null) {
			clausesFilter.append(clausesFilter(null, rdfTargetEntityType.entityTypeName, indent, this.filterClause
					.getNavPropertyPropertyFilters().get(rdfTargetEntityType.entityTypeName).getPropertyFilters()));
		} else {
			//clausesFilter.append(clausesFilter(null, rdfEntityType.entityTypeName, indent,null));
		}
		return clausesFilter;
	}

	private HashMap<String, RdfAssociation> createExpandSelectNavPropertyMap(List<SelectItem> select,
			List<ArrayList<NavigationPropertySegment>> expand) throws EdmException {
		HashMap<String, RdfAssociation> expandSelectNavPropertyMap = new HashMap<String, RdfAssociation>();
		for (SelectItem selectItem : select) {
			for (NavigationPropertySegment navigationPropertySegment : selectItem.getNavigationPropertySegments()) {
				expandSelectNavPropertyMap.put(
						navigationPropertySegment.getNavigationProperty().getName(),
//						rdfModelToMetadata.getMappedNavigationProperty(new FullQualifiedName(navigationPropertySegment
//								.getNavigationProperty().getRelationship().getNamespace(), navigationPropertySegment
//								.getNavigationProperty().getRelationship().getName())));
				rdfModelToMetadata.getMappedNavigationProperty(navigationPropertySegment.getNavigationProperty().getRelationship()));
				}
		}
		for (ArrayList<NavigationPropertySegment> navigationPropertySegments : expand) {
			for (NavigationPropertySegment navigationPropertySegment : navigationPropertySegments) {
				expandSelectNavPropertyMap.put(
						navigationPropertySegment.getNavigationProperty().getName(),
//						rdfModelToMetadata.getMappedNavigationProperty(new FullQualifiedName(navigationPropertySegment
//								.getNavigationProperty().getRelationship().getNamespace(), navigationPropertySegment
//								.getNavigationProperty().getRelationship().getName())));
				rdfModelToMetadata.getMappedNavigationProperty(navigationPropertySegment.getNavigationProperty().getRelationship()));
			}
		}
		return expandSelectNavPropertyMap;
	}

	private StringBuilder expandSelectTreeNodeConstruct(String targetKey, ExpandSelectTreeNode expandTreeNode,
			String indent) throws EdmException {
		StringBuilder expandSelectTreeNodeConstruct = new StringBuilder();
		String nextTargetKey = "";
		for (Entry<String, ExpandSelectTreeNode> expandTreeNodeLinksEntry : expandTreeNode.getLinks().entrySet()) {
			nextTargetKey = targetKey + expandTreeNodeLinksEntry.getKey();
			RdfAssociation navProperty = this.expandSelectNavPropertyMap.get(expandTreeNodeLinksEntry.getKey());
			if (navProperty.getRangeClass().isOperation()) {
				expandSelectTreeNodeConstruct.append(indent + "\t").append(
						"?" + targetKey + "_s <" + navProperty.getAssociationIRI() + ">\n");
				expandSelectTreeNodeConstruct.append(indent).append(
						constructOperation(navProperty.getRangeClass(), indent));
			} else if (navProperty.getDomainClass().isOperation()) {

			} else {
				expandSelectTreeNodeConstruct.append(indent + "\t").append(
						"?" + targetKey + "_s <" + navProperty.getAssociationIRI() + "> ?" + nextTargetKey + "_s .\n");
			}
			if ((expandTreeNodeLinksEntry.getValue() != null)
					&& !expandTreeNodeLinksEntry.getValue().getLinks().isEmpty()) {
				expandSelectTreeNodeConstruct.append(expandSelectTreeNodeConstruct(nextTargetKey,
						expandTreeNodeLinksEntry.getValue(), indent + "\t"));
			}
			if (navProperty.getRangeClass().isOperation()) {
			} else {
				expandSelectTreeNodeConstruct.append(constructType(navProperty.getRangeClass(), nextTargetKey, indent
						+ "\t"));
				expandSelectTreeNodeConstruct.append(indent + "\t").append(
						"?" + nextTargetKey + "_s ?" + nextTargetKey + "_p ?" + nextTargetKey + "_o .\n");
			}
		}
		return expandSelectTreeNodeConstruct;
	}

	private StringBuilder expandSelectTreeNodeWhere(String targetKey, ExpandSelectTreeNode expandSelectTreeNode,
			String indent) throws EdmException, OData2SparqlException {
		StringBuilder expandSelectTreeNodeWhere = new StringBuilder();
		String nextTargetKey = "";
		for (Entry<String, ExpandSelectTreeNode> expandSelectTreeNodeLinksEntry : expandSelectTreeNode.getLinks()
				.entrySet()) {
			nextTargetKey = targetKey + expandSelectTreeNodeLinksEntry.getKey();

			RdfAssociation navProperty = expandSelectNavPropertyMap.get(expandSelectTreeNodeLinksEntry.getKey());
			if (navProperty.getDomainClass().isOperation()) {
				for (RdfProperty property : navProperty.getDomainClass().getProperties()) {
					if (property.getPropertyTypeName().equals(navProperty.getRangeClass().getIRI()))
						expandSelectTreeNodeWhere.append(indent).append(
								"BIND(?" + property.varName + " AS ?" + nextTargetKey + "_s)\n");
				}
			}
			expandSelectTreeNodeWhere.append(indent);
			//Not optional if filter imposed on path
			if (!this.filterClause.getNavPropertyPropertyFilters().containsKey(nextTargetKey))
				expandSelectTreeNodeWhere.append("OPTIONAL");
			expandSelectTreeNodeWhere.append("{\n");
			if (navProperty.getRangeClass().isOperation()) {
				expandSelectTreeNodeWhere.append(clausesOperationProperties(navProperty.getRangeClass()));
				//				BIND(?order as ?Order_s)
				//				BIND(?prod	as ?Orderorder_orderSummaryorderSummary_product_s )							
				for (RdfProperty property : navProperty.getRangeClass().getProperties()) {
					if (property.getPropertyTypeName().equals(navProperty.getDomainClass().getIRI()))
						expandSelectTreeNodeWhere.append("BIND(?" + property.varName + " AS ?" + targetKey + "_s)\n");
				}
			} else {
				if (navProperty.getDomainClass().isOperation()) {
					//Nothing to add as BIND assumed to be created
				} else if (navProperty.IsInverse()) {
					expandSelectTreeNodeWhere
							.append(indent)
							.append("\t")
							.append("?" + nextTargetKey + "_s <" + navProperty.getInversePropertyOf().getIRI() + "> ?"
									+ targetKey + "_s .\n");
				} else {
					expandSelectTreeNodeWhere
							.append(indent)
							.append("\t")
							.append("?" + targetKey + "_s <" + navProperty.getAssociationIRI() + "> ?" + nextTargetKey
									+ "_s .\n");
				}
				expandSelectTreeNodeWhere.append(clausesSelect(nextTargetKey, nextTargetKey, indent + "\t"));
			}

			if (expandSelectTreeNodeLinksEntry.getValue() != null) {
				expandSelectTreeNodeWhere.append(expandSelectTreeNodeWhere(nextTargetKey,
						expandSelectTreeNodeLinksEntry.getValue(), indent + "\t"));
			}
			expandSelectTreeNodeWhere.append(indent).append("}\n");
		}
		return expandSelectTreeNodeWhere;
	}

	private StringBuilder clausesSelect(String nextTargetKey, String navPath, String indent) {
		StringBuilder clausesSelect = new StringBuilder();
		clausesSelect.append(indent);
		if (navPath.equals(nextTargetKey) || this.filterClause.getNavPropertyPropertyFilters().containsKey(navPath)) {
		} else {
			clausesSelect.append("OPTIONAL");
		}
		clausesSelect.append("{\n");
		clausesSelect.append(indent).append("\t")
				.append("?" + nextTargetKey + "_s ?" + nextTargetKey + "_p ?" + nextTargetKey + "_o .\n");
		//      If specific properties from filters, but we should assume that if not specified otherwise *ALL* properties should be returned, not just those in the filter
		//		if ((navPropertyPropertyFilter != null && !navPropertyPropertyFilter.getPropertyFilters().isEmpty())
		//				|| 
		if ((this.selectPropertyMap != null && this.selectPropertyMap.containsKey(navPath) && !this.selectPropertyMap
				.get(navPath).isEmpty())) {
			clausesSelect.append(indent).append("\t").append("VALUES(?" + nextTargetKey + "_p){");
			//	      If specific properties from filters, but we should assume that if not specified otherwise *ALL* properties should be returned, not just those in the filter
			//			if (navPropertyPropertyFilter != null && !navPropertyPropertyFilter.getPropertyFilters().isEmpty()) {
			//				for (Entry<String, PropertyFilter> propertyFilterEntry : navPropertyPropertyFilter.getPropertyFilters()
			//						.entrySet()) {
			//					PropertyFilter propertyFilter = propertyFilterEntry.getValue();
			//					clausesSelect.append("(<" + propertyFilter.getProperty().getPropertyURI() + ">)");
			//				}
			//			}
			if (this.selectPropertyMap != null && this.selectPropertyMap.containsKey(navPath)) {
				for (String selectProperty : this.selectPropertyMap.get(navPath)) {
					clausesSelect.append("(<" + selectProperty + ">)");
				}
			}
			clausesSelect.append("}\n");
		} else {
			// Assume select=*, and fetch all non object property values
			clausesSelect.append(indent).append("\t")
					.append("FILTER(!isIRI(?" + nextTargetKey + "_o) && !isBLANK(?" + nextTargetKey + "_o))\n");
		}
		clausesSelect.append(indent).append("}\n");
		return clausesSelect;
	}

	private StringBuilder expandSelectTreeNodeFilter(String targetKey, ExpandSelectTreeNode expandSelectTreeNode,
			String indent) {
		StringBuilder expandSelectTreeNodeFilter = new StringBuilder();
		String nextTargetKey = "";
		for (Entry<String, ExpandSelectTreeNode> expandSelectTreeNodeLinksEntry : expandSelectTreeNode.getLinks()
				.entrySet()) {
			nextTargetKey = targetKey + expandSelectTreeNodeLinksEntry.getKey();
			//Not included if no filter in path	
			if (this.filterClause.getNavPropertyPropertyFilters().containsKey(nextTargetKey)) {
				expandSelectTreeNodeFilter.append(indent).append("{\n");
				NavPropertyPropertyFilter navPropertyPropertyFilter = this.filterClause.getNavPropertyPropertyFilters()
						.get(nextTargetKey);
				RdfAssociation navProperty = expandSelectNavPropertyMap.get(expandSelectTreeNodeLinksEntry.getKey());
				if (navProperty.IsInverse()) {
					expandSelectTreeNodeFilter
							.append(indent)
							.append("\t")
							.append("?" + nextTargetKey + "_s <" + navProperty.getInversePropertyOfURI() + "> ?"
									+ targetKey + "_s .\n");
				} else {
					expandSelectTreeNodeFilter
							.append(indent)
							.append("\t")
							.append("?" + targetKey + "_s <" + navProperty.getAssociationIRI() + "> ?" + nextTargetKey
									+ "_s .\n");
				}
				if (navPropertyPropertyFilter != null && !navPropertyPropertyFilter.getPropertyFilters().isEmpty()) {
					expandSelectTreeNodeFilter.append(clausesFilter(expandSelectTreeNodeLinksEntry, nextTargetKey,
							indent + "\t", navPropertyPropertyFilter.getPropertyFilters()));
				}
				if (expandSelectTreeNodeLinksEntry.getValue() != null) {
					expandSelectTreeNodeFilter.append(expandSelectTreeNodeFilter(nextTargetKey,
							expandSelectTreeNodeLinksEntry.getValue(), indent + "\t"));
				}
				expandSelectTreeNodeFilter.append(indent).append("}\n");
			}
		}
		return expandSelectTreeNodeFilter;
	}

	private StringBuilder expandSelectTreeNodeVariables(String targetKey, ExpandSelectTreeNode expandSelectTreeNode) {
		StringBuilder expandSelectTreeNodeVariables = new StringBuilder();
		String nextTargetKey = "";
		for (Entry<String, ExpandSelectTreeNode> expandSelectTreeNodeLinksEntry : expandSelectTreeNode.getLinks()
				.entrySet()) {
			nextTargetKey = targetKey + expandSelectTreeNodeLinksEntry.getKey();
			//Only include in this list if a navProperty indirectly involved in a filter expression.
			if (this.filterClause.getNavPropertyPropertyFilters().containsKey(nextTargetKey)) {
				expandSelectTreeNodeVariables.append(" ?" + nextTargetKey + "_s");
				if (expandSelectTreeNodeLinksEntry.getValue() != null) {
					expandSelectTreeNodeVariables.append(expandSelectTreeNodeVariables(nextTargetKey,
							expandSelectTreeNodeLinksEntry.getValue()));
				}
			}
		}
		return expandSelectTreeNodeVariables;
	}

	private StringBuilder clausesFilter(Entry<String, ExpandSelectTreeNode> expandSelectTreeNodeLinksEntry,
			String nextTargetKey, String indent, HashMap<String, PropertyFilter> propertyFilters) {
		StringBuilder clausesFilter = new StringBuilder();
		clausesFilter.append(indent).append("{\n");
		//Repeat for each filtered property associated with this navProperty
		for (Entry<String, PropertyFilter> propertyFilterEntry : propertyFilters.entrySet()) {
			PropertyFilter propertyFilter = propertyFilterEntry.getValue();
			clausesFilter
					.append(indent)
					.append("\t")
					.append("?" + nextTargetKey + "_s <" + propertyFilter.getProperty().getPropertyURI() + "> ?"
							+ nextTargetKey + propertyFilter.getProperty().getEDMPropertyName() + "_value .\n");
			for (String filter : propertyFilter.getFilters()) {
				clausesFilter.append(indent).append("\t").append("FILTER((?" + filter + "_value))\n");
			}
		}
		clausesFilter.append(indent).append("}\n");
		return clausesFilter;
	}

	private StringBuilder limitClause() {
		StringBuilder limitClause = new StringBuilder();
		if (this.uriType.equals(UriType.URI1)) {
			Integer top;
			Integer skip;
			int defaultLimit = rdfModel.getRdfRepository().getModelRepository().getDefaultQueryLimit();
			if (uriInfo.getTop() == null) {
				top = defaultLimit;
				skip = 0;
			} else {
				top = uriInfo.getTop();

				if ((top > defaultLimit) && (defaultLimit != 0))
					top = defaultLimit;
				if (uriInfo.getSkip() == null) {
					skip = 0;
				} else {
					skip = uriInfo.getSkip();
				}
			}
			if (top > 0 || skip > 0) {

				if (top > 0) {
					limitClause.append(" LIMIT ").append(top.toString());
				}
				if (skip > 0) {
					limitClause.append(" OFFSET ").append(skip.toString());
				}
			}
		}
		return limitClause;
	}

	private StringBuilder defaultLimitClause() {
		StringBuilder defaultLimitClause = new StringBuilder();
		int defaultLimit = rdfModel.getRdfRepository().getModelRepository().getDefaultQueryLimit();
		defaultLimitClause.append(" LIMIT ").append(defaultLimit);
		return defaultLimitClause;
	}

	private HashMap<String, HashSet<String>> createSelectPropertyMap(List<SelectItem> selectProperties)
			throws EdmException {
		//Align variables
		RdfEntityType entityType = rdfTargetEntityType;//rdfEntityType;
		String key = entityType.entityTypeName;
		HashMap<String, HashSet<String>> values = new HashMap<String, HashSet<String>>();
		//Boolean emptyClause = true;

		for (SelectItem property : selectProperties) {
			HashSet<String> valueProperties;
			RdfEntityType segmentEntityType = entityType;
			RdfEntityType priorSegmentEntityType = null;
			key = entityType.entityTypeName;
			//check property.getNavigationPropertySegments() 
			// if so then 
			for (NavigationPropertySegment navigationPropertySegment : property.getNavigationPropertySegments()) {
				priorSegmentEntityType = segmentEntityType;
				segmentEntityType = rdfModelToMetadata.getRdfEntityTypefromEdmEntitySet(navigationPropertySegment
						.getTargetEntitySet());
				key = key + navigationPropertySegment.getNavigationProperty().getName();
			}
			if (!values.containsKey(key)) {
				valueProperties = new HashSet<String>();
				values.put(key, valueProperties);
			} else {
				valueProperties = values.get(key);
			}

			if (property.isStar()) {
				//TODO Does/should segmentEntityType.getProperties get inhererited properties as well? 
				//TODO Why not get all
				for (RdfProperty rdfProperty : segmentEntityType.getProperties()) {
					if (rdfProperty.propertyNode != null) {
						valueProperties.add(rdfProperty.propertyNode.getIRI().toString());
						//emptyClause = false;
					}
				}

			} else if (property.getProperty() != null) {
				if (!property.getProperty().getName().equals(RdfConstants.SUBJECT)) {
					RdfProperty rdfProperty = null;
					try {
						rdfProperty = segmentEntityType.findProperty(property.getProperty().getName());
					} catch (EdmException e) {
						log.error("Failed to locate property:" + property.getProperty().getName());
					}

					if (rdfProperty.getIsKey()) {
						//TODO specifically asked for key so should be added to VALUES
						valueProperties.add("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
						//emptyClause = false;

					} else {
						valueProperties.add(rdfProperty.propertyNode.getIRI().toString());
						//emptyClause = false;
					}
				}
			} else {
				//Must be a navigation property
				@SuppressWarnings("unused")
				RdfAssociation rdfAssociation = null;
				try {
					//TODO which of the navigation properties???
					rdfAssociation = priorSegmentEntityType.findNavigationProperty(property
							.getNavigationPropertySegments().get(0).getNavigationProperty().getName());
				} catch (EdmException e) {
					log.error("Failed to locate navigation property:"
							+ property.getNavigationPropertySegments().get(0).getNavigationProperty().getName());
				}
			}

		}
		return values;
	}

}