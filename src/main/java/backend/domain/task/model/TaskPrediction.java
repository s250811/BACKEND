package backend.domain.task.model;

import lombok.Builder;

import java.util.List;

@Builder
public record TaskPrediction(Double predictedDurationDays, Double confidenceScore, List<SimilarTask> similarTasks,
                             String reasoning) {
}
