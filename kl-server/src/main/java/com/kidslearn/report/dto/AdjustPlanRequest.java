package com.kidslearn.report.dto;

import java.util.UUID;

public record AdjustPlanRequest(UUID planId, String adjustments) {}
