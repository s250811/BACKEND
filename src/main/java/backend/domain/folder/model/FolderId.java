package backend.domain.folder.model;

import backend.domain.common.ValueObject;
import backend.domain.workspace.model.WorkspaceId;
import lombok.Getter;

@Getter
public class FolderId extends ValueObject {

    private final Long value;

    public FolderId(Long value) {
        this.value = value;
    }

    public static FolderId of(Long id) {
        return new FolderId(id);
    }
}