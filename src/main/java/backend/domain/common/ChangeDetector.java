package backend.domain.common;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.*;

@Slf4j
public class ChangeDetector {
    /**
     * 두 객체를 비교하여 변경된 필드들을 반환
     * @param original 원본 객체 (DB에서 조회한 값)
     * @param updated 변경된 객체 (업데이트할 값)
     * @param excludeFields 비교에서 제외할 필드명들
     * @return 변경된 필드명과 새로운 값의 맵
     */
    private static <T> Map<String, Object> detectChanges(T original, T updated, String... excludeFields) {
        if (original == null || updated == null) {
            throw new RuntimeException();
        }

        Set<String> excludeSet = Set.of(excludeFields);
        Map<String, Object> changes = new HashMap<>();

        Class<?> clazz = original.getClass();
        Field[] fields = getAllFields(clazz);

        for (Field field : fields) {
            if (shouldSkipField(field, excludeSet)) {
                continue;
            }

            try {
                field.setAccessible(true);
                Object originalValue = field.get(original);
                Object updatedValue = field.get(updated);

                if (!Objects.equals(originalValue, updatedValue)) {
                    changes.put(field.getName(), updatedValue);
                    log.debug("필드 변경 감지: {}={} -> {}",
                            field.getName(), originalValue, updatedValue);
                }
            } catch (IllegalAccessException e) {
                log.warn("필드 접근 실패: {}", field.getName(), e);
            }
        }
        return changes;
    }

    /**
     * 변경된 필드들의 이름을 문자열로 반환
     */
    public static <T> String getChangedFieldsAsString(T original, T updated, String... excludeFields) {
        Map<String, Object> changes = detectChanges(original, updated, excludeFields);
        if (changes.isEmpty()) {
            throw new RuntimeException();
        }
        List<String> fieldNames = new ArrayList<>(changes.keySet());
        return String.join(", ", fieldNames);
    }

    /**
     * 특정 필드가 변경되었는지 확인
     */
    public static <T> boolean isFieldChanged(T original, T updated, String fieldName) {
        Map<String, Object> changes = detectChanges(original, updated);
        return changes.containsKey(fieldName);
    }

    private static Field[] getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return fields.toArray(new Field[0]);
    }

    private static boolean shouldSkipField(Field field, Set<String> excludeFields) {
        return skipIfRequested(excludeFields, field.getName())
                || skipIfIdOrAudit(field.getName())
                || skipIfStaticOrFinal(field);
    }

    private static boolean skipIfRequested(Set<String> excludeFields, String fieldName) {
        if (excludeFields.contains(fieldName)) {
            return true;
        }
        return false;
    }

    private static boolean skipIfIdOrAudit(String fieldName) {
        if (fieldName.equals("id") ||
                fieldName.equals("createdAt") ||
                fieldName.equals("updatedAt") ||
                fieldName.startsWith("$")) {
            return true;
        }
        return false;
    }

    private static boolean skipIfStaticOrFinal(Field field) {
        int modifiers = field.getModifiers();
        if (java.lang.reflect.Modifier.isStatic(modifiers) ||
                java.lang.reflect.Modifier.isFinal(modifiers)) {
            return true;
        }
        return false;
    }
}