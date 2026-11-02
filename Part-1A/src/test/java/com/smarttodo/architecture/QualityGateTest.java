package com.smarttodo.architecture;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.smarttodo.acceptance.TodoAcceptanceTest;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class QualityGateTest {

  @Test
  void nfr5_acceptanceTestsAreTraceableAndQualityGatesAreConfigured() throws IOException {
    Pattern requirement = Pattern.compile("^(fr|nfr)\\d+_.*$");
    for (Method method : TodoAcceptanceTest.class.getDeclaredMethods()) {
      if (method.isAnnotationPresent(Test.class)) {
        assertTrue(requirement.matcher(method.getName()).matches(), method.getName());
      }
    }

    String pom = Files.readString(Path.of("pom.xml"));
    assertTrue(pom.contains("junit-jupiter"));
    assertTrue(pom.contains("maven-checkstyle-plugin"));
    assertTrue(pom.contains("google_checks.xml"));
    assertTrue(pom.contains("com.github.spotbugs"));
    assertTrue(pom.contains("com.h3xstream.findsecbugs"));
    assertTrue(pom.contains("<failOnError>true</failOnError>"));
  }
}
