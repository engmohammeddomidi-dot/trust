package com.trust.web.dto;

import java.util.Map;

public record AdminOverviewDto(
        int totalOrganizations,
        int totalBranches,
        double avgHealthScore,
        double totalStagnantValue,
        Map<String, Long> organizationsByCategory
) {}
