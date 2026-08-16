package mg.school.hei.conf;

import org.springframework.test.context.DynamicPropertyRegistry;

public class EnvConf {

  void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("JWT_SECRET", () -> "test-secret-key-only-for-integration-tests-32-chars-min");
  }
}
