package org.springsource.ide.eclipse.boot.maven.analyzer.server;

import java.util.Arrays;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.cache.concurrent.ConcurrentMapCache;

/**
 * Spring Boot Application to serve serve jar-type content assist type graph
 * via a rest end point.
 */
@ComponentScan
@EnableAutoConfiguration
@EnableCaching
public class RestServer {

    public static void main(String[] args) {
        SpringApplication.run(RestServer.class, args);
    }
    
    @Bean
    public CacheManager cacheManager() {
    	SimpleCacheManager cm = new SimpleCacheManager();
    	cm.setCaches(Arrays.asList(
    			new ConcurrentMapCache("default")
    	));
    	return cm;
    }    
}
