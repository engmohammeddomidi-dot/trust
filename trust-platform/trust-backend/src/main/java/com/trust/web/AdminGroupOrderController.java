package com.trust.web;

import com.trust.domain.GroupOrder;
import com.trust.repository.GroupOrderParticipantRepository;
import com.trust.repository.GroupOrderRepository;
import com.trust.web.dto.CreateGroupOrderRequest;
import com.trust.web.dto.GroupOrderDto;
import com.trust.web.dto.NegotiateGroupOrderRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** إدارة الطلبات الجماعية من منظور الأدمن - القسم 8.3 من خطة MVP */
@RestController
@RequestMapping("/api/admin/group-orders")
public class AdminGroupOrderController {

    private final GroupOrderRepository groupOrderRepository;
    private final GroupOrderParticipantRepository participantRepository;

    public AdminGroupOrderController(GroupOrderRepository groupOrderRepository,
                                      GroupOrderParticipantRepository participantRepository) {
        this.groupOrderRepository = groupOrderRepository;
        this.participantRepository = participantRepository;
    }

    @GetMapping
    public List<GroupOrderDto> list() {
        return groupOrderRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toDto).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupOrderDto create(@Valid @RequestBody CreateGroupOrderRequest request) {
        GroupOrder order = new GroupOrder();
        order.setItemName(request.itemName());
        order.setTargetQuantity(request.targetQuantity());
        order.setEstimatedMarketPrice(request.estimatedMarketPrice());
        return toDto(groupOrderRepository.save(order));
    }

    @PatchMapping("/{id}/negotiate")
    public GroupOrderDto negotiate(@PathVariable Long id, @Valid @RequestBody NegotiateGroupOrderRequest request) {
        GroupOrder order = groupOrderRepository.findById(id).orElseThrow();
        order.setNegotiatedPrice(request.negotiatedPrice());
        order.setStatus(GroupOrder.Status.NEGOTIATED);
        return toDto(groupOrderRepository.save(order));
    }

    @PatchMapping("/{id}/distribute")
    public GroupOrderDto distribute(@PathVariable Long id) {
        GroupOrder order = groupOrderRepository.findById(id).orElseThrow();
        order.setStatus(GroupOrder.Status.DISTRIBUTED);
        return toDto(groupOrderRepository.save(order));
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
