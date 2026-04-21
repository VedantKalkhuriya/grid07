package com.grid07;

import com.grid07.service.GuardrailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
class GuardrailServiceTest {

    @MockBean
    private RedisTemplate<String, String> redisTemplate;

    @MockBean
    private ValueOperations<String, String> valueOps;

    @Autowired
    private GuardrailService guardrailService;

    @Test
    @DisplayName("Should allow bot comment when count is below 100")
    void shouldAllowBotCommentUnderLimit() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(50L); // count = 50, under limit

        boolean allowed = guardrailService.checkAndIncrementBotCount(1L);

        assertThat(allowed).isTrue();
    }

    @Test
    @DisplayName("Should reject bot comment when count hits 101")
    void shouldRejectBotCommentOverLimit() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(101L); // over the 100 cap

        boolean allowed = guardrailService.checkAndIncrementBotCount(1L);

        assertThat(allowed).isFalse();
        // should also decrement to restore count
        verify(valueOps, times(1)).decrement(anyString());
    }

    @Test
    @DisplayName("Should reject comment deeper than 20 levels")
    void shouldRejectDeepComment() {
        boolean allowed = guardrailService.checkDepthLevel(21);
        assertThat(allowed).isFalse();
    }

    @Test
    @DisplayName("Should allow comment at exactly depth 20")
    void shouldAllowCommentAtMaxDepth() {
        boolean allowed = guardrailService.checkDepthLevel(20);
        assertThat(allowed).isTrue();
    }
}
