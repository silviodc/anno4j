package com.github.anno4j;

import com.github.anno4j.persistence.IDGenerator;
import com.github.anno4j.persistence.PersistenceService;
import com.github.anno4j.persistence.impl.IDGeneratorAnno4jURN;
import com.github.anno4j.querying.QueryService;
import com.github.anno4j.querying.evaluation.LDPathEvaluatorConfiguration;
import com.github.anno4j.querying.extension.QueryEvaluator;
import com.github.anno4j.querying.extension.annotation.Evaluator;
import org.apache.commons.lang3.ClassUtils;
import org.apache.marmotta.ldpath.api.functions.SelectorFunction;
import org.apache.marmotta.ldpath.api.functions.TestFunction;
import org.apache.marmotta.ldpath.api.selectors.NodeSelector;
import org.apache.marmotta.ldpath.api.tests.NodeTest;
import org.openrdf.model.URI;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * Read and write API for W3C Web Annotation Data Model (http://www.w3.org/TR/annotation-model/) and W3C Open Annotation Data Model (http://www.openannotation.org/spec/core/).
 * <p/>
 * <br/><br/>Anno4j can be configured by using the specific setter-methodes (e.g. setIdGenerator, setRepository). A default configuration (in-memory SPARQL endpoint) will be used if no configuration is set.
 * <p/>
 * <br/><br/> Usage: Anno4j implements a singelton pattern. The getInstance() methode can be called to get a Anno4j object.
 */
public class Anno4j {

    /**
     * Logger of this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(Anno4j.class);

    /**
     * Singleton instance of this class.
     */
    private static Anno4j instance = null;

    /**
     * Configured ID generator for Anno4j.
     */
    private IDGenerator idGenerator = new IDGeneratorAnno4jURN();

    /**
     * Configured openrdf/sesame repository for connecting a local/remote SPARQL endpoint.
     */
    private Repository repository;

    /**
     * Wrapper of the repository field for alibaba, will be updated if a new repository is set.
     */
    private ObjectRepository objectRepository;

    private LDPathEvaluatorConfiguration evaluatorConfiguration = new LDPathEvaluatorConfiguration();

    /**
     * Private constructor because of singleton pattern
     */
    private Anno4j() {
        // In-memory default configuration
        Repository sailRepository = new SailRepository(new MemoryStore());
        try {
            sailRepository.initialize();
            this.setRepository(sailRepository);
        } catch (RepositoryException e) {
            e.printStackTrace();
        } catch (RepositoryConfigException e) {
            e.printStackTrace();
        }

        scanForEvaluators();
    }

    private void scanForEvaluators() {
        Set<URL> classpath = new HashSet<>();
        classpath.addAll(ClasspathHelper.forClassLoader());
        classpath.addAll(ClasspathHelper.forJavaClassPath());
        classpath.addAll(ClasspathHelper.forManifest());
        classpath.addAll(ClasspathHelper.forPackage(""));

        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(classpath)
                .setScanners(new SubTypesScanner(), new TypeAnnotationsScanner()));

        Set<Class<?>> defaultEvaluatorAnnotations = reflections.getTypesAnnotatedWith(Evaluator.class, true);

        Map<Class<? extends TestFunction>, Class<QueryEvaluator>> testFunctionEvaluators = new HashMap<>();
        Map<Class<? extends NodeSelector>, Class<QueryEvaluator>> defaultEvaluators = new HashMap<>();
        Map<Class<? extends NodeTest>, Class<QueryEvaluator>> testEvaluators = new HashMap<>();
        Map<Class<? extends SelectorFunction>, Class<QueryEvaluator>> functionEvaluators = new HashMap<>();

        for (Class clazz : defaultEvaluatorAnnotations) {
            Evaluator evaluator = (Evaluator) clazz.getAnnotation(Evaluator.class);

            if (ClassUtils.isAssignable(evaluator.value(), TestFunction.class)) {
                logger.debug("Found evaluator {} for TestFunction {}", clazz.getCanonicalName(), evaluator.value().getCanonicalName());
                testFunctionEvaluators.put((Class<? extends TestFunction>) evaluator.value(), clazz);
            } else if (ClassUtils.isAssignable(evaluator.value(), NodeTest.class)) {
                logger.debug("Found evaluator {} for NodeTest {}", clazz.getCanonicalName(), evaluator.value().getCanonicalName());
                testEvaluators.put((Class<? extends NodeTest>) evaluator.value(), clazz);
            } else if (ClassUtils.isAssignable(evaluator.value(), SelectorFunction.class)) {
                logger.debug("Found evaluator {} for NodeFunction {}", clazz.getCanonicalName(), evaluator.value().getCanonicalName());
                functionEvaluators.put((Class<? extends SelectorFunction>) evaluator.value(), clazz);
            } else {
                logger.debug("Found evaluator {} for NodeSelector {}", clazz.getCanonicalName(), evaluator.value().getCanonicalName());
                defaultEvaluators.put((Class<? extends NodeSelector>) evaluator.value(), clazz);
            }
        }

        evaluatorConfiguration.setDefaultEvaluators(defaultEvaluators);
        evaluatorConfiguration.setTestEvaluators(testEvaluators);
        evaluatorConfiguration.setTestFunctionEvaluators(testFunctionEvaluators);
        evaluatorConfiguration.setFunctionEvaluators(functionEvaluators);
    }

    /**
     * Create persistence object
     *
     * @return persistence object
     */
    public PersistenceService createPersistenceService() {
        return new PersistenceService(objectRepository);
    }

    /**
     * Create persistence object
     *
     * @param graph Graph context to query
     * @return persistence object
     */
    public PersistenceService createPersistenceService(URI graph) {
        return new PersistenceService(objectRepository, graph);
    }

    /**
     * Create query service
     *
     * @return query service object for specified type
     */
    public QueryService createQueryService() {
        return new QueryService(objectRepository, evaluatorConfiguration);
    }

    /**
     * Create query service
     *
     * @param graph Graph context to query
     * @return query service object for specified type
     */
    public QueryService createQueryService(URI graph) {
        return new QueryService(objectRepository, evaluatorConfiguration, graph);
    }

    /**
     * Getter for the configured IDGenerator intance.
     *
     * @return configured IDGenerator instance.
     */
    public IDGenerator getIdGenerator() {
        return idGenerator;
    }

    /**
     * Configures the IDGenerator to use in Anno4j.
     *
     * @param idGenerator IDGenerator to use in Anno4j.
     */
    public void setIdGenerator(IDGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    /**
     * Getter for the configured Repository instance (Connector for local/remote SPARQL repository).
     *
     * @return configured Repository instance
     */
    public Repository getRepository() {
        return repository;
    }

    /**
     * Configures the Repository (Connector for local/remote SPARQL repository) to use in Anno4j.
     *
     * @param repository Repository to use in Anno4j.
     * @throws RepositoryException
     * @throws RepositoryConfigException
     */
    public void setRepository(Repository repository) throws RepositoryException, RepositoryConfigException {
        this.repository = repository;
        // update alibaba wrapper
        this.objectRepository = new ObjectRepositoryFactory().createRepository(repository);
    }

    /**
     * Getter for configured ObjectRepository (openrdf/alibaba wrapper for the internal Repository).
     *
     * @return configured ObjectRepository.
     */
    public ObjectRepository getObjectRepository() {
        return objectRepository;
    }

    /**
     * Getter for the Anno4j getter instance.
     *
     * @return singleton Anno4j instance.
     */
    public static Anno4j getInstance() {
        if (instance == null) {
            synchronized (Anno4j.class) {
                if (instance == null) {
                    instance = new Anno4j();
                }
            }
        }
        return instance;
    }
}