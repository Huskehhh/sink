package pro.husk.sqlannotations;

import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Serialisation Resolver class, providing means of resolving fields to SQL-safe strings
 */
public class SerialisationResolver {

    private final Map<Class<?>, Function<Field, String>> serialisationResolver;
    private final AnnotatedSQLMember member;

    /**
     * Constructor
     *
     * @param member annotated class
     */
    public SerialisationResolver(AnnotatedSQLMember member) {
        this.serialisationResolver = new HashMap<>();
        this.member = member;

        addDefaultResolvers();
    }

    /**
     * Adds a few defaults resolvers
     */
    private void addDefaultResolvers() {
        addResolver(boolean.class, (field) -> {
            field.setAccessible(true);

            try {
                Boolean bool = (Boolean) field.get(member);
                return bool ? "1" : "0";
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

            return null;
        });

        addResolver(UUID.class, (field) -> {
            field.setAccessible(true);

            UUID uuid = null;
            try {
                uuid = (UUID) field.get(member);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

            if (uuid == null) {
                uuid = new UUID(0L, 0L);
            }

            return "'" + uuid.toString() + "'";
        });
    }

    /**
     * Default resolver for types that aren't found in the map
     *
     * @param field to resolve
     * @return attempted SQL safe string
     */
    @SneakyThrows
    private String defaultResolver(Field field) {
        field.setAccessible(true);
        return Optional.of("'" + field.get(member).toString() + "'").orElse(null);
    }

    /**
     * Resolves the given field either through a given resolver, or through the default
     *
     * @param field to resolve
     * @return SQL safe string of the given field
     */
    public String resolve(Field field) {
        Function<Field, String> resolver = serialisationResolver.get(field.getType());

        if (resolver == null) {
            return defaultResolver(field);
        }

        return resolver.apply(field);
    }

    /**
     * Provides a way to add a resolve for class type
     *
     * @param clazz    type to resolve with resolver
     * @param resolver to return the resolved value
     */
    public void addResolver(Class<?> clazz, Function<Field, String> resolver) {
        serialisationResolver.put(clazz, resolver);
    }

    /**
     * Provides a way to remove a resolver for the given class type
     *
     * @param clazz type to remove
     */
    public void removeResolver(Class<?> clazz) {
        serialisationResolver.remove(clazz);
    }
}
