package com.example.cyber.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.mongodb.autoconfigure.MongoClientSettingsBuilderCustomizer;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.transaction.TransactionManager;
import org.bson.UuidRepresentation;

@Configuration
public class MongoConfiguration {
    @Bean
    MongoClientSettingsBuilderCustomizer mongoUuidRepresentation() {
        return builder -> builder.uuidRepresentation(UuidRepresentation.STANDARD);
    }

    @Bean
    TransactionManager transactionManager(MongoDatabaseFactory databaseFactory) {
        return new MongoTransactionManager(databaseFactory);
    }
}
