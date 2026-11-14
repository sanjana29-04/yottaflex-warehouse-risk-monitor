package com.warehouse.riskmonitor;

import com.warehouse.riskmonitor.dto.ValidationResultDto;
import com.warehouse.riskmonitor.dto.ValidationSummaryDto;
import com.warehouse.riskmonitor.entity.Zone;
import com.warehouse.riskmonitor.generator.SyntheticDataGenerator;
import com.warehouse.riskmonitor.repository.ZoneRepository;
import com.warehouse.riskmonitor.validation.ValidationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class DataValidationTest {

    @Autowired
    private SyntheticDataGenerator generator;

    @Autowired
    private ValidationService validationService;

    @Autowired
    private ZoneRepository zoneRepository;

    @Test
    @DisplayName("Verify full validation suite passes 100% on valid generated data")
    public void testFullValidationSuitePassesOnCleanData() {
        // Generate clean sample dataset in DB
        generator.generate("sample", 20261022L, true);

        // Run validation suite
        ValidationSummaryDto summary = validationService.executeFullValidation();

        assertNotNull(summary);
        assertEquals(50, summary.getTotalRulesRun(), "Must run all 50 validation checks");
        assertEquals(50, summary.getPassedRules(), "All rules should pass on valid clean dataset");
        assertEquals(0, summary.getFailedRules(), "Zero rules should fail on clean data");
        assertTrue(summary.isFullyValid());
    }

    @Test
    @DisplayName("Verify validation detects corrupt data and marks rule as FAILED")
    public void testValidationFailsWhenConstraintViolated() {
        // Generate clean sample dataset in DB
        generator.generate("sample", 20261022L, true);

        // Invert temperature thresholds on an existing zone (min 25.0 >= max 10.0)
        Zone zone = zoneRepository.findAll().get(0);
        zone.setTemperatureMin(25.0);
        zone.setTemperatureMax(10.0);
        zoneRepository.save(zone);

        // Run validation suite
        ValidationSummaryDto summary = validationService.executeFullValidation();

        assertNotNull(summary);
        assertFalse(summary.isFullyValid(), "Suite must fail when corrupt data is present");
        assertTrue(summary.getFailedRules() > 0, "At least one rule must fail");

        // Locate Zone Temperature Threshold Boundaries rule
        Optional<ValidationResultDto> tempRule = summary.getResults().stream()
                .filter(r -> r.getRuleName().contains("Temperature Threshold Boundaries"))
                .findFirst();

        assertTrue(tempRule.isPresent());
        assertEquals("FAILED", tempRule.get().getStatus(), "Temperature threshold rule must report FAILED status");
        assertTrue(tempRule.get().getViolationsCount() > 0);
        assertNotNull(tempRule.get().getDetails());
    }
}
