package com.github.anno4j.schema_parsing.generation;

import com.github.anno4j.schema_parsing.building.OntGenerationConfig;
import com.github.anno4j.schema_parsing.validation.ValidatorChain;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Map;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * Test for {@link RDFSJavaFileGenerator}.
 * This test class utilizes compilation of generated Java Code at runtime
 * and thus the Java compiler (<code>javac</code>) and Maven (<code>mvn</code>)
 * must be available on the running system.<br>
 * <br>
 * <strong>This test is ignored by default.</strong>
 * This test is ignored by default when running multiple tests, because requirements
 * for it are not satisfied on continous integration server.
 * If you want to run this test either remove the <code>@Ignore</code> annotation
 * below or run it explicitly.
 */
public class RDFSJavaFileGeneratorTest {

    /**
     * Loader used for Java class compilation and loading.
     */
    private static JavaSourceFileLoader loader;

    /**
     * Temporary directory where generated .java files will be placed.
     * This directory is removed after the test ran.
     */
    private static File outputDir;

    /**
     * Temporary directory where compiled .class files will be placed.
     * This directory is removed after the test ran.
     */
    private static File workingDir;

    @Before
    public void setUp() throws Exception {
        // For this test Maven and the Java compiler are required.
        // Ignore this test if any of its requirements is not satisfied:
        boolean commandsFound = true;
        try {
            SystemCommand.runCommand("javac -help");
            SystemCommand.runCommand("mvn -help");

        } catch (Exception e) {
            commandsFound = false;
        }
        boolean mavenTargetExists = new File("target").isDirectory();
        assumeTrue(commandsFound && mavenTargetExists);

        // For the compilation of Anno4j resource classes, Anno4j must be provided as a dependency.
        // Build and package Anno4j as a single JAR if it does not exist yet:
        File dependencyJar = findDependencyJar();
        if(dependencyJar == null) {
            File anno4jRootPom = new File(System.getProperty("user.dir") + "/../pom.xml");
            assertTrue(anno4jRootPom.exists());
            SystemCommand.runCommand("mvn -f " + anno4jRootPom.getAbsolutePath() + " -Dmaven.test.skip=true package assembly:single");
        }

        // Add the Anno4j JAR to the loaders dependencies:
        loader = new JavaSourceFileLoader();
        dependencyJar = findDependencyJar();
        assertNotNull(dependencyJar);
        loader.addDependency(dependencyJar.getAbsolutePath());

        // Choose the temporary directories with some random name:
        outputDir = new File(getClass().getSimpleName() + "_output_" + System.currentTimeMillis());
        workingDir = new File(getClass().getSimpleName() + "_working_" + System.currentTimeMillis());
    }

    /**
     * Searches for a Anno4j JAR file with dependencies in the ./target directory.
     * @return The JAR file or null if it was not found.
     */
    private static File findDependencyJar() {
        File target = new File("target");
        File[] subFiles = target.listFiles();
        if(subFiles != null) {
            for (File subFile : subFiles) {
                if(subFile.getName().matches("anno4j-core-(.*)-jar-with-dependencies.jar")) {
                    return subFile;
                }
            }
        }
        return null;
    }

    /**
     * Recursively removes all files and directories contained in <code>dir</code>,
     * which is also removed.
     * @param dir The directory to empty.
     */
    private static void deleteRecursive(File dir) {
        File[] subFiles = dir.listFiles();
        if(subFiles != null) {
            for (File subFile : subFiles) {
                if(subFile.isFile()) {
                    subFile.delete();
                } else {
                    deleteRecursive(subFile);
                }
            }
        }
        dir.delete();
    }

    @After
    public void tearDown() throws Exception {
        // Remove the temporary directories:
        if(outputDir != null) {
            deleteRecursive(outputDir);
        }
        if(outputDir != null) {
            deleteRecursive(workingDir);
        }
    }

