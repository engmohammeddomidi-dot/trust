package com.trust.web.dto;

import java.util.List;

public record SupplierPortalOverviewDto(
        String supplierName,
        int organizationsServedCount,
        int openOrdersCount,
        double openOrdersValue,
        int receivedOrdersCount,
        double totalReceivedValue,
        Double avgRating,
        List<SupplierPortalPurchaseDto> recentOrders
) {}
