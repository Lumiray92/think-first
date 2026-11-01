package com.thinkfirst.coach;

import com.thinkfirst.auth.CurrentUser;
import com.thinkfirst.coach.dto.CoachActionRequest;
import com.thinkfirst.coach.dto.CoachStartRequest;

import com.thinkfirst.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/coach/session")
public class CoachSessionController {

    private final CoachService service;
    private final CurrentUser currentUser;

    public CoachSessionController(CoachService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ApiResponse<CoachSessionView> start(
            @Valid @RequestBody(required = false) CoachStartRequest request) {
        CoachStartRequest safe = request == null ? new CoachStartRequest("", "quick") : request;
        return ApiResponse.ok(service.start(currentUser.id(), safe.idea(), safe.mode()));
    }

    @PostMapping("/{id}/turn")
    public ApiResponse<CoachSessionView> turn(@PathVariable String id,
                                              @RequestBody(required = false) CoachActionRequest action) {
        return ApiResponse.ok(service.act(id, currentUser.id(), action));
    }

    @GetMapping
    public ApiResponse<java.util.List<CoachSession>> history() {
        return ApiResponse.ok(service.historyFor(currentUser.id()));
    }

    @GetMapping("/{id}")
    public ApiResponse<CoachSessionView> get(@PathVariable String id) {
        return ApiResponse.ok(service.resumeView(id, currentUser.id()));
    }

    @GetMapping("/{id}/prd")
    public ApiResponse<CoachPrdResult> prd(@PathVariable String id) {
        return ApiResponse.ok(service.getPrd(id, currentUser.id()));
    }
}
