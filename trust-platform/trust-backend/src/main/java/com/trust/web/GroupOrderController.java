package com.trust.web;

import com.trust.config.AuthenticatedUser;
import com.trust.domain.GroupOrder;
import com.trust.domain.GroupOrderParticipant;
import com.trust.domain.Organization;
import com.trust.repository.GroupOrderParticipantRepository;
import com.trust.repository.GroupOrderRepository;
import com.trust.repository.OrganizationRepository;
import com.trust.web.dto.GroupOrderDto;
import com.trust.web.dto.GroupOrderParticipationDto;
import com.trust.web.dto.JoinGroupOrderRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * الطلبات الجماعية من الموردين من منظور المؤسسة (المشاركة فقط) - القسم 7.10 و 8.3
 * من خطة MVP. إنشاء الطلبات والتفاوض والتوزيع من صلاحيات الأدمن فقط (AdminGroupOrderController).
 */
@RestController
@RequestMapping("/api/group-orders")
public class GroupOrderController {

    private final GroupOrderRepository groupOrderRepository;
    private final GroupOrderParticipantRepository participantRepository;
    private final OrganizationRepository organizationRepository;

    public GroupOrderController(GroupOrderRepository groupOrderRepository,
                                 GroupOrderParticipantRepository participantRepository,
                                 OrganizationRepository organizationRepository) {
        this.groupOrderRepository = groupOrderRepository;
        this.participantRepository = participantRepository;
        this.organizationRepository = organizationRepository;
    }

    /** الطلبات الجماعية المفتوحة حاليًا وقابلة للانضمام إليها */
    @GetMapping("/open")
    public List<GroupOrderDto> listOpen() {
        return groupOrderRepository.findByStatusOrderByCreatedAtDesc(GroupOrder.Status.COLLECTING)
                .stream().map(this::toDto).toList();
    }

    @PostMapping("/{id}/join")
    public GroupOrderDto join(@PathVariable Long id, @Valid @RequestBody JoinGroupOrderRequest request,
                               @AuthenticationPrincipal AuthenticatedUser principal) {
        GroupOrder order = groupOrderRepository.findById(id).orElseThrow();
        if (order.getStatus() != GroupOrder.Status.COLLECTING) {
            throw new IllegalStateException("لا يمكن الانضمام لطلب تم إغلاقه بالفعل");
        }
        Organization org = organizationRepository.findById(principal.organizationId()).orElseThrow();

        GroupOrderParticipant participant = new GroupOrderParticipant();
        participant.setGroupOrder(order);
        participant.setOrganization(org);
        participant.setQuantity(request.quantity());
        participantRepository.save(participant);

        order.setCurrentQuantity(order.getCurrentQuantity() + request.quantity());
        return toDto(groupOrderRepository.save(order));
    }

    /** سجل مشاركات هذه المؤسسة في كل الطلبات الجماعية - لصفحة الموردون */
    @GetMapping("/my-participation")
    public List<GroupOrderParticipationDto> myParticipation(@AuthenticationPrincipal AuthenticatedUser principal) {
        return participantRepository.findByOrganizationIdOrderByIdDesc(principal.organizationId()).stream()
                .map(p -> {
                    GroupOrder order = p.getGroupOrder();
                    Double savings = order.getNegotiatedPrice() != null
                            ? (order.getEstimatedMarketPrice() - order.getNegotiatedPrice()) * p.getQuantity()
                            : null;
                    return new GroupOrderParticipationDto(
                            order.getId(), order.getItemName(), p.getQuantity(), order.getStatus().name(),
                            order.getEstimatedMarketPrice(), order.getNegotiatedPrice(), savings
                    );
                }).toList();
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
