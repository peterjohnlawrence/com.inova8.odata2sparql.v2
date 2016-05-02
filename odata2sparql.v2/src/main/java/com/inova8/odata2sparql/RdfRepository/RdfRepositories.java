package com.inova8.odata2sparql.RdfRepository;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openrdf.model.Namespace;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.NamespaceImpl;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.config.RepositoryConfig;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.config.RepositoryImplConfig;
import org.openrdf.repository.manager.LocalRepositoryManager;
import org.openrdf.repository.manager.RemoteRepositoryManager;
import org.openrdf.repository.manager.RepositoryManager;
import org.openrdf.repository.sail.config.SailRepositoryConfig;
import org.openrdf.repository.sparql.config.SPARQLRepositoryConfig;
import org.openrdf.rio.RDFParseException;
import org.openrdf.sail.config.SailImplConfig;
import org.openrdf.sail.memory.config.MemoryStoreConfig;

import com.inova8.odata2sparql.OData2SparqlException.OData2SparqlException;
import com.inova8.odata2sparql.RdfConstants.*;

public class RdfRepositories {
	private final Log log = LogFactory.getLog(RdfRepositories.class);
	private RepositoryManager repositoryManager = null;

	private HashMap<String, RdfRepository> rdfRepositoryList = new HashMap<String, RdfRepository>();

	private final Properties properties = new Properties();

	public RdfRepositories() {
		super();
		try {
			loadRepositories();
		} catch (OData2SparqlException e) {
			log.fatal("Cannot load repositories", e);
		} catch (RepositoryConfigException e) {
			log.fatal("Cannot load repositories", e);
		}
	}
	public void reset(String rdfRepositoryID) {
		if(rdfRepositoryID.equals(RdfConstants.WILDCARD)){
			rdfRepositoryList = new HashMap<String, RdfRepository>();
		}else{
			rdfRepositoryList.remove(rdfRepositoryID.toUpperCase());
		}	
	}
	public void reload(String rdfRepositoryID) {
		repositoryManager.shutDown();
		try {
			loadRepositories();
		} catch (OData2SparqlException e) {
			log.fatal("Cannot load repositories", e);
		} catch (RepositoryConfigException e) {
			log.fatal("Cannot load repositories", e);
		}
	}
	public RdfRepository getRdfRepository(String rdfRepositoryID) {
		return rdfRepositoryList.get(rdfRepositoryID.toUpperCase());
	}

	public HashMap<String, RdfRepository> getRdfRepositories() {
		return rdfRepositoryList;
	}

