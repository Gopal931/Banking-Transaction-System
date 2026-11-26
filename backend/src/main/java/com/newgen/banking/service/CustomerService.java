package com.newgen.banking.service;

import com.newgen.banking.dto.CustomerRequest;
import com.newgen.banking.dto.CustomerResponse;
import com.newgen.banking.exception.DuplicateEmailException;
import com.newgen.banking.exception.ResourceNotFoundException;
import com.newgen.banking.model.Customer;
import com.newgen.banking.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public CustomerResponse registerCustomer(CustomerRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (customerRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new DuplicateEmailException("A customer with email '" + normalizedEmail + "' already exists");
        }

        OffsetDateTime now = OffsetDateTime.now();
        Customer customer = new Customer();
        customer.setFirstName(request.getFirstName().trim());
        customer.setLastName(request.getLastName().trim());
        customer.setEmail(normalizedEmail);
        customer.setPhone(request.getPhone().trim());
        customer.setStatus("ACTIVE");
        customer.setCreatedAt(now);
        customer.setUpdatedAt(now);

        Customer savedCustomer = customerRepository.save(customer);
        return CustomerResponse.fromEntity(savedCustomer);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
        return CustomerResponse.fromEntity(customer);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> getAllCustomers() {
        return customerRepository.findAll()
                .stream()
                .map(CustomerResponse::fromEntity)
                .toList();
    }

    /**
     * Updates permitted profile fields for an existing customer.
     *
     * @param id      Customer ID
     * @param request Validated update request
     * @return Updated CustomerResponse DTO
     */
    @Transactional
    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        customerRepository.findByEmail(normalizedEmail).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new DuplicateEmailException("A customer with email '" + normalizedEmail + "' already exists");
            }
        });

        customer.setFirstName(request.getFirstName().trim());
        customer.setLastName(request.getLastName().trim());
        customer.setEmail(normalizedEmail);
        customer.setPhone(request.getPhone().trim());
        customer.setUpdatedAt(OffsetDateTime.now());

        Customer updatedCustomer = customerRepository.save(customer);
        return CustomerResponse.fromEntity(updatedCustomer);
    }
}
