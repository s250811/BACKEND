package backend.application.service.validation;

import backend.application.port.in.WorkspaceUseCase.*;
import backend.exception.user.UserErrorCode;
import backend.exception.user.UserException;
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
                .switchIfEmpty(Mono.error(new UserException(UserErrorCode.USER_NOT_FOUND)))
                .flatMap(userId -> businessValidator.validateCreateWorkspace(command, userId));
    }

    public Mono<Long> validateGetWorkspace(Long workspaceId) {
        return businessValidator.validateWorkspaceExists(workspaceId)
                .then(securityValidator.validateWorkspaceAccess(workspaceId));
    }
}