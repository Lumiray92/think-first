package com.thinkfirst.coach;

import com.thinkfirst.coach.dto.CoachRetrieveRequest;
import com.thinkfirst.coach.rag.CoachRetrievalResult;
import com.thinkfirst.coach.rag.CoachRetriever;
import com.thinkfirst.coach.rag.CoachUnavailableException;
import com.thinkfirst.common.ApiResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ThinkFirst 教练检索接口。当前只支持每步固定 query；
 * 用户自由输入需要接入 EmbeddingClient 后再开放。
 */
@RestController
@RequestMapping("/api/v1/coach")
public class CoachRetrieveController {

    private static final Logger log = LoggerFactory.getLogger(CoachRetrieveController.class);

    private final CoachRetriever retriever;

    public CoachRetrieveController(CoachRetriever retriever) {
        this.retriever = retriever;
    }

    @PostMapping("/retrieve")
    public ResponseEntity<ApiResponse<CoachRetrievalResult>> retrieve(
            @Valid @RequestBody CoachRetrieveRequest request) {
        if (request.query() != null && !request.query().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "自定义 query 暂未接入 EmbeddingClient，请只传 step"));
        }
        try {
            CoachRetrievalResult result = retriever.retrieve(request.step(), request.topK());
            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (CoachUnavailableException e) {
            return ResponseEntity.status(503).body(ApiResponse.error(503, e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            log.error("ThinkFirst coach 检索失败: step={}", request.step(), e);
            return ResponseEntity.status(500).body(ApiResponse.error(500, "检索失败：" + e.getMessage()));
        }
    }
}
