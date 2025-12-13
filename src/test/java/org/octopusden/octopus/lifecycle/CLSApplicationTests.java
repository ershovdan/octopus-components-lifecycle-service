package org.octopusden.octopus.lifecycle;

import org.octopusden.octopus.lifecycle.api.ApiController;
import org.octopusden.octopus.lifecycle.api.entities.Build;
import org.octopusden.octopus.lifecycle.api.entities.Component;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.octopusden.octopus.lifecycle.db.Rule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CLSApplicationTests {
	@Autowired
	private ApiController apiController;

	@BeforeAll
    public void beforeAll() {

	}

	@Test
	public void testGetComponents() throws IOException {
		List<String> components = apiController.getComponents(Optional.empty(), Optional.empty());

        assertEquals(2, components.size());
		assertTrue(components.contains("component1"));
		assertTrue(components.contains("component2"));

		components = apiController.getComponents(Optional.empty(), Optional.of("abiba"));
		assertTrue(components.contains("component1"));
		assertFalse(components.contains("component2"));
	}

	@Test
	public void testGetComponent() throws IOException {
		Component component = apiController.getComponent("component1");

		assertEquals("component1", component.id);
		assertEquals("abiba", component.componentOwner);
	}

	@Test
	public void testGetBuilds() throws IOException {
		List<String> builds = apiController.getBuildsByComponent("component1").stream().map(b -> b.id).collect(Collectors.toList());

		assertEquals(3, builds.size());
		assertTrue(builds.contains("comp1-1.0.551"));
		assertTrue(builds.contains("comp1-1.0.552"));
		assertTrue(builds.contains("comp1-1.0.553"));
	}

	@Test
	public void testGetVersions() throws IOException {
		List<String> versions = apiController.getVersionsByComponent("component1");

		assertEquals(3, versions.size());
		assertTrue(versions.contains("1.0.551"));
		assertTrue(versions.contains("1.0.552"));
		assertTrue(versions.contains("1.0.553"));
	}

	@Test
	public void testGetBuild() throws IOException {
		Build build = apiController.getBuild("component1", "comp1-1.0.552");

        assertEquals("comp1-1.0.552", build.id);
	}

	@Test
	public void testGetVersion() {
		String version = apiController.getVersion("component1", "comp1-1.0.552");

		assertEquals("1.0.552", version.substring(1, version.length() - 1));
	}


	@Test
	public void testRuleOperations() throws IOException {
		List<Rule> rules = apiController.getRules(Optional.of("component1"));

		assertEquals(0, rules.size());

		apiController.addRule("component1", "test-rule1", "component", "active",
                Optional.of("rel"), Optional.empty(), Optional.empty(), Optional.of("9999999"),
				Optional.empty());

		rules = apiController.getRules(Optional.of("component1"));

		assertEquals(1, rules.size());
		assertEquals("test-rule1", apiController.getRule("test-rule1").name);
		assertEquals(Optional.of(9999999), Optional.ofNullable(rules.getFirst().timeGap));

		apiController.changeRule("test-rule1", Optional.of("test-rule1-newName"),
				Optional.empty(), Optional.empty(), Optional.empty(),
				Optional.empty(), Optional.of("100"), Optional.empty());
		rules = apiController.getRules(Optional.of("component1"));

		assertEquals(1, rules.size());
		assertEquals("test-rule1-newName", apiController.getRule("test-rule1-newName").name);
		assertEquals(Optional.of(100), Optional.ofNullable(rules.getFirst().timeGap));

		apiController.deleteRule("test-rule1-newName", "component1");
		rules = apiController.getRules(Optional.of("component1"));

		assertEquals(0, rules.size());
	}

	@Test
	public void testLifecycleStages() throws IOException, InvalidVersionSpecificationException {
		assertEquals("\"" + "unsupported" + "\"", apiController.getLifecycleStage("component1", "1.0.551"));
		assertEquals("\"" + "unsupported" + "\"", apiController.getLifecycleStage("component1", "1.0.552"));
		assertEquals("\"" + "unsupported" + "\"", apiController.getLifecycleStage("component1", "1.0.553"));

		apiController.addRule("component1", "test-rule1", "component", "active",
				Optional.of("rel"), Optional.empty(), Optional.empty(), Optional.of("9999999"),
				Optional.empty());


		assertEquals("\"" + "active" + "\"", apiController.getLifecycleStage("component1", "1.0.551"));
		assertEquals("\"" + "active" + "\"", apiController.getLifecycleStage("component1", "1.0.552"));
		assertEquals("\"" + "active" + "\"", apiController.getLifecycleStage("component1", "1.0.553"));

		apiController.deleteRule("test-rule1", "component1");

		assertEquals("\"" + "unsupported" + "\"", apiController.getLifecycleStage("component1", "1.0.551"));
		assertEquals("\"" + "unsupported" + "\"", apiController.getLifecycleStage("component1", "1.0.552"));
		assertEquals("\"" + "unsupported" + "\"", apiController.getLifecycleStage("component1", "1.0.553"));

		apiController.addRule("component1", "test-rule1", "component", "maintenance",
				Optional.of("null"), Optional.empty(), Optional.empty(), Optional.empty(),
				Optional.of("[1.0.552,)"));

		assertEquals("\"" + "unsupported" + "\"", apiController.getLifecycleStage("component1", "1.0.551"));
		assertEquals("\"" + "maintenance" + "\"", apiController.getLifecycleStage("component1", "1.0.552"));
		assertEquals("\"" + "maintenance" + "\"", apiController.getLifecycleStage("component1", "1.0.553"));

		apiController.deleteRule("test-rule1", "component1");

		assertEquals("\"" + "unsupported" + "\"", apiController.getLifecycleStage("component1", "1.0.551"));
		assertEquals("\"" + "unsupported" + "\"", apiController.getLifecycleStage("component1", "1.0.552"));
		assertEquals("\"" + "unsupported" + "\"", apiController.getLifecycleStage("component1", "1.0.553"));

		apiController.addRule("component1", "test-rule1", "component", "maintenance",
				Optional.of("abs"), Optional.of("2025-09-19"), Optional.of("2025-09-21"), Optional.empty(),
				Optional.empty());

		assertEquals("\"" + "unsupported" + "\"", apiController.getLifecycleStage("component1", "1.0.551"));
		assertEquals("\"" + "maintenance" + "\"", apiController.getLifecycleStage("component1", "1.0.552"));
		assertEquals("\"" + "unsupported" + "\"", apiController.getLifecycleStage("component1", "1.0.553"));

		apiController.deleteRule("test-rule1", "component1");
	}

	@Test
	public void testSetRuleOrder() throws IOException, InvalidVersionSpecificationException {
		assertEquals("\"" + "unsupported" + "\"", apiController.getLifecycleStage("component1", "1.0.551"));
		assertEquals("\"" + "unsupported" + "\"", apiController.getLifecycleStage("component1", "1.0.552"));
		assertEquals("\"" + "unsupported" + "\"", apiController.getLifecycleStage("component1", "1.0.553"));

		apiController.addRule("component1", "test-rule1", "component", "maintenance",
				Optional.of("abs"), Optional.of("2025-09-19"), Optional.of("2025-09-21"), Optional.empty(),
				Optional.empty());

		apiController.addRule("component1", "test-rule2", "component", "active",
				Optional.of("null"), Optional.empty(), Optional.empty(), Optional.empty(),
				Optional.of("[1.0.552,)"));


		assertEquals("\"" + "unsupported" + "\"", apiController.getLifecycleStage("component1", "1.0.551"));
		assertEquals("\"" + "active" + "\"", apiController.getLifecycleStage("component1", "1.0.552"));
		assertEquals("\"" + "active" + "\"", apiController.getLifecycleStage("component1", "1.0.553"));

		apiController.setRuleOrder("component1", "test-rule1,test-rule2");

		assertEquals("\"" + "unsupported" + "\"", apiController.getLifecycleStage("component1", "1.0.551"));
		assertEquals("\"" + "maintenance" + "\"", apiController.getLifecycleStage("component1", "1.0.552"));
		assertEquals("\"" + "active" + "\"", apiController.getLifecycleStage("component1", "1.0.553"));

		apiController.deleteRule("test-rule1", "component1");
		apiController.deleteRule("test-rule2", "component1");
	}

}
