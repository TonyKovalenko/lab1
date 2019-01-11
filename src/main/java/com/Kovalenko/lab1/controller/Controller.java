package com.Kovalenko.lab1.controller;

import com.Kovalenko.lab1.model.*;
import org.apache.log4j.Logger;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Controller class of TaskManager
 *
 * @author Anton Kovalenko
 * @version 1.1
 * @see Task
 * @see TaskList
 * @see TaskIO
 * @since 1.8
 */
public enum Controller {
    INSTANCE;

    private static final String DEFAULT_STORAGE_FILE_NAME = "myTasks.txt";
    private static Logger log = Logger.getLogger(Controller.class.getName());
    String inputChoice;
    private volatile ArrayTaskList taskList;
    private volatile Boolean listMutated;
    private NotificationsManager notifier;
    private String[] menuItems;

    Controller() {
        listMutated = false;
        notifier = new NotificationsManager();
        taskList = new ArrayTaskList();
    }

    public TaskList getTaskList() {
        return taskList;
    }

    public boolean getListMutated() {
        return listMutated;
    }

    public void setListMutated(boolean value) {
        synchronized (this) {
            listMutated = value;
        }
    }

    /**
     * Main method to start an application,
     * {@link #chooseTaskList()}
     */
    public void run() {
        log.info("App started.");
        chooseTaskList();
    }

    /**
     * Utility method for printing menu, using {@code menuFormat}.
     * <p>
     * In following methods, user can select menu items
     * by typing it's corresponding number to console, e.g. 1, 2, 3, etc.
     *
     * @param items menu items, that should be displayed
     */
    private void menuUtil(String... items) {
        String menuFormat = "#%d\t%s%n";

        for (int i = 0; i < items.length; ++i) {
            System.out.printf(menuFormat, i + 1, items[i]);
        }
    }

    /**
     * Method for reading user input from console using BufferedReader class
     *
     * @return trimmed value of users input
     * @see BufferedReader
     */
    private String getTrimmedInput() {
        System.out.print("\n>>> Your input: ");
        String input = "";
        BufferedReader buff = new BufferedReader(new InputStreamReader(System.in));
        do {
            try {
                input = buff.readLine();
            } catch (IOException e) {
                log.error("NULL returned from user's input stream. ", e);
                System.out.print("! Cannot read input because of internal error, please retry the input.");
            }
        } while (input == null);
        buff = null;
        return input.trim();
    }

    /**
     * Method for printing the menu, by using which user can choose,
     * from where he wants all the tasks to be loaded from
     * <p>
     * Menu is printed by using {@link #menuUtil(String...)} method
     */
    private void showChooseTaskListMenu() {
        System.out.println("\n - Please choose a further action by typing it's number in following menu \n");
        menuItems = new String[]{
            "Create new empty list of tasks.",
            "Load list of tasks from existing file.",
            "Continue with last saved list of tasks.",
        };

        menuUtil(menuItems);
    }

    /**
     * Method for actual choosing from where he wants all the tasks to be loaded from
     * Predefined statements will be resolved in {@link #routeIfControlWord(String, Menus, Menus, String, int...)}
     * In case of wrong input, user will be asked to retry.
     */
    private void chooseTaskList() {
        showChooseTaskListMenu();
        do {
            inputChoice = getTrimmedInput();
            switch (inputChoice) {
                case "1":
                    taskList = new ArrayTaskList();
                    System.out.println("New empty list was created.");
                    log.info("User created new empty list of tasks.");
                    break;
                case "2":
                    log.info("User tried to load list of tasks from custom file.");
                    System.out.println("\n - Please enter path to the file with tasks, including it's type. \n For example ../path/to/my/file.txt");
                    loadFromFile(getTrimmedInput());
                    log.info("User's specified file was loaded.");
                    break;
                case "3":
                    log.info("User tried to load list from last saved file.");
                    loadFromFile(DEFAULT_STORAGE_FILE_NAME);
                    log.info("Last saved default storage file was loaded successfully.");
                    break;
                default:
                    boolean routed = routeIfControlWord(inputChoice, Menus.CHOOSE_TASKLIST, Menus.VOID, "");
                    //above method will resolve predefined words and will route the flow of the program
                    if (!routed) {
                        System.out.print("Incorrect input, please retry.");
                    }
                    continue;
            }
            pokeNotificationsManager(true);
            taskListMain();
        } while (true);
    }

