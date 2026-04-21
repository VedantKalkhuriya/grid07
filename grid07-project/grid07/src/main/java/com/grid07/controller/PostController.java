package com.grid07.controller;

import com.grid07.entity.Comment;
import com.grid07.entity.Post;
import com.grid07.service.PostService;
import com.grid07.service.ViralityService;
import com.grid07.service.GuardrailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Slf4j
public class PostController {

    private final PostService postService;
    private final ViralityService viralityService;
    private final GuardrailService guardrailService;

    @PostMapping
    public ResponseEntity<Dtos.ApiResponse<Post>> createPost(@RequestBody Dtos.CreatePostRequest request) {
        log.info("Creating post for user={} bot={}", request.getAuthorUserId(), request.getAuthorBotId());

        Post post = postService.createPost(
                request.getAuthorUserId(),
                request.getAuthorBotId(),
                request.getContent()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Dtos.ApiResponse.ok("Post created successfully", post));
    }

    @GetMapping
    public ResponseEntity<Dtos.ApiResponse<List<Post>>> getAllPosts() {
        return ResponseEntity.ok(Dtos.ApiResponse.ok(postService.getAllPosts()));
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<Dtos.ApiResponse<Comment>> addComment(
            @PathVariable Long postId,
            @RequestBody Dtos.AddCommentRequest request) {

        log.info("Comment on post {} by user={} bot={} depth={}",
                postId, request.getAuthorUserId(), request.getAuthorBotId(), request.getDepthLevel());

        Comment comment = postService.addComment(
                postId,
                request.getAuthorUserId(),
                request.getAuthorBotId(),
                request.getContent(),
                request.getDepthLevel()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Dtos.ApiResponse.ok("Comment added", comment));
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<Dtos.ApiResponse<Map<String, Object>>> likePost(
            @PathVariable Long postId,
            @RequestBody Dtos.LikePostRequest request) {

        postService.likePost(postId, request.getUserId());

        Long viralityScore = viralityService.getViralityScore(postId);

        Map<String, Object> result = Map.of(
                "postId", postId,
                "likedBy", request.getUserId(),
                "currentViralityScore", viralityScore
        );

        return ResponseEntity.ok(Dtos.ApiResponse.ok("Post liked", result));
    }

    @GetMapping("/{postId}/virality")
    public ResponseEntity<Dtos.ApiResponse<Map<String, Object>>> getViralityScore(@PathVariable Long postId) {
        Long score = viralityService.getViralityScore(postId);
        long botCount = guardrailService.getBotCount(postId);

        Map<String, Object> result = Map.of(
                "postId", postId,
                "viralityScore", score,
                "botReplyCount", botCount,
                "botReplyLimit", 100
        );

        return ResponseEntity.ok(Dtos.ApiResponse.ok(result));
    }
}
