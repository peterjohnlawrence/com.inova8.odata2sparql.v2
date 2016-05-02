package com.inova8.odata2sparql.RdfEdmProvider;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.olingo.odata2.api.edm.EdmEntitySet;
import org.apache.olingo.odata2.api.edm.EdmException;
import org.apache.olingo.odata2.api.edm.FullQualifiedName;
import org.apache.olingo.odata2.api.edm.provider.Association;
import org.apache.olingo.odata2.api.edm.provider.AssociationSet;
import org.apache.olingo.odata2.api.edm.provider.ComplexType;
import org.apache.olingo.odata2.api.edm.provider.EdmProvider;
import org.apache.olingo.odata2.api.edm.provider.EntityContainer;
import org.apache.olingo.odata2.api.edm.provider.EntityContainerInfo;
import org.apache.olingo.odata2.api.edm.provider.EntitySet;
import org.apache.olingo.odata2.api.edm.provider.EntityType;
import org.apache.olingo.odata2.api.edm.provider.FunctionImport;
import org.apache.olingo.odata2.api.edm.provider.Schema;
import org.apache.olingo.odata2.api.exception.ODataException;
import com.inova8.odata2sparql.OData2SparqlException.OData2SparqlException;
import com.inova8.odata2sparql.RdfConstants.RdfConstants;
import com.inova8.odata2sparql.RdfModel.RdfModel;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfAssociation;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfEntityType;
import com.inova8.odata2sparql.RdfModel.RdfModel.RdfProperty;
import com.inova8.odata2sparql.RdfModelToMetadata.RdfModelToMetadata;
import com.inova8.odata2sparql.RdfRepository.RdfRepository;

public class RdfEdmProvider extends EdmProvider {
	private final Log log = LogFactory.getLog(RdfEdmProvider.class);
	private final RdfEdmModelProvider rdfEdmModelProvider;
//	private final SparqlODataSingleProcessor sparqlODataSingleProcessor;

//	private final UrlValidator defaultValidator = new UrlValidator();

	RdfEdmProvider( String odataVersion,RdfRepository rdfRepository) throws OData2SparqlException {
		this.rdfEdmModelProvider = new RdfEdmModelProvider(rdfRepository, odataVersion);
//		this.sparqlODataSingleProcessor = new SparqlODataSingleProcessor(this);
	}

//	public SparqlODataSingleProcessor getSparqlODataSingleProcessor() {
//		return sparqlODataSingleProcessor;
//	}

	public RdfEntityType getMappedEntityType(FullQualifiedName fullQualifiedName) {
		return this.rdfEdmModelProvider.getEdmMetadata().getMappedEntityType(fullQualifiedName);
	}

	public RdfEntityType getRdfEntityTypefromEdmEntitySet(EdmEntitySet edmEntitySet) throws EdmException {
		return this.getMappedEntityType(new FullQualifiedName(edmEntitySet.getEntityType().getNamespace(), edmEntitySet
				.getEntityType().getName()));
	}

	public RdfProperty getMappedProperty(FullQualifiedName fqnProperty) {
		return this.rdfEdmModelProvider.getEdmMetadata().getMappedProperty(fqnProperty);
	}

	public RdfAssociation getMappedNavigationProperty(FullQualifiedName edmNavigationProperty) {
		return this.rdfEdmModelProvider.getEdmMetadata().getMappedNavigationProperty(edmNavigationProperty);
	}

	/**
	 * @return the odataVersion
	 */
	public String getOdataVersion() {
		return this.rdfEdmModelProvider.getOdataVersion();
	}

	/**
	 * @return the edmMetadata
	 */
	public RdfModelToMetadata getEdmMetadata() {
		return this.rdfEdmModelProvider.getEdmMetadata();
	}

	/**
	 * @return the rdfRepository
	 */
	public RdfRepository getRdfRepository() {
		return this.rdfEdmModelProvider.getRdfRepository();
	}

	/**
	 * @return the rdfModel
	 */
	public RdfModel getRdfModel() {
		return this.rdfEdmModelProvider.getRdfModel();
	}

	@Override
	public List<Schema> getSchemas() throws ODataException {
		return this.rdfEdmModelProvider.getEdmMetadata().getSchemas();
	}

	@Override
	public EntityType getEntityType(final FullQualifiedName edmFQName) throws ODataException {

		String nameSpace = edmFQName.getNamespace();
		try {
			for (Schema schema : this.rdfEdmModelProvider.getEdmMetadata().getSchemas()) {
				if (nameSpace.equals(schema.getNamespace())) {
					String entityTypeName = edmFQName.getName();
					for (EntityType entityType : schema.getEntityTypes()) {
						if (entityTypeName.equals(entityType.getName())) {
							return entityType;
						}
					}
				}
			}
		} catch (NullPointerException e) {
			log.fatal("NullPointerException getEntityType " + edmFQName);
			throw new ODataException();
		}
		return null;
	}

