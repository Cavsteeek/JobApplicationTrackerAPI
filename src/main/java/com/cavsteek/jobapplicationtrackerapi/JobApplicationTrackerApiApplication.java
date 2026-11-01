package com.cavsteek.jobapplicationtrackerapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class JobApplicationTrackerApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobApplicationTrackerApiApplication.class, args);
    }

}
