package com.atomicjar.todos.repository;

import com.atomicjar.todos.entity.Todo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TodoRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
            .withReuse(true);

    @Autowired
    TodoRepository todoRepository;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeEach
    void setUp() {
        Todo todo1 = new Todo("1", "Buy milk", false);
        Todo todo2 = new Todo("2", "Complete homework", true);
        Todo todo3 = new Todo("3", "Call mom", false);
        todoRepository.saveAll(List.of(todo1, todo2, todo3));
    }

    @AfterEach
    void tearDown() {
        todoRepository.deleteAll();
    }

    @Test
    void shouldGetPendingTodos() {
        List<Todo> pendingTodos = todoRepository.getPendingTodos();
        
        assertThat(pendingTodos).hasSize(2);
        assertThat(pendingTodos).extracting(Todo::getCompleted).containsOnly(false);
        assertThat(pendingTodos).extracting(Todo::getTitle)
                .containsExactlyInAnyOrder("Buy milk", "Call mom");
    }

    @Test
    void shouldGetCompletedTodos() {
        List<Todo> completedTodos = todoRepository.getCompletedTodos();
        
        assertThat(completedTodos).hasSize(1);
        assertThat(completedTodos).extracting(Todo::getCompleted).containsOnly(true);
        assertThat(completedTodos).extracting(Todo::getTitle)
                .containsExactly("Complete homework");
    }
}