package com.blog.controller;

import com.blog.entity.Article;
import com.blog.entity.User;
import com.blog.repository.ArticleRepository;
import com.blog.repository.UserRepository;
import com.blog.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Transactional
class ArticleVisibilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private Article draft;
    private String authorToken;
    private String otherUserToken;

    @BeforeEach
    void setUp() {
        User author = userRepository.save(User.builder()
                .username("draft-author")
                .password("unused")
                .email("draft-author@example.com")
                .role("USER")
                .build());
        User otherUser = userRepository.save(User.builder()
                .username("other-reader")
                .password("unused")
                .email("other-reader@example.com")
                .role("USER")
                .build());

        draft = articleRepository.save(Article.builder()
                .title("Private draft")
                .slug("private-draft")
                .summary("Draft summary")
                .content("Draft content must stay private")
                .author(author)
                .status("DRAFT")
                .build());

        articleRepository.save(Article.builder()
                .title("Public article")
                .slug("public-article")
                .summary("Public summary")
                .content("Public content")
                .author(author)
                .status("PUBLISHED")
                .build());

        authorToken = jwtUtil.generateToken(author.getUsername());
        otherUserToken = jwtUtil.generateToken(otherUser.getUsername());
    }

    @Test
    void anonymousCannotReadDraftBySlug() throws Exception {
        mockMvc.perform(get("/api/articles/private-draft"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(content().string(not(containsString("Draft content must stay private"))));
    }

    @Test
    void anonymousCannotReadDraftById() throws Exception {
        mockMvc.perform(get("/api/articles/{id}", draft.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void authorCanPreviewOwnDraftWithValidToken() throws Exception {
        mockMvc.perform(get("/api/articles/private-draft")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.content").value("Draft content must stay private"));
    }

    @Test
    void otherAuthenticatedUserCannotReadDraft() throws Exception {
        mockMvc.perform(get("/api/articles/private-draft")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherUserToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void publishedArticleRemainsPubliclyReadable() throws Exception {
        mockMvc.perform(get("/api/articles/public-article"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.content").value("Public content"));
    }
}
