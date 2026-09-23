package com.example.fintech;

import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public class UserRepository {
    public Optional<User> findById(String userId) {
        // Giả lập truy vấn DB chậm
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if ("U001".equals(userId)) {
            return Optional.of(new User("U001", "Nguyen Van A"));
        }
        return Optional.empty();
    }
}
