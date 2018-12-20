package com.Kovalenko.lab1.model;

import java.io.Serializable;
import java.util.*;

/**
 * class ArrayTaskList
 * contains methods to work with list of Tasks
 * @see Task
 *
 * @author  Anton Kovalenko
 * @version 1.0
 * @since   10-26-2018
 */
public class ArrayTaskList extends TaskList implements Cloneable, Iterable<Task>, Serializable {

    public static long createdTaskArraysTotalCount;
    public static final int DEFAULT_ARRAY_SIZE = 10;
    protected Task [] taskArray;

    static {
        createdTaskArraysTotalCount = 0;
    }

    {
        createdTaskArraysTotalCount++;
        lastTaskIndex = -1;
    }
    /**
     * Default constructor for ArrayTasklist class
     */
    public  ArrayTaskList() {
        this.taskArray = new Task[DEFAULT_ARRAY_SIZE];
    }

    /**
     * Method for adding non-unique Tasks to array
     *
     * @param task Task instance, to be added to array, should not be empty
     *
     * @see Task
     * @see Task#equals(Object)
     * @see System#arraycopy(Object, int, Object, int, int)
     * @throws IllegalArgumentException if one tries to add {@code emtyTask}
     * or null value in list
     */
    @Override
    public void add(Task task) throws IllegalArgumentException {
        Task emptyTask = new Task();
        if (task == null || task.equals(emptyTask)) {
            throw new IllegalArgumentException("Adding empty tasks into list is not allowed");
        }
        int currentCapacity = this.taskArray.length;
        if (size() < currentCapacity ) {
            this.taskArray[++lastTaskIndex] = task;
        } else {
            Task [] newTaskArray = new Task[currentCapacity + (currentCapacity * 3 / 2 + 1)];
            System.arraycopy(this.taskArray, 0, newTaskArray, 0, ++lastTaskIndex);
            newTaskArray[lastTaskIndex] = task;
            this.taskArray = newTaskArray.clone();
        }
    }

    /**
     * Method for removing Tasks from array
     * First Task from array to be equal to {@code task} will be removed
     *
     * @param index index, from which to remove a Task, should not be in list size bounds
     * @return true is such Task was found,
     *         false if not
     * @see Task
     */
    public boolean remove(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("Invalid get index(less than zero)");
        } else if (index >= this.size()) {
            throw new IllegalArgumentException("Invalid get index(exceeds active array size)");
        } else {
            Task [] newTaskArray = new Task[this.taskArray.length - 1];
            System.arraycopy(this.taskArray, 0, newTaskArray, 0, index);
            System.arraycopy(this.taskArray, index + 1, newTaskArray, index, this.size() - 1);
            this.taskArray = newTaskArray.clone();
            --lastTaskIndex;
            return true;
        }
    }

    /**
     * Method for removing Tasks from array
     * First Task from list to be equal to {@code task} will be removed
     *
     * @param task Task instance, to be removed from array, should not be empty
     * @return true is such Task was found,
     *         false if not
     * @see Task
     * @throws IllegalArgumentException if one tries to remove {@code emtyTask}
     * or null value in list
     */
    public boolean remove(Task task) throws IllegalArgumentException {
        Task emptyTask = new Task();
        if (task == null || task.equals(emptyTask)) {
            throw new IllegalArgumentException("Removing empty tasks from list is not allowed");
        }
        for (int i = 0; i <= lastTaskIndex; i++) {
            if (this.taskArray[i].equals(task)) {
                Task [] newTaskArray = new Task[this.taskArray.length - 1];
                if (i == 0) {
                    System.arraycopy(this.taskArray, 1, newTaskArray, 0, this.size() - 1);
                    this.taskArray = newTaskArray.clone();
                    --lastTaskIndex;
                    //continue;
                    return true;
                } else if (i == lastTaskIndex) {
                    System.arraycopy(this.taskArray, 0, newTaskArray, 0, this.size() - 1);
                    --lastTaskIndex;
                    this.taskArray = newTaskArray.clone();
                    //continue;
                    return true;
                }
                System.arraycopy(this.taskArray, 0, newTaskArray, 0, i);
                System.arraycopy(this.taskArray, ++i, newTaskArray, --i, this.size() - 1);
                --lastTaskIndex;
                --i;
                this.taskArray = newTaskArray.clone();
                return true;
            }
        }
        return false;
    }

    /**
     * Method for getting Task by given index in array
     *
     * @param index index of Task in array,
     *              should be more than 0 and less than list size
     * @return Task in list by given index
     * @see Task
     * @throws IndexOutOfBoundsException if code {@code index} was out of list bounds
     */
    @Override
    public Task getTask(int index) throws IndexOutOfBoundsException {
        if (index < 0) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
        } else if (index >= this.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
        } else {
            return taskArray[index];
        }
    }

    /**
     * Method for getting Task by given notification start and end times
     *
     * @param from  time of notification start(excluded)
     * @param to     time of notification end(included)
     *
     * @return list of suitable Tasks
     * @see Task
     * @see TaskList
     * @throws IllegalArgumentException if {@code from} time is not less than {@to} time value
     */
    public Iterable<Task> incoming(Date from, Date to) throws IllegalArgumentException {
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
        return new ArrayTaskList.Iter();
    }

    private class Iter implements Iterator<Task> {
        int cursor;
        int lastReturnedIndex = -1;
        Iter() { }

        public boolean hasNext() {
            return cursor < size();
        }

        public Task next() {
            int i = cursor;
            if (i >= size()) {
                throw new NoSuchElementException();
            }
            Task [] tasksToReturn = ArrayTaskList.this.taskArray;
            cursor = i + 1;
            return tasksToReturn[lastReturnedIndex = i];
        }
        public void remove() {
            if (lastReturnedIndex < 0)
                throw new IllegalStateException();

            ArrayTaskList.this.remove(lastReturnedIndex);
            cursor = lastReturnedIndex;
            lastReturnedIndex = -1;

        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        ArrayTaskList that = (ArrayTaskList) o;
        if (this.size() != that.size()) return false;
        for(int i = 0; i < size(); i++) {
            Task thisTask = this.taskArray[i];
            Task thatTask = that.taskArray[i];
            if(!thisTask.equals(thatTask)) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 1;
        for (int i = 0; i < size(); i++) {
             hash ^= Objects.hash(this.taskArray[i].hashCode());
        }
        return hash;
    }

    @Override
    public String toString() {
        return Arrays.toString(taskArray);
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
    public ArrayTaskList clone() throws CloneNotSupportedException {
        return (ArrayTaskList)super.clone();
    }
}