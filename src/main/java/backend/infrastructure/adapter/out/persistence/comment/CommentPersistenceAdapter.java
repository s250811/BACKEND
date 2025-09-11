package backend.infrastructure.adapter.out.persistence.comment;

import backend.application.port.out.comment.CommentRepositoryPort;
import backend.domain.comment.model.Comment;
import backend.domain.comment.model.CommentId;
import backend.domain.task.model.TaskId;
import backend.domain.user.model.User;
import backend.domain.user.model.UserId;
import backend.infrastructure.adapter.out.persistence.user.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class CommentPersistenceAdapter implements CommentRepositoryPort {

    private final CommentR2dbcRepository repository;

    private CommentEntity toEntity(Comment comment){
        return CommentEntity.builder()
                .id(comment.getIdValue())
                .taskId(comment.getTaskId().getValue())
                .userId(comment.getUserId().getValue())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();

    }

    private Comment toDomain(CommentEntity entity) {
        return Comment.builder()
                .id(CommentId.of(entity.getId()))
                .taskId(TaskId.of(entity.getTaskId()))
                .userId(UserId.of(entity.getUserId()))
                .content(entity.getContent())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();

    }

    @Override
    public Mono<Comment> save(Comment comment) {
        CommentEntity entity = toEntity(comment);
        return repository.save(entity)
                .map(this::toDomain);
    }

    @Override
    public Mono<Comment> findById(CommentId id) {
        return repository.findById(id.getValue())
                .map(this::toDomain);
    }
    @Override
    public Flux<Comment> findByTaskId(TaskId taskId) {
        return repository.findByTaskId(taskId.getValue())
                .map(this::toDomain);
    }

    @Override
    public Flux<Comment> findByTaskIdOrderByCreatedAtDesc(TaskId taskId, int page, int size) {
        return null;
    }

    @Override
    public Mono<Void> deleteById(CommentId id) {
        return repository.deleteById(id.getValue());
    }
}