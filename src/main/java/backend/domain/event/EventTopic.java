package backend.domain.event;

import lombok.Getter;

@Getter
public enum EventTopic {
    WORKSPACE_TOPIC("workspace"),
    FOLDER_TOPIC("folder"),
    PROJECT_TOPIC("project"),
    TASK_TOPIC("task"),
    COMMENT_TOPIC("comment"),
    USER_TOPIC("user"),
    NOTIFICATION_TOPIC("notification");

    private final String name;

    EventTopic(String workspace) {
        this.name = name();
    }

}
