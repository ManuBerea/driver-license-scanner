package com.dls.driverlicensescannerapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.dls.driverlicensescannerapi.config.ScanProperties;

@SpringBootApplication
@EnableConfigurationProperties(ScanProperties.class)
public class DriverLicenseScannerApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(DriverLicenseScannerApiApplication.class, args);
    }

}
