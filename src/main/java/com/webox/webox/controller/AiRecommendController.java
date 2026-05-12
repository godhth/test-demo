package com.webox.webox.controller;

import com.webox.webox.dto.AiRecommendRequest;
import com.webox.webox.dto.AiRecommendResponse;
import com.webox.webox.model.LoginUser;
import com.webox.webox.service.ai.AiRecommendService;
import com.webox.webox.support.SessionKeys;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiRecommendController {

    private final AiRecommendService aiRecommendService;

    public AiRecommendController(AiRecommendService aiRecommendService) {
        this.aiRecommendService = aiRecommendService;
    }

    @PostMapping("/api/ai/recommend")
    public ResponseEntity<AiRecommendResponse> recommend(@Valid @RequestBody AiRecommendRequest req,
                                                         HttpSession session) {
        LoginUser u = (LoginUser) session.getAttribute(SessionKeys.LOGIN_USER);
        if (u == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(aiRecommendService.recommend(u.id(), req.getQuery()));
    }
}
