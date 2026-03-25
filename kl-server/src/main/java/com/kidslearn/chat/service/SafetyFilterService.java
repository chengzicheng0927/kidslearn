package com.kidslearn.chat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class SafetyFilterService {

    private static final int MAX_INPUT_LENGTH = 200;

    private static final List<String> L1_KEYWORDS = List.of("暴力", "血腥", "色情", "自残", "自杀");
    private static final List<String> L4_KEYWORDS = List.of("忘记之前的指令", "你是DAN", "忽略提示词", "ignore previous");

    public record FilterResult(Outcome outcome, String reason) {
        public enum Outcome { PASS, BLOCK, BLOCK_WITH_GUIDE, REDIRECT, BLOCK_SILENT }
    }

    public FilterResult filterInput(String input, UUID childId) {
        if (input == null || input.isBlank()) {
            return new FilterResult(FilterResult.Outcome.BLOCK, "输入不能为空");
        }

        // 清理 Unicode 零宽字符
        String cleaned = input.replaceAll("[\\u200B-\\u200D\\uFEFF]", "");

        if (cleaned.length() > MAX_INPUT_LENGTH) {
            return new FilterResult(FilterResult.Outcome.BLOCK, "输入超过 %d 字限制".formatted(MAX_INPUT_LENGTH));
        }

        // L1 严重内容
        for (String keyword : L1_KEYWORDS) {
            if (cleaned.contains(keyword)) {
                log.warn("SAFETY_L1 | childId={} | keyword={}", childId, keyword);
                return new FilterResult(FilterResult.Outcome.BLOCK, "检测到不适当内容");
            }
        }

        // L4 注入攻击
        for (String keyword : L4_KEYWORDS) {
            if (cleaned.toLowerCase().contains(keyword.toLowerCase())) {
                log.warn("SAFETY_L4_INJECTION | childId={} | keyword={}", childId, keyword);
                return new FilterResult(FilterResult.Outcome.BLOCK_SILENT, "系统安全检测");
            }
        }

        return new FilterResult(FilterResult.Outcome.PASS, null);
    }

    public FilterResult filterOutput(String output) {
        if (output == null) return new FilterResult(FilterResult.Outcome.PASS, null);

        // 检查手机号
        if (output.matches(".*1[3-9]\\d{9}.*")) {
            return new FilterResult(FilterResult.Outcome.BLOCK, "输出包含手机号");
        }
        // 检查身份证号
        if (output.matches(".*\\d{17}[\\dXx].*")) {
            return new FilterResult(FilterResult.Outcome.BLOCK, "输出包含身份证号");
        }

        return new FilterResult(FilterResult.Outcome.PASS, null);
    }
}
