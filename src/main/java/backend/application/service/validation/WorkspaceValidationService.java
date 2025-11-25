package backend.application.service.validation;

import backend.application.port.in.workspace.WorkspaceUseCase.*;
import backend.infrastructure.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class WorkspaceValidationService {

    private final WorkspaceSecurityValidator securityValidator;
    private final WorkspaceBusinessValidator businessValidator;

    public Mono<CreateWorkspaceCommand> validateCreateWorkspace(CreateWorkspaceCommand command) {
        return SecurityUtils.getCurrentUserId()
                .flatMap(userId -> businessValidator.validateCreateWorkspace(command, userId));
    }

    public Mono<CreateWorkspaceCommand> validateUpdateWorkspace(CreateWorkspaceCommand command) {
        return SecurityUtils.getCurrentUserId()
                .flatMap(userId -> securityValidator.validateWorkspaceOwner(command.workspaceId(), userId))
                .thenReturn(command);
    }

    public Mono<Long> validateGetWorkspace(Long workspaceId) {
        return businessValidator.validateWorkspaceExists(workspaceId)
                .then(securityValidator.validateWorkspaceAccess(workspaceId));
    }
}