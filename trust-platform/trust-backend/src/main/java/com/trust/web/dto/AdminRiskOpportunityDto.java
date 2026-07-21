package com.trust.web.dto;

public record AdminRiskOpportunityDto(
        int openRisksCount,
        double openRisksValue,
        int openOpportunitiesCount,
        double openOpportunitiesValue
) {}
