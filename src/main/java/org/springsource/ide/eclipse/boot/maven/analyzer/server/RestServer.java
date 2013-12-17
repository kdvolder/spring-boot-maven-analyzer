package org.springsource.ide.eclipse.boot.maven.analyzer.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Spring Boot Application to serve serve jar-type content assist type graph
 * via a rest end point.
 */
@ComponentScan
@EnableAutoConfiguration
public class RestServer {

    public static void main(String[] args) {
        SpringApplication.run(RestServer.class, args);
    }
}
