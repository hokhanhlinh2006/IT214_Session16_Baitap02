package com.example.fintech;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    // Chỉ cache nếu userId hợp lệ, và không cache kết quả null (unless = "#result == null")
    @Cacheable(value = "users", key = "#userId", condition = "#userId != null and !#userId.trim().isEmpty()", unless = "#result == null")
    public User getUserById(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID must not be null or empty");
        }
        System.out.println(">>> Truy vấn Database cho userId: " + userId);
        return userRepository.findById(userId).orElse(null);
    }
}