	private void loadRepositories() throws OData2SparqlException, RepositoryConfigException {
		//RepositoryManager repositoryManager = null;
		try {
			if (loadProperties()) {
				try {
					repositoryManager = bootstrapRemoteRepository(properties.getProperty(RdfConstants.repositoryUrl));
				} catch (OData2SparqlException e) {
					try {
						repositoryManager = bootstrapLocalRepository();
					} catch (OData2SparqlException e1) {
						log.fatal("Tried everything. Cannot locate a suitable repository", e1);
					}
				}
			} else {
				try {
					repositoryManager = bootstrapLocalRepository();
				} catch (OData2SparqlException e1) {
					log.fatal("Tried everything. Cannot locate a suitable repository", e1);
				}
			}

			Repository systemRepository = repositoryManager.getRepository(RdfConstants.systemId);
			RepositoryConnection modelsConnection = systemRepository.getConnection();

			//Bootstrap the standard queries

			readQueries(modelsConnection, RdfConstants.RDFSModel);

			try {
				//Identify endpoints and create corresponding repositories:
				String queryString = RdfConstants.getMetaQueries().get(RdfConstants.URI_REPOSITORYQUERY);

				TupleQuery tupleQuery = modelsConnection.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
				TupleQueryResult result = tupleQuery.evaluate();
				try {
					while (result.hasNext()) { // iterate over the result
						try {
							BindingSet bindingSet = result.next();
							Value valueOfDataset = bindingSet.getValue("Dataset");
							@SuppressWarnings("unused")
							Value valueOfDatasetName = bindingSet.getValue("DatasetName");
							log.info("Dataset loaded: " + valueOfDataset.toString());

							Value valueOfDefaultNamespace = bindingSet.getValue("defaultNamespace");
							Value valueOfDefaultPrefix = bindingSet.getValue("defaultPrefix");

							Value valueOfDataRepositoryID = bindingSet.getValue("DataRepositoryID");
							Value valueOfDataRepositoryImplQueryEndpoint = bindingSet
									.getValue("DataRepositoryImplQueryEndpoint");
							Value valueOfDataRepositoryImplUpdateEndpoint = bindingSet
									.getValue("DataRepositoryImplUpdateEndpoint");
							Value valueOfDataRepositoryImplProfile = bindingSet.getValue("DataRepositoryImplProfile");
							Value valueOfDataRepositoryImplQueryLimit = bindingSet
									.getValue("DataRepositoryImplQueryLimit");

							Value valueOfVocabularyRepositoryID = bindingSet.getValue("VocabularyRepositoryID");
							Value valueOfVocabularyRepositoryImplQueryEndpoint = bindingSet
									.getValue("VocabularyRepositoryImplQueryEndpoint");
							Value valueOfVocabularyRepositoryImplUpdateEndpoint = bindingSet
									.getValue("VocabularyRepositoryImplUpdateEndpoint");
							Value valueOfVocabularyRepositoryImplProfile = bindingSet
									.getValue("VocabularyRepositoryImplProfile");
							Value valueOfVocabularyRepositoryImplQueryLimit = bindingSet
									.getValue("VocabularyRepositoryImplQueryLimit");
							Value valueOfWithRdfAnnotations = bindingSet.getValue("withRdfAnnotations");
							Value valueOfWithSapAnnotations = bindingSet.getValue("withSapAnnotations");
							//Create and add the corresponding repositories
							SPARQLRepositoryConfig dataRepositoryTypeSpec = new SPARQLRepositoryConfig();
							dataRepositoryTypeSpec.setQueryEndpointUrl(valueOfDataRepositoryImplQueryEndpoint
									.stringValue());
							dataRepositoryTypeSpec.setUpdateEndpointUrl(valueOfDataRepositoryImplUpdateEndpoint
									.stringValue());
							RepositoryConfig dataRepositoryConfig = new RepositoryConfig(
									valueOfDataRepositoryID.stringValue(), dataRepositoryTypeSpec);
							repositoryManager.addRepositoryConfig(dataRepositoryConfig);

							SPARQLRepositoryConfig vocabularyRepositoryTypeSpec = new SPARQLRepositoryConfig();
							vocabularyRepositoryTypeSpec
									.setQueryEndpointUrl(valueOfVocabularyRepositoryImplQueryEndpoint.stringValue());
							vocabularyRepositoryTypeSpec
									.setUpdateEndpointUrl(valueOfVocabularyRepositoryImplUpdateEndpoint.stringValue());
							RepositoryConfig vocabularyRepositoryConfig = new RepositoryConfig(
									valueOfVocabularyRepositoryID.stringValue(), vocabularyRepositoryTypeSpec);
							repositoryManager.addRepositoryConfig(vocabularyRepositoryConfig);

							Hashtable<String, Namespace> namespaces = readPrefixes(modelsConnection, valueOfDataset);
							Namespace defaultPrefix = null;
							try {
								if ((valueOfDefaultPrefix == null) || (valueOfDefaultNamespace == null)) {
									log.error("Null default prefix or namespace for "
											+ valueOfDataset
											+ ". Check repository or models.ttl for statements { odata4sparql:<prefix>  odata4sparql:defaultPrefix odata4sparql:<prefix> ;   odata4sparql:namespace <namespace> ; .. }");
									throw new RepositoryConfigException("Null default prefix or namespace for"
											+ valueOfDataset);
								} else {
									defaultPrefix = namespaces.get(valueOfDefaultPrefix.stringValue());
									if (defaultPrefix == null) {
										defaultPrefix = new NamespaceImpl(valueOfDefaultPrefix.stringValue(),
												valueOfDefaultNamespace.stringValue());
										namespaces.put(valueOfDefaultPrefix.stringValue(), defaultPrefix);
									}
								}
							} catch (NullPointerException e) {
								log.warn("Null default prefix", e);
							}
							RdfRepository repository = new RdfRepository(((URI) valueOfDataset).getLocalName(),
									defaultPrefix, namespaces);
							repository.setDataRepository(new RdfRoleRepository(repositoryManager
									.getRepository(valueOfDataRepositoryID.stringValue()), Integer
									.parseInt(valueOfDataRepositoryImplQueryLimit.stringValue()), SPARQLProfile
									.get(valueOfDataRepositoryImplProfile.stringValue())));
							repository.setModelRepository(new RdfRoleRepository(repositoryManager
									.getRepository(valueOfVocabularyRepositoryID.stringValue()), Integer
									.parseInt(valueOfVocabularyRepositoryImplQueryLimit.stringValue()), SPARQLProfile
									.get(valueOfVocabularyRepositoryImplProfile.stringValue())));

							if (valueOfWithRdfAnnotations != null) {
								repository.setWithRdfAnnotations(Boolean.parseBoolean(valueOfWithRdfAnnotations
										.stringValue()));
							} else {
								repository.setWithRdfAnnotations(false);
							}
							if (valueOfWithSapAnnotations != null) {
								repository.setWithSapAnnotations(Boolean.parseBoolean(valueOfWithSapAnnotations
										.stringValue()));
							} else {
								repository.setWithSapAnnotations(false);
							}
							rdfRepositoryList.put(((URI) valueOfDataset).getLocalName(), repository);

						} catch (RepositoryConfigException e) {
							log.warn("Failed to complete definition of dataset");
						}
					}
				} finally {
					result.close();
				}

			} catch (MalformedQueryException e) {
				log.fatal("MalformedQuery " + RdfConstants.getMetaQueries().get(RdfConstants.URI_REPOSITORYQUERY), e);
				throw new OData2SparqlException();
			} catch (QueryEvaluationException e) {
				log.fatal(
						"Query Evaluation Exception "
								+ RdfConstants.getMetaQueries().get(RdfConstants.URI_REPOSITORYQUERY), e);
				throw new OData2SparqlException();
			} finally {
				modelsConnection.close();
			}
		} catch (RepositoryException e) {
			log.fatal("Cannot get connection to repository: check directory", e);
			throw new OData2SparqlException();
		} finally {
			//Cannot shutdown repositoryManager at this stage as it will terminate the connections to the individual repositories
			//repositoryManager.shutDown();
		}
	}

