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
import org.springframework.cache.annotation.Cacheable;
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
    private static final int BATCH_SIZE = 50;

    public AiApiClientAdapter(
            @Value("${ai.api.base-url}") String baseUrl,
            WebClient.Builder webClientBuilder,
            TaskRepositoryPort taskRepositoryPort) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.taskRepositoryPort = taskRepositoryPort;
    }

    @Override
    @Cacheable(
            value = "task:prediction",
            key = "#taskName + ':' + #assigneeId + ':' + #topK",
            unless = "#result == null"
    )
    public Mono<TaskPrediction> predictDuration(
            String taskName,
            String description,
            Long assigneeId,
            Integer topK) {

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
                .buffer(BATCH_SIZE)
                .index()
                .concatMap(tuple -> {
                    int batchNumber = tuple.getT1().intValue() + 1;
                    List<TaskHistoryItemResponse> batch = tuple.getT2();
                    return syncBatch(batch, batchNumber);
                })
                .then();
    }

    private Mono<Void> syncBatch(List<TaskHistoryItemResponse> batch, int batchNumber) {
        // 정렬해서 넘어오므로 task id만으로 범위 확인 가능
        long firstTaskId = batch.get(0).id();
        long lastTaskId  = batch.get(batch.size() - 1).id();
        return webClient.post()
                .uri("/api/v1/tasks/history/sync")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(batch)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnError(error ->
                        log.error("Batch {} sync failed ({} tasks: idRange {}~{})", batchNumber, batch.size(), firstTaskId, lastTaskId, error)
                )
                .onErrorResume(error -> Mono.empty()); // 실패 배치 스킵
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