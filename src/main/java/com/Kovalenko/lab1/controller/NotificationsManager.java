package com.Kovalenko.lab1.controller;

import com.Kovalenko.lab1.model.Task;
import com.Kovalenko.lab1.model.TaskList;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static jdk.nashorn.internal.runtime.regexp.joni.Config.log;

/**
 * Controller class of TaskManager
 *
 * @author Anton Kovalenko
 * @version 1.1
 *
 * @see Controller
 */
public class NotificationsManager extends Thread {

    private Controller parentController;
    private TaskList taskList;

    public void setParentController(Controller parentController) {
        this.parentController = parentController;
    }

    /**
     * Method that starts notifications in other thread,
     * If list of tasks, we currently iterate through mutated(user edited or added new tasks)
     * iteration is restarted from beginning.
     *
     * Thread starts, gets current time {@code currentDate}, computes 1 second after current time {@code nextSecond}.
     * If there are any tasks that should be notified of within this period, there will be a message about that.
     * Task can be only notified in time that is multiple of 1 second, so milliseconds are ignored.
     * Then thread will sleep for 1 second, and retry this procedure.
     *
     */
    @Override
    public void run() {
        //System.out.println("STARTED");
        taskList = parentController.getTaskList();
        Set<Task> incomingTasks;
        Date currentDate;
        Date nextSecond;
        Date nextTimeAfter;
        while (true) {
            if(Thread.interrupted()) {
                break;
            }
            incomingTasks = new HashSet<>();
            currentDate = new Date();
            nextSecond = new Date(currentDate.getTime() + 1000);
            for (Task task : taskList) {
                if(parentController.getListMutated()) { //if list mutated while we iterate through, start from beginning
                    taskList = parentController.getTaskList();
                    parentController.setListMutated(false);
                    break;
                }
                if(task.isActive() && task.isRepeated()) {
                    nextTimeAfter = task.nextTimeAfter(currentDate);
                    if(nextTimeAfter != null && nextTimeAfter.getTime() <= nextSecond.getTime())
                    incomingTasks.add(task);
                } else if (task.isActive()) {
                    if(task.getStartTime().getTime() >= currentDate.getTime() && task.getStartTime().getTime() <= nextSecond.getTime()) {
                        incomingTasks.add(task);
                    }
                }
            }
            try {
                if(incomingTasks.size() > 0) {
                    //if there were some notifiable tasks in 1 second window of thread's work,
                    // they will be displayed on the screen
                    notifyUser(incomingTasks);
                }
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
                break;
            }
        }
    }

    /**
     * Method to display notification on screen.
     * All the tasks, contained in set will be displayed.
     *
     * @param tasks
     *        Set of tasks, that should be displayed on the screen.
     */
    public void notifyUser(Set<Task> tasks) {
            System.out.println("\n\n========= NOTIFICATION ==========");
            for (Task task : tasks) {
                System.out.println(task);
            }
            System.out.println("=================================\n");
    }
}
