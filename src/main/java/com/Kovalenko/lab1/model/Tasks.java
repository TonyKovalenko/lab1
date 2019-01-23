package com.kovalenko.lab1.model;

import java.util.*;

/**
 * Class with static functions to work with collections of Tasks
 *
 * @see Task
 */
public class Tasks {

    private Tasks() {
    }

    /**
     * Method to return Collection of active Tasks, contained between {@code from} and {@code to} dates
     *
     * @param tasks TaskList, should implement Iterable interface
     * @param from  Date, from which to search for active Tasks
     * @param to    Date, to which to search for active Tasks
     * @return Collection of incoming Tasks between specified dates
     * @see Task
     * @see Iterable
     * @see TaskList
     */
    public static Iterable<Task> incoming(Iterable<Task> tasks, Date from, Date to) {
        Iterator<Task> iter = tasks.iterator();
        while (iter.hasNext()) {
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
        return tasks;
    }

    /**
     * Method to return SortedMap of all tasks, contained between {@code from} and {@code to} dates
     * All the tasks, which have same notification date will be contained in {@code calendarToReturn}
     * as Set<Task> {@code newSet} by Date key {@code timeToAdd}
     *
     * @param tasks TaskList, should impement Iterable interface
     * @param from  Date, from which to search for active Tasks
     * @param to    Date, to which to search for active Tasks
     * @return calendar for tasks, which are scheduled between {@code from} and {@code to} dates
     */
    public static SortedMap<Date, Set<Task>> calendar(Iterable<Task> tasks, Date from, Date to) {
        SortedMap<Date, Set<Task>> calendarToReturn = new TreeMap<>();
        Task currentTask;
        Date timeToAdd, currentFromTime = from;
        Set<Task> setOfTasks = new HashSet<>();

        Iterator<Task> iter = tasks.iterator();
        while (iter.hasNext()) {
            currentTask = iter.next();
            if (currentTask.isActive() && currentTask.getEndTime().after(from)) {
                while (Task.compareDates(currentTask.nextTimeAfter(currentFromTime), to) <= 0) {
                    timeToAdd = currentTask.nextTimeAfter(currentFromTime);
                    if (timeToAdd == null) {
                        break;
                    }
                    if (calendarToReturn.get(timeToAdd) != null) {
                        Set<Task> newSet = new HashSet<>(calendarToReturn.get(timeToAdd));
                        newSet.add(currentTask);
                        calendarToReturn.put(timeToAdd, newSet);
                    } else {
                        setOfTasks.add(currentTask);
                        calendarToReturn.put(timeToAdd, setOfTasks);
                    }
                    if (!currentTask.isRepeated()) {
                        break;
                    }
                    currentFromTime = new Date(timeToAdd.getTime());
                }
                currentFromTime = from;
                setOfTasks = new HashSet<>();
            }

        }
        return calendarToReturn;
    }
}

