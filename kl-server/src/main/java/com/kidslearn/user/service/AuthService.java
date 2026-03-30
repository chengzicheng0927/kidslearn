package com.kidslearn.user.service;

import com.kidslearn.common.exception.BusinessException;
import com.kidslearn.common.exception.ResourceNotFoundException;
import com.kidslearn.user.domain.*;
import com.kidslearn.user.dto.*;
import com.kidslearn.user.repository.*;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final InviteCodeRepository inviteCodeRepository;
    private final ParentRepository parentRepository;
    private final ParentSettingsRepository parentSettingsRepository;
    private final ChildRepository childRepository;
    private final DeviceRepository deviceRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecretKey jwtSecretKey;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    /**
     * 校验邀请码
     */
    public ValidateInviteCodeResponse validateCode(String code) {
        var inviteCode = inviteCodeRepository.findByCode(code).orElse(null);
        if (inviteCode == null) {
            return new ValidateInviteCodeResponse(false, "邀请码不存在");
        }
        if (inviteCode.isExpired()) {
            return new ValidateInviteCodeResponse(false, "邀请码已过期");
        }
        if (inviteCode.isExhausted()) {
            return new ValidateInviteCodeResponse(false, "邀请码已使用");
        }
        return new ValidateInviteCodeResponse(true, "邀请码有效");
    }

    /**
     * 根据设备 ID 获取用户信息（用于前端初始化）
     */
    @Transactional(readOnly = true)
    public CheckUserStatusResponse checkUserStatus(String deviceId) {
        Device device = deviceRepository.findByDeviceId(deviceId).orElse(null);
        if (device == null) return new CheckUserStatusResponse(null, null, null);

        Child child = childRepository.findById(device.getChildId()).orElse(null);
        if (child == null) return new CheckUserStatusResponse(null, null, null);

        Parent parent = parentRepository.findById(child.getParentId()).orElse(null);
        if (parent == null || parent.isDisabled()) return new CheckUserStatusResponse(null, null, null);

        ParentSettings settings = parentSettingsRepository.findByParentId(parent.getId()).orElse(null);
        ParentSettingsDTO settingsDTO = settings != null ? new ParentSettingsDTO(
            settings.getDailyLimitMinutes(), settings.isDndEnabled(), settings.getDndStart(),
            settings.getDndEnd(), settings.isEyeCareReminder(), settings.isBreakReminder(),
            settings.getTeacherName()
        ) : null;

        String token = generateToken(parent.getId());
        return new CheckUserStatusResponse(token, toChildDTO(child), settingsDTO);
    }

    /**
     * 根据前端传的身份证验证并计算年龄
     */
    public ValidateIdCardResponse validateIdCard(String idCard) {
        try {
            int age = calculateAgeFromIdCard(idCard);
            String ageGroup = Child.calcAgeGroup(age);
            return new ValidateIdCardResponse(true, age, ageGroup, "身份证有效");
        } catch (BusinessException e) {
            return new ValidateIdCardResponse(false, null, null, e.getMessage());
        }
    }

    /**
     * 激活账户：邀请码 → 创建家长 + 孩子 + 设备
     */
    @Transactional
    public ActivateResponse activate(ActivateRequest req) {
        // 1. 校验邀请码
        InviteCode inviteCode = inviteCodeRepository.findByCode(req.inviteCode())
            .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_CODE", "邀请码不存在"));

        if (inviteCode.isExpired()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "CODE_EXPIRED", "邀请码已过期");
        }
        if (inviteCode.isExhausted()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "CODE_EXHAUSTED", "邀请码已使用");
        }

        // 2. 从身份证号计算年龄
        int age = calculateAgeFromIdCard(req.idCard());

        // 3. 创建家长
        Parent parent = Parent.builder()
            .passwordHash("")
            .passwordSalt("")
            .build();
        parent = parentRepository.save(parent);

        // 4. 创建家长设置
        ParentSettings settings = ParentSettings.builder()
            .parentId(parent.getId())
            .teacherName(req.teacherName() != null ? req.teacherName() : "小星老师")
            .build();
        parentSettingsRepository.save(settings);

        // 5. 创建孩子
        Child child = Child.builder()
            .parentId(parent.getId())
            .name(req.name())
            .age(age)
            .ageGroup(Child.calcAgeGroup(age))
            .build();
        child = childRepository.save(child);

        // 6. 注册设备
        Device device = Device.builder()
            .childId(child.getId())
            .deviceId(req.deviceId())
            .build();
        deviceRepository.save(device);

        // 7. 更新邀请码
        inviteCode.setUsedCount(inviteCode.getUsedCount() + 1);
        inviteCode.setUsedAt(Instant.now());
        inviteCode.setUsedBy(parent.getId());
        inviteCodeRepository.save(inviteCode);

        log.info("ACTIVATE_SUCCESS | code={} | parentId={} | childId={} | childName={}",
            req.inviteCode(), parent.getId(), child.getId(), req.name());

        String token = generateToken(parent.getId());
        return new ActivateResponse(token, toChildDTO(child));
    }

    /**
     * 登录
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest req) {
        InviteCode inviteCode = inviteCodeRepository.findByCode(req.inviteCode())
            .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_CODE", "邀请码不存在"));

        if (inviteCode.getUsedBy() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "CODE_NOT_ACTIVATED", "邀请码尚未激活");
        }

        Parent parent = parentRepository.findById(inviteCode.getUsedBy())
            .orElseThrow(() -> new ResourceNotFoundException("Parent", inviteCode.getUsedBy()));

        if (parent.isDisabled()) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED", "账户已禁用");
        }

        List<Child> children = childRepository.findByParentId(parent.getId());

        String token = generateToken(parent.getId());

        return new LoginResponse(
            token,
            "Bearer",
            jwtExpiration / 1000,
            new ParentDTO(parent.getId(), !parent.getPasswordHash().isEmpty()),
            children.stream().map(this::toChildDTO).toList()
        );
    }

    private String generateToken(UUID parentId) {
        return Jwts.builder()
            .subject(parentId.toString())
            .claim("role", "PARENT")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
            .signWith(jwtSecretKey)
            .compact();
    }

    private int calculateAgeFromIdCard(String idCard) {
        try {
            String birthStr = idCard.substring(6, 14);
            LocalDate birth = LocalDate.parse(birthStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
            return LocalDate.now().getYear() - birth.getYear();
        } catch (Exception e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_ID_CARD", "身份证号格式不正确");
        }
    }

    public ChildDTO toChildDTO(Child child) {
        return new ChildDTO(
            child.getId(), child.getName(), child.getAge(), child.getAgeGroup(),
            child.getStars(), child.getStreak(), child.getTodayMinutes(),
            child.getLevel(), child.getXp(), child.getXpNeeded(),
            child.getTotalAnswered(), child.getTotalDays(),
            child.getDailyTargetEn(), child.getDailyTargetMa(),
            child.getGrade()
        );
    }

    // ========== 密码管理 ==========

    public PasswordStatusResponse getPasswordStatus(UUID parentId) {
        Parent parent = parentRepository.findById(parentId)
            .orElseThrow(() -> new ResourceNotFoundException("Parent", parentId));
        return new PasswordStatusResponse(!parent.getPasswordHash().isEmpty());
    }

    @Transactional
    public void setPassword(UUID parentId, String password) {
        Parent parent = parentRepository.findById(parentId)
            .orElseThrow(() -> new ResourceNotFoundException("Parent", parentId));
        parent.setPasswordHash(passwordEncoder.encode(password));
        parent.setPasswordSalt("bcrypt");
        parentRepository.save(parent);
    }

    @Transactional
    public VerifyPasswordResponse verifyPassword(UUID parentId, String password) {
        Parent parent = parentRepository.findById(parentId)
            .orElseThrow(() -> new ResourceNotFoundException("Parent", parentId));

        // 检查是否被锁定
        if (parent.isLocked()) {
            return new VerifyPasswordResponse(false, 0, parent.getLockedUntil());
        }

        boolean matches = passwordEncoder.matches(password, parent.getPasswordHash());
        if (matches) {
            parent.setPasswordAttempts(0);
            parent.setLockedUntil(null);
            parentRepository.save(parent);
            return new VerifyPasswordResponse(true, 3, null);
        }

        // 错误次数 +1
        parent.setPasswordAttempts(parent.getPasswordAttempts() + 1);
        int attemptsLeft = 3 - parent.getPasswordAttempts();

        if (attemptsLeft <= 0) {
            parent.setLockedUntil(Instant.now().plusSeconds(30));
            parent.setPasswordAttempts(0);
            parentRepository.save(parent);
            return new VerifyPasswordResponse(false, 0, parent.getLockedUntil());
        }

        parentRepository.save(parent);
        return new VerifyPasswordResponse(false, attemptsLeft, null);
    }
}
