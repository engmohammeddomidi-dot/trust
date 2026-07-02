package com.trust.web;

import com.trust.config.AuthenticatedUser;
import com.trust.config.TenantAccessGuard;
import com.trust.service.ItemService;
import com.trust.web.dto.ItemCreateRequest;
import com.trust.web.dto.ItemDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService itemService;
    private final TenantAccessGuard accessGuard;

    public ItemController(ItemService itemService, TenantAccessGuard accessGuard) {
        this.itemService = itemService;
        this.accessGuard = accessGuard;
    }

    @GetMapping
    public List<ItemDto> list(@RequestParam Long branchId, @AuthenticationPrincipal AuthenticatedUser principal) {
        accessGuard.requireBranch(principal, branchId);
        return itemService.listByBranch(branchId);
    }

    @GetMapping("/needing-attention")
    public List<ItemDto> needingAttention(@RequestParam Long branchId, @AuthenticationPrincipal AuthenticatedUser principal) {
        accessGuard.requireBranch(principal, branchId);
        return itemService.listNeedingAttention(branchId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemDto create(@Valid @RequestBody ItemCreateRequest request, @AuthenticationPrincipal AuthenticatedUser principal) {
        accessGuard.requireBranch(principal, request.branchId());
        return itemService.create(request);
    }
}
