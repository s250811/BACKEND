package backend.domain.notification.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum NotificationType {
    TASK_ASSIGNED, TASK_STATUS_CHANGED, TASK_FIELDS_CHANGED, SUBTASK_CREATED,
    MENTION_IN_TASK_DESCRIPTION, MENTION_IN_COMMENT
}