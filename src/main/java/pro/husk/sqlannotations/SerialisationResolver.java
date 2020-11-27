package pro.husk.sqlannotations;

import lombok.Getter;
import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class SerialisationResolver {

    @Getter
    private final Map<Class<?>, Function<Field, String>> serialisationResolver;

    AnnotatedSQLMember member;

    public SerialisationResolver(AnnotatedSQLMember member) {
        this.serialisationResolver = new HashMap<>();
        this.member = member;
    }

    @SneakyThrows
    private String defaultResolver(Field field) {
        field.setAccessible(true);
        return Optional.of("'" + field.get(member).toString() + "'").orElse(null);
    }

    public String resolve(Field field) {
        Function<Field, String> resolver = serialisationResolver.get(field.getType());

        if (resolver == null) {
            return defaultResolver(field);
        }

        return resolver.apply(field);
    }

    public void addResolver(Class<?> clazz, Function<Field, String> resolver) {
        serialisationResolver.put(clazz, resolver);
    }

    public void removeResolver(Class<?> clazz) {
        serialisationResolver.remove(clazz);
    }
}
