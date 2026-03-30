package com.kidslearn.chat.repository;

import com.kidslearn.chat.domain.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, UUID> {

    /**
     * 获取最近 N 条对话记录
     */
    @Query(value = """
        SELECT * FROM chat_conversations
        WHERE parent_id = :parentId
        ORDER BY created_at DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<ChatConversation> findRecentByParentId(@Param("parentId") UUID parentId, @Param("limit") int limit);

    /**
     * 获取指定孩子的最近 N 条对话记录
     */
    @Query(value = """
        SELECT * FROM chat_conversations
        WHERE parent_id = :parentId AND (child_id = :childId OR child_id IS NULL)
        ORDER BY created_at DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<ChatConversation> findRecentByParentIdAndChildId(
        @Param("parentId") UUID parentId,
        @Param("childId") UUID childId,
        @Param("limit") int limit);

    /**
     * 删除超过保留期限的记录
     */
    @Modifying
    @Query(value = """
        DELETE FROM chat_conversations
        WHERE created_at < :before
        """, nativeQuery = true)
    int deleteOldRecords(@Param("before") java.time.Instant before);
}
