package com.trust.web.dto;

import java.util.List;
import java.util.Map;

public record AdminOverviewDto(
        int totalOrganizations,
        int totalBranches,
        double avgHealthScore,
        double totalStagnantValue,
        double totalSalesToday,
        Map<String, Long> organizationsByCategory,
        List<AdminPlatformTrendPointDto> salesTrend,
        List<AdminCityBreakdownDto> cityBreakdown
) {}
