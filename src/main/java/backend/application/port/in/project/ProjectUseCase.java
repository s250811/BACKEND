package backend.application.port.in.project;

import reactor.core.publisher.Mono;

public interface ProjectUseCase {
    Mono<Void> createProject(CreateProjectCommand command);

    record CreateProjectCommand(
            Long folderId,
            String projectName,
            String description
    ) {}
}
