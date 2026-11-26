package com.newgen.banking.controller;

import com.newgen.banking.exception.GlobalExceptionHandler;
import com.newgen.banking.model.Customer;
import com.newgen.banking.repository.CustomerRepository;
import com.newgen.banking.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CustomerControllerTest {

    private MockMvc mockMvc;
    private InMemoryCustomerRepository inMemoryRepo;

    @BeforeEach
    void setUp() {
        inMemoryRepo = new InMemoryCustomerRepository();
        CustomerService customerService = new CustomerService(inMemoryRepo);
        CustomerController customerController = new CustomerController(customerService);

        mockMvc = MockMvcBuilders.standaloneSetup(customerController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("1. Successful customer registration returns 201 CREATED")
    void testRegisterCustomerSuccess() throws Exception {
        String payload = """
                {
                  "firstName": "Bruce",
                  "lastName": "Wayne",
                  "email": "bruce.wayne@example.com",
                  "phone": "+1-555-0199"
                }
                """;

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.firstName").value("Bruce"))
                .andExpect(jsonPath("$.lastName").value("Wayne"))
                .andExpect(jsonPath("$.email").value("bruce.wayne@example.com"))
                .andExpect(jsonPath("$.phone").value("+1-555-0199"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    @DisplayName("2. Duplicate email registration returns 409 CONFLICT")
    void testDuplicateEmailReturnsConflict() throws Exception {
        String payload = """
                {
                  "firstName": "Alex",
                  "lastName": "Morgan",
                  "email": "alex.morgan@example.com",
                  "phone": "+1-555-0101"
                }
                """;

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("A customer with email 'alex.morgan@example.com' already exists"));
    }

    @Test
    @DisplayName("3. Invalid email returns 400 BAD REQUEST")
    void testInvalidEmailReturnsBadRequest() throws Exception {
        String payload = """
                {
                  "firstName": "Clark",
                  "lastName": "Kent",
                  "email": "not-a-valid-email",
                  "phone": "+1-555-0188"
                }
                """;

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.email").value("Email must be a valid email address"));
    }

    @Test
    @DisplayName("4. Missing required fields returns 400 BAD REQUEST")
    void testMissingRequiredFieldsReturnsBadRequest() throws Exception {
        String payload = """
                {
                  "firstName": "",
                  "email": "diana@example.com"
                }
                """;

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.firstName").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.lastName").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.phone").isNotEmpty());
    }

    @Test
    @DisplayName("5. Retrieve existing customer by ID returns 200 OK")
    void testGetCustomerByIdSuccess() throws Exception {
        mockMvc.perform(get("/api/customers/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("Alex"))
                .andExpect(jsonPath("$.lastName").value("Morgan"))
                .andExpect(jsonPath("$.email").value("alex.morgan@example.com"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("6. Retrieve nonexistent customer by ID returns 404 NOT FOUND")
    void testGetCustomerByIdNotFound() throws Exception {
        mockMvc.perform(get("/api/customers/99999")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Customer not found with id: 99999"));
    }

    @Test
    @DisplayName("7. Retrieve all customers returns 200 OK with list of CustomerResponse DTOs")
    void testGetAllCustomersReturnsList() throws Exception {
        mockMvc.perform(get("/api/customers")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].email").value("alex.morgan@example.com"));
    }

    @Test
    @DisplayName("8. Retrieve all customers when empty returns 200 OK with empty list")
    void testGetAllCustomersReturnsEmptyList() throws Exception {
        inMemoryRepo.deleteAll();

        mockMvc.perform(get("/api/customers")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("9. Successful customer update returns 200 OK")
    void testUpdateCustomerSuccess() throws Exception {
        String payload = """
                {
                  "firstName": "Alexandra",
                  "lastName": "Morgan-Smith",
                  "email": "alexandra.ms@example.com",
                  "phone": "+1-555-9999"
                }
                """;

        mockMvc.perform(put("/api/customers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("Alexandra"))
                .andExpect(jsonPath("$.lastName").value("Morgan-Smith"))
                .andExpect(jsonPath("$.email").value("alexandra.ms@example.com"))
                .andExpect(jsonPath("$.phone").value("+1-555-9999"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("10. Update customer retaining own existing email returns 200 OK")
    void testUpdateCustomerSameEmailAllowed() throws Exception {
        String payload = """
                {
                  "firstName": "Alex",
                  "lastName": "Morgan",
                  "email": "alex.morgan@example.com",
                  "phone": "+1-555-2222"
                }
                """;

        mockMvc.perform(put("/api/customers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("alex.morgan@example.com"))
                .andExpect(jsonPath("$.phone").value("+1-555-2222"));
    }

    @Test
    @DisplayName("11. Update customer with duplicate email of another customer returns 409 CONFLICT")
    void testUpdateCustomerDuplicateEmailReturnsConflict() throws Exception {
        Customer second = new Customer("Sarah", "Connor", "sarah.connor@example.com", "+1-555-0102", "ACTIVE");
        inMemoryRepo.save(second);

        String payload = """
                {
                  "firstName": "Alex",
                  "lastName": "Morgan",
                  "email": "sarah.connor@example.com",
                  "phone": "+1-555-0101"
                }
                """;

        mockMvc.perform(put("/api/customers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("A customer with email 'sarah.connor@example.com' already exists"));
    }

    @Test
    @DisplayName("12. Update nonexistent customer ID returns 404 NOT FOUND")
    void testUpdateCustomerNotFound() throws Exception {
        String payload = """
                {
                  "firstName": "Ghost",
                  "lastName": "User",
                  "email": "ghost@example.com",
                  "phone": "+1-555-0000"
                }
                """;

        mockMvc.perform(put("/api/customers/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Customer not found with id: 99999"));
    }

    @Test
    @DisplayName("13. Update customer with invalid email returns 400 BAD REQUEST")
    void testUpdateCustomerInvalidEmail() throws Exception {
        String payload = """
                {
                  "firstName": "Alex",
                  "lastName": "Morgan",
                  "email": "invalid-email",
                  "phone": "+1-555-0101"
                }
                """;

        mockMvc.perform(put("/api/customers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.email").value("Email must be a valid email address"));
    }

    @Test
    @DisplayName("14. Update customer with missing required fields returns 400 BAD REQUEST")
    void testUpdateCustomerMissingFields() throws Exception {
        String payload = """
                {
                  "firstName": "",
                  "email": "alex.morgan@example.com"
                }
                """;

        mockMvc.perform(put("/api/customers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.firstName").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.lastName").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.phone").isNotEmpty());
    }

    static class InMemoryCustomerRepository implements CustomerRepository {
        private final Map<Long, Customer> store = new ConcurrentHashMap<>();
        private long idSequence = 1L;

        InMemoryCustomerRepository() {
            Customer existing = new Customer("Alex", "Morgan", "alex.morgan@example.com", "+1-555-0101", "ACTIVE");
            existing.setId(1L);
            store.put(1L, existing);
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

        @Override public Optional<Customer> findById(Long id) { return Optional.ofNullable(store.get(id)); }
        @Override public boolean existsById(Long id) { return store.containsKey(id); }
        @Override public List<Customer> findAll() { return new ArrayList<>(store.values()); }
        @Override public List<Customer> findAllById(Iterable<Long> ids) { return Collections.emptyList(); }
        @Override public long count() { return store.size(); }
        @Override public void deleteById(Long id) { store.remove(id); }
        @Override public void delete(Customer entity) {}
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
}
