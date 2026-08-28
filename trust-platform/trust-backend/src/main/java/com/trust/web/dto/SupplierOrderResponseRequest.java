package com.trust.web.dto;

/** ردّ المورّد: تاريخ التزام عند القبول، أو سبب عند الاعتذار */
public record SupplierOrderResponseRequest(String promisedDate, String reason) {}
