package backend.domain.workspaceMember.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WorkspaceMemberRole {

    OWNER("OWNER"),
    MEMBER("MEMBER");

    private final String value;
}