    /**
     * - Method for adding tasks to {@code taskList}
     * by loading collection of Tasks from file
     *
     * @param path path to file to load tasks from
     * @see TaskIO
     */
    private void loadFromFile(String path) {
        File readFromFile = new File(path);
        taskList = new ArrayTaskList();
        try {
            TaskIO.readText(taskList, readFromFile);
            System.out.println("File was loaded successfully! ");
        } catch (FileNotFoundException ex) {
            log.info("File was not found. ", ex);
            try {
                if (readFromFile.createNewFile()) {
                    File defaultStorage = new File(DEFAULT_STORAGE_FILE_NAME);
                    defaultStorage.createNewFile();
                }
            } catch (IOException ioe) {
                log.fatal("Exception while creating new empty default storage file. ", ioe);
            } finally {
                System.out.println("! Seems like file is missing, "
                                       + "so new empty default file was created and list of tasks is now empty");
            }
        } catch (IOException | ParseException | StringIndexOutOfBoundsException ex) {
            taskList = new ArrayTaskList();
            try {
                File defaultStorage = new File(DEFAULT_STORAGE_FILE_NAME);
                defaultStorage.createNewFile();
            } catch (IOException ioe) {
                log.fatal("Exception while creating new empty default storage file. ", ioe);
            }
            System.out.println("! Seems like the content of file is corrupted, "
                                   + "so new empty file was created and list of tasks is now empty");
            log.warn("Last saved file was corrupted outside the application. File was emptied and new list of tasks was created. ", ex);
        }
    }

    /**
     * Main menu, printed using {@link #menuUtil(String...)}
     * <p>
     * Here user can choose what to do next with collection
     */
    private void showTaskListMainMenu() {
        System.out.println("\n---------- Main menu -----------");
        System.out.println("\n - Please choose what do you want to do next \n");
        String[] menuItems = new String[]{
            "View all your tasks.",
            "Add a new task to list.",
            "Remove tasks from list.",
            "Edit task in list.",
            "View calendar.",
        };
        menuUtil(menuItems);
    }

    /**
     * Method for actual choosing what user wants to do with the loaded collection
     * Predefined statements are resolved in {@link #routeIfControlWord(String, Menus, Menus, String, int...)}
     * In case of wrong input, user will be asked to retry.
     */
    private void taskListMain() {
        showTaskListMainMenu();
        do {
            inputChoice = getTrimmedInput();
            switch (inputChoice) {
                case "1":
                    //View all tasks
                    viewTasks();
                    showTaskListMainMenu();
                    break;
                case "2":
                    //Add new task
                    addTask();
                    break;
                case "3":
                    //Remove task from list
                    removeTasks();
                    break;
                case "4":
                    //Edit task in list
                    editTaskList();
                    break;
                case "5":
                    //View calendar
                    calendar();
                    break;
                default:
                    boolean routed = routeIfControlWord(inputChoice, Menus.TASKLIST_MAIN, Menus.VOID, "");
                    if (!routed) {
                        System.out.print("Incorrect input, please retry.");
                    }
            }
        } while (true);
    }

    /**
     * Method for viewing current list of tasks
     */
    private void viewTasks() {
        checkIfEmptyCollectionThenStepOut("Nothing to view.");
        System.out.println("\n----------- View menu -----------\n");
        System.out.println("List of all tasks is displayed below.\n");
        String[] collectionItems = menuItemsOutOfCollection(taskList);
        menuUtil(collectionItems);
    }

    /**
     * Util method, waits for ENTER press, then returns
     */
    private void waitForEnterButton() {
        System.out.println("\nHit ENTER to go to previous menu.");
        Scanner scan = new Scanner(System.in);
        scan.nextLine();
    }

    /**
     * Method to check if collection is empty(i.e. has no task inside)
     * {@link TaskList#size()}
     *
     * @see TaskList
     */
    private void checkIfEmptyCollectionThenStepOut(String str) {
        if (taskList.size() == 0) {
            System.out.println("\nYour list of tasks is empty at the moment. " + str);
            taskListMain();
        }
    }

    /**
     * Method to show remove menu to user, and get indexes to further remove tasks.
     */
    private void removeTasks() {
        checkIfEmptyCollectionThenStepOut("Nothing to remove.");
        Integer[] indexesToRemoveTasksFrom = null;
        showRemoveTasksMenu();
        do {
            inputChoice = getTrimmedInput();
            try {
                indexesToRemoveTasksFrom = parseNeededRemoveIndexes(inputChoice); //parse user input to get desired remove indexes
                Set<Integer> uniqueRemovalIndexes = checkForInvalidRemovalIndexes(indexesToRemoveTasksFrom); //validate just inputted indexes
                indexesToRemoveTasksFrom = uniqueRemovalIndexes.toArray(new Integer[]{});
            } catch (NumberFormatException ex) {
                boolean routed = routeIfControlWord(inputChoice, Menus.REMOVE_TASKS, Menus.VOID, "");
                if (!routed) {
                    System.out.print("Incorrect input, please retry.");
                    log.info("Invalid index in user's input while removing tasks", ex);
                    continue;
                }
            } catch (IndexOutOfBoundsException ex) {
                System.out.print(ex.getMessage());
                indexesToRemoveTasksFrom = null;
                log.info("Index out of bounds in user's input while removing tasks. ", ex);
                continue;
            }

            if (indexesToRemoveTasksFrom != null) {
                removeTasksByGivenIndexesConfirmation(indexesToRemoveTasksFrom);
            }
        } while (true);
    }

