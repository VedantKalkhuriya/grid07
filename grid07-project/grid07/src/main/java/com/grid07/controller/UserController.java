package com.grid07.controller;

import com.grid07.entity.Bot;
import com.grid07.entity.User;
import com.grid07.repository.BotRepository;
import com.grid07.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserRepository userRepository;
    private final BotRepository botRepository;

    @PostMapping("/api/users")
    public ResponseEntity<Dtos.ApiResponse<User>> createUser(@RequestBody Dtos.CreateUserRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPremium(request.isPremium());
        User saved = userRepository.save(user);
        log.info("Created user: {} (id={})", saved.getUsername(), saved.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(Dtos.ApiResponse.ok(saved));
    }

    @GetMapping("/api/users")
    public ResponseEntity<Dtos.ApiResponse<List<User>>> getAllUsers() {
        return ResponseEntity.ok(Dtos.ApiResponse.ok(userRepository.findAll()));
    }

    @GetMapping("/api/users/{id}")
    public ResponseEntity<Dtos.ApiResponse<User>> getUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));
        return ResponseEntity.ok(Dtos.ApiResponse.ok(user));
    }

    @PostMapping("/api/bots")
    public ResponseEntity<Dtos.ApiResponse<Bot>> createBot(@RequestBody Dtos.CreateBotRequest request) {
        Bot bot = new Bot();
        bot.setName(request.getName());
        bot.setPersonaDescription(request.getPersonaDescription());
        Bot saved = botRepository.save(bot);
        log.info("Created bot: {} (id={})", saved.getName(), saved.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(Dtos.ApiResponse.ok(saved));
    }

    @GetMapping("/api/bots")
    public ResponseEntity<Dtos.ApiResponse<List<Bot>>> getAllBots() {
        return ResponseEntity.ok(Dtos.ApiResponse.ok(botRepository.findAll()));
    }

    @GetMapping("/api/bots/{id}")
    public ResponseEntity<Dtos.ApiResponse<Bot>> getBot(@PathVariable Long id) {
        Bot bot = botRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Bot not found"));
        return ResponseEntity.ok(Dtos.ApiResponse.ok(bot));
    }
}
