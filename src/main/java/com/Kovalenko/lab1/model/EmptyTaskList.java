package com.Kovalenko.lab1.model;

/**
 * class EmptyTaskList
 * contains representation for empty TaskList
 * @see Task
 * @see TaskList
 *
 * @author  Anton Kovalenko
 * @version 1.1
 * @since   12-9-2018
 */
public enum EmptyTaskList implements TaskListable {
    INSTANCE;

    @Override
    public String toString() {
        return "]";
    }
}
