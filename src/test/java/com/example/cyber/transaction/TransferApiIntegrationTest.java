package com.example.cyber.transaction;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.cyber.account.domain.BankAccount;
import com.example.cyber.account.infrastructure.AccountRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class TransferApiIntegrationTest {
    private static final Pattern ACCOUNT_ID = Pattern.compile("\\\"accountId\\\":\\\"([^\\\"]+)\\\"");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void ownerCanTransferFundsBetweenAccountsAndBalancesAreUpdated() throws Exception {
        String email = "transfer-" + UUID.randomUUID() + "@example.test";
        String password = "LocalTest-Password-123!";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isCreated());

        String authorization = basicAuth(email, password);
        String sourceId = createAccount(authorization);
        String destinationId = createAccount(authorization);

        BankAccount fundedSource = accountRepository
                .findByPublicIdAndOwnerEmail(UUID.fromString(sourceId), email)
                .orElseThrow();
        fundedSource.credit(new BigDecimal("100.00"));
        accountRepository.save(fundedSource);

        mockMvc.perform(post("/api/v1/transactions/transfers")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceAccountId\":\"" + sourceId
                                + "\",\"destinationAccountId\":\"" + destinationId
                                + "\",\"amount\":40.00}"))
                .andExpect(status().isCreated());

        MvcResult accountsResult = mockMvc.perform(get("/api/v1/accounts")
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andReturn();
        String accountList = accountsResult.getResponse().getContentAsString();
        assertTrue(accountList.contains("\"accountId\":\"" + sourceId + "\""));
        assertTrue(accountList.contains("\"accountId\":\"" + destinationId + "\""));
        assertTrue(accountList.contains("\"balance\":60.00"));
        assertTrue(accountList.contains("\"balance\":40.00"));
    }

    @Test
    void protectedAccountApiRejectsAnonymousRequests() throws Exception {
        mockMvc.perform(get("/api/v1/accounts")).andExpect(status().isUnauthorized());
    }

    private String createAccount(String authorization) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"INR\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        Matcher matcher = ACCOUNT_ID.matcher(result.getResponse().getContentAsString());
        assertTrue(matcher.find(), "Account API response should contain an accountId");
        return matcher.group(1);
    }

    private static String basicAuth(String username, String password) {
        String credentials = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
