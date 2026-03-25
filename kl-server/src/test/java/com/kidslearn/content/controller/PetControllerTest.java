package com.kidslearn.content.controller;

import com.kidslearn.content.domain.PetState;
import com.kidslearn.content.service.PetService;
import com.kidslearn.user.domain.Child;
import com.kidslearn.user.repository.ChildRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PetController.class)
@AutoConfigureMockMvc(addFilters = false)
class PetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PetService petService;

    @MockBean
    private ChildRepository childRepository;

    private final UUID childId = UUID.randomUUID();
    private PetState petState;
    private Child child;

    @BeforeEach
    void setUp() {
        petState = PetState.builder()
                .childId(childId)
                .petName("小星星")
                .evolutionId(1)
                .hunger(70)
                .happiness(80)
                .xp(50)
                .xpToNext(100)
                .build();

        child = Child.builder()
                .id(childId)
                .name("小明")
                .age(8)
                .ageGroup("7-8岁")
                .stars(286)
                .streak(7)
                .dailyTargetEn(5)
                .dailyTargetMa(3)
                .todayMinutes(24)
                .build();
    }

    @Test
    void getPetState_returnsPetWithoutChildStars() throws Exception {
        when(petService.getPetState(childId)).thenReturn(petState);

        mockMvc.perform(get("/api/children/{childId}/pet", childId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("小星星"))
                .andExpect(jsonPath("$.data.evolutionId").value(1))
                .andExpect(jsonPath("$.data.happiness").value(80))
                .andExpect(jsonPath("$.data.hunger").value(70))
                .andExpect(jsonPath("$.data.xp").value(50))
                .andExpect(jsonPath("$.data.xpToNext").value(100))
                .andExpect(jsonPath("$.data.childStars").value(0));
    }

    @Test
    void feedPet_returnsPetWithChildStars() throws Exception {
        when(petService.feedPet(eq(childId), any())).thenReturn(petState);
        when(childRepository.findById(childId)).thenReturn(Optional.of(child));

        mockMvc.perform(post("/api/children/{childId}/pet/feed", childId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"foodItemId\":\"apple\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("小星星"))
                .andExpect(jsonPath("$.data.childStars").value(286));
    }

    @Test
    void feedPet_childStarsReflectsActualBalance() throws Exception {
        child.setStars(50);
        when(petService.feedPet(eq(childId), any())).thenReturn(petState);
        when(childRepository.findById(childId)).thenReturn(Optional.of(child));

        mockMvc.perform(post("/api/children/{childId}/pet/feed", childId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"foodItemId\":\"premium_meat\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.childStars").value(50));
    }

    @Test
    void interactPet_returnsPetWithoutChildStars() throws Exception {
        when(petService.interactPet(childId)).thenReturn(petState);

        mockMvc.perform(post("/api/children/{childId}/pet/interact", childId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("小星星"))
                .andExpect(jsonPath("$.data.childStars").value(0));
    }

    @Test
    void feedPet_evolutionIdIsReturnedCorrectly() throws Exception {
        petState.setEvolutionId(2);
        petState.setXpToNext(150);
        when(petService.feedPet(eq(childId), any())).thenReturn(petState);
        when(childRepository.findById(childId)).thenReturn(Optional.of(child));

        mockMvc.perform(post("/api/children/{childId}/pet/feed", childId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"foodItemId\":\"deluxe_potion\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.evolutionId").value(2))
                .andExpect(jsonPath("$.data.xpToNext").value(150))
                .andExpect(jsonPath("$.data.childStars").value(286));
    }

    @Test
    void feedPet_dirtyData_XpToNextIsZero_RestoredFromEvolutionId() throws Exception {
        // 脏数据：xpToNext=0，应从 evolutionId 反推为 100（1.5^0 * 100）
        petState.setEvolutionId(1);
        petState.setXpToNext(0);
        when(petService.feedPet(eq(childId), any())).thenReturn(petState);
        when(childRepository.findById(childId)).thenReturn(Optional.of(child));

        mockMvc.perform(post("/api/children/{childId}/pet/feed", childId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"foodItemId\":\"apple\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.xpToNext").value(100)); // 反推而非硬编码 0
    }
}
