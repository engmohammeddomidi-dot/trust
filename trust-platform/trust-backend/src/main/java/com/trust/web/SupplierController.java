package com.trust.web;

import com.trust.config.AuthenticatedUser;
import com.trust.config.TenantAccessGuard;
import com.trust.domain.Supplier;
import com.trust.service.SupplierService;
import com.trust.web.dto.SupplierCreateRequest;
import com.trust.web.dto.SupplierDto;
import com.trust.web.dto.SupplierUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierService supplierService;
    private final TenantAccessGuard accessGuard;

    public SupplierController(SupplierService supplierService, TenantAccessGuard accessGuard) {
        this.supplierService = supplierService;
        this.accessGuard = accessGuard;
    }

    @GetMapping
    public List<SupplierDto> list(@RequestParam Long organizationId, @AuthenticationPrincipal AuthenticatedUser principal) {
        accessGuard.requireOrganization(principal, organizationId);
        return supplierService.listByOrganization(organizationId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierDto create(@Valid @RequestBody SupplierCreateRequest request, @AuthenticationPrincipal AuthenticatedUser principal) {
        accessGuard.requireOrganization(principal, request.organizationId());
        return supplierService.create(request);
    }

    @PutMapping("/{id}")
    public SupplierDto update(@PathVariable Long id, @Valid @RequestBody SupplierUpdateRequest request,
                               @AuthenticationPrincipal AuthenticatedUser principal) {
        Supplier supplier = supplierService.requireById(id);
        accessGuard.requireOrganizationOwnership(principal, supplier.getOrganization());
        return supplierService.update(id, request);
    }
}
