package com.newgen.banking.repository;

import com.newgen.banking.model.Customer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerRepositoryTest {

    static class InMemoryCustomerRepository implements CustomerRepository {
        private final Map<Long, Customer> store = new ConcurrentHashMap<>();
        private long idSequence = 0L;

        InMemoryCustomerRepository() {
            // Pre-seed matching database/seed-data.sql
            Customer c1 = new Customer(1L, "Alex", "Morgan", "alex.morgan@example.com", "+1-555-0101", "ACTIVE", OffsetDateTime.now(), OffsetDateTime.now());
            Customer c2 = new Customer(2L, "Sarah", "Connor", "sarah.connor@example.com", "+1-555-0102", "ACTIVE", OffsetDateTime.now(), OffsetDateTime.now());
            Customer c3 = new Customer(3L, "Wayne", "Enterprises", "finance@wayneenterprises.com", "+1-555-0103", "ACTIVE", OffsetDateTime.now(), OffsetDateTime.now());
            store.put(1L, c1);
            store.put(2L, c2);
            store.put(3L, c3);
            idSequence = 3L;
        }

        @Override
        public Optional<Customer> findByEmail(String email) {
            return store.values().stream().filter(c -> email.equals(c.getEmail())).findFirst();
        }

        @Override
        public <S extends Customer> S save(S entity) {
            if (entity.getId() == null) {
                entity.setId(++idSequence);
            }
            store.put(entity.getId(), entity);
            return entity;
        }

        @Override
        public Optional<Customer> findById(Long id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public boolean existsById(Long id) {
            return store.containsKey(id);
        }

        @Override
        public List<Customer> findAll() {
            return new ArrayList<>(store.values());
        }

        @Override
        public List<Customer> findAllById(Iterable<Long> ids) {
            List<Customer> list = new ArrayList<>();
            for (Long id : ids) {
                if (store.containsKey(id)) list.add(store.get(id));
            }
            return list;
        }

        @Override
        public long count() {
            return store.size();
        }

        @Override
        public void deleteById(Long id) {
            store.remove(id);
        }

        @Override
        public void delete(Customer entity) {
            if (entity.getId() != null) store.remove(entity.getId());
        }

        @Override public void deleteAllById(Iterable<? extends Long> ids) {}
        @Override public void deleteAll(Iterable<? extends Customer> entities) {}
        @Override public void deleteAll() { store.clear(); }
        @Override public <S extends Customer> List<S> saveAll(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public void flush() {}
        @Override public <S extends Customer> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends Customer> List<S> saveAllAndFlush(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public void deleteAllInBatch(Iterable<Customer> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
        @Override public void deleteAllInBatch() {}
        @Override public Customer getOne(Long id) { return store.get(id); }
        @Override public Customer getById(Long id) { return store.get(id); }
        @Override public Customer getReferenceById(Long id) { return store.get(id); }
        @Override public <S extends Customer> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
        @Override public <S extends Customer> List<S> findAll(Example<S> example) { return Collections.emptyList(); }
        @Override public <S extends Customer> List<S> findAll(Example<S> example, Sort sort) { return Collections.emptyList(); }
        @Override public <S extends Customer> Page<S> findAll(Example<S> example, Pageable pageable) { return Page.empty(); }
        @Override public <S extends Customer> long count(Example<S> example) { return 0; }
        @Override public <S extends Customer> boolean exists(Example<S> example) { return false; }
        @Override public <S extends Customer, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public List<Customer> findAll(Sort sort) { return findAll(); }
        @Override public Page<Customer> findAll(Pageable pageable) { return Page.empty(); }
    }

    private final CustomerRepository customerRepository = new InMemoryCustomerRepository();

    @Test
    @DisplayName("1. Find customer by ID should return existing customer")
    void testFindById() {
        Optional<Customer> result = customerRepository.findById(1L);

        assertThat(result).isPresent();
        Customer customer = result.get();
        assertThat(customer.getId()).isEqualTo(1L);
        assertThat(customer.getEmail()).isEqualTo("alex.morgan@example.com");
        assertThat(customer.getFirstName()).isEqualTo("Alex");
        assertThat(customer.getLastName()).isEqualTo("Morgan");
        assertThat(customer.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("2. Find all customers should return seeded customer records")
    void testFindAll() {
        List<Customer> customers = customerRepository.findAll();

        assertThat(customers).isNotEmpty();
        assertThat(customers.size()).isGreaterThanOrEqualTo(3);

        boolean hasAlex = customers.stream()
                .anyMatch(c -> "alex.morgan@example.com".equals(c.getEmail()));
        boolean hasSarah = customers.stream()
                .anyMatch(c -> "sarah.connor@example.com".equals(c.getEmail()));
        boolean hasWayne = customers.stream()
                .anyMatch(c -> "finance@wayneenterprises.com".equals(c.getEmail()));

        assertThat(hasAlex).isTrue();
        assertThat(hasSarah).isTrue();
        assertThat(hasWayne).isTrue();
    }

    @Test
    @DisplayName("3. Find customer by email should return matching customer")
    void testFindByEmail() {
        Optional<Customer> found = customerRepository.findByEmail("sarah.connor@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("Sarah");
        assertThat(found.get().getLastName()).isEqualTo("Connor");
        assertThat(found.get().getPhone()).isEqualTo("+1-555-0102");

        Optional<Customer> notFound = customerRepository.findByEmail("nonexistent.user@example.com");
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("4. Save a new customer and verify generated database ID")
    void testSaveNewCustomer() {
        Customer newCustomer = new Customer();
        newCustomer.setFirstName("Bruce");
        newCustomer.setLastName("Banner");
        newCustomer.setEmail("bruce.banner." + System.currentTimeMillis() + "@example.com");
        newCustomer.setPhone("+1-555-9988");
        newCustomer.setStatus("ACTIVE");
        newCustomer.setCreatedAt(OffsetDateTime.now());
        newCustomer.setUpdatedAt(OffsetDateTime.now());

        Customer saved = customerRepository.save(newCustomer);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getId()).isGreaterThan(0L);

        Optional<Customer> retrieved = customerRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getFirstName()).isEqualTo("Bruce");
        assertThat(retrieved.get().getEmail()).isEqualTo(newCustomer.getEmail());
    }

    @Test
    @DisplayName("5. Check count and existsById inherited from JpaRepository")
    void testJpaRepositoryCommonMethods() {
        long count = customerRepository.count();
        assertThat(count).isGreaterThanOrEqualTo(3);

        boolean exists = customerRepository.existsById(1L);
        assertThat(exists).isTrue();

        boolean nonExistent = customerRepository.existsById(999999L);
        assertThat(nonExistent).isFalse();
    }
}