package com.warehouse.riskmonitor;

import com.warehouse.riskmonitor.generator.SyntheticDataGenerator;
import com.warehouse.riskmonitor.repository.WarehouseRepository;
import com.warehouse.riskmonitor.service.ExportService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@SpringBootApplication
public class WarehouseRiskMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(WarehouseRiskMonitorApplication.class, args);
    }

    @Bean
    @Profile("!test")
    public CommandLineRunner initDatabase(WarehouseRepository warehouseRepository,
                                         SyntheticDataGenerator generator,
                                         ExportService exportService,
                                         @Value("${app.dataset.scale:full}") String scale) {
        return args -> {
            long currentCount = warehouseRepository.count();
            int requiredWarehouses = "full".equalsIgnoreCase(scale) ? 10 : 4;
            if (currentCount < requiredWarehouses) {
                System.out.println("========================================================================");
                System.out.printf("   INITIALIZING YOTTAFLEX WAREHOUSE MONITORING SYSTEM WITH %s DATASET (SEED 20261022)%n", scale.toUpperCase());
                System.out.println("========================================================================");
                generator.generate(scale, 20261022L, true);
                java.io.File exportCsv = new java.io.File("data/csv/sensor_readings.csv");
                if (!exportCsv.exists() || exportCsv.length() < 1000) {
                    exportService.exportAllToDataDirectory();
                }
                System.out.println("   INITIALIZATION COMPLETE. READY FOR REAL-TIME MONITORING.");
                System.out.println("========================================================================");
            }
        };
    }
}