    /**
     * Menu, that will be displayed, when user chooses to delete tasks from collection
     */
    private void showRemoveTasksMenu() {
        System.out.println("----------- Remove menu -----------");
        String[] collectionItemsAsMenu = menuItemsOutOfCollection(taskList);
        System.out.println("\n - Choose the number of a task you want to remove from list");
        System.out.println("(Note, you can remove several tasks by typing their numbers separated by spaces\n" +
                               "e.g. 1 3 5 - will remove tasks by number 1, 3 and 5 )\n");
        menuUtil(collectionItemsAsMenu);
    }

    /**
     * Method to form menu items from tasks in collection for their future deletion or editing.
     *
     * @param tasks collection of tasks, each task will be converted into string representation
     *              and put into String array by corresponding index.
     * @return String array, each element of which is corresponding task in collection.
     * @see TaskList
     */
    private String[] menuItemsOutOfCollection(TaskList tasks) {
        String[] collectionItemsAsMenu = new String[taskList.size()];
        for (int i = 0; i < collectionItemsAsMenu.length; i++) {
            collectionItemsAsMenu[i] = taskList.getTask(i).toString();
        }
        return collectionItemsAsMenu;

    }

    /**
     * Method to form int[] of indexes from user input that will be used to delete tasks from collection.
     *
     * @param input user's String input, which shows tasks that should be removed from collection,
     *              if several tasks are deleted, their indexes should be separated by spaces
     * @return int[] indexes, by which tasks will be deleted from collection
     * @throws NumberFormatException in case of any parsing error from String input to int,
     *                               using above defined format(space separation)
     */
    private Integer[] parseNeededRemoveIndexes(String input) throws NumberFormatException {
        String trimmedInput = input.trim().replaceAll(" +", " ");
        String s[] = trimmedInput.split(" ");
        Integer indexesToRemoveTasksFrom[] = new Integer[s.length];

        for (int i = 0; i < s.length; i++) {
            indexesToRemoveTasksFrom[i] = Integer.parseInt(s[i]);
        }


        return indexesToRemoveTasksFrom;
    }

    /**
     * Method to validate parsed indexes formed by {@link #parseNeededRemoveIndexes(String)}
     *
     * @param removalIndexes int[] of tasks indexes, that will be used in removal process from collection
     * @throws IndexOutOfBoundsException if validation was failed, so there is no tasks by given indexes in collection.
     *                                   Keep in mind, that tasks in the menu are shown from index 1 to i,
     *                                   but we validate indexes shifting them 1 position to the left due to tasks
     *                                   indexing in collection starting from 0.
     *                                   User will be notified about wrong indexes by a message, containing all
     *                                   wrong indexes entered.
     */
    private Set<Integer> checkForInvalidRemovalIndexes(Integer[] removalIndexes) throws IndexOutOfBoundsException {
        boolean invalidIndexDetected = false;
        ArrayList<Integer> invalidIndexes = new ArrayList<>();
        Set<Integer> uniqueRemovalIndexes = new HashSet<>();
        for (int i = 0; i < removalIndexes.length; i++) {
            if (removalIndexes[i] < 1 || removalIndexes[i] > taskList.size()) {
                invalidIndexDetected = true;
                invalidIndexes.add(removalIndexes[i]);
            }
            uniqueRemovalIndexes.add(removalIndexes[i]);
        }
        if (invalidIndexDetected) {
            throw new IndexOutOfBoundsException("! There were invalid removal indexes in your input " + Collections.singletonList(invalidIndexes) + ", please retry.");
        }
        return uniqueRemovalIndexes;
    }

    /**
     * Method to confirm or cancel removal from collection.
     *
     * @param indexes validated int[] of indexes that will be used to delete tasks from collection.
     */
    private void removeTasksByGivenIndexesConfirmation(Integer[] indexes) {
        System.out.println("Are you sure you want to remove tasks " + Arrays.toString(indexes)
                               + "? (This action can't be undone.)\n "
                               + " - To confirm - type 'y', to cancel - type 'n' ");
        do {
            inputChoice = getTrimmedInput();
            switch (inputChoice) {
                case "y":
                    synchronized (this) {
                        removeByIndexesConfirmed(indexes);
                        setListMutated(true);
                    }
                    System.out.println(" --- Tasks were deleted successfully! --- ");
                    log.info("User successfully deleted tasks from list.");
                    taskListMain();
                    break;
                case "n":
                case "back":
                case "prev":
                    removeTasks();
                    break;
                default:
                    System.out.print("To confirm removal please type 'y', to cancel it - type 'n'");
            }
        } while (true);
    }

