package com.kovalenko.lab1.model;

import java.io.Serializable;
import java.util.*;

/**
 * class LinkedTaskList
 * contains methods to work with list of Tasks
 * @see Task
 *
 * @author  Anton Kovalenko
 * @version 1.0
 * @since   10-26-2018
 */
public class LinkedTaskList extends TaskList implements TaskListable, Cloneable, Iterable<Task>, Serializable {

    private TaskListable value;

    {
        lastTaskIndex = -1;
    }

    /**
     * Default constructor, creates empty LinkedTaskList
     *
     * @see EmptyTaskList
     */
    public LinkedTaskList() {
        this.value = EmptyTaskList.INSTANCE;
    }

    /**
     * Constructor, creates LinkedTaskList with {@code value} as element in it
     *
     * @param value Task, to be added as element
     * @see FilledTaskList
     */
    public LinkedTaskList(Task value) {
        this.value = new FilledTaskList(value);
    }

    /**
     * Constructor, creates LinkedTaskList with {@code value} as Task element in it
     * and {@code rest} as rest part of LinkedTaskList
     *
     * @param value Task, to be added as element
     * @param rest  rest, rest part of List
     * @see FilledTaskList
     */
    public LinkedTaskList(Task value, LinkedTaskList rest) {
        this.value = new FilledTaskList(value, rest.value);
    }

    private LinkedTaskList(TaskListable value) {
        this.value = value;
    }

    /**
     * Method for getting first Task in LinkedTaskList
     *
     * @return value first Task in LinkedTaskList
     */
    public Task getFirst() {
        return ((FilledTaskList) value).getFirst();
    }

    /**
     * Method for getting rest part of LinkedTaskList
     *
     * @return value Task as rest part of LinkedTaskList
     */
    public LinkedTaskList getRest() {
        return new LinkedTaskList(((FilledTaskList) value).getRest());
    }

