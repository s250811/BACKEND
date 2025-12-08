package backend.domain.task.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record PredictTaskRequest(
        @JsonProperty("task_name") @NotBlank String taskName,
        @JsonProperty("description") String description,
        @JsonProperty("assignee_id") @NotNull Long assigneeId,
        @JsonProperty("similar_task_count") Integer similarTaskCount
) {
    public PredictTaskRequest {
        if (similarTaskCount == null) {
            similarTaskCount = 5;
        }
    }
}
