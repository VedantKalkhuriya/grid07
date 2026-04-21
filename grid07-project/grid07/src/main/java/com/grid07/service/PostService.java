package com.grid07.service;

import com.grid07.entity.Comment;
import com.grid07.entity.Post;
import com.grid07.entity.User;
import com.grid07.entity.Bot;
import com.grid07.repository.BotRepository;
import com.grid07.repository.CommentRepository;
import com.grid07.repository.PostRepository;
import com.grid07.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final BotRepository botRepository;

    private final GuardrailService guardrailService;
    private final ViralityService viralityService;
    private final NotificationService notificationService;

    @Transactional
    public Post createPost(Long authorUserId, Long authorBotId, String content) {
        if (authorUserId == null && authorBotId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Need either authorUserId or authorBotId");
        }

        if (authorUserId != null) {
            userRepository.findById(authorUserId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        }

        if (authorBotId != null) {
            botRepository.findById(authorBotId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bot not found"));
        }

        Post post = new Post();
        post.setAuthorUserId(authorUserId);
        post.setAuthorBotId(authorBotId);
        post.setContent(content);

        Post saved = postRepository.save(post);
        log.info("Post created with id {}", saved.getId());
        return saved;
    }

    @Transactional
    public Comment addComment(Long postId, Long authorUserId, Long authorBotId, String content, int depthLevel) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));

        boolean isBotComment = authorBotId != null;

            // ── GUARDRAIL 1: Vertical cap ──
            if (!guardrailService.checkDepthLevel(depthLevel)) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Comment thread is too deep (max depth: 20)");
            }

            if (!guardrailService.checkAndIncrementBotCount(postId)) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Post has reached the maximum bot reply limit (100)");
            }

            Long postOwnerUserId = post.getAuthorUserId();
            if (postOwnerUserId != null) {
                if (!guardrailService.checkAndSetCooldown(authorBotId, postOwnerUserId)) {
                    throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                            "Bot is on cooldown for this user. Wait 10 minutes.");
                }
            }

            Comment comment = buildComment(postId, null, authorBotId, content, depthLevel);
            Comment saved = commentRepository.save(comment);

            viralityService.addBotReplyPoints(postId);

            if (postOwnerUserId != null) {
                notificationService.handleBotNotification(postOwnerUserId, bot.getName(), postId);
            }

            log.info("Bot {} commented on post {}", authorBotId, postId);
            return saved;

        } else {
            userRepository.findById(authorUserId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

            Comment comment = buildComment(postId, authorUserId, null, content, depthLevel);
            Comment saved = commentRepository.save(comment);

            viralityService.addHumanCommentPoints(postId);

            log.info("User {} commented on post {}", authorUserId, postId);
            return saved;
        }
    }

    @Transactional
    public void likePost(Long postId, Long userId) {
        postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));

        userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        viralityService.addHumanLikePoints(postId);

        log.info("User {} liked post {} (virality +20)", userId, postId);
    }

    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }

    private Comment buildComment(Long postId, Long userId, Long botId, String content, int depth) {
        Comment c = new Comment();
        c.setPostId(postId);
        c.setAuthorUserId(userId);
        c.setAuthorBotId(botId);
        c.setContent(content);
        c.setDepthLevel(depth);
        return c;
    }
}
