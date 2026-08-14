/*
* Copyright (C) 2026 Software Consulting Dreams LLC
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <www.gnu.org>.
*/
package com.github.javydreamercsw.management.service.achievement;

import groovy.lang.Binding;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.springframework.stereotype.Service;

/**
 * Evaluates Groovy boolean expressions used as achievement unlock conditions. Compiled script
 * classes are cached by snippet text since achievement checks run on every legacy score
 * recalculation, challenge completion, and match adjudication — a hot path where recompiling the
 * same snippet on every call would be wasted work.
 *
 * <p>Fails closed: any exception (syntax error, runtime error, non-boolean result) is logged and
 * treated as "not unlocked" rather than propagated, since a broken achievement script must never
 * block gameplay.
 */
@Service
@Slf4j
public class AchievementScriptService {

  private final Map<String, Class<?>> compiledScriptCache = new ConcurrentHashMap<>();
  private final groovy.lang.GroovyClassLoader groovyClassLoader =
      new groovy.lang.GroovyClassLoader(
          Thread.currentThread().getContextClassLoader(), new CompilerConfiguration());

  public boolean evaluate(final String script, @NonNull final Map<String, Object> variables) {
    if (script == null || script.isBlank()) {
      return false;
    }
    try {
      Class<?> scriptClass =
          compiledScriptCache.computeIfAbsent(script, groovyClassLoader::parseClass);
      Binding binding = new Binding(variables);
      Object result = InvokerHelper.createScript(scriptClass, binding).run();
      return Boolean.TRUE.equals(result);
    } catch (Exception e) {
      log.debug("Achievement unlock script failed — treating as not-unlocked: {}", e.getMessage());
      return false;
    }
  }
}
