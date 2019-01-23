package com.kovalenko.lab1.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Iterator;

/**
 * abstract class TaskList
 * contains common methods to work with list of Tasks
 * @see Task
 *
 * @author  Anton Kovalenko
 * @version 1.0
 * @since   11-10-2018
 */
public abstract class TaskList implements Iterable<Task>, Serializable {

    protected int lastTaskIndex;

    /**
     * Method for adding non-unique Tasks to list
     *
     * @param task  Task instance, to be added to list, should not be empty
     *
     * @see Task
     * @see ArrayTaskList
     */
    public abstract void add(Task task);

    /**
     * Method for removing Tasks from list
     * All Tasks from list equal to {@code task} should be removed
     *
     * @param task  Task instance, to be removed from list, should not be empty
     * @return true, if Task instance was found and removed from list
     *         false, otherwise
     * @see Task
     * @see ArrayTaskList
     */
    public abstract boolean remove(Task task);

    /**
     * Method for getting size of list
     *
     * @return  size of list
     *
     * @see Task
     * @see ArrayTaskList
     */
    public int size() {
        return lastTaskIndex + 1;
    }

    /**
     * Method for getting Task by given index in list
     *
     * @param index  index of Task in list
     * @return  Task in list by given index
     *
     * @see Task
     * @see ArrayTaskList
     */
    public abstract Task getTask(int index);

    /**
     * Method for getting Task by given notification start and end times
     *
     * @param from  time of notification start(excluded)
     * @param to     time of notification end(included)
     *
     * @return list of suitable Tasks
     * @see Task
     */
    public Iterable<Task> incoming(Date from, Date to) {
        Iterator<Task> iter = this.iterator();
        while(iter.hasNext()) {
            Task task = iter.next();
            if ((task.isActive()
                     && task.getTime().getTime() > from.getTime()
                     && task.getEndTime().getTime() <= to.getTime())
                    || (task.isActive()
                            && task.isRepeated()
                            && Task.compareDates(task.nextTimeAfter(from), from) > 0
                            && Task.compareDates(task.nextTimeAfter(from), to) <= 0)) {
                continue;
            }
            iter.remove();
        }
        return this;
    }
}