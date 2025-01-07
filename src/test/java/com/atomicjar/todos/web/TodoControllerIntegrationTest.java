package com.atomicjar.todos.web;

import com.atomicjar.todos.entity.Todo;
import com.atomicjar.todos.repository.TodoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
class TodoControllerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:14-alpine"))
            .withReuse(true);

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TodoRepository todoRepository;

    private String baseUrl;
    private Todo testTodo;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/todos";
        testTodo = new Todo("Test todo");
        testTodo.setCompleted(false);
        testTodo.setOrder(1);
        testTodo = todoRepository.save(testTodo);
    }

    @AfterEach
    void tearDown() {
        todoRepository.deleteAll();
    }

    @Test
    void shouldGetAllTodos() {
        ResponseEntity<Todo[]> response = restTemplate.getForEntity(baseUrl, Todo[].class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody()[0].getTitle()).isEqualTo("Test todo");
    }

    @Test
    void shouldGetTodoById() {
        ResponseEntity<Todo> response = restTemplate.getForEntity(
                baseUrl + "/" + testTodo.getId(), Todo.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getTitle()).isEqualTo("Test todo");
    }

    @Test
    void shouldCreateNewTodo() {
        Todo newTodo = new Todo("New todo");
        
        ResponseEntity<Todo> response = restTemplate.postForEntity(baseUrl, newTodo, Todo.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getTitle()).isEqualTo("New todo");
        assertThat(response.getBody().getId()).isNotNull();
    }

    @Test
    void shouldUpdateTodo() {
        Todo updateTodo = new Todo("Updated todo");
        updateTodo.setCompleted(true);
        
        restTemplate.patchForObject(
                baseUrl + "/" + testTodo.getId(), updateTodo, Todo.class);

        Todo updatedTodo = todoRepository.findById(testTodo.getId()).orElseThrow();
        assertThat(updatedTodo.getTitle()).isEqualTo("Updated todo");
        assertThat(updatedTodo.getCompleted()).isTrue();
    }

    @Test
    void shouldDeleteTodo() {
        restTemplate.delete(baseUrl + "/" + testTodo.getId());
        
        assertThat(todoRepository.findById(testTodo.getId())).isEmpty();
    }

    @Test
    void shouldDeleteAllTodos() {
        restTemplate.delete(baseUrl);
        
        assertThat(todoRepository.count()).isZero();
    }
}