    @Test
    public void generateJavaFiles() throws Exception {
        // Get the vehicle ontology from test resources:
        URL vehicleOntUrl = getClass().getClassLoader().getResource("vehicle.rdf.xml");
        InputStream vehicleOntStream = new FileInputStream(vehicleOntUrl.getFile());

        // Setup the configuration:
        OntGenerationConfig config = new OntGenerationConfig();
        config.setIdentifierLanguagePreference(new String[] {"en"});
        config.setJavaDocLanguagePreference(new String[] {"en"});
        config.setValidators(ValidatorChain.getRDFSDefault());

        // Generate Java files from the ontology and store them in the output directory:
        JavaFileGenerator generator = new RDFSJavaFileGenerator();
        generator.addRDF(vehicleOntStream, "http://example.de/ont#");
        generator.generateJavaFiles(config, outputDir);

        // Compile and load the generated classes:
        Map<String, Class<?>> loadedClazzNames = loader.compileAndLoad(outputDir, workingDir);
        assertNotNull(loadedClazzNames);
        assertTrue(loadedClazzNames.containsKey("de.example.Vehicle"));
        assertTrue(loadedClazzNames.containsKey("de.example.Car"));
        assertTrue(loadedClazzNames.containsKey("de.example.Truck"));
        assertTrue(loadedClazzNames.containsKey("de.example.Home"));

        // Now load the actual test...
        // The actual test depends on the previously generated and loaded classes:
        loader.addDependency(workingDir.getAbsolutePath());
        // Compile and load runtime test:
        File runtimeTestsDir = new File(getClass().getClassLoader().getResource("runtime_tests").getFile());
        loadedClazzNames = loader.compileAndLoad(runtimeTestsDir, workingDir);
        assertTrue(loadedClazzNames.size() > 1);
        assertTrue(loadedClazzNames.containsKey("runtime.VehicleOntologyRuntimeTest"));
        assertTrue(loadedClazzNames.containsKey("runtime.VehicleSupportImpl"));

        // Run the actual test. It throws an exception on failure:
        Class<?> runtimeTestClass = loadedClazzNames.get("runtime.VehicleOntologyRuntimeTest");
        Object runtimeTest = runtimeTestClass.newInstance();
        Method testMethod = runtimeTestClass.getDeclaredMethod("run");
        try {
            testMethod.invoke(runtimeTest);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testInvalidOntology() throws Exception {
        JavaFileGenerator generator = new RDFSJavaFileGenerator();

        // Get the vehicle ontology from test resources:
        URL vehicleOntUrl = getClass().getClassLoader().getResource("vehicle.rdf.xml");
        InputStream vehicleOntStream = new FileInputStream(vehicleOntUrl.getFile());
        generator.addRDF(vehicleOntStream, "http://example.de/ont#");

        // Set range of ex:seat_num to something not numeric (violates range constraint):
        generator.addRDF(IOUtils.toInputStream("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
                "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n" +
                "         xmlns:ns0=\"http://example.de/ont#\">\n" +
                "  <rdf:Description rdf:about=\"http://example.de/ont#VWGolf\">\n" +
                "    <rdf:type rdf:resource=\"http://example.de/ont#Car\"/>\n" +
                "    <ns0:seat_num>Some non-numeric string</ns0:seat_num>\n" +
                "  </rdf:Description>\n" +
                "</rdf:RDF>"), "http://example.de/ont#");

        // Setup the configuration:
        OntGenerationConfig config = new OntGenerationConfig();
        config.setIdentifierLanguagePreference(new String[] {"en"});
        config.setJavaDocLanguagePreference(new String[] {"en"});
        config.setValidators(ValidatorChain.getRDFSDefault());

        boolean exceptionThrown = false;
        try {
            generator.generateJavaFiles(config, outputDir);
        } catch (JavaFileGenerator.InvalidOntologyException e) {
            exceptionThrown = true;
        }

        assertTrue(exceptionThrown);
    }
}