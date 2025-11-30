package backend.domain.task.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record TaskDetailResponse(
        Long taskId,
        String taskName,
        String status,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        String description,
        String fileUrl,
        List<ManagerResponse> managers
) {
    public record ManagerResponse(Long userId, String nickname, String profileImgUrl) {}
}
