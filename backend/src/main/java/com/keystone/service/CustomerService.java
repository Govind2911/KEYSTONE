package com.keystone.service;

import com.keystone.domain.Customer;
import com.keystone.domain.Role;
import com.keystone.domain.Site;
import com.keystone.dto.CommonDtos.PageResponse;
import com.keystone.dto.CustomerDtos.*;
import com.keystone.exception.ApiException;
import com.keystone.repository.CustomerRepository;
import com.keystone.repository.SiteRepository;
import com.keystone.security.AppUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customers;
    private final SiteRepository sites;

    @Transactional(readOnly = true)
    public PageResponse<CustomerDto> list(String q, Pageable pageable) {
        Pageable p = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("name"));
        Page<Customer> page = (q == null || q.isBlank()) ? customers.findAll(p)
                : customers.findByNameContainingIgnoreCase(q.trim(), p);
        return PageResponse.of(page, this::toDto);
    }

    @Transactional(readOnly = true)
    public CustomerDto get(Long id) {
        return toDto(find(id));
    }

    @Transactional
    public CustomerDto create(CustomerRequest req) {
        if (customers.existsByNameIgnoreCase(req.name().trim())) {
            throw new ApiException.Conflict("A customer named \"" + req.name().trim() + "\" already exists");
        }
        Customer c = new Customer();
        apply(c, req);
        return toDto(customers.save(c));
    }

    @Transactional
    public CustomerDto update(Long id, CustomerRequest req) {
        Customer c = find(id);
        customers.findByNameIgnoreCase(req.name().trim()).filter(o -> !o.getId().equals(id))
                .ifPresent(o -> { throw new ApiException.Conflict("Another customer already uses that name"); });
        apply(c, req);
        return toDto(c);
    }

    // ---------------------------------------------------------------- sites

    /** Staff may list any customer's sites; a customer may list only their own organisation's. */
    @Transactional(readOnly = true)
    public PageResponse<SiteDto> listSites(Long customerId, String q, Pageable pageable, AppUserPrincipal me) {
        if (me.role() == Role.CUSTOMER && !customerId.equals(me.customerId())) {
            throw new ApiException.NotFound("Customer not found");
        }
        find(customerId);
        Pageable p = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("name"));
        Page<Site> page = (q == null || q.isBlank()) ? sites.findByCustomerId(customerId, p)
                : sites.findByCustomerIdAndNameContainingIgnoreCase(customerId, q.trim(), p);
        return PageResponse.of(page, this::toDto);
    }

    @Transactional
    public SiteDto createSite(Long customerId, SiteRequest req) {
        Site s = new Site();
        s.setCustomer(find(customerId));
        apply(s, req);
        return toDto(sites.save(s));
    }

    @Transactional
    public SiteDto updateSite(Long siteId, SiteRequest req) {
        Site s = sites.findById(siteId).orElseThrow(() -> new ApiException.NotFound("Site not found"));
        apply(s, req);
        return toDto(s);
    }

    // -------------------------------------------------------------- helpers

    private Customer find(Long id) {
        return customers.findById(id).orElseThrow(() -> new ApiException.NotFound("Customer not found"));
    }

    private void apply(Customer c, CustomerRequest r) {
        c.setName(r.name().trim());
        c.setContactName(trim(r.contactName()));
        c.setContactEmail(trim(r.contactEmail()));
        c.setContactPhone(trim(r.contactPhone()));
        c.setAddress(trim(r.address()));
        if (r.active() != null) c.setActive(r.active());
    }

    private void apply(Site s, SiteRequest r) {
        s.setName(r.name().trim());
        s.setAddressLine(r.addressLine().trim());
        s.setCity(r.city().trim());
        s.setState(trim(r.state()));
        s.setPostalCode(trim(r.postalCode()));
        s.setCountry(trim(r.country()));
    }

    private CustomerDto toDto(Customer c) {
        return new CustomerDto(c.getId(), c.getName(), c.getContactName(), c.getContactEmail(), c.getContactPhone(),
                c.getAddress(), c.isActive(), sites.countByCustomerId(c.getId()), c.getCreatedAt());
    }

    private SiteDto toDto(Site s) {
        return new SiteDto(s.getId(), s.getCustomer().getId(), s.getCustomer().getName(), s.getName(), s.getAddressLine(),
                s.getCity(), s.getState(), s.getPostalCode(), s.getCountry());
    }

    private static String trim(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
