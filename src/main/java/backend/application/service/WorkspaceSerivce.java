package backend.application.service;

import backend.application.port.in.WorkspaceUseCase;
import backend.application.port.out.folder.FolderRepositoryPort;
import backend.application.port.out.project.ProjectRepositoryPort;
import backend.application.port.out.task.TaskRepositoryPort;
import backend.application.port.out.user.UserRepositoryPort;
import backend.application.port.out.workspace.WorkspaceMemberRepositoryPort;
import backend.application.port.out.workspace.WorkspaceRepositoryPort;
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


    @Override
    public Mono<Void> createWorkspace(CreateWorkspaceCommand command) {

        return getCurrentUser()
                .flatMap(user -> {
                    return isOwnerOfWorkspace(user.getId())
                            .flatMap(isOwner -> {
                                if (isOwner) {
                                    // 자신이 주인인 워크스페이스는 하나만 가질 수 있다
                                    return Mono.error(new WorkspaceException(WorkspaceErrorCode.USER_ALREADY_OWNS_WORKSPACE));
                                }

                                Workspace build = Workspace.builder()
                                        .workspaceName(command.workspaceName())
                                        .workspaceImgUrl(command.workspaceUrl())
                                        .description(command.description())
                                        .build();

                                return saveWorkspace(build)
                                        .flatMap(savedWorkspace -> {
                                            WorkspaceMember ownerMember = WorkspaceMember.builder()
                                                    .workspaceId(savedWorkspace.getId())
                                                    .userId(user.getId())
                                                    .nickname(user.getNickname())
                                                    .role(WorkspaceMemberRole.OWNER)
                                                    .isDeleted(false)
                                                    .build();
                                            return saveWorkspaceMember(ownerMember);
                                        });
                            });
                })
                .then();
    }

    // 워크스페이스 저장
    private Mono<Workspace> saveWorkspace(Workspace workspace) {
        return workspaceRepository.save(workspace);
    }

    // 워크스페이스 멤버 저장
    private Mono<WorkspaceMember> saveWorkspaceMember(WorkspaceMember workspaceMember) {
        return workspaceMemberRepository.save(workspaceMember);
    }


    // 유저 조회
    private Mono<User> getCurrentUser() {
        return SecurityUtils.getCurrentUserId()
                .flatMap(userId -> {
                    return userRepository.findById(UserId.of(userId))
                            .switchIfEmpty(Mono.error(new UserException(UserErrorCode.USER_NOT_FOUND)));
                });
    }

    // 워크스페이스 오너인지 확인
    private Mono<Boolean> isOwnerOfWorkspace(UserId userId) {
        return workspaceMemberRepository.findByUserId(userId.getValue())
                .map(member -> member.getRole() == WorkspaceMemberRole.OWNER)
                .defaultIfEmpty(false);
    }

    /**
     * 워크스페이스 ID로 워크스페이스 정보 조회
     * @param workspaceId
     * @return
     */
    @Override
    @Transactional
    public Mono<GetWorkspaceResult> getWorkspaceById(Long workspaceId) {
        // 현재 사용자 정보 조회
        return getCurrentUser()
                .flatMap(currentUser -> {
                    Long currentUserId = currentUser.getId().getValue();

                    // 워크스페이스 기본 정보 조회
                    Mono<Workspace> workspaceMono = workspaceRepository.findById(workspaceId)
                            .switchIfEmpty(Mono.error(new WorkspaceException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND)));

                    // 현재 사용자가 워크스페이스 멤버인지 확인
                    Mono<WorkspaceMember> membershipMono = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, currentUserId)
                            .switchIfEmpty(Mono.error(new WorkspaceException(WorkspaceErrorCode.NOT_WORKSPACE_MEMBER)));

                    // 워크스페이스의 모든 멤버 정보를 한 번에 조회하여 처리
                    Mono<List<MemberInfo>> membersInfoMono = membershipMono.then(
                            workspaceMemberRepository.findAllByWorkspaceId(workspaceId)
                                    .collectList()
                                    .flatMap(members -> {
                                        // 워크스페이스 멤버 모두 조회
                                        List<Long> userIds = members.stream()
                                                .map(member -> member.getUserId().getValue())
                                                .distinct()
                                                .collect(Collectors.toList());

                                        return userRepository.findAllById(userIds)
                                                .collectMap(user -> user.getId().getValue(), Function.identity())
                                                .map(userMap ->
                                                        members.stream()
                                                                .map(member -> {
                                                                    User user = userMap.get(member.getUserId().getValue());

                                                                    return new MemberInfo(
                                                                            user.getId().getValue(),
                                                                            user.getNickname(),
                                                                            user.getProfileImageUrl(),
                                                                            member.getRole().name()
                                                                    );
                                                                })
                                                                .collect(Collectors.toList())
                                                );
                                    })
                    );

                    // 계층구조로 Workspace 관련 데이터 조회
                    Mono<List<FolderInfo>> foldersInfoMono = membershipMono.then(buildHierarchicalStructure(workspaceId));

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
                });
    }

    /**
     * N+1 쿼리 문제를 해결하기 위한 계층 구조 조회 메서드
     */
    private Mono<List<FolderInfo>> buildHierarchicalStructure(Long workspaceId) {
        // 모든 폴더를 한 번에 조회
        Mono<List<Folder>> foldersMono = folderRepository.findAllByWorkspaceId(workspaceId)
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