    /**
     * Method to completely delete tasks from collection, this action can't be undone.
     *
     * @param indexes indexes, by which tasks will be permanently deleted from collection
     */
    private void removeByIndexesConfirmed(Integer[] indexes) {
        for (int i = 0, offset = 0; i < indexes.length; i++, offset++) {
            taskList.remove(indexes[i] - 1 - offset);
        }
    }

    /**
     * Method to create calendar for dates that will be specified by a user
     */
    private void calendar() {
        Date startDate;
        Date endDate;
        checkIfEmptyCollectionThenStepOut("Can't create calendar");
        System.out.println("----------- Calendar menu -----------");
        boolean correctInput = true;
        do {
            startDate = getDateOrStepOutTo(Menus.TASKLIST_MAIN, "START date for calendar");
            endDate = getDateOrStepOutTo(Menus.TASKLIST_MAIN, "END date for calendar");
            if (endDate.before(startDate)) {
                correctInput = false;
                System.out.println("! END date [" + endDate + "] should be after START [" + startDate + "] date, please retry your input.");
            }
        } while (!correctInput);
        renderCalendar(startDate, endDate);
        log.info("User successfully rendered calendar for specified dates.");
    }

    /**
     * Method to get date from user in one of following formats:
     * - YYYY-mm-DD HH:mm:ss
     * - YYYY-mm-DD
     * <p>
     * In case of wrong input user will be asked to retry input.
     * This method can be used in different menus.
     *
     * @param stepOutTo      menu, where to step out from current menu
     * @param message        String, that will be used, for showing messages according to the menu, that this method is used in
     * @param indexIfEditing in case we step out from edit menu, while using this method, we need to provide index for editing task menu
     * @return in case of correct input, valid Date object will be returned
     */
    private Date getDateOrStepOutTo(Menus stepOutTo, String message, int... indexIfEditing) {
        System.out.print("\nPlease enter " + message + " in following format 'YYYY-mm-DD HH:mm:ss', you may not enter seconds \n");
        Date actualDate = new Date();
        boolean correctInput;
        do {
            correctInput = true;
            inputChoice = getTrimmedInput();
            try {
                String pattern = "^\\d{4}-\\d{2}-\\d{2}(\\s\\d{2}:\\d{2}(:\\d{2})?)?";
                boolean matches = Pattern.matches(pattern, inputChoice);
                if (!matches) {
                    log.info("Invalid date format was inputted. " + inputChoice);
                    throw new ParseException("Date wasn't matched with the pattern", -1);
                }
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                if (inputChoice.lastIndexOf(':') == 16) {
                    sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                } else if (inputChoice.lastIndexOf(':') == 13) {
                    sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                }
                sdf.setLenient(false);
                actualDate = sdf.parse(inputChoice);
            } catch (ParseException ex) {
                //log.debug("Probable unexpected ParseException if pattern was matched. ", ex);
                correctInput = false;
                boolean routed = routeIfControlWord(inputChoice, Menus.GET_DATE, stepOutTo, message, indexIfEditing);
                //depending on the menu, predefined statements can route to different menus, so we use above method
                if (!routed)
                    System.out.print("! You've entered " + message + " in invalid format, please retry.");
            }
            if (correctInput) {
                return actualDate;
            }
        } while (true);
    }

    /**
     * Method to get title from user input.
     * <p>
     * Title is validated not to consist of spaces or newline character only.
     *
     * @param stepOutTo      the menu, we can step out to from current menu, using {@link #routeIfControlWord(String, Menus, Menus, String, int...)}
     * @param message        String value, that can be used in the menu messages, we are using the method in
     * @param indexIfEditing in case we use this method in edit menu, we need to provide index of task to edit, when we step out
     */
    private String getTitleOrStepOutTo(Menus stepOutTo, String message, int... indexIfEditing) {
        System.out.print("\nPlease enter title for your " + message + " task\n");
        do {
            inputChoice = getTrimmedInput();
            switch (inputChoice) {
                case "":
                    System.out.println("\n ! You might want to change task title, as it is empty or consists of spaces only.");
                    break;
                default:
                    boolean routed = routeIfControlWord(inputChoice, Menus.GET_TITLE, stepOutTo, message, indexIfEditing);
                    if (!routed) {
                        return inputChoice;
                    }
            }
        } while (true);
    }

