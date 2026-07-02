package com.trust.web.dto;

public record BenchmarkDto(
        double targetMarginPercent,
        double marginRangeLow,
        double marginRangeHigh,
        double liquidityRatioMin,
        double liquidityRatioMax
) {}
