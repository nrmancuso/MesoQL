package com.mesoql;

import com.mesoql.config.MesoQLConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(MesoQLConfig.class)
public class MesoQLApplication {

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(
            SpringApplication.run(MesoQLApplication.class, args)
        ));
    }
}
