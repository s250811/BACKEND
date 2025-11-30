package backend.domain.folder.model;

import backend.domain.common.ValueObject;
import backend.domain.workspace.model.WorkspaceId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

public record FolderId(Long value) implements ValueObject {
    @JsonCreator
    public FolderId(@JsonProperty("value") Long value) {
        this.value = value;
    }
    public static FolderId of(Long id) {
        return new FolderId(id);
    }
}