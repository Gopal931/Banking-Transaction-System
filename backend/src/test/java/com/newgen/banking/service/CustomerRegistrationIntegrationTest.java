package com.newgen.banking.service;

import com.newgen.banking.dto.CustomerRequest;
import com.newgen.banking.dto.CustomerResponse;
import com.newgen.banking.exception.DuplicateEmailException;
import com.newgen.banking.exception.ResourceNotFoundException;
import com.newgen.banking.model.Customer;
import com.newgen.banking.repository.CustomerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests verifying CustomerService persists new customers to PostgreSQL
 * and rejects duplicate emails using live banking_db.
 * Automatically rolls back changes via @Transactional.
 */
@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class CustomerRegistrationIntegrationTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    @DisplayName("Customer is persisted in PostgreSQL with generated ID and ACTIVE status")
    void testRegisterCustomerPersistedInPostgres() {
        String uniqueEmail = "integration.user." + System.currentTimeMillis() + "@example.com";
        CustomerRequest request = new CustomerRequest("Diana", "Prince", uniqueEmail, "+1-555-0177");

        CustomerResponse response = customerService.registerCustomer(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getId()).isGreaterThan(0L);
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getEmail()).isEqualTo(uniqueEmail);

        Optional<Customer> persisted = customerRepository.findById(response.getId());
        assertThat(persisted).isPresent();
        assertThat(persisted.get().getFirstName()).isEqualTo("Diana");
        assertThat(persisted.get().getLastName()).isEqualTo("Prince");
        assertThat(persisted.get().getCreatedAt()).isNotNull();
        assertThat(persisted.get().getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Duplicate email against seeded PostgreSQL customer throws DuplicateEmailException")
    void testDuplicateEmailRejectedInPostgres() {
        CustomerRequest request = new CustomerRequest("Alex", "Morgan", "alex.morgan@example.com", "+1-555-0101");

        assertThatThrownBy(() -> customerService.registerCustomer(request))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("alex.morgan@example.com");
    }

    @Test
    @DisplayName("Retrieve existing customer by ID from PostgreSQL")
    void testGetCustomerByIdFromPostgres() {
        CustomerResponse response = customerService.getCustomerById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("alex.morgan@example.com");
        assertThat(response.getFirstName()).isEqualTo("Alex");
    }

    @Test
    @DisplayName("Retrieve nonexistent customer by ID throws ResourceNotFoundException")
    void testGetCustomerByIdNotFoundInPostgres() {
        assertThatThrownBy(() -> customerService.getCustomerById(999999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999999");
    }

    @Test
    @DisplayName("Retrieve all customers from PostgreSQL returns seeded records")
    void testGetAllCustomersFromPostgres() {
        List<CustomerResponse> customers = customerService.getAllCustomers();

        assertThat(customers).isNotEmpty();
        assertThat(customers.size()).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("Update customer profile in PostgreSQL preserves createdAt and updates updatedAt")
    void testUpdateCustomerInPostgres() {
        CustomerResponse before = customerService.getCustomerById(1L);
        String uniqueEmail = "updated.alex." + System.currentTimeMillis() + "@example.com";

        CustomerRequest updateRequest = new CustomerRequest("Alex", "Morgan-Updated", uniqueEmail, "+1-555-7777");
        CustomerResponse updated = customerService.updateCustomer(1L, updateRequest);

        assertThat(updated.getId()).isEqualTo(1L);
        assertThat(updated.getLastName()).isEqualTo("Morgan-Updated");
        assertThat(updated.getEmail()).isEqualTo(uniqueEmail);
        assertThat(updated.getPhone()).isEqualTo("+1-555-7777");
        assertThat(updated.getCreatedAt()).isEqualTo(before.getCreatedAt());
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(before.getUpdatedAt());
    }

    @Test
    @DisplayName("Update customer with another customer's email throws DuplicateEmailException in PostgreSQL")
    void testUpdateCustomerDuplicateEmailInPostgres() {
        CustomerRequest updateRequest = new CustomerRequest("Alex", "Morgan", "sarah.connor@example.com", "+1-555-0101");

        assertThatThrownBy(() -> customerService.updateCustomer(1L, updateRequest))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("sarah.connor@example.com");
    }
}
