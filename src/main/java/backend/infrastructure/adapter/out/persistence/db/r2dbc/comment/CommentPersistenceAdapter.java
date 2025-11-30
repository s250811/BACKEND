package backend.infrastructure.adapter.out.persistence.db.r2dbc.comment;

import backend.application.port.out.comment.CommentRepositoryPort;
import backend.domain.comment.model.Comment;
import backend.domain.comment.model.CommentId;
import backend.domain.task.model.TaskId;
import backend.domain.user.model.UserId;
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
                .taskId(comment.getTaskId().value())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .lastModifiedBy(comment.getLastModifiedBy().value())
                .build();
    }

    private Comment toDomain(CommentEntity entity) {
        return Comment.builder()
                .id(CommentId.of(entity.getId()))
                .taskId(TaskId.of(entity.getTaskId()))
                .content(entity.getContent())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .lastModifiedBy(UserId.of(entity.getLastModifiedBy()))
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
        return repository.findById(id.value())
                .map(this::toDomain);
    }
    @Override
    public Flux<Comment> findByTaskId(TaskId taskId) {
        return repository.findByTaskId(taskId.value())
                .map(this::toDomain);
    }

    @Override
    public Flux<Comment> findByTaskIdOrderByCreatedAtDesc(TaskId taskId, int page, int size) {
        return null;
    }

    @Override
    public Mono<Void> deleteById(CommentId id) {
        return repository.deleteById(id.value());
    }
}