package backend.infrastructure.adapter.out.client;

import backend.application.port.out.task.TaskHistoryPort;
import backend.application.port.out.task.TaskPredictionPort;
import backend.application.port.out.task.TaskRepositoryPort;
import backend.domain.task.dto.request.PredictTaskRequest;
import backend.domain.task.dto.response.PredictTaskResponse;
import backend.domain.task.dto.response.TaskHistoryItemResponse;
import backend.domain.task.model.SimilarTask;
import backend.domain.task.model.Task;
import backend.domain.task.model.TaskPrediction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class AiApiClientAdapter implements TaskHistoryPort, TaskPredictionPort {
    private final WebClient webClient;
    private final TaskRepositoryPort taskRepositoryPort;

    public AiApiClientAdapter(
            @Value("${ai.api.base-url}") String baseUrl,
            WebClient.Builder webClientBuilder,
            TaskRepositoryPort taskRepositoryPort) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.taskRepositoryPort = taskRepositoryPort;
    }
    @Override
    public Mono<TaskPrediction> predictDuration(String taskName, String description, Long assigneeId, Integer topK) {
        var request = PredictTaskRequest.builder()
                .taskName(taskName)
                .description(description)
                .assigneeId(assigneeId)
                .similarTaskCount(topK)
                .build();
        return webClient.post()
                .uri("/api/v1/tasks/predict")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PredictTaskResponse.class)
                .map(this::toDomain)
                .doOnError(error ->
                        log.error("AI API call failed for task: {}", taskName, error)
                );
    }

    @Override
    public Mono<Void> syncTaskHistoryToAi() {
        return taskRepositoryPort.findAllCompletedTasks()
                .map(this::toHistoryItem)
                .collectList()
                .flatMap(tasks -> {
                    if (tasks.isEmpty()) {
                        return Mono.empty();
                    }

                    return webClient.post()
                            .uri("/api/v1/tasks/history/sync")
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(tasks)
                            .retrieve()
                            .bodyToMono(Void.class)
                            .doOnSuccess(v ->
                                    log.info("Synced {} tasks to AI server", tasks.size())
                            );
                });
    }

    private TaskPrediction toDomain(PredictTaskResponse response) {
        List<SimilarTask> similarTasks = response.similarTasks().stream()
                .map(dto -> SimilarTask.builder()
                        .taskName(dto.taskName())
                        .durationDays(dto.durationDays())
                        .similarityScore(dto.similarityScore())
                        .build())
                .collect(Collectors.toList());

        return TaskPrediction.builder()
                .predictedDurationDays(response.predictedDurationDays())
                .confidenceScore(response.confidenceScore())
                .similarTasks(similarTasks)
                .reasoning(response.reasoning())
                .build();
    }

    private TaskHistoryItemResponse toHistoryItem(Task task) {
        double durationDays = 0.0;
        if (task.getStartDate() != null && task.getEndDate() != null) {
            durationDays = java.time.Duration.between(
                    task.getStartDate(),
                    task.getEndDate()
            ).toHours() / 24.0;
        }
        return TaskHistoryItemResponse.builder()
                .id(task.getId().value())
                .taskName(task.getTaskName())
                .description(task.getDescription())
                .assigneeId(task.getLastModifiedBy().value())
                .durationDays(durationDays)
                .startDate(task.getStartDate())
                .endDate(task.getEndDate())
                .build();
    }

}
