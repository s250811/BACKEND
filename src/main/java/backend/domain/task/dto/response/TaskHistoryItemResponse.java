package backend.domain.task.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record TaskHistoryItemResponse(
        Long id,
        @JsonProperty("task_name") String taskName,
        String description,
        @JsonProperty("assignee_id") Long assigneeId,
        @JsonProperty("duration_days") Double durationDays,
        @JsonProperty("start_date") LocalDateTime startDate,
        @JsonProperty("end_date") LocalDateTime endDate
) {}
