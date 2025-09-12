package backend.application.port.out.comment;

import backend.domain.comment.model.Comment;
import backend.domain.comment.model.CommentId;
import backend.domain.task.model.TaskId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CommentRepositoryPort {
    Mono<Comment> save(Comment comment);
    Mono<Comment> findById(CommentId id);
    Flux<Comment> findByTaskId(TaskId taskId);
    Flux<Comment> findByTaskIdOrderByCreatedAtDesc(TaskId taskId, int page, int size);
    Mono<Void> deleteById(CommentId id);

}