	@Override
	public ComplexType getComplexType(final FullQualifiedName edmFQName) throws ODataException {

		String nameSpace = edmFQName.getNamespace();
		try {
			for (Schema schema : this.rdfEdmModelProvider.getEdmMetadata().getSchemas()) {
				if (nameSpace.equals(schema.getNamespace())) {
					String complexTypeName = edmFQName.getName();
					for (ComplexType complexType : schema.getComplexTypes()) {
						if (complexTypeName.equals(complexType.getName())) {
							return complexType;
						}
					}
				}
			}
		} catch (NullPointerException e) {
			log.fatal("NullPointerException getComplexType " + edmFQName);
			throw new ODataException();
		}
		return null;
	}

	@Override
	public Association getAssociation(final FullQualifiedName edmFQName) throws ODataException {

		String nameSpace = edmFQName.getNamespace();
		try {
			for (Schema schema : this.rdfEdmModelProvider.getEdmMetadata().getSchemas()) {
				if (nameSpace.equals(schema.getNamespace())) {
					String associationName = edmFQName.getName();
					for (Association association : schema.getAssociations()) {
						if (associationName.equals(association.getName())) {
							return association;
						}
					}
				}
			}
		} catch (NullPointerException e) {
			log.fatal("NullPointerException getAssociation " + edmFQName);
			throw new ODataException();
		}
		return null;
	}

	@Override
	public EntitySet getEntitySet(final String entityContainer, final String name) throws ODataException {

		try {
			for (Schema schema : this.rdfEdmModelProvider.getEdmMetadata().getSchemas()) {
				for (EntityContainer schemaEntityContainer : schema.getEntityContainers()) {
					if (entityContainer.equals(schemaEntityContainer.getName())) {
						for (EntitySet entitySet : schemaEntityContainer.getEntitySets()) {
							if (name.equals(entitySet.getName())) {
								return entitySet;
							}
						}
					}
				}
			}
		} catch (NullPointerException e) {
			log.fatal("NullPointerException getEntitySet " + entityContainer + " " + name);
			throw new ODataException();
		}
		return null;
	}

	@Override
	public AssociationSet getAssociationSet(final String entityContainer, final FullQualifiedName association,
			final String sourceEntitySetName, final String sourceEntitySetRole) throws ODataException {

		try {
			for (Schema schema : this.rdfEdmModelProvider.getEdmMetadata().getSchemas()) {
				for (EntityContainer schemaEntityContainer : schema.getEntityContainers()) {
					if (entityContainer.equals(schemaEntityContainer.getName())) {

						for (AssociationSet associationSet : schemaEntityContainer.getAssociationSets()) {
							if (association.equals(associationSet.getAssociation())) {
								return associationSet;
							}
						}
					}
				}
			}
		} catch (NullPointerException e) {
			log.fatal("NullPointerException getAssociationSet " + entityContainer + " " + association);
			throw new ODataException();
		}
		return null;
	}

	@Override
	public FunctionImport getFunctionImport(final String entityContainer, final String name) throws ODataException {

		try {
			for (Schema schema : this.rdfEdmModelProvider.getEdmMetadata().getSchemas()) {
				for (EntityContainer schemaEntityContainer : schema.getEntityContainers()) {
					if (entityContainer.equals(schemaEntityContainer.getName())) {
						for (FunctionImport functionImport : schemaEntityContainer.getFunctionImports()) {
							if (name.equals(functionImport.getName())) {
								return functionImport;
							}
						}
					}
				}
			}
		} catch (NullPointerException e) {
			log.fatal("NullPointerException getFunctionImport " + entityContainer + " " + name);
			throw new ODataException();
		}

		return null;
	}

	@Override
	public EntityContainerInfo getEntityContainerInfo(final String name) throws ODataException {
		if (name == null) {
			// Assume request for null container means default container
			return new EntityContainerInfo().setName(RdfConstants.ENTITYCONTAINER).setDefaultEntityContainer(true);
		} else {
			try {
				for (Schema schema : this.rdfEdmModelProvider.getEdmMetadata().getSchemas()) {

					for (EntityContainer schemaEntityContainer : schema.getEntityContainers()) {
						if (name.equals(schemaEntityContainer.getName())) {
							return new EntityContainerInfo().setName(name).setDefaultEntityContainer(
									schemaEntityContainer.isDefaultEntityContainer());
						}
					}
				}
			} catch (NullPointerException e) {
				log.fatal("NullPointerException getEntityContainerInfo " + name);
				throw new ODataException();
			}
		}
		return null;
	}
}
