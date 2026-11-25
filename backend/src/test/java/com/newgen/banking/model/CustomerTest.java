package com.newgen.banking.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerTest {

    @Test
    @DisplayName("Default constructor and setters/getters populate fields properly")
    void testGettersAndSetters() {
        Customer customer = new Customer();
        OffsetDateTime now = OffsetDateTime.now();

        customer.setId(100L);
        customer.setFirstName("Bruce");
        customer.setLastName("Wayne");
        customer.setEmail("bruce.wayne@example.com");
        customer.setPhone("+1-555-0199");
        customer.setStatus("ACTIVE");
        customer.setCreatedAt(now);
        customer.setUpdatedAt(now);

        assertThat(customer.getId()).isEqualTo(100L);
        assertThat(customer.getFirstName()).isEqualTo("Bruce");
        assertThat(customer.getLastName()).isEqualTo("Wayne");
        assertThat(customer.getEmail()).isEqualTo("bruce.wayne@example.com");
        assertThat(customer.getPhone()).isEqualTo("+1-555-0199");
        assertThat(customer.getStatus()).isEqualTo("ACTIVE");
        assertThat(customer.getCreatedAt()).isEqualTo(now);
        assertThat(customer.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Convenience constructor sets default timestamps and fields correctly")
    void testConvenienceConstructor() {
        Customer customer = new Customer("Clark", "Kent", "clark.kent@example.com", "+1-555-0188", "ACTIVE");

        assertThat(customer.getFirstName()).isEqualTo("Clark");
        assertThat(customer.getLastName()).isEqualTo("Kent");
        assertThat(customer.getEmail()).isEqualTo("clark.kent@example.com");
        assertThat(customer.getPhone()).isEqualTo("+1-555-0188");
        assertThat(customer.getStatus()).isEqualTo("ACTIVE");
        assertThat(customer.getCreatedAt()).isNotNull();
        assertThat(customer.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Full constructor maps all fields")
    void testFullConstructor() {
        OffsetDateTime now = OffsetDateTime.now();
        Customer customer = new Customer(1L, "Diana", "Prince", "diana@example.com", "+1-555-0177", "ACTIVE", now, now);

        assertThat(customer.getId()).isEqualTo(1L);
        assertThat(customer.getFirstName()).isEqualTo("Diana");
        assertThat(customer.getLastName()).isEqualTo("Prince");
        assertThat(customer.getEmail()).isEqualTo("diana@example.com");
        assertThat(customer.getPhone()).isEqualTo("+1-555-0177");
        assertThat(customer.getStatus()).isEqualTo("ACTIVE");
        assertThat(customer.getCreatedAt()).isEqualTo(now);
        assertThat(customer.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("toString includes essential customer attributes")
    void testToString() {
        Customer customer = new Customer("Barry", "Allen", "barry@example.com", "+1-555-0166", "ACTIVE");
        customer.setId(5L);

        String str = customer.toString();
        assertThat(str).contains("id=5");
        assertThat(str).contains("firstName='Barry'");
        assertThat(str).contains("email='barry@example.com'");
    }
}
