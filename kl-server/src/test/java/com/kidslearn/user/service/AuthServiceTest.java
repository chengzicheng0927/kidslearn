package com.kidslearn.user.service;

import com.kidslearn.common.exception.BusinessException;
import com.kidslearn.user.domain.InviteCode;
import com.kidslearn.user.repository.InviteCodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private InviteCodeRepository inviteCodeRepository;

    @InjectMocks
    private AuthService authService;

    private InviteCode validCode;
    private InviteCode expiredCode;
    private InviteCode usedCode;

    @BeforeEach
    void setUp() {
        validCode = InviteCode.builder()
            .code("KIDS123")
            .maxUses(10)
            .usedCount(0)
            .expiresAt(Instant.now().plusSeconds(86400))
            .build();

        expiredCode = InviteCode.builder()
            .code("EXP123")
            .maxUses(10)
            .usedCount(0)
            .expiresAt(Instant.now().minusSeconds(86400))
            .build();

        usedCode = InviteCode.builder()
            .code("USED123")
            .maxUses(1)
            .usedCount(1)
            .expiresAt(Instant.now().plusSeconds(86400))
            .build();
    }

    @Test
    void testValidateCode_Valid() {
        when(inviteCodeRepository.findByCode("KIDS123")).thenReturn(Optional.of(validCode));
        
        var result = authService.validateCode("KIDS123");
        assertTrue(result.valid());
        assertEquals("邀请码有效", result.message());
    }

    @Test
    void testValidateCode_Expired() {
        when(inviteCodeRepository.findByCode("EXP123")).thenReturn(Optional.of(expiredCode));

        var result = authService.validateCode("EXP123");
        assertFalse(result.valid());
        assertEquals("邀请码已过期", result.message());
    }

    @Test
    void testValidateCode_Used() {
        when(inviteCodeRepository.findByCode("USED123")).thenReturn(Optional.of(usedCode));

        var result = authService.validateCode("USED123");
        assertFalse(result.valid());
        assertEquals("邀请码已使用", result.message());
    }

    @Test
    void testValidateCode_NotFound() {
        when(inviteCodeRepository.findByCode(anyString())).thenReturn(Optional.empty());

        var result = authService.validateCode("INVALID");
        assertFalse(result.valid());
        assertEquals("邀请码不存在", result.message());
    }
}
