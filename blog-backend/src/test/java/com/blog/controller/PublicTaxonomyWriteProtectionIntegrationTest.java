package com.blog.controller;

import com.blog.entity.User;
import com.blog.repository.CategoryRepository;
import com.blog.repository.TagRepository;
import com.blog.repository.UserRepository;
import com.blog.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Transactional
class PublicTaxonomyWriteProtectionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private String writerToken;

    @BeforeEach
    void setUp() {
        User writer = userRepository.save(User.builder()
                .username("taxonomy-writer")
                .password("unused")
                .email("taxonomy-writer@example.com")
                .role("USER")
                .build());
        writerToken = jwtUtil.generateToken(writer.getUsername());
    }

    @Test
    void categoriesRemainPubliclyReadable() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk());
    }

    @Test
    void tagsRemainPubliclyReadable() throws Exception {
        mockMvc.perform(get("/api/tags"))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousCannotCreateCategory() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"anonymous-category\"}"))
                .andExpect(status().isForbidden());

        assertThat(categoryRepository.findByName("anonymous-category")).isEmpty();
    }

    @Test
    void anonymousCannotCreateTag() throws Exception {
        mockMvc.perform(post("/api/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"anonymous-tag\"}"))
                .andExpect(status().isForbidden());

        assertThat(tagRepository.findByName("anonymous-tag")).isEmpty();
    }

    @Test
    void authenticatedWriterCanCreateCategoryAndTag() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + writerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"integration-category\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("integration-category"));

        mockMvc.perform(post("/api/tags")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + writerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"integration-tag\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("integration-tag"));
    }
}
