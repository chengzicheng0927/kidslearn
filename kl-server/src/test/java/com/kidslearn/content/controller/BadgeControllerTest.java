package com.kidslearn.content.controller;

import com.kidslearn.content.dto.BadgeDTO;
import com.kidslearn.content.service.BadgeService;
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

@WebMvcTest(BadgeController.class)
@AutoConfigureMockMvc(addFilters = false)
class BadgeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BadgeService badgeService;

    @Test
    void getBadges_returnsBadgesAndCurrentLevel() throws Exception {
        UUID childId = UUID.randomUUID();
        BadgeDTO dto = new BadgeDTO(
                Collections.emptyList(),
                Collections.emptyList(),
                15,
                1
        );
        when(badgeService.getBadges(childId)).thenReturn(dto);

        mockMvc.perform(get("/api/children/{childId}/badges", childId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAnswered").value(15))
                .andExpect(jsonPath("$.data.currentLevel").value(1));
    }

    @Test
    void getBadges_noSuchChild_returnsZeroStats() throws Exception {
        UUID unknownId = UUID.randomUUID();
        BadgeDTO dto = new BadgeDTO(
                Collections.emptyList(),
                Collections.emptyList(),
                0,
                0
        );
        when(badgeService.getBadges(unknownId)).thenReturn(dto);

        mockMvc.perform(get("/api/children/{childId}/badges", unknownId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAnswered").value(0))
                .andExpect(jsonPath("$.data.currentLevel").value(0));
    }
}
