package com.kidslearn.user.dto;

import java.util.List;

public record LoginResponse(
    String accessToken,
    String tokenType,
    long expiresIn,
    ParentDTO parent,
    List<ChildDTO> children
) {}