    /**
     * Method to check if LinkedTaskList is empty
     *
     * @return value true if LinkedTaskList instance is empty
     * false if not
     */
    public boolean isEmpty() {
        return value == EmptyTaskList.INSTANCE;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param list the reference TaskList with which to compare.
     * @return {@code true} if this object is the same as the task
     * argument; {@code false} otherwise.
     */
    @Override
    public boolean equals(Object list) {
        if (list == null) {
            return false;
        }
        if (this.getClass() != list.getClass()) {
            return false;
        } else {
            return this.value.equals(((LinkedTaskList) list).value);
        }
    }

    /**
     * Returns a hash code value for the object. This method is
     * supported for the benefit of hash tables such as those provided by
     * {@link HashMap}.
     *
     * @return a hash code value for this object.
     * @see Object#equals(Object)
     * @see System#identityHashCode
     */
    @Override
    public int hashCode() {
        return Objects.hash(value.hashCode());
    }

    /**
     * Method for finding first Task's index, equal to {@code task}
     *
     * @param task Task instance, to be found in list, should not be empty
     * @return index of first founded Task,
     * -1 if there is no such Task in list
     * @see Task
     */
    public int indexOf(Task task) {
        int indexToReturn = -1;
        Task emptyTask = new Task();
        if (task == null || task.equals(emptyTask)) {
            return indexToReturn;
        }
        int tempIndex = size() - 1;
        Task taskInList = ((FilledTaskList) value).getFirst();
        TaskListable restTasks = ((FilledTaskList) value).getRest();

        for (int i = 0; i < size(); i++, tempIndex--) {
            if (taskInList.equals(task)) {
                indexToReturn = tempIndex;
                break;
            }

            if (restTasks instanceof EmptyTaskList) {
                break;
            }
            taskInList = ((FilledTaskList) restTasks).getFirst();
            restTasks = ((FilledTaskList) restTasks).getRest();

        }
        return indexToReturn;
    }

    /**
     * Method for adding non-unique Tasks to list
     *
     * @param task Task instance, to be added to list, should not be empty
     * @see Task
     */
    @Override
    public void add(Task task) throws IllegalArgumentException {
        Task emptyTask = new Task();
        if (task == null || task.equals(emptyTask)) {
            throw new IllegalArgumentException("Adding empty tasks into list is not allowed");
        }
        value = new FilledTaskList(task, this.value);
        lastTaskIndex++;
    }

    /**
     * Method for removing Tasks from list
     * First Tasks from list equal to {@code task} should be removed from list
     *
     * @param task Task instance, to be removed from list, should not be empty
     * @see Task
     */
    @Override
    public boolean remove(Task task) throws IllegalArgumentException {
        Task emptyTask = new Task();
        if (task == null || task.equals(emptyTask)) {
            throw new IllegalArgumentException("Removing empty tasks from list is not allowed");
        }
        boolean taskWasFound = false;
        int indexOfRemovableTask = indexOf(task);
        if (indexOfRemovableTask >= 0) {
            taskWasFound = true;
        } else {
            return taskWasFound;
        }

        if (indexOfRemovableTask == 0) {

            if(lastTaskIndex == 0) {
                this.value = EmptyTaskList.INSTANCE;
                lastTaskIndex--;
                return true;
            }

            TaskListable next = getTaskList(0);
            if (next == EmptyTaskList.INSTANCE) {
                return true;
            }
            ((FilledTaskList) next).setRest(EmptyTaskList.INSTANCE);
            lastTaskIndex--;
            return true;
        } else if (indexOfRemovableTask == lastTaskIndex) {
            TaskListable next = getTaskList(lastTaskIndex);
            value = ((FilledTaskList) next).getRest();
            lastTaskIndex--;
            return true;
        } else {
            int nextIndex = indexOfRemovableTask + 1;
            TaskListable next = getTaskList(nextIndex);
            TaskListable current = getTaskList(indexOfRemovableTask);
            if (current == EmptyTaskList.INSTANCE) {
                return true;
            }
            ((FilledTaskList) next).setRest(((FilledTaskList) current).getRest());
            lastTaskIndex--;
            return true;
        }
    }

    /**
     * Method for getting Task by given index in list
     *
     * @param index index of Task in list
     * @return Task in list by given index
     * @see Task
     */
    @Override
    public Task getTask(int index) throws IndexOutOfBoundsException {
        if (size() == 0 || index < 0 || index + 1 > size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
        } else {
            index = index - size();
            Task taskToReturn = ((FilledTaskList) value).getFirst();
            TaskListable restTasks = ((FilledTaskList) value).getRest();
            index++;
            while (index < 0) {
                taskToReturn = ((FilledTaskList) restTasks).getFirst();
                restTasks = ((FilledTaskList) restTasks).getRest();
                index++;
            }
            return taskToReturn;
        }
    }

    /**
     * Method for getting TaskList by given index in list
     *
     * @param index index of Task in list
     * @return Task in list by given index
     * @see Task
     */
    private TaskListable getTaskList(int index) {
        if (this.isEmpty()) {
            System.out.println("List is empty");
            return EmptyTaskList.INSTANCE;
        }
        if (index < 0) {
            System.out.println("Invalid index(less than zero)");
            return EmptyTaskList.INSTANCE;
        } else if (index > this.size()) {
            System.out.println("Invalid index(exceeds list size)");
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
        } else {
            index = index - size() + 1;
            TaskListable taskListToReturn = value;
            while (index < 0) {
                taskListToReturn = ((FilledTaskList) taskListToReturn).getRest();
                index++;
            }
            return taskListToReturn;
        }
    }

    /**
     * Method for getting Task by given notification start and end times
     *
     * @param from time of notification start(excluded)
     * @param to   time of notification end(included)
     * @return array of suitable Tasks
     * @see Task
     */
    public Iterable<Task> incoming(Date from, Date to) throws UnsupportedOperationException, IllegalArgumentException {
        return super.incoming(from,to);
    }

    /**
     * Returns an iterator over the elements in this list in proper sequence.
     *
     * <p>The returned iterator is <a href="#fail-fast"><i>fail-fast</i></a>.
     *
     * @return an iterator over the elements in this list in proper sequence
     */
    public Iterator<Task> iterator() {
        return new LinkedTaskList.Iter();
    }

    private class Iter implements Iterator<Task> {
        int cursor;
        TaskListable lastReturnedElement;
        TaskListable next = LinkedTaskList.this.value;
        Task taskToReturn;
        Iter() {}


        public boolean hasNext() {
            return cursor < size();
        }

        public Task next() {
            int i = cursor;
            if(!hasNext()) {
                throw new NoSuchElementException();
            }
                lastReturnedElement = next;
                taskToReturn = ((FilledTaskList) lastReturnedElement).getFirst();
                cursor = i + 1;
                next = ((FilledTaskList) next).getRest();
                return taskToReturn;
        }

        public void remove() {
            if (lastReturnedElement == null)
                throw new IllegalStateException();

            TaskListable lastNext = ((FilledTaskList)lastReturnedElement).getRest();
            LinkedTaskList.this.remove(((FilledTaskList)lastReturnedElement).getFirst());

            if(next == lastReturnedElement) {
                next = lastNext;
            } else {
                cursor--;
                lastReturnedElement = null;
            }
        }
    }

    @Override
    public String toString() {
        return "[" + value.toString();
    }

    /**
     * Creates and returns a copy of this object.  The precise meaning
     * of "copy" may depend on the class of the object. The general
     * intent is that, for any object {@code x}, the expression:
     * <blockquote>
     * <pre>
     * x.clone() != x</pre></blockquote>
     * will be true, and that the expression:
     * <blockquote>
     * <pre>
     * x.clone().getClass() == x.getClass()</pre></blockquote>
     * will be {@code true}, but these are not absolute requirements.
     * While it is typically the case that:
     * <blockquote>
     * <pre>
     * x.clone().equals(x)</pre></blockquote>
     * will be {@code true}, this is not an absolute requirement.
     * <p>
     * By convention, the returned object should be obtained by calling
     * {@code super.clone}.  If a class and all of its superclasses (except
     * {@code Object}) obey this convention, it will be the case that
     * {@code x.clone().getClass() == x.getClass()}.
     * <p>
     * By convention, the object returned by this method should be independent
     * of this object (which is being cloned).  To achieve this independence,
     * it may be necessary to modify one or more fields of the object returned
     * by {@code super.clone} before returning it.  Typically, this means
     * copying any mutable objects that comprise the internal "deep structure"
     * of the object being cloned and replacing the references to these
     * objects with references to the copies.  If a class contains only
     * primitive fields or references to immutable objects, then it is usually
     * the case that no fields in the object returned by {@code super.clone}
     * need to be modified.
     * <p>
     * The method {@code clone} for class {@code Object} performs a
     * specific cloning operation. First, if the class of this object does
     * not implement the interface {@code Cloneable}, then a
     * {@code CloneNotSupportedException} is thrown. Note that all arrays
     * are considered to implement the interface {@code Cloneable} and that
     * the return type of the {@code clone} method of an array type {@code T[]}
     * is {@code T[]} where T is any reference or primitive type.
     * Otherwise, this method creates a new instance of the class of this
     * object and initializes all its fields with exactly the contents of
     * the corresponding fields of this object, as if by assignment; the
     * contents of the fields are not themselves cloned. Thus, this method
     * performs a "shallow copy" of this object, not a "deep copy" operation.
     * <p>
     * The class {@code Object} does not itself implement the interface
     * {@code Cloneable}, so calling the {@code clone} method on an object
     * whose class is {@code Object} will result in throwing an
     * exception at run time.
     *
     * @return a clone of this instance.
     * @throws CloneNotSupportedException if the object's class does not
     *                                    support the {@code Cloneable} interface. Subclasses
     *                                    that override the {@code clone} method can also
     *                                    throw this exception to indicate that an instance cannot
     *                                    be cloned.
     * @see Cloneable
     */
    @Override
    public LinkedTaskList clone() throws CloneNotSupportedException {
        return (LinkedTaskList)super.clone();
    }

    public void reverse() {
        LinkedTaskList reversed = new LinkedTaskList();
        Iterator<Task> iter = this.iterator();
        for(Task task : this) {
            reversed.add(task);
        }
        this.value = reversed.value;
    }
}