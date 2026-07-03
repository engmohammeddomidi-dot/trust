package com.trust.web;

import com.trust.domain.GroupOrder;
import com.trust.domain.GroupOrderParticipant;
import com.trust.domain.Notification;
import com.trust.repository.GroupOrderParticipantRepository;
import com.trust.repository.GroupOrderRepository;
import com.trust.service.AuditLogService;
import com.trust.service.NotificationService;
import com.trust.web.dto.CreateGroupOrderRequest;
import com.trust.web.dto.GroupOrderDto;
import com.trust.web.dto.NegotiateGroupOrderRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.trust.config.AuthenticatedUser;

/** إدارة الطلبات الجماعية من منظور الأدمن - القسم 8.3 من خطة MVP */
@RestController
@RequestMapping("/api/admin/group-orders")
public class AdminGroupOrderController {

    private final GroupOrderRepository groupOrderRepository;
    private final GroupOrderParticipantRepository participantRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public AdminGroupOrderController(GroupOrderRepository groupOrderRepository,
                                      GroupOrderParticipantRepository participantRepository,
                                      NotificationService notificationService,
                                      AuditLogService auditLogService) {
        this.groupOrderRepository = groupOrderRepository;
        this.participantRepository = participantRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public List<GroupOrderDto> list() {
        return groupOrderRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toDto).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupOrderDto create(@Valid @RequestBody CreateGroupOrderRequest request, @AuthenticationPrincipal AuthenticatedUser principal) {
        GroupOrder order = new GroupOrder();
        order.setItemName(request.itemName());
        order.setTargetQuantity(request.targetQuantity());
        order.setEstimatedMarketPrice(request.estimatedMarketPrice());
        GroupOrder saved = groupOrderRepository.save(order);
        auditLogService.record(null, principal.email(), "CREATE_GROUP_ORDER", "GroupOrder", saved.getId().toString(),
                "itemName=" + saved.getItemName() + ", targetQuantity=" + saved.getTargetQuantity());
        return toDto(saved);
    }

    @PatchMapping("/{id}/negotiate")
    public GroupOrderDto negotiate(@PathVariable Long id, @Valid @RequestBody NegotiateGroupOrderRequest request,
                                    @AuthenticationPrincipal AuthenticatedUser principal) {
        GroupOrder order = groupOrderRepository.findById(id).orElseThrow();
        order.setNegotiatedPrice(request.negotiatedPrice());
        order.setStatus(GroupOrder.Status.NEGOTIATED);
        GroupOrder saved = groupOrderRepository.save(order);
        auditLogService.record(null, principal.email(), "NEGOTIATE_GROUP_ORDER", "GroupOrder", saved.getId().toString(),
                "negotiatedPrice=" + saved.getNegotiatedPrice());
        notifyParticipants(saved, "تم التفاوض على طلبكم الجماعي",
                "تم تحديد سعر الجملة لـ" + saved.getItemName() + ": " + saved.getNegotiatedPrice() + " شيكل — راجعوا صفحة الموردين لمعرفة توفيركم");
        return toDto(saved);
    }

    @PatchMapping("/{id}/distribute")
    public GroupOrderDto distribute(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser principal) {
        GroupOrder order = groupOrderRepository.findById(id).orElseThrow();
        order.setStatus(GroupOrder.Status.DISTRIBUTED);
        GroupOrder saved = groupOrderRepository.save(order);
        auditLogService.record(null, principal.email(), "DISTRIBUTE_GROUP_ORDER", "GroupOrder", saved.getId().toString(), null);
        notifyParticipants(saved, "تم توزيع طلبكم الجماعي",
                "تم توزيع " + saved.getItemName() + " على المؤسسات المشاركة");
        return toDto(saved);
    }

    private void notifyParticipants(GroupOrder order, String title, String message) {
        for (GroupOrderParticipant p : participantRepository.findByGroupOrderId(order.getId())) {
            notificationService.notify(p.getOrganization().getId(), title, message, Notification.Severity.SUCCESS);
        }
    }

    private GroupOrderDto toDto(GroupOrder order) {
        int participantCount = participantRepository.findByGroupOrderId(order.getId()).size();
        return new GroupOrderDto(
                order.getId(), order.getItemName(), order.getTargetQuantity(), order.getCurrentQuantity(),
                order.getEstimatedMarketPrice(), order.getNegotiatedPrice(), order.getStatus().name(),
                participantCount, order.getCreatedAt()
        );
    }
}
