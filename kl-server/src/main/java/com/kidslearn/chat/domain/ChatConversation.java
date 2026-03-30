package com.kidslearn.chat.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "chat_conversations")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ChatConversation {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "parent_id", nullable = false)
    private UUID parentId;

    @Column(name = "child_id")
    private UUID childId;

    @Column(nullable = false, length = 20)
    private String role;  // 'user' / 'assistant'

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tool_calls", columnDefinition = "jsonb")
    private List<Object> toolCalls;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tool_results", columnDefinition = "jsonb")
    private Object toolResults;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
