package com.example.cyber.transaction.infrastructure;

import com.example.cyber.transaction.domain.TransferIdempotencyRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TransferIdempotencyRepository extends MongoRepository<TransferIdempotencyRecord, String> {}
