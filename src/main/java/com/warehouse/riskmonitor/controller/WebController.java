package com.warehouse.riskmonitor.controller;

import com.warehouse.riskmonitor.dto.DashboardStatsDto;
import com.warehouse.riskmonitor.dto.EvaluationMetricsDto;
import com.warehouse.riskmonitor.dto.ValidationSummaryDto;
import com.warehouse.riskmonitor.entity.Warehouse;
import com.warehouse.riskmonitor.entity.Zone;
import com.warehouse.riskmonitor.entity.enums.AlertStatus;
import com.warehouse.riskmonitor.entity.enums.DoorStatus;
import com.warehouse.riskmonitor.entity.enums.LotStatus;
import com.warehouse.riskmonitor.entity.enums.ProductCategory;
import com.warehouse.riskmonitor.entity.enums.ReadingStatus;
import com.warehouse.riskmonitor.entity.enums.RiskLevel;
import com.warehouse.riskmonitor.entity.enums.SensorStatus;
import com.warehouse.riskmonitor.entity.enums.SensorType;
import com.warehouse.riskmonitor.entity.enums.ZoneStatus;
import com.warehouse.riskmonitor.service.*;
import com.warehouse.riskmonitor.validation.ValidationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class WebController {

    private final DashboardService dashboardService;
    private final WarehouseService warehouseService;
    private final ZoneService zoneService;
    private final SensorService sensorService;
    private final TelemetryService telemetryService;
    private final DoorEventService doorEventService;
    private final InventoryLotService inventoryLotService;
    private final AlertService alertService;
    private final InspectionService inspectionService;
    private final RemediationService remediationService;
    private final ValidationService validationService;
    private final EvaluationService evaluationService;

    public WebController(DashboardService dashboardService,
                         WarehouseService warehouseService,
                         ZoneService zoneService,
                         SensorService sensorService,
                         TelemetryService telemetryService,
                         DoorEventService doorEventService,
                         InventoryLotService inventoryLotService,
                         AlertService alertService,
                         InspectionService inspectionService,
                         RemediationService remediationService,
                         ValidationService validationService,
                         EvaluationService evaluationService) {
        this.dashboardService = dashboardService;
        this.warehouseService = warehouseService;
        this.zoneService = zoneService;
        this.sensorService = sensorService;
        this.telemetryService = telemetryService;
        this.doorEventService = doorEventService;
        this.inventoryLotService = inventoryLotService;
        this.alertService = alertService;
        this.inspectionService = inspectionService;
        this.remediationService = remediationService;
        this.validationService = validationService;
        this.evaluationService = evaluationService;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        DashboardStatsDto stats = dashboardService.getDashboardStats();
        model.addAttribute("stats", stats);
        model.addAttribute("activeNav", "dashboard");
        return "dashboard";
    }

    @GetMapping("/warehouses")
    public String warehouses(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(required = false) String search,
                             Model model) {
        model.addAttribute("warehouses", warehouseService.getWarehousesPaged(search, PageRequest.of(page, 15)));
        model.addAttribute("search", search);
        model.addAttribute("activeNav", "warehouses");
        return "warehouses";
    }

    @GetMapping("/zones")
    public String zones(@RequestParam(defaultValue = "0") int page,
                        @RequestParam(required = false) String warehouseId,
                        @RequestParam(required = false) ZoneStatus status,
                        @RequestParam(required = false) String search,
                        Model model) {
        model.addAttribute("zones", zoneService.getZonesPaged(status, warehouseId, search, PageRequest.of(page, 15)));
        model.addAttribute("warehouses", warehouseService.getAllWarehouses());
        model.addAttribute("warehouseId", warehouseId);
        model.addAttribute("status", status);
        model.addAttribute("search", search);
        model.addAttribute("activeNav", "zones");
        return "zones";
    }

    @GetMapping("/sensors")
    public String sensors(@RequestParam(defaultValue = "0") int page,
                          @RequestParam(required = false) SensorType type,
                          @RequestParam(required = false) SensorStatus status,
                          @RequestParam(required = false) String zoneId,
                          Model model) {
        model.addAttribute("sensors", sensorService.getSensorsPaged(type, status, zoneId, PageRequest.of(page, 15)));
        model.addAttribute("zones", zoneService.getAllZones());
        model.addAttribute("type", type);
        model.addAttribute("status", status);
        model.addAttribute("zoneId", zoneId);
        model.addAttribute("activeNav", "sensors");
        return "sensors";
    }

    @GetMapping("/readings")
    public String readings(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(required = false) String zoneId,
                           @RequestParam(required = false) String sensorId,
                           @RequestParam(required = false) SensorType type,
                           @RequestParam(required = false) ReadingStatus status,
                           Model model) {
        model.addAttribute("readings", telemetryService.getReadingsPaged(zoneId, sensorId, type, status, PageRequest.of(page, 20)));
        model.addAttribute("zones", zoneService.getAllZones());
        model.addAttribute("zoneId", zoneId);
        model.addAttribute("sensorId", sensorId);
        model.addAttribute("type", type);
        model.addAttribute("status", status);
        model.addAttribute("activeNav", "readings");
        return "readings";
    }

    @GetMapping("/doors")
    public String doors(@RequestParam(defaultValue = "0") int page,
                        @RequestParam(required = false) String zoneId,
                        @RequestParam(required = false) DoorStatus status,
                        Model model) {
        model.addAttribute("doors", doorEventService.getDoorEventsPaged(zoneId, status, PageRequest.of(page, 20)));
        model.addAttribute("zones", zoneService.getAllZones());
        model.addAttribute("zoneId", zoneId);
        model.addAttribute("status", status);
        model.addAttribute("activeNav", "doors");
        return "doors";
    }

    @GetMapping("/inventory")
    public String inventory(@RequestParam(defaultValue = "0") int page,
                            @RequestParam(required = false) String zoneId,
                            @RequestParam(required = false) LotStatus status,
                            @RequestParam(required = false) ProductCategory category,
                            @RequestParam(required = false) String search,
                            Model model) {
        model.addAttribute("lots", inventoryLotService.getLotsPaged(zoneId, status, category, search, PageRequest.of(page, 15)));
        model.addAttribute("zones", zoneService.getAllZones());
        model.addAttribute("zoneId", zoneId);
        model.addAttribute("status", status);
        model.addAttribute("category", category);
        model.addAttribute("search", search);
        model.addAttribute("activeNav", "inventory");
        return "inventory";
    }

    @GetMapping("/alerts")
    public String alerts(@RequestParam(defaultValue = "0") int page,
                         @RequestParam(required = false) String zoneId,
                         @RequestParam(required = false) AlertStatus status,
                         @RequestParam(required = false) RiskLevel risk,
                         Model model) {
        model.addAttribute("alerts", alertService.getAlertsPaged(zoneId, status, risk, null, PageRequest.of(page, 15)));
        model.addAttribute("zones", zoneService.getAllZones());
        model.addAttribute("zoneId", zoneId);
        model.addAttribute("status", status);
        model.addAttribute("risk", risk);
        model.addAttribute("activeNav", "alerts");
        return "alerts";
    }

    @GetMapping("/inspections")
    public String inspections(@RequestParam(defaultValue = "0") int page,
                              Model model) {
        model.addAttribute("inspections", inspectionService.getInspectionsPaged(null, null, PageRequest.of(page, 15)));
        model.addAttribute("activeNav", "inspections");
        return "inspections";
    }

    @GetMapping("/remediations")
    public String remediations(@RequestParam(defaultValue = "0") int page,
                               Model model) {
        model.addAttribute("remediations", remediationService.getRemediationsPaged(null, null, PageRequest.of(page, 15)));
        model.addAttribute("activeNav", "remediations");
        return "remediations";
    }

    @GetMapping("/generator")
    public String generator(Model model) {
        model.addAttribute("activeNav", "generator");
        return "generator";
    }

    @GetMapping("/validation")
    public String validation(Model model) {
        ValidationSummaryDto summary = validationService.executeFullValidation();
        model.addAttribute("summary", summary);
        model.addAttribute("activeNav", "validation");
        return "validation";
    }

    @GetMapping("/evaluation")
    public String evaluation(Model model) {
        EvaluationMetricsDto metrics = evaluationService.evaluateAgainstGroundTruth();
        model.addAttribute("metrics", metrics);
        model.addAttribute("activeNav", "evaluation");
        return "evaluation";
    }

    @GetMapping("/export")
    public String export(Model model) {
        model.addAttribute("activeNav", "export");
        return "export";
    }
}

