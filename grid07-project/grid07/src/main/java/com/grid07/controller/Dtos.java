package com.grid07.controller;

import lombok.Data;

public class Dtos {

    @Data
    public static class CreatePostRequest {
        private Long authorUserId;
        private Long authorBotId;
        private String content;
    }

    @Data
    public static class AddCommentRequest {
        private Long authorUserId;
        private Long authorBotId;
        private String content;
        private int depthLevel = 0;
    }

    @Data
    public static class LikePostRequest {
        private Long userId;
    }

    @Data
    public static class CreateUserRequest {
        private String username;
        private boolean isPremium;
    }

    @Data
    public static class CreateBotRequest {
        private String name;
        private String personaDescription;
    }

    @Data
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;

        public static <T> ApiResponse<T> ok(T data) {
            ApiResponse<T> r = new ApiResponse<>();
            r.success = true;
            r.data = data;
            return r;
        }

        public static <T> ApiResponse<T> ok(String message, T data) {
            ApiResponse<T> r = new ApiResponse<>();
            r.success = true;
            r.message = message;
            r.data = data;
            return r;
        }
    }
}