    /**
     * Method to render calendar to console.
     * Calendar is formed using {@link Tasks#calendar(Iterable, Date, Date)} method
     *
     * @param from date to search for scheduled tasks from
     * @param to   date to search for scheduled tasks to
     * @see Tasks
     */
    private void renderCalendar(Date from, Date to) {
        System.out.println("Tasks, contained between start: "
                               + from.toString() + " and end: "
                               + to.toString() + " dates are shown below.\n");
        SortedMap<Date, Set<Task>> calendar = Tasks.calendar(taskList, from, to);
        if (calendar.size() == 0) {
            System.out.println("No tasks for selected period.\n");
        } else {
            calendar.forEach((K, V) -> {
                System.out.println(" --- " + K + " --- ");
                for (Task task : V) {
                    System.out.println(task);
                }
                System.out.println(" ------------------------------------\n");
            });
        }
        taskListMain();
    }

    /**
     * Method to add tasks to collection.
     *
     * @see Task
     */
    private void addTask() {
        Task taskToAdd;
        String taskTitle;
        boolean taskIsActive;
        boolean taskIsRepeated;
        Date taskTime = new Date();
        Date taskStart = new Date();
        Date taskEnd = new Date();
        int taskRepeatInterval = -1;
        System.out.println("----------- Add menu -----------");
        taskTitle = getTitleOrStepOutTo(Menus.TASKLIST_MAIN, "new");
        taskIsRepeated = getStateOrStepOutTo("repeated");
        if (taskIsRepeated) {
            do {
                taskStart = getDateOrStepOutTo(Menus.TASKLIST_MAIN, "START date for your new task");
                taskEnd = getDateOrStepOutTo(Menus.TASKLIST_MAIN, "END date for your new task");
                if (taskStart.after(taskEnd)) {
                    System.out.println("\n! END date [" + taskEnd + "] should be after START date [" + taskStart + "], please retry your input.");
                }
            } while (taskStart.after(taskEnd));
            taskRepeatInterval = getRepeatIntervalOrStepOutTo(Menus.TASKLIST_MAIN, "a");
        } else {
            taskTime = getDateOrStepOutTo(Menus.TASKLIST_MAIN, "date for your task");
        }
        taskIsActive = getStateOrStepOutTo("active");

        if (taskIsRepeated) {
            taskToAdd = new Task(taskTitle, taskStart, taskEnd, taskRepeatInterval);
            taskToAdd.setActive(taskIsActive);
            taskList.add(taskToAdd);
        } else {
            taskToAdd = new Task(taskTitle, taskTime);
            taskToAdd.setActive(taskIsActive);
            taskList.add(taskToAdd);
        }
        System.out.println("Your task was successfully added!");
        log.info("New task was added to list successfully.");
        taskListMain();
    }

    /**
     * Method to get state for boolean variables from user
     *
     * @param str String, to be used in displayed menu
     * @return true,  if input was 'y'
     * false, if input tas 'n'
     */
    private boolean getStateOrStepOutTo(String str) {
        System.out.print("\nPlease enter 'y' if your task is " + str + ", enter 'n' otherwise\n");
        do {
            inputChoice = getTrimmedInput();
            switch (inputChoice) {
                case "y":
                    return true;
                case "n":
                    return false;
                default:
                    boolean routed = routeIfControlWord(inputChoice, Menus.CHANGE_TASK_STATE, Menus.VOID, str);
                    if (!routed) {
                        System.out.print("Wrong input, please retry.");
                    }
            }
        } while (true);
    }

    /**
     * Method to get repeat interval for the task in minutes.
     * User should eventually enter valid interval value or step out to previous menu.
     *
     * @param stepOutTo      the menu, we can step out to from current menu, using {@link #routeIfControlWord(String, Menus, Menus, String, int...)}
     * @param message        String value, that can be used in the menu messages, we are using the method in
     * @param indexIfEditing in case we use this method in edit menu, we need to provide index of task to edit, when we step out
     * @return parsed correct int value as repeat interval
     */
    private int getRepeatIntervalOrStepOutTo(Menus stepOutTo, String message, int... indexIfEditing) {
        System.out.print("\nPlease enter " + message + " repeat interval for your task in MINUTES\n");
        int interval;
        do {
            inputChoice = getTrimmedInput();
            boolean routed = routeIfControlWord(inputChoice, Menus.GET_REPEAT_INTERVAL, stepOutTo, message, indexIfEditing);
            if (!routed) {
                try {
                    interval = Integer.parseInt(inputChoice);
                } catch (NumberFormatException ex) {
                    log.info("Invalid repeat interval format in user's input. ", ex);
                    System.out.println("You've entered repeat interval in wrong format, please retry.");
                    continue;
                }
                return interval * 60;
            }
        } while (true);
    }

