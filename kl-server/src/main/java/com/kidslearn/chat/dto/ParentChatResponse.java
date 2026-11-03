package com.kidslearn.chat.dto;

import java.util.List;

public record ParentChatResponse(
    String reply, String type, ChatCardDTO card,
    Object plan, String planAction, boolean thinking, String conversationId
) {
    public record ChatCardDTO(
        String style, String title, String icon,
        List<RowDTO> rows, List<PhaseDetailDTO> phases, List<ActionDTO> actions
    ) {
        public record RowDTO(String label, String value) {}
        public record PhaseDetailDTO(String dayRange, String topic, int questionsPerDay, String difficulty, List<QuestionDTO> questions) {}
        public record ActionDTO(String label, String action) {}
        public record QuestionDTO(String id, String icon, String question, List<String> options, String answer) {}
    }
}
