package io.kylin.mdx.insight.common.util;

import lombok.SneakyThrows;

import java.lang.reflect.Field;

public class BeanUtils {

    @SneakyThrows
    public static void setField(Object object, String fieldName, Object fieldValue) {
        Class clazz = object.getClass();
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(object, fieldValue);
    }

    @SneakyThrows
    public static <T> void setField(Object object, Class<T> fieldClass, T fieldValue) {
        Class clazz = object.getClass();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            Class<?> currentClazz = field.getType();
            if (currentClazz.isAssignableFrom(fieldClass)) {
                field.setAccessible(true);
                field.set(object, fieldValue);
                break;
            }
        }
    }

    @SneakyThrows
    public static Object getField(Object object, String fieldName) {
        Class clazz = object.getClass();
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(object);
    }

}