    /**
     * Method to show edit menu to user,
     * it will display all tasks description if list of tasks is not empty,
     * otherwise user will be notified about it and proposed to hit ENTER key,
     * which will return him to previous menu {@link #taskListMain()}.
     */
    private void editTaskList() {
        checkIfEmptyCollectionThenStepOut("Nothing to edit.");
        editTaskMenu();
        boolean routed = false;
        int indexToEditTask = -1;
        do {
            inputChoice = getTrimmedInput();
            try {
                indexToEditTask = checkForValidEditIndex(inputChoice);
            } catch (NumberFormatException | IndexOutOfBoundsException ex) {
                log.info("Invalid edit indexes in user's input " + ex);
                routed = routeIfControlWord(inputChoice, Menus.EDIT_TASK_LIST, Menus.VOID, "");
                if (!routed) {
                    System.out.println(ex.getMessage());
                    continue;
                }
            }
            if (!routed)
                editTaskByIndex(indexToEditTask - 1);
            routed = false;
        } while (true);
    }

    /**
     * Menu, that will be displayed, when user wants to edit tasks in collection
     */
    private void editTaskMenu() {
        System.out.println("----------- Edit menu -----------");
        String[] collectionItemsAsMenu = menuItemsOutOfCollection(taskList);
        System.out.println("\n - Choose the number of a task list that you want to edit\n");
        menuUtil(collectionItemsAsMenu);
    }

    /**
     * Method to check if user has input correct index for editing the task
     *
     * @param input String, user's input
     * @return index of needed task in collection to edit,
     * if it isn't out of collection bounds and
     * was parsed to int correctly.
     * @throws NumberFormatException if {@code input} was not parsed to int correctly,
     *                               or specified parsed index is out of collection bounds
     */
    private int checkForValidEditIndex(String input) throws NumberFormatException {
        int index;
        try {
            index = Integer.parseInt(input);
        } catch (NumberFormatException ex) {
            throw new NumberFormatException("\nYou've entered invalid task number [" + input + "]. Please, retry your input.");
        }

        if (index < 1 || index > taskList.size()) {
            throw new IndexOutOfBoundsException("\nYour task number is out of bounds [" + input + "]. Please, retry your input.");
        } else {
            return index;
        }
    }

    /**
     * Method to determine what type of task will be edited(repeated or non-repeated) using user inputted index.
     * <p>
     * Depending on type of the task {@link #editRepeatedTask(Task, int)} or {@link #editNonRepeatedTask(Task, int)}
     * will be used to edit the task.
     *
     * @param index actual index of the task from collection, that will be edited
     */
    private void editTaskByIndex(int index) {
        System.out.println("\n--- You are editing --- \n" + taskList.getTask(index));
        Task editedTask = taskList.getTask(index);
        boolean taskIsRepeated = editedTask.isRepeated();
        if (taskIsRepeated) {
            editRepeatedTask(editedTask, index);
        } else {
            editNonRepeatedTask(editedTask, index);
        }
    }

    /**
     * Method to edit repeated task.
     * Options for editing repeated task are provided by {@link #editOptionsForRepeatedTask(Task)}
     * <p>
     * After task was edited, user is returned again on the menu with options to edit same task,
     * as there are several edit options to choose from.
     *
     * @param editedTask the actual task that will be edited
     * @param index      index of the task that will be edited,
     *                   used also while stepping out from this method to previous menu {@link #editTaskByIndex(int)}
     */
    private void editRepeatedTask(Task editedTask, int index) {
        editOptionsForRepeatedTask(editedTask);
        do {
            inputChoice = getTrimmedInput();
            switch (inputChoice) {
                case "1": //Edit the title
                    String newTitle = getTitleOrStepOutTo(Menus.EDIT_TASK_BY_INDEX, "new", index);
                    synchronized (this) {
                        editedTask.setTitle(newTitle);
                        setListMutated(true);
                    }
                    log.info("Task title was edited successfully.");
                    break;
                case "2": //Edit time
                    synchronized (this) {
                        editStartAndEndTimes(editedTask, index);
                        setListMutated(true);
                    }
                    log.info("Task times was edited successfully.");
                    break;
                case "3": //Change repeat interval
                    int newRepeatInterval = getRepeatIntervalOrStepOutTo(Menus.EDIT_TASK_BY_INDEX, "new", index);
                    synchronized (this) {
                        editedTask.setRepeatInterval(newRepeatInterval);
                        setListMutated(true);
                    }
                    log.info("Task repeat interval was edited successfully.");
                    System.out.println("Repeat interval was edited successfully!");
                    break;
                case "4": //Change active state
                    synchronized (this) {
                        editChangeActiveState(editedTask);
                        setListMutated(true);
                    }
                    log.info("Task state was edited successfully.");
                    break;
                default:
                    boolean routed = routeIfControlWord(inputChoice, Menus.EDIT_REPEATED_TASK, Menus.VOID, "", index);
                    if (!routed) {
                        System.out.println("Incorrect input, please retry.");
                        continue;
                    }
            }
            taskListMain();
        } while (true);
    }

