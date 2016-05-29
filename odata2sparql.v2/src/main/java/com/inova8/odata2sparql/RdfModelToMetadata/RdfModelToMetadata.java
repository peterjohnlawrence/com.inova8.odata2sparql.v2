package com.inova8.odata2sparql.RdfModelToMetadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.inova8.odata2sparql.Constants.ODataServiceVersion;
import com.inova8.odata2sparql.Constants.RdfConstants;

import org.apache.olingo.odata2.api.edm.EdmAssociation;
import org.apache.olingo.odata2.api.edm.EdmEntitySet;
import org.apache.olingo.odata2.api.edm.EdmException;
import org.apache.olingo.odata2.api.edm.EdmMultiplicity;
import org.apache.olingo.odata2.api.edm.EdmProperty;
import org.apache.olingo.odata2.api.edm.EdmSimpleTypeKind;
import org.apache.olingo.odata2.api.edm.EdmTyped;
import org.apache.olingo.odata2.api.edm.FullQualifiedName;
import org.apache.olingo.odata2.api.edm.provider.AnnotationAttribute;
import org.apache.olingo.odata2.api.edm.provider.Association;
import org.apache.olingo.odata2.api.edm.provider.AssociationEnd;
import org.apache.olingo.odata2.api.edm.provider.AssociationSet;
import org.apache.olingo.odata2.api.edm.provider.AssociationSetEnd;
import org.apache.olingo.odata2.api.edm.provider.ComplexType;
import org.apache.olingo.odata2.api.edm.provider.EntityContainer;
import org.apache.olingo.odata2.api.edm.provider.EntitySet;
import org.apache.olingo.odata2.api.edm.provider.EntityType;
import org.apache.olingo.odata2.api.edm.provider.Facets;
import org.apache.olingo.odata2.api.edm.provider.FunctionImport;
import org.apache.olingo.odata2.api.edm.provider.FunctionImportParameter;
import org.apache.olingo.odata2.api.edm.provider.Key;
import org.apache.olingo.odata2.api.edm.provider.NavigationProperty;
import org.apache.olingo.odata2.api.edm.provider.Property;
import org.apache.olingo.odata2.api.edm.provider.PropertyRef;
import org.apache.olingo.odata2.api.edm.provider.ReferentialConstraint;
import org.apache.olingo.odata2.api.edm.provider.ReferentialConstraintRole;
import org.apache.olingo.odata2.api.edm.provider.ReturnType;
import org.apache.olingo.odata2.api.edm.provider.Schema;
import org.apache.olingo.odata2.api.edm.provider.SimpleProperty;
import org.apache.olingo.odata2.api.exception.ODataException;

import com.inova8.odata2sparql.RdfModel.RdfModel;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfAssociation;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfEntityType;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfPrimaryKey;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfProperty;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfSchema;

public class RdfModelToMetadata {


	private class PrefixedNamespace {

		private final String uri;
		private final String prefix;

		private PrefixedNamespace(String uri, String prefix) {
			this.uri = uri;
			this.prefix = prefix;
		}

		@Override
		public String toString() {
			return uri + " " + prefix;
		}
	}

	private final List<Schema> rdfEdm = new ArrayList<Schema>();

	public List<Schema> getSchemas() throws ODataException {
		return rdfEdm;
	}

	private final Map<FullQualifiedName, RdfEntityType> entitySetMapping = new HashMap<FullQualifiedName, RdfEntityType>();
	private final Map<FullQualifiedName, RdfProperty> propertyMapping = new HashMap<FullQualifiedName, RdfProperty>();
	private final Map<FullQualifiedName, RdfAssociation> navigationPropertyMapping = new HashMap<FullQualifiedName, RdfAssociation>();

