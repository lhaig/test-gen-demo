package com.atomicjar.todos.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
class TodoNotFoundExceptionTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:14-alpine"))
            .withReuse(true);

    @Autowired
    private TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void shouldReturnNotFoundMessageWithId() {
        String nonExistentId = "999";
        
        var response = restTemplate.getForEntity("/api/todos/" + nonExistentId, String.class);
        
        assertThat(response.getStatusCodeValue()).isEqualTo(404);
        assertThat(response.getBody()).contains("Todo with id: '" + nonExistentId + "' not found");
    }

    @Test 
    void shouldThrowExceptionWithCustomMessage() {
        var exception = new TodoNotFoundException("test-id");
        
        assertThat(exception)
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Todo with id: 'test-id' not found");
    }

}