package backend.domain.task.model;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.stream.Stream;

@RequiredArgsConstructor
@Getter
public enum TaskStatus {
    BACKLOG("백로그"),
    TO_DO("진행 예정"),
    IN_PROGRESS("진행중"),
    PENDING("보류"),
    DONE("완료");

    private final String displayName;

    public static TaskStatus fromString(String statusString) {
        if (statusString == null || statusString.trim().isEmpty()) {
            throw new IllegalArgumentException("상태값이 비어있을 수 없습니다.");
        }

        return Stream.of(TaskStatus.values())
                .filter(status -> status.name().equalsIgnoreCase(statusString))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 상태값입니다: " + statusString));
    }
}

