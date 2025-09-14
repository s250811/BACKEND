package backend.application.service.validation;

import backend.application.port.in.TaskUseCase.UpdateTaskCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class TaskValidationService {

    private final TaskSecurityValidator securityValidator;
    private final TaskHierarchyValidator hierarchyValidator;

    public Mono<UpdateTaskCommand> validate(UpdateTaskCommand command) {
        return Mono.just(command)
                .flatMap(securityValidator::validate)
                .flatMap(hierarchyValidator::validate);
    }
}
