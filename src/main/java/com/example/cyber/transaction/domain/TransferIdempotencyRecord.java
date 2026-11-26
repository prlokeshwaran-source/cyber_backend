package com.example.cyber.transaction.domain;

import com.example.cyber.transaction.api.TransactionResponse;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Stores the result associated with an owner's idempotency key. */
@Document("transfer_idempotency")
public class TransferIdempotencyRecord {
    @Id
    private String id;
    private String requestHash;
    private TransactionResponse response;

    protected TransferIdempotencyRecord() {}

    public TransferIdempotencyRecord(String id, String requestHash, TransactionResponse response) {
        this.id = id;
        this.requestHash = requestHash;
        this.response = response;
    }

    public String getRequestHash() { return requestHash; }
    public TransactionResponse getResponse() { return response; }
}
