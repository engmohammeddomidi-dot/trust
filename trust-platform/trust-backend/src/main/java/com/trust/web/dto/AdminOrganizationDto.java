package com.trust.web.dto;

import java.time.LocalDate;

public record AdminOrganizationDto(
        Long id,
        String name,
        String category,
        int branchCount,
        double avgHealthScore,
        LocalDate lastActivityDate
) {}