    /**
     * Edit options when the edited task is repeated.
     * Option to make task active/inactive is computed using current task state.
     *
     * @param editedTask actual task that will be edited
     */
    private void editOptionsForRepeatedTask(Task editedTask) {
        System.out.println("\n - Choose what do you want to edit in your task.\n");
        menuItems = new String[]{
            "Edit the title.",
            "Edit times for the task.",
            "Edit repeat interval for the task.",
            editedTask.isActive() ? "Make your task inactive." : "Make your task active.",
        };

        menuUtil(menuItems);
    }

    /**
     * Method to edit start and end time while editing repeated task.
     *
     * @param editedTask the actual task that will be edited
     * @param index      index of the task that will be edited,
     *                   used also while stepping out from this method to previous menu {@link #editTaskByIndex(int)}
     */
    private void editStartAndEndTimes(Task editedTask, int index) {
        Date newTaskStart;
        Date newTaskEnd;
        do {
            newTaskStart = getDateOrStepOutTo(Menus.EDIT_TASK_BY_INDEX, "START date for your task", index);
            newTaskEnd = getDateOrStepOutTo(Menus.EDIT_TASK_BY_INDEX, "END date for your task", index);
            if (newTaskStart.after(newTaskEnd)) {
                System.out.println("\n! END date [" + newTaskEnd + "] should be after START date [" + newTaskStart + "], please retry your input.");
            }
        } while (newTaskStart.after(newTaskEnd));
        synchronized (this) {
            boolean isActive = editedTask.isActive();
            editedTask.setTime(newTaskStart, newTaskEnd, editedTask.getRepeatInterval());
            editedTask.setActive(isActive);
            setListMutated(true);
        }
        System.out.println("Times were edited successfully!");
    }

    /**
     * Method to edit non-repeated task.
     *
     * @param editedTask the actual task that will be edited
     * @param index      index of the task that will be edited,
     *                   used also while stepping out from this method to previous menu {@link #editTaskByIndex(int)}
     */
    private void editNonRepeatedTask(Task editedTask, int index) {
        editOptionsForNonRepeatedTask(editedTask);
        do {
            inputChoice = getTrimmedInput();
            switch (inputChoice) {
                case "1": //Edit the title
                    String newTitle = getTitleOrStepOutTo(Menus.EDIT_TASK_BY_INDEX, "new", index);
                    synchronized (this) {
                        editedTask.setTitle(newTitle);
                        setListMutated(true);
                    }
                    log.info("Task title was edited successfully.");
                    break;
                case "2": //Edit time
                    Date newDate = getDateOrStepOutTo(Menus.EDIT_TASK_BY_INDEX, "new", index);
                    synchronized (this) {
                        boolean isActive = editedTask.isActive();
                        editedTask.setTime(newDate);
                        editedTask.setActive(isActive);
                        setListMutated(true);
                    }
                    log.info("Task time was edited successfully.");
                    System.out.println("Scheduled time was edited successfully!");
                    break;
                case "3": //Change active state
                    synchronized (this) {
                        editChangeActiveState(editedTask);
                        setListMutated(true);
                    }
                    log.info("Task state was edited successfully.");
                    break;
                default:
                    boolean routed = routeIfControlWord(inputChoice, Menus.EDIT_NON_REPEATED_TASK, Menus.VOID, "", index);
                    if (!routed) {
                        System.out.println("Incorrect input, please retry.");
                        continue;
                    }
            }
            taskListMain();
        } while (true);
    }

    /**
     * Edit options when the edited task is non-repeated.
     *
     * @param editedTask actual task that will be edited,
     *                   used to compute menu item about it's current active/inactive state
     */
    private void editOptionsForNonRepeatedTask(Task editedTask) {
        System.out.println("\n - Choose what do you want to edit in your task.\n");
        menuItems = new String[]{
            "Edit the title.",
            "Edit scheduled time for the task.",
            editedTask.isActive() ? "Make your task inactive." : "Make your task active.",
        };

        menuUtil(menuItems);
    }

    /**
     * Method to make {@code task} active/inactive
     * by using {@link Task#setActive(boolean)} method
     *
     * @param task task, which state is edited
     */
    private void editChangeActiveState(Task task) {
        if (task.isActive()) {
            System.out.println("Task is now INACTIVE");
        } else {
            System.out.println("Task is now ACTIVE");
        }
        task.setActive(!task.isActive());
    }

