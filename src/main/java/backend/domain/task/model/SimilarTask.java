package backend.domain.task.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record SimilarTask(
        @JsonProperty("task_name") String taskName,
        @JsonProperty("duration_days") Double durationDays,
        @JsonProperty("similarity_score") Double similarityScore) {
}