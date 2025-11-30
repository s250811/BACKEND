package backend.application.service.validation;

import backend.domain.task.dto.request.UpdateTaskRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class TaskValidationService {

    private final TaskSecurityValidator securityValidator;
    private final TaskHierarchyValidator hierarchyValidator;

    public Mono<UpdateTaskRequest> validate(UpdateTaskRequest request) {
        return Mono.just(request)
                .flatMap(securityValidator::validate)
                .flatMap(hierarchyValidator::validate);
    }
}
