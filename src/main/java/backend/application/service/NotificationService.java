package backend.application.service;

import backend.application.port.in.notification.NotificationStreamUseCase;
import backend.application.port.in.notification.NotificationUseCase;
import backend.application.port.out.event.audit.EventAuditRepositoryPort;
import backend.application.port.out.notification.NotificationRepositoryPort;
import backend.application.port.out.task.TaskRepositoryPort;
import backend.domain.comment.model.Comment;
import backend.domain.event.Event;
import backend.domain.event.audit.EventAudit;
import backend.domain.event.impl.CommentUpdatedEvent;
import backend.domain.event.impl.TaskUpdatedEvent;
import backend.domain.notification.dto.response.NotificationDetailResponse;
import backend.domain.notification.model.Notification;
import backend.domain.notification.model.impl.*;
import backend.domain.task.model.Task;
import backend.domain.user.model.UserId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService implements NotificationUseCase {
    private final TaskRepositoryPort taskRepository;
    private final EventAuditRepositoryPort eventAuditRepository;
    private final NotificationRepositoryPort notificationRepository;
    private final NotificationStreamUseCase notificationStreamUseCase;

    @Override
    @Transactional
    public <T extends Serializable> Mono<Void> processEvent(Event<T> event) {
        EventAudit audit = EventAudit.createStarted(event.getId(), event.getType());

        return eventAuditRepository.save(audit)
                .flatMap(savedAudit -> processEventByType(event)
                        .then(Mono.defer(() -> {
                            savedAudit.markSuccess();
                            return eventAuditRepository.save(savedAudit);
                        }))
                        .onErrorResume(error -> {
                            savedAudit.markFailed(error.getMessage());
                            return eventAuditRepository.save(savedAudit)
                                    .then(Mono.error(error));
                        })
                )
                .then();
    }

    private <T extends Serializable> Mono<Void> processEventByType(Event<T> event) {
        return switch (event) {
            case TaskUpdatedEvent taskEvent -> processTaskUpdatedEvent(taskEvent);
            case CommentUpdatedEvent commentEvent -> processCommentEvent(commentEvent);
            default -> {
                log.warn("처리되지 않은 이벤트 타입: eventId={}, type={}", event.getId(), event.getType());
                yield Mono.empty();
            }
        };
    }

    private Mono<Void> processTaskUpdatedEvent(Event<Task> event) {
        Task currentTask = event.getParam();
        Task previousTask = currentTask.getPreviousTask();

        return Flux.fromIterable(currentTask.getManagerIds())
                .filter(managerId -> !managerId.equals(currentTask.getLastModifiedBy().value()))
                .flatMap(recipientId -> {
                    // 1. 새로운 매니저 배정 알림
                    if (isNewManagerAssigned(currentTask, previousTask, recipientId)) {
                        return createAndSaveNotification(TaskAssignedNotification.builder()
                                .senderId(UserId.of(currentTask.getLastModifiedBy().value()))
                                .recipientId(UserId.of(recipientId))
                                .isRead(false)
                                .eventId(event.getId())
                                .param(currentTask)
                                .build());
                    }

                    // 2. 상태 변경 알림
                    if (currentTask.isStatusChanged(previousTask)) {
                        return createAndSaveNotification(TaskStatusChangedNotification.builder()
                                .senderId(UserId.of(currentTask.getLastModifiedBy().value()))
                                .recipientId(UserId.of(recipientId))
                                .isRead(false)
                                .eventId(event.getId())
                                .param(currentTask)
                                .build());
                    }

                    // 3. 필드 변경 알림
                    String changedFields = currentTask.collectChangedFields(previousTask);
                    if (changedFields != null && !changedFields.trim().isEmpty()) {
                        return createAndSaveNotification(TaskFieldsChangedNotification.builder()
                                .senderId(UserId.of(currentTask.getLastModifiedBy().value()))
                                .recipientId(UserId.of(recipientId))
                                .isRead(false)
                                .eventId(event.getId())
                                .param(currentTask)
                                .build());
                    }

                    return Mono.empty();
                })
                .then()
                .then(createMentionNotifications(event, currentTask, previousTask));
    }

    private <T extends Serializable> Mono<Void> createMentionNotifications(Event<T> event, Task currentTask, Task previousTask) {
        List<Long> currentMentions = currentTask.extractMentionedUserIds();
        List<Long> previousMentions = previousTask != null ? previousTask.extractMentionedUserIds() : List.of();

        List<Long> newMentions = currentMentions.stream()
                .filter(userId -> !previousMentions.contains(userId))
                .toList();

        if (newMentions.isEmpty()) {
            return Mono.empty();
        }

        return Flux.fromIterable(newMentions)
                .filter(userId -> currentTask.getLastModifiedBy() != null && !userId.equals(currentTask.getLastModifiedBy().value()))
                .flatMap(recipientId -> createAndSaveNotification(
                        TaskMentionInDescriptionNotification.builder()
                                .senderId(UserId.of(currentTask.getLastModifiedBy().value()))
                                .recipientId(UserId.of(recipientId))
                                .isRead(false)
                                .eventId(event.getId())
                                .param(currentTask)
                                .build()))
                .then();
    }

    private boolean isNewManagerAssigned(Task currentTask, Task previousTask, Long userId) {
        if (previousTask == null) {
            return currentTask.getManagerIds().contains(userId);
        }
        return currentTask.getManagerIds().contains(userId) &&
                !previousTask.getManagerIds().contains(userId);
    }

    private  Mono<Void> processCommentEvent(Event<Comment> event) {
        Comment comment = event.getParam();

        if (!comment.hasMentions()) {
            return Mono.empty();
        }

        return taskRepository.findById(comment.getTaskId().value())
                .flatMapMany(task ->
                        Flux.fromIterable(comment.extractMentionedUserIds())
                                .filter(mentionedUserId -> !mentionedUserId.equals(comment.getLastModifiedBy()))
                                .flatMap(recipientId ->
                                        createAndSaveNotification(
                                                CommentMentionNotification.builder()
                                                        .senderId(UserId.of(comment.getLastModifiedBy().value()) )
                                                        .recipientId(UserId.of(recipientId))
                                                        .isRead(false)
                                                        .eventId(event.getId())
                                                        .param(task)
                                                        .build()
                                        )
                                )
                )
                .then();
    }

    private Mono<Notification> createAndSaveNotification(Notification notification) {
        return notificationRepository.save(notification)
                .doOnNext(savedNotification -> {
                    notificationStreamUseCase.sendToUser(
                            savedNotification.getRecipientId().value(),
                            savedNotification
                    ).subscribe();
                });
    }
    @Override
    public Flux<NotificationDetailResponse> getNotificationsByRecipientId(Long recipientId, int page, int size) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId, page, size)
                .map(entity -> new NotificationDetailResponse(
                        entity.getIdValue(),
                        entity.getSenderId().value(),
                        entity.getRecipientId().value(),
                        entity.getIsRead(),
                        entity.getType(),
                        entity.getCreatedAt(),
                        entity.getReadAt(),
                        entity.getMessage()
                ));
    }

    @Override
    public Mono<Void> markAsRead(Long notificationId) {
        return notificationRepository.findById(notificationId)
                .flatMap(notification -> {
                    notification.markAsRead();
                    return notificationRepository.save(notification);
                })
                .then();
    }

    @Override
    public Mono<Long> getUnreadCount(Long userId) {
        return notificationRepository.countUnreadByRecipientId(userId);
    }
}