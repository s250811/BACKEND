package backend.application.service.validation;

import backend.application.port.in.workspace.WorkspaceUseCase.*;
import backend.domain.workspace.dto.request.UpdateWorkspaceRequest;
import backend.infrastructure.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class WorkspaceValidationService {

    private final WorkspaceSecurityValidator securityValidator;
    private final WorkspaceBusinessValidator businessValidator;

    public Mono<UpdateWorkspaceRequest> validateCreateWorkspace(UpdateWorkspaceRequest request) {
        return SecurityUtils.getCurrentUserId()
                .flatMap(userId -> businessValidator.validateCreateWorkspace(request, userId));
    }

    public Mono<UpdateWorkspaceRequest> validateUpdateWorkspace(UpdateWorkspaceRequest request) {
        return SecurityUtils.getCurrentUserId()
                .flatMap(userId -> securityValidator.validateWorkspaceOwner(request.workspaceId(), userId))
                .thenReturn(request);
    }

    public Mono<Long> validateGetWorkspace(Long workspaceId) {
        return businessValidator.validateWorkspaceExists(workspaceId)
                .then(securityValidator.validateWorkspaceAccess(workspaceId));
    }
}