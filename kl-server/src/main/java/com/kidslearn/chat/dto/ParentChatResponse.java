package com.kidslearn.chat.dto;

import java.util.List;

public record ParentChatResponse(
    String reply, String type, ChatCardDTO card,
    Object plan, String planAction, boolean thinking
) {
    public record ChatCardDTO(
        String style, String title, String icon,
        List<RowDTO> rows, List<PhaseDetailDTO> phases, List<ActionDTO> actions
    ) {
        public record RowDTO(String label, String value) {}
        public record PhaseDetailDTO(String dayRange, String topic, int questionsPerDay, String difficulty) {}
        public record ActionDTO(String label, String action) {}
    }
}
