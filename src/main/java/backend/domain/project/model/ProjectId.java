package backend.domain.project.model;

import backend.domain.common.ValueObject;
import lombok.Getter;

@Getter
public class ProjectId extends ValueObject {

    private final Long value;

    public ProjectId(Long value) {
        this.value = value;
    }

    public static ProjectId of(Long id) {
        return new ProjectId(id);
    }
}