	private boolean loadProperties() {
		InputStream input = null;
		try {

			input = new FileInputStream(RdfConstants.repositoryManagerDir + "/config.properties");

			// load a properties file
			properties.load(input);

			// get the property value and print it out
			log.info("repositoryUrl: " + properties.getProperty("repositoryUrl"));

		} catch (IOException ex) {
			return false;
		} finally {
			if (input != null) {
				try {
					input.close();
					return true;
				} catch (IOException e) {
					e.printStackTrace();
					return false;
				}
			}
		}
		return false;
	}

	private RepositoryManager bootstrapRemoteRepository(String repositoryUrl) throws OData2SparqlException {
		RemoteRepositoryManager repositoryManager = new RemoteRepositoryManager(repositoryUrl);
		log.info("Repository loading from " + repositoryUrl);
		try {
			repositoryManager.initialize();
			//Make sure we can find the bootstrap repository
			repositoryManager.getRepositoryInfo(RdfConstants.systemId);
		} catch (RepositoryException e) {
			log.warn("Cannot initialize remote repository manager at " + repositoryUrl
					+ ". Will use local repository instead", null);
			throw new OData2SparqlException("RdfRepositories bootstrapRemoteRepository failure", null);
		}
		return repositoryManager;
	}

	private RepositoryManager bootstrapLocalRepository() throws OData2SparqlException {
		//Create a local repository manager for managing all of the endpoints including the model itself
		LocalRepositoryManager repositoryManager = new LocalRepositoryManager(RdfConstants.repositoryManagerDir);
		log.info("Repository loaded from " + RdfConstants.repositoryManagerDir.toString());
		try {
			repositoryManager.initialize();
		} catch (RepositoryException e) {
			log.fatal("Cannot initialize repository manager at " + RdfConstants.repositoryManagerDir.toString(), e);
			throw new OData2SparqlException("RdfRepositories loadRepositories failure", e);
		}

		//Create a configuration for the system repository implementation which is a native store

		SailImplConfig systemRepositoryImplConfig = new MemoryStoreConfig();
		RepositoryImplConfig systemRepositoryTypeSpec = new SailRepositoryConfig(systemRepositoryImplConfig);
		RepositoryConfig systemRepositoryConfig = new RepositoryConfig(RdfConstants.systemId, systemRepositoryTypeSpec);
		try {
			repositoryManager.addRepositoryConfig(systemRepositoryConfig);
		} catch (RepositoryException e) {
			log.fatal("Cannot add configuration to repository", e);
			throw new OData2SparqlException();
		} catch (RepositoryConfigException e) {
			log.fatal("Cannot add configuration to repository", e);
			throw new OData2SparqlException();
		}

		Repository systemRepository = null;

		try {
			systemRepository = repositoryManager.getRepository(RdfConstants.systemId);
		} catch (RepositoryConfigException e) {
			log.fatal("Cannot find " + RdfConstants.systemId + " repository", e);
			throw new OData2SparqlException();
		} catch (RepositoryException e) {
			log.fatal("Cannot find " + RdfConstants.systemId + " repository", e);
			throw new OData2SparqlException();
		}

		RepositoryConnection modelsConnection;
		try {
			modelsConnection = systemRepository.getConnection();

			try {
				modelsConnection.add(new File(RdfConstants.modelFile), null, null);
			} catch (RDFParseException e) {
				log.fatal("Cannot parse  " + RdfConstants.modelFile + " Check to ensure valid RDF/XML or TTL", e);
				System.exit(1);
				//throw new Olingo2SparqlException();
			} catch (IOException e) {
				log.fatal("Cannot access " + RdfConstants.modelFile + " Check it is located in WEBINF/classes/", e);
				System.exit(1);
				//throw new Olingo2SparqlException();
			} finally {

			}
			try {
				modelsConnection.add(new File(RdfConstants.odata4sparqlFile), null, null);
			} catch (RDFParseException e) {
				log.fatal("Cannot parse " + RdfConstants.odata4sparqlFile, e);
				throw new OData2SparqlException();
			} catch (IOException e) {
				log.fatal("Cannot access " + RdfConstants.odata4sparqlFile, e);
				throw new OData2SparqlException();
			} finally {

			}
		} catch (RepositoryException e) {
			log.fatal("Cannot connect to local system repository", e);
			throw new OData2SparqlException();
		}
		return repositoryManager;

	}

