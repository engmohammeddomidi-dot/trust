package com.trust.service;

import com.trust.domain.Organization;
import com.trust.domain.Supplier;
import com.trust.repository.OrganizationRepository;
import com.trust.repository.SupplierRepository;
import com.trust.web.dto.SupplierCreateRequest;
import com.trust.web.dto.SupplierDto;
import com.trust.web.dto.SupplierUpdateRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final OrganizationRepository organizationRepository;

    public SupplierService(SupplierRepository supplierRepository, OrganizationRepository organizationRepository) {
        this.supplierRepository = supplierRepository;
        this.organizationRepository = organizationRepository;
    }

    public SupplierDto create(SupplierCreateRequest req) {
        Organization org = organizationRepository.findById(req.organizationId())
                .orElseThrow(() -> new IllegalArgumentException("المؤسسة غير موجودة"));

        Supplier supplier = new Supplier();
        supplier.setOrganization(org);
        supplier.setName(req.name());
        supplier.setContactInfo(req.contactInfo());
        supplier.setLeadTimeDays(req.leadTimeDays());
        supplier.setCreditTermsDays(req.creditTermsDays());
        supplier.setRating(req.rating() > 0 ? req.rating() : 80.0);

        return toDto(supplierRepository.save(supplier));
    }

    public List<SupplierDto> listByOrganization(Long organizationId) {
        return supplierRepository.findByOrganizationId(organizationId).stream().map(this::toDto).toList();
    }

    public SupplierDto update(Long id, SupplierUpdateRequest req) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("المورد غير موجود"));
        supplier.setName(req.name());
        supplier.setContactInfo(req.contactInfo());
        supplier.setLeadTimeDays(req.leadTimeDays());
        supplier.setCreditTermsDays(req.creditTermsDays());
        supplier.setRating(req.rating());
        return toDto(supplierRepository.save(supplier));
    }

    public Supplier requireById(Long id) {
        return supplierRepository.findById(id).orElseThrow(() -> new NoSuchElementException("المورد غير موجود"));
    }

    private SupplierDto toDto(Supplier s) {
        return new SupplierDto(s.getId(), s.getName(), s.getContactInfo(), s.getLeadTimeDays(),
                s.getCreditTermsDays(), s.getRating());
    }
}
