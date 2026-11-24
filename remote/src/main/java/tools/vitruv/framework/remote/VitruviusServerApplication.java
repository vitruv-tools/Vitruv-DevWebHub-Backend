package tools.vitruv.framework.remote;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Spring Boot entry point of the Vitruvius server. */
@SpringBootApplication
public class VitruviusServerApplication {

  /** Starts the Spring Boot application. */
  public static void main(String[] args) {
    SpringApplication.run(VitruviusServerApplication.class, args);
  }
}
