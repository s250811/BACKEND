package backend.domain.task.dto.response;

import backend.domain.task.model.SimilarTask;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public record PredictTaskResponse(
        @JsonProperty("predicted_duration_days") Double predictedDurationDays,
        @JsonProperty("confidence_score") Double confidenceScore,
        @JsonProperty("similar_tasks") List<SimilarTask> similarTasks,
        String reasoning
) {}
