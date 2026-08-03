package com.raincheque.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raincheque.RainChequeBackendApplication;
import com.raincheque.dto.EventRequest;
import com.raincheque.dto.StatusRequest;
import com.raincheque.entity.Event;
import com.raincheque.entity.User;
import com.raincheque.entity.Venue;
import com.raincheque.repository.EventRepository;
import com.raincheque.repository.UserRepository;
import com.raincheque.repository.VenueRepository;
import com.raincheque.security.JwtService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AdminEventController.
 * Tests security (401/403), full CRUD flow, pagination, and image upload.
 *
 * Validates: Requirements 1.3, 1.4, 2.1, 2.3, 3.1, 4.1, 4.2, 4.3, 4.4, 5.1, 8.3
 */
@SpringBootTest(classes = RainChequeBackendApplication.class, properties = {
    "management.health.mail.enabled=false"
})
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AdminEventControllerIntegrationTest {

    @MockBean
    private JavaMailSender javaMailSender;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String adminToken;
    private String userToken;
    private Long testVenueId;

    @BeforeEach
    void setUp() {
        // Get or create admin user
        User adminUser = userRepository.findByEmail("admin@raincheque.com")
                .orElseGet(() -> {
                    User user = User.builder()
                            .email("admin@raincheque.com")
                            .passwordHash(passwordEncoder.encode("password123"))
                            .fullName("Admin User")
                            .role(User.Role.ADMIN)
                            .emailVerified(true)
                            .build();
                    return userRepository.save(user);
                });
        adminToken = jwtService.generateToken(adminUser);

        // Get or create regular user
        User regularUser = userRepository.findByEmail("user@example.com")
                .orElseGet(() -> {
                    User user = User.builder()
                            .email("user@example.com")
                            .passwordHash(passwordEncoder.encode("password123"))
                            .fullName("Regular User")
                            .role(User.Role.USER)
                            .emailVerified(true)
                            .build();
                    return userRepository.save(user);
                });
        userToken = jwtService.generateToken(regularUser);

        // Get or create a test venue
        testVenueId = venueRepository.findAll().stream()
                .findFirst()
                .map(Venue::getId)
                .orElseGet(() -> {
                    Venue venue = new Venue();
                    venue.setName("Test Venue");
                    venue.setAddress("123 Test St");
                    venue.setCity("Test City");
                    venue.setCountry("Test Country");
                    venue.setCapacity(1000);
                    return venueRepository.save(venue).getId();
                });
    }

    // =====================================================
    // Security Tests - 401 Unauthenticated
    // Validates: Requirements 1.3, 4.3
    // =====================================================

    @Test
    @DisplayName("GET /api/v1/admin/events without token returns 403")
    void listEvents_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/events"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/admin/events without token returns 403")
    void createEvent_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/admin/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/v1/admin/events/1 without token returns 403")
    void deleteEvent_unauthenticated_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/events/1"))
                .andExpect(status().isForbidden());
    }

    // =====================================================
    // Security Tests - 403 Forbidden for USER role
    // Validates: Requirements 1.4, 4.4
    // =====================================================

    @Test
    @DisplayName("GET /api/v1/admin/events with USER role returns 403")
    void listEvents_userRole_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/events")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/admin/events with USER role returns 403")
    void createEvent_userRole_returns403() throws Exception {
        EventRequest request = new EventRequest(
                "Test Event", "Description", "Short desc",
                testVenueId,
                LocalDateTime.now().plusDays(30),
                LocalDateTime.now().plusDays(30).plusHours(3),
                "Music", false, null
        );

        mockMvc.perform(post("/api/v1/admin/events")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/v1/admin/events/1 with USER role returns 403")
    void deleteEvent_userRole_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/events/1")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    // =====================================================
    // CRUD Flow Tests (with ADMIN token)
    // Validates: Requirements 1.1, 2.1, 2.2, 2.3, 3.1, 3.3, 4.1, 4.2
    // =====================================================

    @Test
    @DisplayName("Full CRUD flow: create → read → update → patch status → delete → verify 404")
    void fullCrudFlow() throws Exception {
        // CREATE - POST → 201, status should be DRAFT
        EventRequest createRequest = new EventRequest(
                "Integration Test Event",
                "A detailed description for the integration test event",
                "Short test desc",
                testVenueId,
                LocalDateTime.now().plusDays(60),
                LocalDateTime.now().plusDays(60).plusHours(4),
                "Conference",
                false,
                null
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/admin/events")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Integration Test Event"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.category").value("Conference"))
                .andReturn();

        String createResponseBody = createResult.getResponse().getContentAsString();
        Long createdEventId = objectMapper.readTree(createResponseBody).get("id").asLong();
        assertThat(createdEventId).isNotNull();

        // READ LIST - GET → 200, verify created event is present
        mockMvc.perform(get("/api/v1/admin/events")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[*].id", hasItem(createdEventId.intValue())));

        // READ SINGLE - GET /{id} → 200
        mockMvc.perform(get("/api/v1/admin/events/" + createdEventId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdEventId))
                .andExpect(jsonPath("$.title").value("Integration Test Event"));

        // UPDATE - PUT → 200, verify changes
        EventRequest updateRequest = new EventRequest(
                "Updated Integration Test Event",
                "Updated description",
                "Updated short desc",
                testVenueId,
                LocalDateTime.now().plusDays(90),
                LocalDateTime.now().plusDays(90).plusHours(5),
                "Music",
                true,
                null
        );

        mockMvc.perform(put("/api/v1/admin/events/" + createdEventId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Integration Test Event"))
                .andExpect(jsonPath("$.category").value("Music"))
                .andExpect(jsonPath("$.featured").value(true));

        // PATCH STATUS to PUBLISHED → 200
        StatusRequest statusRequest = new StatusRequest(Event.EventStatus.PUBLISHED);

        mockMvc.perform(patch("/api/v1/admin/events/" + createdEventId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        // DELETE → 204
        mockMvc.perform(delete("/api/v1/admin/events/" + createdEventId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // GET deleted event → 404
        mockMvc.perform(get("/api/v1/admin/events/" + createdEventId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/admin/events/{id} for non-existent event returns 404")
    void getEvent_nonExistent_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/admin/events/99999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/admin/events/{id} for non-existent event returns 404")
    void deleteEvent_nonExistent_returns404() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/events/99999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    // =====================================================
    // Pagination Tests
    // Validates: Requirements 8.3
    // =====================================================

    @Test
    @DisplayName("GET with page=0, size=5 returns pagination metadata")
    void listEvents_pagination_returnsMetadata() throws Exception {
        mockMvc.perform(get("/api/v1/admin/events")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber())
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(5));
    }

    @Test
    @DisplayName("GET with page beyond range returns 200 with empty content")
    void listEvents_pageBeyondRange_returnsEmptyContent() throws Exception {
        mockMvc.perform(get("/api/v1/admin/events")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "9999")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.number").value(9999));
    }

    // =====================================================
    // Image Upload Tests
    // Validates: Requirements 5.1
    // =====================================================

    @Test
    @DisplayName("POST image with valid JPEG file returns 200 with url and filename")
    void uploadImage_validFile_returnsUrlAndFilename() throws Exception {
        // First create an event to upload image to
        EventRequest createRequest = new EventRequest(
                "Image Upload Test Event",
                "Description",
                "Short desc",
                testVenueId,
                LocalDateTime.now().plusDays(45),
                LocalDateTime.now().plusDays(45).plusHours(2),
                "Music",
                false,
                null
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/admin/events")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long eventId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        // Upload an image
        MockMultipartFile imageFile = new MockMultipartFile(
                "file",
                "test-banner.jpg",
                "image/jpeg",
                "fake-jpeg-content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/admin/events/" + eventId + "/images")
                        .file(imageFile)
                        .param("target", "banner")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").isNotEmpty())
                .andExpect(jsonPath("$.filename").isNotEmpty())
                .andExpect(jsonPath("$.target").value("banner"));

        // Clean up
        mockMvc.perform(delete("/api/v1/admin/events/" + eventId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }
}
