package com.example.cyber.auth.infrastructure;

import com.example.cyber.auth.domain.DeviceApproval;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DeviceApprovalRepository extends MongoRepository<DeviceApproval, String> {
    Optional<DeviceApproval> findFirstByEmailAndDeviceHashOrderByExpiresAtDesc(String email, String deviceHash);
}
