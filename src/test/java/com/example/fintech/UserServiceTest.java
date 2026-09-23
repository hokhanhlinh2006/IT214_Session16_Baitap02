package com.example.fintech;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class UserServiceTest {

    @Autowired
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void testCacheHitAndMiss() {
        String userId = "U001";
        User mockUser = new User(userId, "Nguyen Van A");
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        // Lần gọi 1: Cache miss -> Phải gọi xuống repository
        User user1 = userService.getUserById(userId);
        assertNotNull(user1);
        
        // Lần gọi 2: Cache hit -> Không gọi xuống repository nữa
        User user2 = userService.getUserById(userId);
        assertNotNull(user2);

        // Verify repository chỉ được gọi đúng 1 lần
        verify(userRepository, times(1)).findById(userId);
    }
}
