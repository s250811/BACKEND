package backend.infrastructure.adapter.in.web;

import backend.application.port.out.auth.PasswordEncodingPort;
import backend.infrastructure.adapter.in.common.ApiResponseDto;
import backend.application.port.in.WorkspaceUseCase;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceUseCase workspaceUseCase;

    @Operation(summary = "워크스페이스 생성, 수정")
    @PostMapping
    public Mono<ApiResponseDto<Void>> createWorkspace(@RequestBody CreateWorkspaceRequest request) {

        WorkspaceUseCase.CreateWorkspaceCommand command = new WorkspaceUseCase.CreateWorkspaceCommand(
                request.workspaceId,
                request.workspaceName(),
                request.workspaceUrl(),
                request.description()
        );

        return workspaceUseCase.createOrUpdateWorkspace(command)
                .then(Mono.just(ApiResponseDto.createSuccessNoContent(null)));
    }

    @Operation(summary = "워크스페이스 멤버 초대")
    @PostMapping("/invite")
    public Mono<ApiResponseDto<Void>> inviteMember(@RequestBody InviteMemberRequest request) {

        WorkspaceUseCase.InviteMemberCommand command = new WorkspaceUseCase.InviteMemberCommand(
                request.workspaceId()
        );

        return workspaceUseCase.inviteMember(command)
                .then(Mono.just(ApiResponseDto.createSuccessNoContent(null)));
    }

    @Operation(summary = "워크스페이스 상세 조회")
    @GetMapping("/{workspaceId}")
    public Mono<ApiResponseDto<GetWorkspaceResponse>> getWorkspaceById(@PathVariable Long workspaceId) {
        return workspaceUseCase.getWorkspaceById(workspaceId)
                .map(result -> ApiResponseDto.createSuccess(
                        GetWorkspaceResponse.from(result),
                        "워크스페이스 조회가 완료되었습니다."
                ));
    }


    /**
     * 워크스페이스 조회 응답 DTO
     */
    record GetWorkspaceResponse(
            Long workspaceId,
            String workspaceName,
            String imageUrl,
            String description,
            MemberInfo owner,
            List<MemberInfo> members,
            List<FolderInfo> folders,
            LocalDateTime createdAt
    ) {
        public static GetWorkspaceResponse from(WorkspaceUseCase.GetWorkspaceResult result) {
            return new GetWorkspaceResponse(
                    result.workspaceId(),
                    result.workspaceName(),
                    result.imageUrl(),
                    result.description(),
                    MemberInfo.from(result.owner()),
                    result.members().stream()
                            .map(MemberInfo::from)
                            .toList(),
                    result.folders().stream()
                            .map(FolderInfo::from)
                            .toList(),
                    result.createdAt()
            );
        }
    }

    /**
     * 멤버 정보 DTO
     */
    record MemberInfo(
            Long userId,
            String nickname,
            String profileImageUrl,
            String role
    ) {
        public static MemberInfo from(WorkspaceUseCase.MemberInfo memberInfo) {
            return new MemberInfo(
                    memberInfo.userId(),
                    memberInfo.nickname(),
                    memberInfo.profileImageUrl(),
                    memberInfo.role()
            );
        }
    }

    /**
     * 폴더 정보 DTO
     */
    record FolderInfo(
            Long folderId,
            String folderName,
            List<ProjectInfo> projects
    ) {
        public static FolderInfo from(WorkspaceUseCase.FolderInfo folderInfo) {
            return new FolderInfo(
                    folderInfo.folderId(),
                    folderInfo.folderName(),
                    folderInfo.projects().stream()
                            .map(ProjectInfo::from)
                            .toList()
            );
        }
    }

    /**
     * 프로젝트 정보 DTO
     */
    record ProjectInfo(
            Long projectId,
            String projectName,
            List<TaskInfo> tasks
    ) {
        public static ProjectInfo from(WorkspaceUseCase.ProjectInfo projectInfo) {
            return new ProjectInfo(
                    projectInfo.projectId(),
                    projectInfo.projectName(),
                    projectInfo.tasks().stream()
                            .map(TaskInfo::from)
                            .toList()
            );
        }
    }

    /**
     * 태스크 정보 DTO
     */
    record TaskInfo(
            Long taskId,
            Long parentId,
            String taskName,
            String status
    ) {
        public static TaskInfo from(WorkspaceUseCase.TaskInfo taskInfo) {
            return new TaskInfo(
                    taskInfo.taskId(),
                    taskInfo.parentId(),
                    taskInfo.taskName(),
                    taskInfo.status()
            );
        }
    }


    record CreateWorkspaceRequest(
            Long workspaceId,
            String workspaceName,
            String workspaceUrl,
            String description
    ){}

    record InviteMemberRequest(
            Long workspaceId
    ){}
}
