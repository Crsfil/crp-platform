package com.example.crp.bpm.tasks;

import org.camunda.bpm.client.task.ExternalTask;

final class TaskVariables {
    private TaskVariables() { }

    static Long getLong(ExternalTask task, String name) {
        Object value = task.getVariable(name);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    static Integer getInt(ExternalTask task, String name) {
        Object value = task.getVariable(name);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }

    static Double getDouble(ExternalTask task, String name) {
        Object value = task.getVariable(name);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(value.toString());
    }

    static String getString(ExternalTask task, String name) {
        Object value = task.getVariable(name);
        return value == null ? null : value.toString();
    }
}
