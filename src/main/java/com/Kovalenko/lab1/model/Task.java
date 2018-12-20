package com.Kovalenko.lab1.model;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;

/**
 * class Task contains information about it's essence, it's status (active/disable),
 * time of notification, interval of notification.
 *
 * @author  Anton Kovalenko
 * @version 1.0
 * @since   10-12-2018
 */
public class Task implements Cloneable, Serializable {

    private String title;
    private boolean active;
    private boolean repeated;
    private Date time;
    private Date start;
    private Date end;
    private int repeat;

    /**
     * Gets a title of a Task
     * @return title of a Task
     */
    public String getTitle() {
        return this.title;
    }
    /**
     * Sets a title for the Task
     * @param title  desired title for the Task
     */
    public void setTitle(String title) throws IllegalArgumentException {
        if (title == null || title.trim().equals("") || title.indexOf('\n') != -1) {
            //System.out.println("You may want to change a name of task, as it consists of spaces only, or has newline symbols in it");
            throw new IllegalArgumentException("You may want to change a name of task, as it empty(null), or consists of spaces only, or has newline symbols in it");
        }
        this.title = title;
    }

    /**
     * Checks if the Task is active
     * @return true if the Task is active, otherwise false
     */
    public boolean isActive() {
        return this.active;
    }

    /**
     * Sets the Task active
     * @param active  state for the Task, true for making it active, false for making it disabled
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Sets the time of notification for Task, Task becomes disabled
     * If called for repeatable Task makes it non repeatable
     * @param time  time for Task notification
     */
    public void setTime(Date time) throws IllegalArgumentException{
        if (time.getTime() >= 0) {
            this.time = time;
            this.start = time;
            this.end = time;
            this.repeat = 0;
            this.repeated = false;
            this.active = false;
        } else {
            throw new IllegalArgumentException("Invalid argument, time should be more or equal than zero");
        }
    }

    /**
     * Sets the time of notification for Task, Task becomes disabled
     * If called for non repeatable Task makes it repeatable
     * @param start    start time for Task notification
     * @param end	  end time for Task notification
     * @param repeat  repeat period for Task notification
     */
    public void setTime(Date start, Date end, int repeat) throws IllegalArgumentException{
        if (( end.getTime() > start.getTime() ) && ( repeat > 0 ) && (start.getTime() >= 0)) {
            this.time = start;
            this.start = start;
            this.end = end;
            this.repeat = repeat * 1000;
            this.repeated = true;
            this.active = false;
        } else {
            throw new IllegalArgumentException("Invalid arguments, repeat time should be more than zero, end time should be greater then start");
        }
    }

    /**
     * Gets the start time for Task notification
     * @return start notification time for repeatable, notification time for non repeatable
     */
    public Date getTime() {
        return this.time;
    }

    /**
     * Gets the start time for Task notification
     * @return start notification time for repeatable, notification time for non repeatable
     */
    public Date getStartTime() {
        return this.start;
    }

    /**
     * Gets the end time for Task notification
     * @return end notification time for repeatable, notification time for non repeatable
     */
    public Date getEndTime(){
        if( !this.repeated ){
            return this.time;
        }else {
            return this.end;
        }
    }

    /**
     * Gets the repeat interval for Task notification
     * @return end repeat interval for repeatable Task, zero for non repeatable
     */
    public int getRepeatInterval() {
        return this.repeat;
    }

    /**
     * Checks if the Task is repeated
     * @return true if task is repeatable, false if task is non repeatable
     */
    public boolean isRepeated() {
        return this.repeated;
    }

    /**
     * Gets the next time for Task notification
     * @param time  after which to search next Task notification  time
     * @return next time for Task notification , -1 if the Task is not active or it is non repeatable or if there will be no Task notification  after specified time
     */
    public Date nextTimeAfter(Date time) throws IllegalArgumentException{
        if(time == null || time.getTime() < 0) {
            throw new IllegalArgumentException("Time after which to search for next notification cannot be less than zero or null");
        }

        if (!this.active) {
            return null;
        }

        if (!this.repeated && this.time.getTime() > time.getTime()) {
            return this.time;
        } else if (!this.repeated && this.time.getTime() <= time.getTime()){
            return null;
        }

        if(this.repeated && this.start.getTime() > time.getTime()) {
            return this.time;
        } else if (this.repeated && this.start.getTime() <= time.getTime()) {
            for (long i = this.start.getTime(); i < this.end.getTime(); i += repeat) {
                if( (time.getTime() >= i) && (time.getTime() < i + repeat) ) {
                    return ( i + repeat > this.end.getTime() ) ? null : new Date(i + repeat);
                    //return new Date(i + repeat);
                }
            }
            //int nextTime = ((( time + this.start ) / this.repeat ) * this.repeat ) + this.start;
            //return nextTime > this.end ? -1 : nextTime;
        }
        return null;
    }

