package com.harddisk.module.dashboard.controller;

import com.harddisk.common.Result;
import com.harddisk.module.dashboard.dto.DashboardStats;
import com.harddisk.module.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public Result<DashboardStats> getStats() {
        return Result.success(dashboardService.getStats());
    }
}
