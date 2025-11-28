package backend.domain.task.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record UpdateTaskRequest(
        @NotNull
        Long projectId,
        @NotNull
        @Schema(description = "부모 태스크 ID (루트 태스크인 경우 0)", example = "0")
        Long parentId,
        @NotNull
        Long workspaceId,
        @Schema(example = "[1, 2, 3]")
        @Nullable
        List<Long> managerIds,
        @Nullable
        String taskName,
        @Nullable
        String taskStatus,
        @Nullable
        LocalDateTime startDate,
        @Nullable
        LocalDateTime endDate,
        @Nullable
        String description,
        @Nullable
        String fileUrl
){}