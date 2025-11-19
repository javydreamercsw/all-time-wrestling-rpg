package com.github.javydreamercsw.base.test;

import java.lang.reflect.Field;

public abstract class BaseTest {

  /**
   * Helper method to set private fields via reflection for testing. This is needed because we
   * switched from constructor injection to field injection.
   */
  protected void setField(Object target, String fieldName, Object value) {
    try {
      Field field = findField(target.getClass(), fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (Exception e) {
      throw new RuntimeException("Failed to set field " + fieldName, e);
    }
  }

  /** Recursively searches for a field in the class hierarchy. */
  private Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
    try {
      return clazz.getDeclaredField(fieldName);
    } catch (NoSuchFieldException e) {
      Class<?> superclass = clazz.getSuperclass();
      if (superclass != null && superclass != Object.class) {
        return findField(superclass, fieldName);
      }
      throw e;
    }
  }
}