    /**
     * Default constructor for Task class, creates unrepeatable Task instance
     */
    public Task() {
    }

    /**
     * Constructor for Task class, creates unrepeatable Task instance
     * @param title  title for the Task
     * @param time  time for unrepeatable Task
     */
    public Task(String title, Date time) throws IllegalArgumentException {
        this.setTitle(title);
        this.setTime(time);
    }

    /**
     * Constructor for Task class, creates repeatable Task instance
     * @param title     title for the Task
     * @param start    start time for repeatable Task
     * @param end      end time for repeatable Task
     * @param repeat  repeat interval for repeatable Task
     */
    public Task(String title, Date start, Date end, int repeat) throws IllegalArgumentException{
        this.setTitle(title);
        this.setTime(start, end, repeat);
    }

    /**
     * Method for text representation of Task
     * @return 3 different outputs if the task is not active, not repeatable and active, repeatable and active
     */
    @Override
    public String toString() {
        if( !this.active ) {
            return "Task \"" + this.title + "\" is inactive";
        } else if ( !repeated ) {
            return "Task \"" + this.title + "\" at " + this.time;
        } else {
            return "Task \"" + this.title + "\" from " + this.start + " to " + this.end + " every " + this.repeat/1000 + " seconds";
        }
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param task the reference Task with which to compare.
     * @return {@code true} if this object is the same as the task
     * argument; {@code false} otherwise.
     * @see #hashCode()
     * @see HashMap
     */
    @Override
    public boolean equals(Object task) {
        if(this == task) return true;
        if(task == null) return false;
        if(this.getTitle() == null && ((Task)task).getTitle() == null) return true;
        if(task.getClass() != this.getClass()) return false;

        Task castedTask = (Task) task;
        return ( this.getTitle().equals(castedTask.getTitle())
                    && castedTask.isActive() == this.isActive()
                    && castedTask.isRepeated() == this.isRepeated()
                    && castedTask.getTime().getTime() == (this.getTime().getTime())
                    && castedTask.getStartTime().getTime() == (this.getStartTime().getTime())
                    && castedTask.getEndTime().getTime() == (this.getEndTime().getTime())
                    && castedTask.getRepeatInterval() == this.getRepeatInterval()
                    && castedTask.hashCode() == this.hashCode());
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
        return Objects.hash(title, active, repeated, time, start, end, repeat);
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
    public Task clone() throws CloneNotSupportedException {
        return (Task)super.clone();
    }

    /**
     * Static method to compare Dates
     *
     * @param o1 First date to compare
     * @param o2 Second date to compare
     * @return 0 if dates are equal
     *        -1 if o1 < o2
     *         1 if o1 > o2
     */
    public static int compareDates(Date o1, Date o2) {
        if (o1 == null) return -1;
        if (o2 == null) return 1;
        if (o1.getTime() > o2.getTime()) return 1;
        if (o1.getTime() < o2.getTime()) return -1;
        if (o1.getTime() == o2.getTime())return 0;
        return 0;
    }

    /**
     *
     * @param date date, to check if Task is being notified at this date
     * @return true, if there will be a notification about Task at specified date
     *         false, otherwise
     */
    public boolean isAtDate(Date date) {
        if (!isActive()) return false;
        if (!isRepeated()) return start.equals(date);
        if (isRepeated()) {
            if (end.compareTo(date) < 0) return false;
            if (end.compareTo(date) == 0) return true;
            if (start.compareTo(date) == 0) return true;

            Date tempDate = start;
            while (tempDate != null) {
                if (tempDate.equals(date)) return true;
                tempDate = nextTimeAfter(tempDate);
            }

        }
        return false;
    }
}
