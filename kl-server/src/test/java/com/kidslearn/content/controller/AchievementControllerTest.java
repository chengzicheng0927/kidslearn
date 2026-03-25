package com.kidslearn.content.controller;

import com.kidslearn.content.dto.AchievementDTO;
import com.kidslearn.content.service.AchievementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
class AchievementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AchievementService achievementService;

    @Test
    void getAchievements_returnsAchievementsAndStats() throws Exception {
        UUID childId = UUID.randomUUID();
        AchievementDTO dto = new AchievementDTO(
                Collections.emptyList(),
                Collections.emptyList(),
                new AchievementDTO.AchievementStats(5, 3, 0, 0, 0)
        );
        when(achievementService.getAchievements(childId)).thenReturn(dto);

        mockMvc.perform(get("/api/children/{childId}/achievements", childId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stats.totalQuestions").value(5))
                .andExpect(jsonPath("$.data.stats.currentStreak").value(3));
    }

    @Test
    void getAchievements_noSuchChild_returnsEmptyStats() throws Exception {
        UUID unknownId = UUID.randomUUID();
        AchievementDTO dto = new AchievementDTO(
                Collections.emptyList(),
                Collections.emptyList(),
                new AchievementDTO.AchievementStats(0, 0, 0, 0, 0)
        );
        when(achievementService.getAchievements(unknownId)).thenReturn(dto);

        mockMvc.perform(get("/api/children/{childId}/achievements", unknownId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stats.totalQuestions").value(0));
    }
}
