package com.keraune.vlvblueberrysystem.controller;

import com.keraune.vlvblueberrysystem.service.DashboardMetricsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final DashboardMetricsService dashboardMetricsService;

    public DashboardController(DashboardMetricsService dashboardMetricsService) {
        this.dashboardMetricsService = dashboardMetricsService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("summary", dashboardMetricsService.obtenerResumen());
        return "dashboard/index";
    }
}