	private Hashtable<String, Namespace> readPrefixes(RepositoryConnection modelsConnection, Value valueOfDataset)
			throws OData2SparqlException {
		Hashtable<String, Namespace> namespaces = new Hashtable<String, Namespace>();
		try {
			//Identify prefixes for the provided dataset:
			String queryString = RdfConstants.getMetaQueries().get(RdfConstants.URI_PREFIXQUERY);
			TupleQuery tupleQuery = modelsConnection.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
			tupleQuery.setBinding("Dataset", valueOfDataset);
			TupleQueryResult result = tupleQuery.evaluate();
			try {
				while (result.hasNext()) { // iterate over the result
					BindingSet bindingSet = result.next();
					Value valueOfPrefix = bindingSet.getValue("prefix");
					Value valueOfNamespace = bindingSet.getValue("namespace");
					namespaces.put(valueOfPrefix.stringValue(), new NamespaceImpl(valueOfPrefix.stringValue(),
							valueOfNamespace.stringValue()));
				}
			} finally {
				result.close();
			}

		} catch (MalformedQueryException e) {
			log.fatal("MalformedQuery " + RdfConstants.getMetaQueries().get(RdfConstants.URI_PREFIXQUERY), e);
			throw new OData2SparqlException("RdfRepositories readPrefixes failure", e);
		} catch (RepositoryException e) {
			log.fatal("RepositoryException " + RdfConstants.getMetaQueries().get(RdfConstants.URI_PREFIXQUERY), e);
			throw new OData2SparqlException("RdfRepositories readPrefixes failure", e);
		} catch (QueryEvaluationException e) {
			log.fatal("QueryEvaluationException " + RdfConstants.getMetaQueries().get(RdfConstants.URI_PREFIXQUERY), e);
			throw new OData2SparqlException("RdfRepositories readPrefixes failure", e);
		} finally {
		}
		return namespaces;

	}

	private void readQueries(RepositoryConnection modelsConnection, Value RDFSModel) throws OData2SparqlException {
		Hashtable<Value, String> queries = RdfConstants.getMetaQueries();// new Hashtable<Value, String>();
		//Bootstrap the standard queries
		try {
			TupleQuery tupleQuery = modelsConnection.prepareTupleQuery(QueryLanguage.SPARQL,
					RdfConstants.bootStrapQuery);
			tupleQuery.setBinding("Metadata", RDFSModel);
			TupleQueryResult result = tupleQuery.evaluate();
			try {
				while (result.hasNext()) {
					BindingSet bindingSet = result.next();
					Value valueOfQuery = bindingSet.getValue("Query");
					Value valueOfQueryString = bindingSet.getValue("QueryString");
					queries.put(valueOfQuery, valueOfQueryString.stringValue());
				}
			} finally {
				result.close();
			}
		} catch (MalformedQueryException e) {
			log.fatal("Malformed Bootstrap Query ", e);
			throw new OData2SparqlException("RdfRepositories readQueries failure", e);
		} catch (RepositoryException e) {
			log.fatal("RepositoryException Bootstrap Query  ", e);
			throw new OData2SparqlException("RdfRepositories readQueries failure", e);
		} catch (QueryEvaluationException e) {
			log.fatal("QueryEvaluationException Bootstrap Query ", e);
			throw new OData2SparqlException("RdfRepositories readQueries failure", e);
		} finally {
		}
	}
}