    /**
     * Method to route user from one menu to another.
     * <p>
     * There are 3 types of predefined statements that can be inputted in most menus:
     * <p>
     * 1. 'menu'        - displays current menu.
     * 2. 'prev'/'back' - routes user to the previous menu.
     * 3. 'exit'/quit'  - exits the application using {@link #exit()} method.
     *
     * @param controlWord  one of predefined statements.
     * @param currentMenu  current menu, user is working in.
     * @param routedToMenu menu, which will be displayed in case of 'back'/'prev' input.
     * @param message      String that could be used for current menu messages.
     * @param index        used in case we are in edit menu.
     * @return true, is {@code controlWord} was one of the predefined statements
     * and method routed user to any of the menus.
     * false, if {@code controlWord} was not a predefined statement.
     */
    private boolean routeIfControlWord(String controlWord, Menus currentMenu, Menus routedToMenu, String message, int... index) {
        switch (controlWord) {
            case "menu":
                switch (currentMenu) {
                    case CHOOSE_TASKLIST:
                        showChooseTaskListMenu();
                        return true;

                    case TASKLIST_MAIN:
                        showTaskListMainMenu();
                        return true;

                    case REMOVE_TASKS:
                        showRemoveTasksMenu();
                        return true;

                    case EDIT_TASK_LIST:
                        editTaskMenu();
                        return true;

                    case EDIT_REPEATED_TASK:
                    case EDIT_NON_REPEATED_TASK:
                        editTaskByIndex(index[0]);
                        return true;

                    case GET_DATE:
                        System.out.print("Please enter " + message + " in following format 'YYYY-mm-dd HH:mm:ss', you may not enter the seconds");
                        return true;

                    case GET_TITLE:
                        System.out.print("\nPlease enter title for your " + message + " task\n");
                        return true;

                    case CHANGE_TASK_STATE:
                        System.out.print("\nPlease enter 'y' if your task is active, enter 'n' otherwise\n");
                        return true;

                    case GET_REPEAT_INTERVAL:
                        System.out.print("\nPlease enter " + message + " repeat interval for your task in MINUTES\n");
                        return true;
                }
                break;

            case "prev":
            case "back":
                switch (currentMenu) {
                    case CHOOSE_TASKLIST:
                        return false;

                    case TASKLIST_MAIN:
                        chooseTaskList();
                        return true;

                    case EDIT_TASK_LIST:
                    case REMOVE_TASKS:
                    case CHANGE_TASK_STATE:
                        taskListMain();
                        return true;

                    case EDIT_REPEATED_TASK:
                    case EDIT_NON_REPEATED_TASK:
                        editTaskList();
                        return true;

                    case GET_TITLE:
                    case GET_DATE:
                    case GET_REPEAT_INTERVAL:
                        if (routedToMenu == Menus.TASKLIST_MAIN) {
                            taskListMain();
                            return true;
                        } else if (routedToMenu == Menus.EDIT_TASK_BY_INDEX) {
                            editTaskByIndex(index[0]);
                            return true;
                        }
                        return false;

                }
                break;
            case "exit":
            case "quit":
                exit();
                return false;
        }
        return false;
    }

    /**
     * Method to start new thread for notifications.
     *
     * @param state true, if notifications should be enabled
     *              false, if they should be disabled
     * @see NotificationsManager
     */
    private void pokeNotificationsManager(boolean state) {
        log.info("Notification manager was poked with: " + state + " state.");
        try {
            notifier.interrupt();
            notifier.join();
        } catch (InterruptedException ex) {
            log.info("Notifications manager thread was interrupted.", ex);
        }
        if (state) {
            notifier = new NotificationsManager();
            notifier.setParentController(this);
            notifier.setPriority(Thread.MAX_PRIORITY);
            notifier.start();
        }
    }

    /**
     * Method to exit the application.
     */
    private void exit() {
        try {
            File oldTasks = new File(DEFAULT_STORAGE_FILE_NAME);
            TaskIO.writeText(taskList, oldTasks);
        } catch (IOException ex) {
            log.error("Exception happened while writing to default storage file while saving upon exiting the application. ", ex);
        }
        log.info("List of tasks was saved before the exit.");
        System.out.println("Saving...");
        log.info("Exiting the app.");
        System.out.println("Exiting...");
        System.exit(0);
    }

    /**
     * Enum that represents different menus, available to user,
     * used while routing between one another.
     */
    private enum Menus {
        CHOOSE_TASKLIST,
        TASKLIST_MAIN,
        REMOVE_TASKS,
        EDIT_TASK_LIST,
        EDIT_TASK_BY_INDEX,
        EDIT_NON_REPEATED_TASK,
        EDIT_REPEATED_TASK,
        GET_DATE,
        GET_TITLE,
        CHANGE_TASK_STATE,
        GET_REPEAT_INTERVAL,
        VOID
    }
}