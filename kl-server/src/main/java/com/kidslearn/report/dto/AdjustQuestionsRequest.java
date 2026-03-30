package com.kidslearn.report.dto;

import java.util.List;
import java.util.UUID;

public record AdjustQuestionsRequest(
    List<UUID> removedQuestionIds
) {}
