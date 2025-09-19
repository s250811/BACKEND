package backend.application.service;

import backend.application.port.in.NotificationUseCase;
import backend.application.port.out.event.audit.EventAuditRepositoryPort;
import backend.application.port.out.notification.NotificationRepositoryPort;
import backend.application.port.out.task.TaskRepositoryPort;
import backend.domain.comment.model.Comment;
import backend.domain.event.Event;
import backend.domain.event.audit.EventAudit;
import backend.domain.notification.model.Notification;
import backend.domain.notification.model.impl.*;
import backend.domain.task.model.Task;
import backend.domain.user.model.UserId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService implements NotificationUseCase {

    private final NotificationRepositoryPort notificationRepository;
    private final TaskRepositoryPort taskRepository;
    private final EventAuditRepositoryPort eventAuditRepository;
    private final SseNotificationService sseNotificationService;

    private static final String CONSUMER_TOPIC = "notification-service";

    @Override
    public <T extends Serializable> Mono<Void> processNotificationEvent(Event<T> event) {
        EventAudit audit = EventAudit.createStarted(event.getId(), event.getType(), CONSUMER_TOPIC);

        return eventAuditRepository.save(audit)
                .flatMap(savedAudit -> processEventByType(event)
                        .doOnSuccess(unused -> {
                            savedAudit.markSuccess();
                            eventAuditRepository.save(savedAudit)
                                    .doOnError(saveError -> log.warn("EventAudit 성공 상태 저장 실패: eventId={}", event.getId(), saveError))
                                    .subscribe();
                            log.debug("이벤트 처리 성공: eventId={}, type={}", event.getId(), event.getType());
                        })
                        .doOnError(error -> {
                            savedAudit.markFailed(error.getMessage());
                            eventAuditRepository.save(savedAudit)
                                    .doOnError(saveError -> log.error("EventAudit 실패 상태 저장 실패: eventId={}", event.getId(), saveError))
                                    .subscribe();
                            log.error("이벤트 처리 실패: eventId={}, type={}", event.getId(), event.getType(), error);
                        })
                );
    }

    private <T extends Serializable> Mono<Void> processEventByType(Event<T> event) {
        return switch (event.getType()) {
            case TASK_UPDATED -> processTaskUpdatedEvent(event);
            case COMMENT_UPDATED -> processCommentEvent(event);
            default -> {
                log.warn("처리되지 않은 이벤트 타입: eventId={}, type={}", event.getId(), event.getType());
                yield Mono.empty();
            }
        };
    }

    private <T extends Serializable> Mono<Void> processTaskUpdatedEvent(Event<T> event) {
        return createTaskUpdateNotifications(event);
    }

    private <T extends Serializable> Mono<Void> createTaskUpdateNotifications(Event<T> event) {
        Task currentTask = (Task) event.getParam();
        Task previousTask = currentTask.getPreviousTask();

        return Flux.fromIterable(currentTask.getManagerIds())
                        .filter(managerId -> !managerId.equals(currentTask.getLastModifiedBy()))
                        .flatMap(recipientId -> {
                            // 1. 새로운 매니저 배정 알림
                            if (isNewManagerAssigned(currentTask, previousTask, recipientId)) {
                                return createAndSaveNotification(TaskAssignedNotification.builder()
                                        .senderId(UserId.of(currentTask.getLastModifiedBy()))
                                        .recipientId(UserId.of(recipientId))
                                        .isRead(false)
                                        .eventId(event.getId())
                                        .param(currentTask)
                                        .build());
                            }

                            // 2. 상태 변경 알림
                            if (currentTask.isStatusChanged(previousTask)) {
                                return createAndSaveNotification(TaskStatusChangedNotification.builder()
                                        .senderId(UserId.of(currentTask.getLastModifiedBy()))
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
                                        .senderId(UserId.of(currentTask.getLastModifiedBy()))
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
                        .filter(userId -> currentTask.getLastModifiedBy() != null && !userId.equals(currentTask.getLastModifiedBy()))
                        .flatMap(recipientId -> createAndSaveNotification(
                                TaskMentionInDescriptionNotification.builder()
                                        .senderId(UserId.of(currentTask.getLastModifiedBy()))
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

    private <T extends Serializable> Mono<Void> processCommentEvent(Event<T> event) {
        Comment comment = (Comment) event.getParam();

        if (!comment.hasMentions()) {
            return Mono.empty();
        }

        return taskRepository.findById(comment.getTaskId().getValue())
                .flatMapMany(task ->
                        Flux.fromIterable(comment.extractMentionedUserIds())
                                .filter(mentionedUserId -> !mentionedUserId.equals(comment.getLastModifiedBy()))
                                .flatMap(recipientId ->
                                        createAndSaveNotification(
                                                CommentMentionNotification.builder()
                                                        .senderId(UserId.of(comment.getLastModifiedBy()) )
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
                    sseNotificationService.sendNotificationToUser(
                            savedNotification.getRecipientId().getValue(),
                            savedNotification
                    ).subscribe();
                });
    }

    @Override
    public Mono<Flux<Notification>> getNotificationsByRecipientId(Long recipientId, int page, int size) {
        return Mono.just(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId, page, size));
    }

    @Override
    public Mono<Void> markAsRead(Long notificationId, Long userId) {
        return notificationRepository.findById(notificationId)
                .filter(notification -> notification.getRecipientId().equals(userId))
                .flatMap(notification -> {
                    notification.markAsRead();
                    return notificationRepository.save(notification);
                })
                .doOnNext(updatedNotification -> {
                    sseNotificationService.sendNotificationToUser(
                            updatedNotification.getRecipientId().getValue(),
                            updatedNotification
                    ).subscribe(
                            unused -> log.debug("읽음 상태 변경 실시간 전송 성공 - 알림 ID: {}", updatedNotification.getIdValue()),
                            error -> log.warn("읽음 상태 변경 실시간 전송 실패 - 알림 ID: {}, 에러: {}",
                                    updatedNotification.getIdValue(), error.getMessage())
                    );
                })
                .then();
    }

    @Override
    public Mono<Long> getUnreadCount(Long userId) {
        return notificationRepository.countUnreadByRecipientId(userId);
    }
}