	public RdfModelToMetadata(RdfModel rdfModel, String oDataVersion, boolean withRdfAnnotations,
			boolean withSapAnnotations) {
		Map<String, EntityType> globalEntityTypes = new HashMap<String, EntityType>();

		Map<String, RdfAssociation> navigationPropertyLookup = new HashMap<String, RdfAssociation>();
		Map<String, RdfAssociation> associationLookup = new HashMap<String, RdfAssociation>();
		Map<String, EntitySet> entitySetsMapping = new HashMap<String, EntitySet>();

		ArrayList<PrefixedNamespace> nameSpaces = new ArrayList<PrefixedNamespace>();
		nameSpaces.add(new PrefixedNamespace(RdfConstants.RDF_SCHEMA, RdfConstants.RDF));
		nameSpaces.add(new PrefixedNamespace(RdfConstants.RDFS_SCHEMA, RdfConstants.RDFS));
		nameSpaces.add(new PrefixedNamespace(RdfConstants.OWL_SCHEMA, RdfConstants.OWL));
		nameSpaces.add(new PrefixedNamespace(RdfConstants.XSD_SCHEMA, RdfConstants.XSD));

		String entityContainerName = RdfConstants.ENTITYCONTAINER;
		EntityContainer entityContainer = new EntityContainer().setName(entityContainerName).setDefaultEntityContainer(
				true);

		//TODO .setLazyLoadingEnabled(false);
		List<EntityContainer> entityContainers = new ArrayList<EntityContainer>();
		entityContainers.add(entityContainer);
		Schema instanceSchema = new Schema().setNamespace(RdfConstants.ENTITYCONTAINERNAMESPACE).setEntityContainers(
				entityContainers);
		rdfEdm.add(instanceSchema);

		//TODO List<EntitySet> entitySets = new ArrayList<EntitySet>();
		HashMap<String, EntitySet> entitySets = new HashMap<String, EntitySet>();		
		
		HashMap<String, AssociationSet> associationSets = new HashMap<String, AssociationSet>();

		//Custom types
		//langString
		List<AnnotationAttribute> langStringAnnotations = new ArrayList<AnnotationAttribute>();
		langStringAnnotations.add(new AnnotationAttribute().setNamespace(RdfConstants.RDFS_SCHEMA)
				.setPrefix(RdfConstants.RDFS).setName(RdfConstants.DATATYPE).setText(RdfConstants.XSD_STRING));
		ArrayList<Property> langStringAnnotationsProperties = new ArrayList<Property>();
		langStringAnnotationsProperties.add(new SimpleProperty().setName(RdfConstants.LANG)
				.setType(EdmSimpleTypeKind.String).setAnnotationAttributes(langStringAnnotations)
		// TODO .setNullable(true)
				);
		langStringAnnotationsProperties.add(new SimpleProperty().setName(RdfConstants.VALUE)
				.setType(EdmSimpleTypeKind.String).setAnnotationAttributes(langStringAnnotations));
		ComplexType langLiteralType = new ComplexType().setName(RdfConstants.LANGSTRING);
		// TODO .setNamespace(RdfConstants.RDF)

		if (withRdfAnnotations)
			langLiteralType.setProperties(langStringAnnotationsProperties);
		for (RdfSchema rdfGraph : rdfModel.graphs) {
			// First pass to locate all classes (entitytypes) and datatypes (typedefinitions)

			for (RdfEntityType rdfClass : rdfGraph.classes) {
				String entityTypeName = rdfClass.getEDMEntityTypeName();
				EntityType entityType = globalEntityTypes.get(rdfClass.getIRI());
				if (entityType == null) {
					entityType = new EntityType().setName(entityTypeName);
					// TODO .setNamespace(modelNamespace)
					globalEntityTypes.put(rdfClass.getIRI(), entityType);
				}
				if (rdfClass.getBaseType() != null) {
					String baseTypeName = rdfClass.getBaseType().getEDMEntityTypeName();
					EntityType baseType = globalEntityTypes.get(rdfClass.getBaseType().getIRI());
					if (baseType == null) {
						baseType = new EntityType().setName(baseTypeName);
						globalEntityTypes.put(rdfClass.getBaseType().getIRI(), baseType);
					}
					//entityType.setBaseType(rdfClass.getBaseType().getFullQualifiedName());
					entityType.setBaseType(RdfFullQualifiedName.getFullQualifiedName(rdfClass.getBaseType()));
					}
			
					List<AnnotationAttribute> entityTypeAnnotations = new ArrayList<AnnotationAttribute>();

					if (withRdfAnnotations) entityTypeAnnotations.add(new AnnotationAttribute().setNamespace(RdfConstants.RDFS_SCHEMA)
							.setPrefix(RdfConstants.RDFS).setName(RdfConstants.RDFS_CLASS_LABEL)
							.setText(rdfClass.getIRI()));
					if (withSapAnnotations) entityTypeAnnotations.add(new AnnotationAttribute().setNamespace(RdfConstants.SAP_ANNOTATION_SCHEMA)
							.setPrefix(RdfConstants.SAP_ANNOTATION_NS).setName(RdfConstants.SAP_LABEL)
							.setText(rdfClass.getEntityTypeLabel()));
					entityType.setAnnotationAttributes(entityTypeAnnotations);			
			}
		}
		;

		for (RdfSchema rdfGraph : rdfModel.graphs) {
			// Second pass to add properties, navigation properties, and entitysets, and create the schema
			Map<String, EntityType> entityTypes = new HashMap<String, EntityType>();
			HashMap<String, Association> associations = new HashMap<String, Association>();
			Map<String, EntityType> entityTypeMapping = new HashMap<String, EntityType>();

			String modelNamespace = rdfModel.getModelNamespace(rdfGraph);

			for (RdfEntityType rdfClass : rdfGraph.classes) {
				String entityTypeName = rdfClass.getEDMEntityTypeName();
				EntityType entityType = globalEntityTypes.get(rdfClass.getIRI());
				entityTypes.put(entityTypeName, entityType);
				entityType.setAbstract(false);
				// TODO entityType.setNamespace(modelNamespace);
				entityTypeMapping.put(entityTypeName, entityType);
				HashMap<String, NavigationProperty> navigationProperties = new HashMap<String, NavigationProperty>();	
				HashMap<String, Property> entityTypeProperties = new HashMap<String, Property>();	
				
				ArrayList<PropertyRef> keys = new ArrayList<PropertyRef>();
				for (RdfPrimaryKey primaryKey : rdfClass.getPrimaryKeys()) {
					String propertyName = primaryKey.getEDMPropertyName();
					keys.add(new PropertyRef().setName(propertyName));
					entityType.setKey(new Key().setKeys(keys));
				}

				for (RdfProperty rdfProperty : rdfClass.getProperties()) {
					String propertyName = rdfProperty.getEDMPropertyName();
					EdmSimpleTypeKind propertyType = RdfEdmType.getEdmType(rdfProperty.propertyTypeName);  //rdfProperty.propertyType;

					Facets notNullableFacets = new Facets();
					notNullableFacets.setNullable(false);
					Facets nullableFacets = new Facets();
					nullableFacets.setNullable(true);

					//TODO langString
					//					if (propertyType.equals(EdmSimpleTypeKind.String)
					//							&& !rdfProperty.propertyName
					//									.equals(RdfConstants.ID))
					//						propertyType = langLiteralType;
					Property property = new SimpleProperty().setName(propertyName).setType(propertyType);

					List<AnnotationAttribute> propertyAnnotations = new ArrayList<AnnotationAttribute>();
					if (!rdfProperty.propertyName.equals(RdfConstants.SUBJECT)) {

						if (withRdfAnnotations)
							propertyAnnotations.add(new AnnotationAttribute().setNamespace(RdfConstants.RDF_SCHEMA)
									.setPrefix(RdfConstants.RDF).setName(RdfConstants.PROPERTY)
									.setText(rdfProperty.getPropertyURI().toString()));

						if (withRdfAnnotations)
							propertyAnnotations.add(new AnnotationAttribute().setNamespace(RdfConstants.RDFS_SCHEMA)
									.setPrefix(RdfConstants.RDFS).setName(RdfConstants.DATATYPE)
									.setText(rdfProperty.propertyTypeName));
						if (rdfProperty.getEquivalentProperty() != null) {
							if (withRdfAnnotations)
								propertyAnnotations.add(new AnnotationAttribute().setNamespace(RdfConstants.OWL_SCHEMA)
										.setPrefix(RdfConstants.OWL).setName(RdfConstants.OWL_EQUIVALENTPROPERTY_LABEL)
										.setText(rdfProperty.getEquivalentProperty()));
						}
						if (withSapAnnotations) propertyAnnotations.add(new AnnotationAttribute().setNamespace(RdfConstants.SAP_ANNOTATION_SCHEMA)
								.setPrefix(RdfConstants.SAP_ANNOTATION_NS).setName(RdfConstants.SAP_LABEL)
								.setText(rdfProperty.getPropertyLabel()));
						if (withSapAnnotations) propertyAnnotations.add(new AnnotationAttribute().setNamespace(RdfConstants.SAP_ANNOTATION_SCHEMA)
								.setPrefix(RdfConstants.SAP_ANNOTATION_NS).setName(RdfConstants.SAP_HEADING)
								.setText(rdfProperty.getPropertyLabel()));
						if (withSapAnnotations) propertyAnnotations.add(new AnnotationAttribute().setNamespace(RdfConstants.SAP_ANNOTATION_SCHEMA)
								.setPrefix(RdfConstants.SAP_ANNOTATION_NS).setName(RdfConstants.SAP_QUICKINFO)
								.setText(rdfProperty.getDescription()));
						property.setAnnotationAttributes(propertyAnnotations);

						if (rdfProperty.getIsKey()) {
							property.setFacets(notNullableFacets);
						} else if (rdfProperty.getCardinality() == RdfConstants.Cardinality.ZERO_TO_ONE
								|| rdfProperty.getCardinality() == RdfConstants.Cardinality.MANY) {
							if (ODataServiceVersion.isBiggerThan(oDataVersion, ODataServiceVersion.V20)) {
								property.setFacets(nullableFacets);
							} else {
								property.setFacets(nullableFacets);
							}
						} else {
							//TODO need to handle case when data violates nullablility, in the meantime allow all to be nullable
							//property.setFacets(notNullableFacets);
							property.setFacets(nullableFacets);
						}
						if (ODataServiceVersion.isBiggerThan(oDataVersion, ODataServiceVersion.V20)) {
							if (rdfProperty.getCardinality() == RdfConstants.Cardinality.MANY
									|| rdfProperty.getCardinality() == RdfConstants.Cardinality.MULTIPLE) {
								//TODO property.setCollectionKind(CollectionKind.List);
							} else {
								//TODO property.setCollectionKind(CollectionKind.NONE);
							}
						}
					} else {

						property.setFacets(notNullableFacets);
						if (withRdfAnnotations)
							propertyAnnotations.add(new AnnotationAttribute().setNamespace(RdfConstants.RDFS_SCHEMA)
									.setPrefix(RdfConstants.RDFS).setName(RdfConstants.DATATYPE)
									.setText(RdfConstants.RDFS_RESOURCE));
						if (withSapAnnotations) propertyAnnotations.add(new AnnotationAttribute().setNamespace(RdfConstants.SAP_ANNOTATION_SCHEMA)
								.setPrefix(RdfConstants.SAP_ANNOTATION_NS).setName(RdfConstants.SAP_LABEL)
								.setText(rdfProperty.getPropertyLabel()));
						if (withSapAnnotations) propertyAnnotations.add(new AnnotationAttribute().setNamespace(RdfConstants.SAP_ANNOTATION_SCHEMA)
								.setPrefix(RdfConstants.SAP_ANNOTATION_NS).setName(RdfConstants.SAP_HEADING)
								.setText(rdfProperty.getPropertyLabel()));
						if (withSapAnnotations) propertyAnnotations.add(new AnnotationAttribute().setNamespace(RdfConstants.SAP_ANNOTATION_SCHEMA)
								.setPrefix(RdfConstants.SAP_ANNOTATION_NS).setName(RdfConstants.SAP_QUICKINFO)
								.setText(rdfProperty.getDescription()));
						property.setAnnotationAttributes(propertyAnnotations);

					}
					//TODO what if duplicates?
					entityTypeProperties.put( property.getName(), property); 
					propertyMapping.put(new FullQualifiedName(rdfClass.getSchema().getSchemaPrefix(), property.getName()),
							rdfProperty);

				}
				entityType.setProperties(new ArrayList<Property>(entityTypeProperties.values()));
			    entityType.setNavigationProperties(new ArrayList<NavigationProperty>(navigationProperties.values()));

				String entitySetName = rdfClass.getEDMEntitySetName();

				EntitySet entitySet = new EntitySet().setName(entitySetName).setEntityType(
						new FullQualifiedName(rdfClass.getSchema().getSchemaPrefix(), entityTypeName));//entityType);

				List<AnnotationAttribute> entitySetAnnotations = new ArrayList<AnnotationAttribute>();
				if (withSapAnnotations) entitySetAnnotations.add(new AnnotationAttribute().setNamespace(RdfConstants.SAP_ANNOTATION_SCHEMA)
						.setPrefix(RdfConstants.SAP_ANNOTATION_NS).setName(RdfConstants.SAP_LABEL)
						.setText(rdfClass.getEntityTypeLabel()));
				entitySet.setAnnotationAttributes(entitySetAnnotations);

				entitySets.put(entitySet.getName(), entitySet);//.add(entitySet);

				entitySetMapping.put(entitySet.getEntityType(), rdfClass);
				entitySetsMapping.put(entitySetName, entitySet);
			}

			for (RdfAssociation rdfAssociation : rdfGraph.associations) {
				// if (!rdfAssociation.isInverse)
				{
					String associationName = rdfAssociation.getEDMAssociationName();
					String duplicate = "";
					if (rdfAssociation.getDomainName().equals(rdfAssociation.getRangeName()))
						duplicate = RdfConstants.DUPLICATEROLE;

					AssociationEnd fromRole = new AssociationEnd().setRole(
							rdfAssociation.getDomainName() + RdfConstants.FROMROLE).setType(
									RdfFullQualifiedName.getFullQualifiedName(rdfAssociation.domainClass));

					AssociationEnd toRole = new AssociationEnd().setRole(
							rdfAssociation.getRangeName() + RdfConstants.TOROLE + duplicate).setType(
									RdfFullQualifiedName.getFullQualifiedName(rdfAssociation.getRangeClass()));
					switch (rdfAssociation.getFromCardinality()) {
					case ZERO_TO_ONE:
						fromRole.setMultiplicity(EdmMultiplicity.ZERO_TO_ONE);
						break;
					case ONE:
						fromRole.setMultiplicity(EdmMultiplicity.ONE);
						break;
					case MANY:
						fromRole.setMultiplicity(EdmMultiplicity.MANY);
						break;
					case MULTIPLE:
						fromRole.setMultiplicity(EdmMultiplicity.MANY);
						break;
					}
					switch (rdfAssociation.getToCardinality()) {
					case ZERO_TO_ONE:
						toRole.setMultiplicity(EdmMultiplicity.ZERO_TO_ONE);
						break;
					case ONE:
						toRole.setMultiplicity(EdmMultiplicity.ONE);
						break;
					case MANY:
						toRole.setMultiplicity(EdmMultiplicity.MANY);
						break;
					case MULTIPLE:
						toRole.setMultiplicity(EdmMultiplicity.MANY);
						break;
					}
					Association association = new Association().setName(associationName).setEnd1(fromRole)
							.setEnd2(toRole)
					//TODO .setNamespace(modelNamespace)
					;
					if (ODataServiceVersion.isBiggerThan(oDataVersion, ODataServiceVersion.V20)) {
						ReferentialConstraintRole principalConstraintRole = new ReferentialConstraintRole();
						ReferentialConstraintRole dependentConstraintRole = new ReferentialConstraintRole();
						principalConstraintRole.setRole(rdfAssociation.getDomainName() + RdfConstants.FROMROLE);
						//TODO principalConstraintRole.setPropertyRefs(RdfConstants.ID);
						dependentConstraintRole.setRole(rdfAssociation.getRangeName() + RdfConstants.TOROLE + duplicate);
						//TODO dependentConstraintRole.setPropertyRefs(RdfConstants.ID);						

						ReferentialConstraint referentialConstraint = new ReferentialConstraint().setPrincipal(
								principalConstraintRole).setDependent(dependentConstraintRole);
						association.setReferentialConstraint(referentialConstraint);
					}

					associations.put(association.getName(), association);

					//TODO if (!rdfAssociation.isInverse)
						associationLookup.put(association.getName(), rdfAssociation);
					
					//TODO Do we need a new navigation property or extend an existing one?
					NavigationProperty navigationProperty = new NavigationProperty().setName(associationName)
							.setRelationship(RdfFullQualifiedName.getFullQualifiedName(rdfAssociation)).setFromRole(fromRole.getRole())
							//.setRelationship(rdfAssociation.getFullQualifiedName()).setFromRole(fromRole.getRole())
							.setToRole(toRole.getRole());

					List<AnnotationAttribute> navigationPropertyAnnotations = new ArrayList<AnnotationAttribute>();
					if (withRdfAnnotations)
						navigationPropertyAnnotations.add(new AnnotationAttribute()
								.setNamespace(RdfConstants.RDF_SCHEMA).setPrefix(RdfConstants.RDF)
								.setName(RdfConstants.PROPERTY)
								.setText(rdfAssociation.getAssociationIRI().toString()));
					
					if (withSapAnnotations) navigationPropertyAnnotations.add(new AnnotationAttribute().setNamespace(RdfConstants.SAP_ANNOTATION_SCHEMA)
							.setPrefix(RdfConstants.SAP_ANNOTATION_NS).setName(RdfConstants.SAP_LABEL)
							.setText(rdfAssociation.getAssociationLabel()));
					if (withSapAnnotations) navigationPropertyAnnotations.add(new AnnotationAttribute().setNamespace(RdfConstants.SAP_ANNOTATION_SCHEMA)
							.setPrefix(RdfConstants.SAP_ANNOTATION_NS).setName(RdfConstants.SAP_HEADING)
							.setText(rdfAssociation.getAssociationLabel()));
					if (withSapAnnotations) navigationPropertyAnnotations.add(new AnnotationAttribute().setNamespace(RdfConstants.SAP_ANNOTATION_SCHEMA)
							.setPrefix(RdfConstants.SAP_ANNOTATION_NS).setName(RdfConstants.SAP_QUICKINFO)
							.setText(rdfAssociation.getDescription()));					
					if (rdfAssociation.IsInverse()) {
						if (withRdfAnnotations)
							navigationPropertyAnnotations.add(new AnnotationAttribute()
									.setNamespace(RdfConstants.OWL_SCHEMA).setPrefix(RdfConstants.OWL)
									.setName(RdfConstants.INVERSEOF)
									.setText(rdfAssociation.getInversePropertyOfURI().toString()));

					}
					navigationProperty.setAnnotationAttributes(navigationPropertyAnnotations);
					//TODO should not add duplicates to the same entity, even though Olingo accepts them							
					globalEntityTypes.get(rdfAssociation.getDomainNodeURI()).getNavigationProperties()
							.add(navigationProperty);

					navigationPropertyLookup.put(navigationProperty.getName(), rdfAssociation);
					navigationPropertyMapping.put(navigationProperty.getRelationship(), rdfAssociation);
					//rdfAssociation.setEdmAssociation(association);
				}
			}

			List<Schema> edmSchemas = new ArrayList<Schema>();

			List<AnnotationAttribute> schemaAnnotations = new ArrayList<AnnotationAttribute>();
			schemaAnnotations.add(new AnnotationAttribute().setNamespace(RdfConstants.OWL_SCHEMA)
						.setPrefix(RdfConstants.OWL).setName(RdfConstants.ONTOLOGY).setText(rdfGraph.getSchemaName()));

			//TODO Create empty container to satisfy Olingo		
			List<EntitySet> modelSchemaEntitySets = new ArrayList<EntitySet>();
			String modelSchemaContainerName = modelNamespace + RdfConstants.ENTITYCONTAINER;
			EntityContainer modelSchemaContainer = new EntityContainer().setName(modelSchemaContainerName)
					.setDefaultEntityContainer(false).setEntitySets(modelSchemaEntitySets);

			List<EntityContainer> modelSchemaContainers = new ArrayList<EntityContainer>();
			modelSchemaContainers.add(modelSchemaContainer);

			Schema modelSchema = new Schema().setNamespace(modelNamespace)
					.setEntityTypes(new ArrayList<EntityType>(entityTypes.values()))
					.setAssociations(new ArrayList<Association>(associations.values()))
					.setEntityContainers(modelSchemaContainers) //No longer required with this version of Olingo
					.setComplexTypes(new ArrayList<ComplexType>());
			modelSchema.setAnnotationAttributes(schemaAnnotations);
			if (modelNamespace.equals(RdfConstants.RDF)) {
				@SuppressWarnings("unused")
				ArrayList<ComplexType> complexTypes = new ArrayList<ComplexType>();
				modelSchema.getComplexTypes().add(langLiteralType);
			}

			edmSchemas.add(modelSchema);

			rdfEdm.add(modelSchema);
		}

		for (RdfSchema rdfGraph : rdfModel.graphs) {
			// Third pass to add associationsets
			for (RdfAssociation rdfAssociation : rdfGraph.associations) {
				// if (!rdfAssociation.isInverse)
				{
					String duplicate = "";
					if (rdfAssociation.getDomainName().equals(rdfAssociation.getRangeName()))
						duplicate = RdfConstants.DUPLICATEROLE;

					String associationSetName = rdfAssociation.getEDMAssociationSetName();
					AssociationSetEnd fromSet = new AssociationSetEnd().setRole(
							rdfAssociation.getDomainName() + RdfConstants.PLURAL)
					//.setRole(rdfAssociation.edmAssociation.getEnd1())
							.setEntitySet(
									entitySetsMapping.get(rdfAssociation.domainClass.getEDMEntitySetName()).getName());
					AssociationSetEnd toSet = new AssociationSetEnd().setRole(
							rdfAssociation.getRangeName() + RdfConstants.PLURAL + duplicate)
					//.setRole(rdfAssociation.edmAssociation.getEnd2())
							.setEntitySet(
									entitySetsMapping.get(rdfAssociation.getRangeClass().getEDMEntitySetName()).getName());

					AssociationSet associationSet = new AssociationSet().setName(associationSetName)
							.setAssociation(RdfFullQualifiedName.getFullQualifiedName(rdfAssociation)).setEnd1(fromSet).setEnd2(toSet);
							//.setAssociation(rdfAssociation.getFullQualifiedName()).setEnd1(fromSet).setEnd2(toSet);

					List<AnnotationAttribute> associationSetAnnotations = new ArrayList<AnnotationAttribute>();
					//TODO we should rely on the metadata queries to determine which properties even of Resource that should be included
					//					associationSetAnnotations.add(new AnnotationAttribute()
					//							.setNamespace(RdfConstants.RDFS_SCHEMA)
					//							.setPrefix(RdfConstants.RDFS)
					//							.setName(RdfConstants.RDFS_LABEL_LABEL)
					//							.setText(rdfAssociation.associationLabel));

					if (withRdfAnnotations)
						associationSet.setAnnotationAttributes(associationSetAnnotations);

					associationSets.put(associationSet.getName(), associationSet);
				}
			}
		}

		List<FunctionImport> functionImports = new ArrayList<FunctionImport>();		
		for (RdfSchema rdfGraph : rdfModel.graphs) {
			// Final pass to add any functionImports
			for (RdfEntityType rdfEntityType : rdfGraph.classes) {
				if(rdfEntityType.isFunctionImport()){	
					FunctionImport functionImport = new FunctionImport();
					List<FunctionImportParameter> functionImportParameters =new ArrayList<FunctionImportParameter>(0);
					for( com.inova8.odata2sparql.RdfModel.RdfModel.FunctionImportParameter functionImportParameter : rdfEntityType.getFunctionImportParameters().values()){
						List<AnnotationAttribute> nullableAnnotations = new ArrayList<AnnotationAttribute>();
						nullableAnnotations.add((new AnnotationAttribute()).setName(RdfConstants.NULLABLE).setText(	RdfConstants.FALSE));
						FunctionImportParameter edmFunctionImportParameter = new FunctionImportParameter();
						edmFunctionImportParameter.setName(functionImportParameter.getName())
						.setType(RdfEdmType.getEdmType(functionImportParameter.getType()))
						.setAnnotationAttributes(nullableAnnotations);						
						functionImportParameters.add(edmFunctionImportParameter);
					}
					ReturnType functionImportReturnType = (new ReturnType()).setTypeName(RdfFullQualifiedName.getFullQualifiedName(rdfEntityType)).setMultiplicity(EdmMultiplicity.MANY);
					List<AnnotationAttribute> functionImportAnnotations = new ArrayList<AnnotationAttribute>();
					functionImportAnnotations.add(new AnnotationAttribute().setName("IsBindable").setText("true"));
					functionImport.setName(rdfEntityType.getEDMEntityTypeName()).setParameters(functionImportParameters).setEntitySet(rdfEntityType.getEDMEntityTypeName())
					.setReturnType(functionImportReturnType).setAnnotationAttributes(functionImportAnnotations)
					.setHttpMethod("GET");				
					functionImports.add(functionImport);				
				}			
			}
		}		
		entityContainer.setFunctionImports(functionImports);
		entityContainer.setEntitySets(new ArrayList<EntitySet>(entitySets.values())).setAssociationSets(
				new ArrayList<AssociationSet>(associationSets.values()));
		//addCoreFunctionImports(entityContainer, globalEntityTypes, entitySetsMapping);
	}
	public RdfEntityType getMappedEntityType(FullQualifiedName fqnEntityType) {
		return entitySetMapping.get(fqnEntityType);
	}
	public RdfEntityType getRdfEntityTypefromEdmEntitySet(EdmEntitySet edmEntitySet) throws EdmException {
		return this.getMappedEntityType(new FullQualifiedName(edmEntitySet.getEntityType().getNamespace(), edmEntitySet
				.getEntityType().getName()));
	}
	public RdfProperty getMappedProperty(FullQualifiedName fqnProperty) {
		return propertyMapping.get(fqnProperty);
	}
	public RdfProperty getMappedProperty(EdmAssociation edmAssociation, EdmTyped edmTyped) throws EdmException {
		FullQualifiedName fqnProperty = new FullQualifiedName(edmAssociation.getNamespace(), edmTyped.getName());
		return propertyMapping.get(fqnProperty);
	}
	public RdfAssociation getMappedNavigationProperty(FullQualifiedName edmNavigationProperty) {
		return navigationPropertyMapping.get(edmNavigationProperty);
	}
	public RdfAssociation getMappedNavigationProperty(EdmAssociation edmAssociation) throws EdmException {
		FullQualifiedName edmNavigationProperty = new FullQualifiedName(edmAssociation.getNamespace(), edmAssociation.getName());
		return navigationPropertyMapping.get(edmNavigationProperty);
	}
}
