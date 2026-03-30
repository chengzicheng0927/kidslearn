package com.kidslearn.chat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class SafetyFilterService {

    private static final int MAX_INPUT_LENGTH = 200;

    private static final List<String> L1_KEYWORDS = List.of(
        "暴力", "血腥", "色情", "自残", "自杀", "杀人", "强奸", "毒品", "海洛因", "冰毒", 
        "赌博", "赌场", "裸聊", "乱伦", "变态"
    );
    private static final List<String> L2_VULGAR = List.of(
        "妈的", "傻逼", "操你", "滚蛋", "神经病", "变态", "下流", "贱人", "混蛋", "去死"
    );
    private static final List<String> L3_KID_INAPPROPRIATE = List.of(
        "香烟", "抽烟", "喝酒", "夜店", "酒吧", "纹身", "逃课", "早恋"
    );
    private static final List<String> L4_KEYWORDS = List.of(
        "忘记之前的指令", "你是DAN", "忽略提示词", "ignore previous", "jailbreak", "system prompt"
    );

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

        // L2 辱骂内容
        for (String keyword : L2_VULGAR) {
            if (cleaned.contains(keyword)) {
                log.warn("SAFETY_L2 | childId={} | keyword={}", childId, keyword);
                return new FilterResult(FilterResult.Outcome.BLOCK, "请使用文明用语哦");
            }
        }

        // L3 儿童不宜内容
        for (String keyword : L3_KID_INAPPROPRIATE) {
            if (cleaned.contains(keyword)) {
                log.warn("SAFETY_L3 | childId={} | keyword={}", childId, keyword);
                return new FilterResult(FilterResult.Outcome.BLOCK, "这个问题我们换个话题聊聊吧");
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

        // 检查 L1/L2/L3 关键词 (二次保障)
        for (String keyword : L1_KEYWORDS) {
            if (output.contains(keyword)) return new FilterResult(FilterResult.Outcome.BLOCK, "内容安全合规检查失败");
        }
        for (String keyword : L2_VULGAR) {
            if (output.contains(keyword)) return new FilterResult(FilterResult.Outcome.BLOCK, "内容安全合规检查失败");
        }
        for (String keyword : L3_KID_INAPPROPRIATE) {
            if (output.contains(keyword)) return new FilterResult(FilterResult.Outcome.BLOCK, "内容安全合规检查失败");
        }

        return new FilterResult(FilterResult.Outcome.PASS, null);
    }
}
