package backend.adapter.in.web;

import backend.adapter.in.common.ApiResponseDto;
import backend.application.port.in.ProjectUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectUseCase projectUseCase;

    // 프로젝트 생성
    @PostMapping("/projects")
    public Mono<ApiResponseDto<Void>> createProject(@RequestBody CreateProjectRequest request) {
        var command = new ProjectUseCase.CreateProjectCommand(
                request.folderId(),
                request.projectName(),
                request.description()
        );
        return projectUseCase.createProject(command)
                .then(Mono.just(ApiResponseDto.createSuccessNoContent("프로젝트 생성 성공")));
    }

    record CreateProjectRequest(
            Long folderId,
            String projectName,
            String description
    ) {}
}
