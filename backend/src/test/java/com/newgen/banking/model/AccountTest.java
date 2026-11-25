package com.newgen.banking.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AccountTest {

    @Test
    @DisplayName("Default constructor and setters/getters populate fields properly")
    void testGettersAndSetters() {
        Customer customer = new Customer("Alex", "Morgan", "alex.morgan@example.com", "+1-555-0101", "ACTIVE");
        customer.setId(1L);

        Account account = new Account();
        OffsetDateTime now = OffsetDateTime.now();

        account.setId(10L);
        account.setCustomer(customer);
        account.setAccountNumber("ACCT-CHK-8921");
        account.setAccountType("CHECKING");
        account.setBalance(new BigDecimal("12450.25"));
        account.setCurrency("USD");
        account.setStatus("ACTIVE");
        account.setCreatedAt(now);
        account.setUpdatedAt(now);

        assertThat(account.getId()).isEqualTo(10L);
        assertThat(account.getCustomer()).isEqualTo(customer);
        assertThat(account.getAccountNumber()).isEqualTo("ACCT-CHK-8921");
        assertThat(account.getAccountType()).isEqualTo("CHECKING");
        assertThat(account.getBalance()).isEqualByComparingTo("12450.25");
        assertThat(account.getCurrency()).isEqualTo("USD");
        assertThat(account.getStatus()).isEqualTo("ACTIVE");
        assertThat(account.getCreatedAt()).isEqualTo(now);
        assertThat(account.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Convenience constructor sets default timestamps and fields correctly")
    void testConvenienceConstructor() {
        Customer customer = new Customer("Sarah", "Connor", "sarah.connor@example.com", "+1-555-0102", "ACTIVE");
        customer.setId(2L);

        Account account = new Account(customer, "ACCT-SAV-3410", "SAVINGS", new BigDecimal("500.00"), "USD", "ACTIVE");

        assertThat(account.getCustomer()).isEqualTo(customer);
        assertThat(account.getAccountNumber()).isEqualTo("ACCT-SAV-3410");
        assertThat(account.getAccountType()).isEqualTo("SAVINGS");
        assertThat(account.getBalance()).isEqualByComparingTo("500.00");
        assertThat(account.getCurrency()).isEqualTo("USD");
        assertThat(account.getStatus()).isEqualTo("ACTIVE");
        assertThat(account.getCreatedAt()).isNotNull();
        assertThat(account.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("toString includes essential account attributes")
    void testToString() {
        Account account = new Account(null, "ACCT-TEST-1001", "CHECKING", new BigDecimal("100.00"), "USD", "ACTIVE");
        account.setId(5L);

        String str = account.toString();
        assertThat(str).contains("id=5");
        assertThat(str).contains("accountNumber='ACCT-TEST-1001'");
        assertThat(str).contains("accountType='CHECKING'");
    }
}
