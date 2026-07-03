package com.trust.service;

import com.trust.domain.Organization;
import com.trust.domain.Policy;
import com.trust.repository.OrganizationRepository;
import com.trust.repository.PolicyRepository;
import org.springframework.stereotype.Service;

@Service
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final OrganizationRepository organizationRepository;

    public PolicyService(PolicyRepository policyRepository, OrganizationRepository organizationRepository) {
        this.policyRepository = policyRepository;
        this.organizationRepository = organizationRepository;
    }

    /** يعيد سياسة المؤسسة، أو قيمًا افتراضية معقولة إن لم تُخصَّص بعد (لا يُنشئ صفًا جديدًا تلقائيًا) */
    public Policy resolveForOrganization(Long organizationId) {
        return policyRepository.findByOrganizationId(organizationId).orElseGet(() -> {
            Policy defaults = new Policy();
            defaults.setMaxPurchaseLiquidityRatio(0.25);
            defaults.setMinSupplierRating(0.0);
            return defaults;
        });
    }

    public Policy upsert(Long organizationId, double maxPurchaseLiquidityRatio, double minSupplierRating) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("المؤسسة غير موجودة"));
        Policy policy = policyRepository.findByOrganizationId(organizationId).orElseGet(() -> {
            Policy p = new Policy();
            p.setOrganization(org);
            return p;
        });
        policy.setMaxPurchaseLiquidityRatio(maxPurchaseLiquidityRatio);
        policy.setMinSupplierRating(minSupplierRating);
        return policyRepository.save(policy);
    }
}
