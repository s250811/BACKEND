package backend.application.service;

import backend.application.port.in.WorkspaceUseCase;
import backend.application.port.out.folder.FolderRepositoryPort;
import backend.application.port.out.project.ProjectRepositoryPort;
import backend.application.port.out.task.TaskRepositoryPort;
import backend.application.port.out.user.UserRepositoryPort;
import backend.application.port.out.workspace.WorkspaceMemberRepositoryPort;
import backend.application.port.out.workspace.WorkspaceRepositoryPort;
import backend.application.service.validation.WorkspaceValidationService;
import backend.domain.folder.model.Folder;
import backend.domain.folder.model.FolderId;
import backend.domain.project.model.Project;
import backend.domain.project.model.ProjectId;
import backend.domain.task.model.Task;
import backend.domain.user.model.User;
import backend.domain.user.model.UserId;
import backend.domain.workspace.model.Workspace;
import backend.domain.workspace.model.WorkspaceId;
import backend.domain.workspaceMember.model.WorkspaceMember;
import backend.domain.workspaceMember.model.WorkspaceMemberRole;
import backend.exception.user.UserErrorCode;
import backend.exception.user.UserException;
import backend.exception.workspace.WorkspaceErrorCode;
import backend.exception.workspace.WorkspaceException;
import backend.infrastructure.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkspaceSerivce implements WorkspaceUseCase {

    private final UserRepositoryPort userRepository;
    private final WorkspaceRepositoryPort workspaceRepository;
    private final WorkspaceMemberRepositoryPort workspaceMemberRepository;
    private final FolderRepositoryPort folderRepository;
    private final ProjectRepositoryPort projectRepository;
    private final TaskRepositoryPort taskRepository;
    private final WorkspaceValidationService validationService;

    @Override
    public Mono<Void> createWorkspace(CreateWorkspaceCommand command) {
        return validationService.validateCreateWorkspace(command)
                .flatMap(validatedCommand -> {
                    return saveWorkspace(validatedCommand)
                            .flatMap(this::saveWorkspaceMember);
                })
                .then();
    }

    // 워크스페이스 저장
    private Mono<Workspace> saveWorkspace(CreateWorkspaceCommand command) {
        Workspace workspace = Workspace.builder()
                .workspaceName(command.workspaceName())
                .workspaceImgUrl(command.workspaceUrl())
                .description(command.description())
                .build();
        return workspaceRepository.save(workspace);
    }

    // 워크스페이스 멤버 저장
    private Mono<WorkspaceMember> saveWorkspaceMember(Workspace workspace) {
        return SecurityUtils.getCurrentUserId()
                .flatMap(userIdStr -> {
                    Long userId = Long.valueOf(userIdStr);
                    WorkspaceMember owner = WorkspaceMember.builder()
                            .workspaceId(workspace.getId())
                            .userId(UserId.of(userId))
                            .role(WorkspaceMemberRole.OWNER)
                            .isDeleted(false)
                            .build();
                    return workspaceMemberRepository.save(owner);
                });
    }

    /**
     * 워크스페이스 ID로 워크스페이스 정보 조회
     * @param workspaceId
     * @return
     */
    @Override
    @Transactional
    public Mono<GetWorkspaceResult> getWorkspaceById(Long workspaceId) {
        return validationService.validateGetWorkspace(workspaceId)
                .flatMap(this::buildWorkspaceResult);
    }

    private Mono<GetWorkspaceResult> buildWorkspaceResult(Long workspaceId) {

        // 워크스페이스 기본 정보 조회
        Mono<Workspace> workspaceMono = workspaceRepository.findById(workspaceId)
                .switchIfEmpty(Mono.error(new WorkspaceException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND)));

        // 워크스페이스의 모든 멤버 정보를 한 번에 조회하여 처리
        Mono<List<MemberInfo>> membersInfoMono = workspaceMemberRepository.findAllByWorkspaceId(workspaceId)
                                    .collectList()
                                    .flatMap(members -> {
                                        // 워크스페이스 멤버 모두 조회
                                        List<Long> userIds = members.stream()
                                                .map(member -> member.getIdValue())
                                                .distinct()
                                                .collect(Collectors.toList());

                                        return userRepository.findAllById(userIds)
                                                .collectMap(user -> user.getIdValue(), Function.identity())
                                                .map(userMap ->
                                                        members.stream()
                                                                .map(member -> {
                                                                    User user = userMap.get(member.getIdValue());
                                                                    return new MemberInfo(
                                                                            user.getId().getValue(),
                                                                            user.getNickname(),
                                                                            user.getProfileImageUrl(),
                                                                            member.getRole().name()
                                                                    );
                                                                })
                                                                .collect(Collectors.toList())
                                                );
                                    });

                    // 계층구조로 Workspace 관련 데이터 조회
                    Mono<List<FolderInfo>> foldersInfoMono = buildHierarchicalStructure(workspaceId);

                    // 모든 정보를 조합하여 최종 결과 생성
                    return Mono.zip(workspaceMono, membersInfoMono, foldersInfoMono)
                            .map(tuple -> {
                                Workspace workspace = tuple.getT1();
                                List<MemberInfo> members = tuple.getT2();
                                List<FolderInfo> folders = tuple.getT3();

                                // 소유자 찾기 (Optional 활용으로 더 안전하게)
                                MemberInfo owner = members.stream()
                                        .filter(member -> "OWNER".equals(member.role()))
                                        .findFirst()
                                        .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.WORKSPACE_OWNER_NOT_FOUND));

                                return new GetWorkspaceResult(
                                        workspace.getId().getValue(),
                                        workspace.getWorkspaceName(),
                                        workspace.getWorkspaceImgUrl(),
                                        workspace.getDescription(),
                                        owner,
                                        members,
                                        folders,
                                        workspace.getCreatedAt()
                                );
                            });
    }

    /**
     * N+1 쿼리 문제를 해결하기 위한 계층 구조 조회 메서드
     */
    private Mono<List<WorkspaceUseCase.FolderInfo>> buildHierarchicalStructure(Long workspaceId) {
        // 모든 폴더를 한 번에 조회
        Mono<List<Folder>> foldersMono = folderRepository.findAllByWorkspaceId(new WorkspaceId(workspaceId))
                .collectList();

        // 모든 프로젝트를 한 번에 조회
        Mono<Map<Long, List<Project>>> projectsByFolderMono = foldersMono
                .flatMap(folders -> {
                    List<FolderId> folderIds = folders.stream()
                            .map(Folder::getId)
                            .collect(Collectors.toList());

                    return projectRepository.findAllByFolderIdIn(folderIds)
                            .collectList()
                            .map(projects -> projects.stream()
                                    .collect(Collectors.groupingBy(
                                            Project::getFolderId
                                    )));
                });

        // 모든 태스크를 한 번에 조회
        Mono<Map<Long, List<Task>>> tasksByProjectMono = projectsByFolderMono
                .flatMap(projectsByFolder -> {
                    List<ProjectId> projectIds = projectsByFolder.values().stream()
                            .flatMap(List::stream)
                            .map(Project::getId)
                            .collect(Collectors.toList());

                    if (projectIds.isEmpty()) {
                        return Mono.just(Collections.emptyMap());
                    }

                    return taskRepository.findAllByProjectIdIn(projectIds)
                            .collectList()
                            .map(tasks -> tasks.stream()
                                    .collect(Collectors.groupingBy(
                                            Task::getProjectId
                                    )));
                });

        // Mono.zip에 모든 정보를 조합하여 계층 구조 생성
        return Mono.zip(foldersMono, projectsByFolderMono, tasksByProjectMono)
                .map(tuple -> {
                    List<Folder> folders = tuple.getT1();
                    Map<Long, List<Project>> projectsByFolder = tuple.getT2();
                    Map<Long, List<Task>> tasksByProject = tuple.getT3();

                    return folders.stream()
                            .map(folder -> {
                                List<ProjectInfo> projectInfos = projectsByFolder
                                        .getOrDefault(folder.getId().getValue(), Collections.emptyList())
                                        .stream()
                                        .map(project -> {
                                            List<TaskInfo> taskInfos = tasksByProject
                                                    .getOrDefault(project.getId().getValue(), Collections.emptyList())
                                                    .stream()
                                                    .map(task -> new TaskInfo(
                                                            task.getId().getValue(),
                                                            task.getParentId() != null ? task.getParentId() : null,
                                                            task.getTaskName(),
                                                            task.getTaskStatus().name()
                                                    ))
                                                    .collect(Collectors.toList());

                                            return new ProjectInfo(
                                                    project.getId().getValue(),
                                                    project.getProjectName(),
                                                    taskInfos
                                            );
                                        })
                                        .collect(Collectors.toList());

                                return new FolderInfo(
                                        folder.getId().getValue(),
                                        folder.getFolderName(),
                                        projectInfos
                                );
                            })
                            .collect(Collectors.toList());
                });
    }

    /**
     * 멤버 초대
     * @param command
     * @return
     */
    @Override
    public Mono<Void> inviteMember(InviteMemberCommand command) {

        return Mono.empty();
    }
}
