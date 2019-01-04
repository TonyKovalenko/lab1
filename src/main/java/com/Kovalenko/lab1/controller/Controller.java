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
 * @since 1.8
 *
 * @see Task
 * @see TaskList
 * @see TaskIO
 */
public enum Controller {
    INSTANCE;
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
        EDIT_NOTIFICATIONS,
        EXIT,
        VOID
    }

    private static Logger log = Logger.getLogger(Controller.class.getName());

    private volatile ArrayTaskList taskList;
    private static final String DEFAULT_STORAGE_FILE_NAME = "myTasks.txt";
    private static final String SAVED_NOTIFICATIONS_STATE_FILE_NAME = "out/nstate.bin";
    private boolean notificationsAreEnabled;
    private volatile Boolean listMutated;
    private NotificationsManager notifier;

    Controller() {
        listMutated = false;
        notifier = new NotificationsManager();
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
     *  Main method to start an application,
     *  it welcomes the user by calling {@link #welcomeMessage()} method
     *  and continues execution by passing the control
     *  to {@link #chooseTaskList()} method
     */
    public void run() {
        log.info("App started.");
        notificationsAreEnabled = loadNotificationsState();
        welcomeMessage();
        chooseTaskList();
    }

    /**
     * Method for printing welcome message to the user
     */
    private void welcomeMessage() {
        System.out.println("-------------------------------\n" +
                           "*** Welcome to Task Manager ***\n" +
                           "-------------------------------");
    }

    /**
     * Utility method for printing menu, using {@code menuFormat}.
     * Example of printed menu:
     * #1   menuItem1
     * #2   menuItem2
     * ...  ...
     * In following methods, user can select menu items
     * by typing it's corresponding number to console, e.g. 1, 2, 3, etc.
     *
     * @param items menu items, that should be displayed
     */
    private void menuUtil(String...items) {
        String menuFormat = "#%d\t%s%n";
        int i = 0;

        for (String item : items) {
            System.out.printf(menuFormat, ++i, item);
        }
    }

    /**
     * Method for reading user input from console using BufferedReader class
     *
     * @return  trimmed value of users input
     * @see BufferedReader
     */
    private String getTrimmedInput() {
        System.out.print("\n>>> Your input: ");
        String input = "";
        BufferedReader buff  = new BufferedReader(new InputStreamReader(System.in));
        do {
            try {
                input = buff.readLine();
            } catch (IOException e) {
                log.error("NULL returned from user's input stream. ", e);
                System.out.print("! Cannot read input because of internal error, please retry the input.");
            }
        } while (input == null);

        return input.trim();
    }

    /**
     * Method for printing the menu, by using which user can choose,
     * from where he wants all the tasks to be loaded from
     *
     * Menu is printed by using {@link #menuUtil(String...)} method
     */
    private void showChooseTaskListMenu()  {
        System.out.println("\n - Please choose a further action by typing it's number in following menu \u2193 \n");
        String menuItemCreateEmptyTaskList = "Create new empty list of tasks.";
        String menuItemLoadTaskListFromFile = "Load list of tasks from existing file.";
        String menuItemUseLastSavedTaskList = "Continue with last saved list of tasks.";

        menuUtil(menuItemCreateEmptyTaskList, menuItemLoadTaskListFromFile, menuItemUseLastSavedTaskList);
    }

    /**
     * Method for actual choosing from where he wants all the tasks to be loaded from
     * Available typing options: '1' - Creating new empty TaskList
     *
     *                           '2' - Load from own file, by specifying path to it,
     *                           by using  {@link #loadFromUserSpecifiedFile()} method
     *
     *                           '3' - Load from last saved default storage file,
     *                           by using {@link #loadFromLastSavedFile()} method
     *
     *                           'exit', 'quit' - will exit the application (this is predefined statement)
     *
     *                           'menu' - will display current menu and will wait for user input (this is predefined statement)
     *
     * Predefined statements will be resolved in {@link #routeIfControlWord(String, Menus, Menus, String, int...)}
     * In case of wrong input, user will be asked to retry.
     */
    private void chooseTaskList() {
        showChooseTaskListMenu();
        String inputChoice;
        do {
            inputChoice = getTrimmedInput();
            switch (inputChoice) {
                case "1":
                    taskList = new ArrayTaskList();
                    System.out.println("New empty list was created.");
                    log.info("User created new empty list of tasks.");
                    pokeNotificationsManager(false);
                    taskListMain();
                    break;
                case "2":
                    log.info("User tried to load list of tasks from custom file.");
                    pokeNotificationsManager(false);
                    loadFromUserSpecifiedFile();
                    taskListMain();
                case "3":
                    log.info("User tried to load list from last saved file.");
                    pokeNotificationsManager(notificationsAreEnabled);
                    loadFromLastSavedFile();
                    taskListMain();
                    break;
                default:
                    boolean routed = routeIfControlWord(inputChoice, Menus.CHOOSE_TASKLIST, Menus.VOID, "");
                    //above method will resolve predefined words and will route the flow of the program
                    if(!routed)
                    System.out.print("Incorrect input, please retry.");
            }
        } while (true);
    }

    /**
     * - Method for adding tasks to {@code taskList}
     * by loading collection of Tasks from
     * default storage text file with {@code defaultFileName} name
     *
     * - In case of last saved storage file is missing,
     * FileNotFoundException will be thrown by {@link com.Kovalenko.lab1.model.TaskIO#readText(TaskList, File)} method
     * So instead new empty default storage file will be created {@link #makeEmptyFileByName(String)},
     * and {@code taskList} will be empty too.
     *
     * - In case of last saved storage file is corrupted,
     * i.e. format of any Task inside the file is not one of such templates:
     *
     *    "Task title" at [2014-06-28 18:00:13.000];
     *    "Very ""Good"" title" at [2013-05-10 20:31:20.001] inactive; (quotes in titles are doubled)
     *    "Other task" from [2010-06-01 08:00:00.000] to [2010-09-01 00:00:00.000] every [1 day].
     *    "not active repeated" from [1970-01-02 05:46:40.000] to [1970-01-02 11:20:00.000] every [7 minutes 30 seconds] inactive;
     *
     * then ParseException will be thrown by {@link com.Kovalenko.lab1.model.TaskIO#readText(TaskList, File)} method
     * So instead new empty default file will be created, and {@code taskList} will be empty too.
     *
     * - In case of any IOException while reading from default storage file,
     * there will be empty {@code taskList} created
     *
     * @see TaskIO
     */
    private void loadFromLastSavedFile() {
        File defaultStorageFile = new File(DEFAULT_STORAGE_FILE_NAME);
        boolean errorHappened = false;
        taskList = new ArrayTaskList();
        try {
            TaskIO.readText(taskList, defaultStorageFile);
        } catch (FileNotFoundException ex) {
            log.info("Last saved file was not found(either this is the first launch or it was accidentally deleted outside the application). ", ex);
            errorHappened = true;
            try {
                if (defaultStorageFile.createNewFile()) {
                    makeEmptyFileByName(DEFAULT_STORAGE_FILE_NAME);
                    System.out.println("! Seems like the last saved file is missing, "
                                           + "so new empty file was created and list of tasks is now empty");
                }
            } catch (IOException ioe) {
                log.fatal("Exception while creating new empty default storage file. ", ioe);
            }
        } catch (ParseException|StringIndexOutOfBoundsException ex) {
            errorHappened = true;
            taskList = new ArrayTaskList();
            makeEmptyFileByName(DEFAULT_STORAGE_FILE_NAME);
            System.out.println("! Seems like the content of last saved file is corrupted, "
                                + "so new empty file was created and list of tasks is now empty");
            log.warn("Last saved file was corrupted outside the application. File was emptied and new list of tasks was created. ", ex);
        } catch (IOException ex) {
            errorHappened = true;
            System.out.println("! Error happened while reading from file, list of tasks is now empty");
            taskList = new ArrayTaskList();
            log.warn("There was an exception, while reading from default storage file to list of tasks, empty collection was created. ", ex);
        }
        if(!errorHappened) {
            System.out.println("File was loaded successfully! ");
            log.info("Last saved default storage file was loaded successfully.");
        }
    }

    /**
     * Method to create new empty file
     * or to empty existing one
     *
     * @param name name of the file,
     *             that will be either created empty or
     *             will be emptied after calling this method
     */
    private void makeEmptyFileByName(String name) {
        File defaultFile = new File(name);
        try {
            PrintWriter writer = new PrintWriter(defaultFile);
            writer.print("");
            writer.close();
        } catch (IOException ex) {
            log.fatal("Exception happened, while emptying or creating new file: " + name, ex);
        }
    }

    /**
     * - Method for adding tasks to {@code taskList}
     * by loading collection of Tasks from
     * user specified {@code inputtedFilePath} text file.
     *
     * - In case of user specified file is missing,
     * FileNotFoundException will be thrown by {@link com.Kovalenko.lab1.model.TaskIO#readText(TaskList, File)} method.
     * So user will be redirected to previous menu {@link #showChooseTaskListMenu()}.
     *
     * - In case of user specified file is corrupted,
     * i.e. format of any Task inside the file is not one of such templates:
     *
     *    "Task title" at [2014-06-28 18:00:13.000];
     *     "Very ""Good"" title" at [2013-05-10 20:31:20.001] inactive; (quotes in titles are doubled)
     *     "Other task" from [2010-06-01 08:00:00.000] to [2010-09-01 00:00:00.000] every [1 day].
     *     "not active repeated" from [1970-01-02 05:46:40.000] to [1970-01-02 11:20:00.000] every [7 minutes 30 seconds] inactive;     *
     *
     * ParseException will be thrown by {@link com.Kovalenko.lab1.model.TaskIO#readText(TaskList, File)} method.
     * So user will be redirected to previous menu {@link #showChooseTaskListMenu()}.
     *
     * - In case of any IOException while reading from user specified file,
     * user will be redirected to previous menu {@link #showChooseTaskListMenu()}.
     *
     * @see TaskIO
     */
    private void loadFromUserSpecifiedFile() {
        String inputtedFilePath;
        boolean errorHappened = false;
        System.out.println("\n - Please enter path to the file with tasks.");
        inputtedFilePath = getTrimmedInput();
        try {
            taskList = new ArrayTaskList();
            TaskIO.readText(taskList, new File(inputtedFilePath));
        } catch (FileNotFoundException ex) {
            System.out.println("\n! Sorry, but the specified file was not found. Returning you to previous menu.");
            log.info("User specified nonexistent file to load tasks from. ", ex);
            errorHappened = true;
        } catch (ParseException|StringIndexOutOfBoundsException ex) {
            System.out.println("\n! Sorry, but the specified file contains incorrect tasks format inside. Returning you to previous menu.");
            log.info("User's specified file contained incorrect task format. ", ex);
            errorHappened = true;
        } catch (IOException ex) {
            System.out.println("\n! Sorry, the specified file can't be read properly. Returning you to previous menu.");
            log.warn("User's specified file was not read correctly. ", ex);
            errorHappened = true;
        }  finally {
         if(errorHappened){
                chooseTaskList();
            }
        }
        System.out.println("File was loaded successfully.");
        log.info("User's specified file was loaded successfully.");
    }

    /**
     * Main menu, printed using {@link #menuUtil(String...)}
     *
     * Here user can choose what to do next with collection
     *
     */
    private void showTaskListMainMenu() {
        System.out.println("\n---------- Main menu -----------");
        System.out.println("\n - Please choose what do you want to do next \u2193 \n");
        String menuItemViewTasks = "View all your tasks.";
        String menuItemAddTask = "Add a new task to list.";
        String menuItemRemoveTask = "Remove tasks from list.";
        String menuItemEditTask = "Edit task in list.";
        String menuItemViewCalendar = "View calendar.";
        String menuItemEditNotifications = "Edit notifications.";

        menuUtil(menuItemViewTasks, menuItemAddTask, menuItemRemoveTask, menuItemEditTask, menuItemViewCalendar, menuItemEditNotifications);
    }

    /**
     * Method for actual choosing what user wants to do with the loaded collection
     * Available typing options: '1' - Viewing all tasks in the list
     *
     *                           '2' - Adding new task to the list
     *
     *                           '3' - Remove task from list
     *
     *                           '4' - Edit task in list
     *
     *                           '5' - View calendar
     *
     *                           '3' - Edit notifications
     *
     *                           'back', 'prev' - will return to previous menu (this is predefined statement)
     *
     *                           'exit', 'quit' - will exit the application (this is predefined statement)
     *
     *                           'menu' - will display current menu and will wait for user input (this is predefined statement)
     * Predefined statements are resolved in {@link #routeIfControlWord(String, Menus, Menus, String, int...)}
     * In case of wrong input, user will be asked to retry.
     */
    private void taskListMain() {
        showTaskListMainMenu();
        pokeNotificationsManager(notificationsAreEnabled);
        String inputChoice;
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
                case "6":
                    //Edit notifications
                    editNotifications();
                    break;
                default:
                    boolean routed = routeIfControlWord(inputChoice, Menus.TASKLIST_MAIN, Menus.VOID, "");
                    if(!routed)
                    System.out.print("Incorrect input, please retry.");
            }
        } while (true);
    }

    /**
     * Method for viewing current list of tasks
     *
     * If it is empty, there will be a message about it,
     * if not, list of tasks will be displayed on the screen by using {@link #viewCollection()}.
     *
     * After viewing user should press ENTER button,
     * this will redirect him to previous menu by calling {@link #showChooseTaskListMenu()}
     */
    private void viewTasks() {
        checkIfEmptyCollectionThenStepOut("Nothing to view.");
        System.out.println("\n----------- View menu -----------\n");
        System.out.println("List of all tasks is displayed below.\n");
        viewCollection(); //will print collection as menu using menuUtil() method
        waitForEnterButton();
    }

    /**
     * Method for viewing list of task as a menu, using {@link #menuUtil(String...)} method.
     */
    private void viewCollection() {
        String [] taskListAsMenu = new String[taskList.size()];
        int i = 0;
        for (Task task : taskList) {
            taskListAsMenu[i] = task.toString();
            ++i;
        }
        menuUtil(taskListAsMenu);
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
     * If so, user will be returned to main menu after pressing ENTER key.
     *
     * @see TaskList
     */
    private void checkIfEmptyCollectionThenStepOut(String str) {
        if (taskList.size() == 0) {
            System.out.println("\nYour list of tasks is empty at the moment. " + str);
            waitForEnterButton();
            taskListMain();
        }
    }

    /**
     * Method to show remove menu to user,
     * it will display all tasks description, if list of tasks is not empty,
     * otherwise user will be notified about it and proposed to hit ENTER key,
     * which will return him to previous menu {@link #showChooseTaskListMenu()}.
     *
     * There is an option to delete several tasks at once if user enters task's
     * menu indexes, separated by spaces. But still, this input will be validated.
     *
     */
    private void removeTasks() {
        checkIfEmptyCollectionThenStepOut("Nothing to remove.");
        int[] indexesToRemoveTasksFrom = null;
        showRemoveTasksMenu();
        String inputChoice;
        do {
            inputChoice = getTrimmedInput();
            try {
                indexesToRemoveTasksFrom = parseNeededRemoveIndexes(inputChoice); //parse user input to get desired remove indexes
                checkForInvalidRemovalIndexes(indexesToRemoveTasksFrom); //validate just inputted indexes
            } catch (NumberFormatException ex) {
                    boolean routed = routeIfControlWord(inputChoice, Menus.REMOVE_TASKS, Menus.VOID , "");
                    if(!routed) {
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

            if(indexesToRemoveTasksFrom != null) {
                removeTasksByGivenIndexesConfirmation(indexesToRemoveTasksFrom);
            }
            } while (true);
    }

    /**
     * Menu, that will be displayed, when user chooses to delete tasks from collection
     *
     * Tasks from collection will be displayed in following format:
     *
     * #1   Task description
     * #2   Task description
     * ...  ...
     *
     * Such output is formed in {@link #menuItemsOutOfCollection(TaskList)} method.
     *
     * Remove logic relies on the fact that actual index of task in collection is (#ofSelectedTask - 1),
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
     *
     * @see TaskList
     */
    private String[] menuItemsOutOfCollection(TaskList tasks) {
        String[] collectionItemsAsMenu = new String[taskList.size()];
        int index = 0;
        for (Task task : tasks) {
            collectionItemsAsMenu[index] = task.toString();
            ++index;
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
    private int[] parseNeededRemoveIndexes(String input) throws NumberFormatException {
        String trimmedInput = input.trim().replaceAll(" +", " ");
        String s[] = trimmedInput.split(" ");
        int indexesToRemoveTasksFrom[] = new int[s.length];

        try {
            for(int i = 0 ; i < s.length ; i++) {
                indexesToRemoveTasksFrom[i] = Integer.parseInt(s[i]);
            }
        } catch (NumberFormatException ex) {
            throw ex;
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
    private void checkForInvalidRemovalIndexes(int[] removalIndexes) throws IndexOutOfBoundsException {
        boolean invalidIndexDetected = false;
        ArrayList<Integer> invalidIndexes = new ArrayList<>();
        for(int i = 0; i < removalIndexes.length; i++) {
            if (removalIndexes[i] < 1 || removalIndexes[i] > taskList.size()) {
                invalidIndexDetected = true;
                invalidIndexes.add(removalIndexes[i]);
            }
        }
        if(invalidIndexDetected) {
            throw new IndexOutOfBoundsException("! There were invalid removal indexes in your input " + Collections.singletonList(invalidIndexes) + ", please retry.");
        }
    }

    /**
     *  Method to confirm or cancel removal from collection.
     *  User should enter 'y' - to confirm deletion, so {@link #removeByIndexesConfirmed(int[])} will be called
     *
     *                    'n',
     *                    'back',
     *                    'prev' - to cancel deletion, then there will be a redirect
     *                             back to remove menu {@link #removeTasks()}
     *
     * @param indexes validated int[] of indexes that will be used to delete tasks from collection.
     */
    private void removeTasksByGivenIndexesConfirmation(int[] indexes) {
        System.out.println("Are you sure you want to remove tasks " + Arrays.toString(indexes)
                               + "? (This action can't be undone.)\n "
                               + " - To confirm - type 'y', to cancel - type 'n' ");
        String inputChoice;
        do {
            inputChoice = getTrimmedInput();
            String trimmedInput = inputChoice;
            switch (trimmedInput) {
                case "y":
                    synchronized (this) {
                        removeByIndexesConfirmed(indexes);
                        setListMutated(true);
                    }
                    System.out.println(" --- Tasks were deleted successfully! --- ");
                    log.info("User successfully deleted tasks from list.");
                    removeTasks();
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
    private void removeByIndexesConfirmed(int[] indexes) {
        int offset = 0;
        for (int index : indexes) {
            taskList.remove(index - 1 - offset);
            offset++;
        }
    }

    /**
     * Method to create calendar for dates that will be specified by a user
     *
     * If a task list is empty, there will be a notification about it,
     * then user will have to press ENTER to go to previous menu {@link #taskListMain()}
     *
     * Next steps will be the input of {@code startDate} and {@code endDate}
     *
     * This input will be validated by checking of {@code endDate} is after {@code startDate}
     * If this validation fails, user will be proposed to retry his input of dates.
     *
     * After input was validated, {@link #renderCalendar(Date, Date)} will be executed,
     * to output calendar for selected dates
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
            if(endDate.before(startDate)) {
                correctInput = false;
                System.out.println("! END date [" + endDate + "] should be after START [" + startDate + "] date, please retry your input.");
            }
        } while (!correctInput);
            renderCalendar(startDate, endDate);
            log.info("User successfully rendered calendar for specified dates.");
        }

    /**
     * Method to get date from user in one of following formats:
     *  - YYYY-mm-DD HH:mm:ss
     *  - YYYY-mm-DD
     *
     *  In case of wrong input user will be asked to retry input.
     *  This method can be used in different menus.
     *
     * @param stepOutTo menu, where to step out from current menu
     * @param message String, that will be used, for showing messages according to the menu, that this method is used in
     * @param indexIfEditing in case we step out from edit menu, while using this method, we need to provide index for editing task menu
     * @return in case of correct input, valid Date object will be returned
     */
    private Date getDateOrStepOutTo(Menus stepOutTo, String message, int...indexIfEditing) {
        System.out.print("\nPlease enter " + message + " in following format 'YYYY-mm-DD HH:mm:ss, you may not enter part after days'\n");
        String inputChoice;
        Date actualDate = new Date();
        boolean correctInput;
        do {
            correctInput = true;
            inputChoice = getTrimmedInput();
            try {
                String pattern = "^\\d{4}-\\d{2}-\\d{2}(\\s\\d{2}:\\d{2}:\\d{2})?$";
                boolean matches = Pattern.matches(pattern, inputChoice);
                if(!matches) {
                    log.info("Invalid date format was inputted. " + inputChoice);
                    throw new ParseException("Date wasn't matched with the pattern", -1);
                }
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                if (inputChoice.indexOf(':') == 13) {
                    sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                }
                sdf.setLenient(false);
                actualDate = sdf.parse(inputChoice);
            } catch (ParseException ex) {
                //log.debug("Probable unexpected ParseException if pattern was matched. ", ex);
                correctInput = false;
                boolean routed = routeIfControlWord(inputChoice, Menus.GET_DATE, stepOutTo, message, indexIfEditing);
                //depending on the menu, predefined statements can route to different menus, so we use above method
                if(!routed)
                System.out.print("! You've entered " + message + " in invalid format, please retry.");
            }
            if(correctInput) {
                return actualDate;
            }
        } while (true);
    }

    /**
     * Method to get title from user input.
     *
     * Title is validated not to consist of spaces or newline character only.
     *
     * @param stepOutTo      the menu, we can step out to from current menu, using {@link #routeIfControlWord(String, Menus, Menus, String, int...)}
     * @param message        String value, that can be used in the menu messages, we are using the method in
     * @param indexIfEditing in case we use this method in edit menu, we need to provide index of task to edit, when we step out
     */
    private String getTitleOrStepOutTo(Menus stepOutTo, String message, int...indexIfEditing) {
        System.out.print("\nPlease enter title for your "+ message +" task\n");
        String inputChoice;
        do {
            inputChoice = getTrimmedInput();
            switch (inputChoice) {
                case "":
                    System.out.println("\n ! You might want to change task title, as it is empty or consists of spaces only.");
                    break;
                default:
                    boolean routed = routeIfControlWord(inputChoice, Menus.GET_TITLE, stepOutTo, message, indexIfEditing);
                    if(!routed)
                        return inputChoice;
            }
        } while (true);
    }

    /**
     * Method to render calendar to console.
     * Calendar is formed using {@link Tasks#calendar(Iterable, Date, Date)} method
     *
     * For each date between {@code from} and {@code to}
     * when there will be a tasks scheduled, following output will be produced
     *
     * --- Date ---
     * Task 1 ....
     * Task 2 ....
     * ------------
     *
     * If there will be no tasks scheduled for specified period
     * there will be notification message about it.
     *
     * After output was produced, user will be redirected to previous menu after pressing ENTER key.
     *
     * @param from date to search for scheduled tasks from
     * @param to   date to search for scheduled tasks to
     *
     * @see Tasks
     */
    private void renderCalendar(Date from, Date to) {
        System.out.println("Tasks, contained between start: "
                               + from.toString() + " and end: "
                               + to.toString() + " dates are shown below.\n");
        SortedMap<Date, Set<Task>> calendar = Tasks.calendar(taskList, from, to);
        if(calendar.size() == 0) {
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
        waitForEnterButton();
        taskListMain();
    }

    /**
     * Method to add tasks to collection.
     *
     * Title is validated not to consist of spaces or newline character only.
     * Start date is validated to be BEFORE end date.
     * User should input all necessary fields before the task could be created.
     * Value for {@code taskIsActive} and {@code taskIsRepeated} is returned by {@link #getStateOrStepOutTo(String)}
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
        if(taskIsRepeated) {
            do {
                taskStart = getDateOrStepOutTo(Menus.TASKLIST_MAIN, "START date for your new task");
                taskEnd = getDateOrStepOutTo(Menus.TASKLIST_MAIN, "END date for your new task");
                if(taskStart.after(taskEnd)) {
                    System.out.println("\n! END date [" + taskEnd + "] should be after START date [" + taskStart + "], please retry your input.");
                }
            } while (taskStart.after(taskEnd));
            taskRepeatInterval = getRepeatIntervalOrStepOutTo(Menus.TASKLIST_MAIN, "a");
        } else {
            taskTime = getDateOrStepOutTo(Menus.TASKLIST_MAIN, "date for your task");
        }
        taskIsActive = getStateOrStepOutTo("active");

        if(taskIsRepeated) {
            taskToAdd = new Task(taskTitle, taskStart, taskEnd, taskRepeatInterval);
            taskToAdd.setActive(taskIsActive);
            taskList.add(taskToAdd);
        } else {
            taskToAdd = new Task(taskTitle, taskTime);
            taskToAdd.setActive(taskIsActive);
            taskList.add(taskToAdd);
        }
        System.out.println("Your task was successfully added!");
        waitForEnterButton();
        log.info("New task was added to list successfully.");
        taskListMain();
    }

    /**
     * Method to get state for boolean variables from user
     *
     * @param str String, to be used in displayed menu
     * @return true,  if input was 'y'
     *         false, if input tas 'n'
     */
    private boolean getStateOrStepOutTo(String str) {
        System.out.print("\nPlease enter 'y' if your task is " + str + ", enter 'n' otherwise\n");
        String inputChoice;
        do {
            inputChoice = getTrimmedInput();
            switch (inputChoice) {
                case "y":
                    return true;
                case "n":
                    return false;
                default:
                    boolean routed = routeIfControlWord(inputChoice, Menus.CHANGE_TASK_STATE, Menus.VOID, str);
                    if(!routed)
                    System.out.print("Wrong input, please retry.");
            }
        } while (true);
    }

    /**
     * Method to get repeat interval for the task in seconds.
     * User should eventually enter valid interval value or step out to previous menu.
     *
     *
     * @param stepOutTo      the menu, we can step out to from current menu, using {@link #routeIfControlWord(String, Menus, Menus, String, int...)}
     * @param message        String value, that can be used in the menu messages, we are using the method in
     * @param indexIfEditing in case we use this method in edit menu, we need to provide index of task to edit, when we step out
     *
     * @return parsed correct int value as repeat interval
     */
    private int getRepeatIntervalOrStepOutTo(Menus stepOutTo, String message, int...indexIfEditing) {
        System.out.print("\nPlease enter "+ message +" repeat interval for your task in SECONDS\n");
        String inputChoice;
        int interval;
        do {
            inputChoice = getTrimmedInput();
            boolean routed = routeIfControlWord(inputChoice, Menus.GET_REPEAT_INTERVAL, stepOutTo, message, indexIfEditing);
            if(!routed) {
                try {
                    interval = Integer.parseInt(inputChoice);
                } catch (NumberFormatException ex) {
                    log.info("Invalid repeat interval format in user's input. ", ex);
                    System.out.println("You've entered repeat interval in wrong format, please retry.");
                    continue;
                }
                return interval;
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
        String inputChoice;
        boolean routed = false;
        int indexToEditTask = -1;
        do {
            inputChoice = getTrimmedInput();
            try {
                indexToEditTask = checkForValidEditIndex(inputChoice);
            } catch (NumberFormatException|IndexOutOfBoundsException ex) {
                log.info("Invalid edit indexes in user's input " + ex);
                routed = routeIfControlWord(inputChoice, Menus.EDIT_TASK_LIST, Menus.VOID, "");
                if(!routed) {
                    System.out.println(ex.getMessage());
                    continue;
                }
            }
            if(!routed)
            editTaskByIndex(indexToEditTask - 1);
            routed = false;
        } while(true);
    }

    /**
     * Menu, that will be displayed, when user wants to edit tasks in collection
     *
     * Tasks from collection will be displayed in following format:
     *
     * #1   Task description
     * #2   Task description
     * ...  ...
     *
     * Such output is formed in {@link #menuItemsOutOfCollection(TaskList)} method.
     *
     * Edit logic relies on the fact that actual index of task in collection is (#i - 1),
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
     *         if it isn't out of collection bounds and
     *         was parsed to int correctly.
     *
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

        if(index < 1 || index > taskList.size() ) {
          throw new IndexOutOfBoundsException("\nYour task number is out of bounds [" + input + "]. Please, retry your input.");
        } else {
            return index;
        }
    }

    /**
     * Method to determine what type of task will be edited(repeated or non-repeated) using user inputted index.
     *
     * Depending on type of the task {@link #editRepeatedTask(Task, int)} or {@link #editNonRepeatedTask(Task, int)}
     * will be used to edit the task.
     *
     * @param index actual index of the task from collection, that will be edited
     */
    private void editTaskByIndex(int index) {
        System.out.println("\n--- You are editing --- \n" + taskList.getTask(index));
        Task editedTask = taskList.getTask(index);
        boolean taskIsRepeated = editedTask.isRepeated();
        if(taskIsRepeated) {
            editRepeatedTask(editedTask, index);
        } else {
            editNonRepeatedTask(editedTask, index);
        }
    }

    /**
     * Method to edit repeated task.
     * Options for editing repeated task are provided by {@link #editOptionsForRepeatedTask(Task)}
     *
     * After task was edited, user is returned again on the menu with options to edit same task,
     * as there are several edit options to choose from.
     *
     * @param editedTask the actual task that will be edited
     * @param index index of the task that will be edited,
     *              used also while stepping out from this method to previous menu {@link #editTaskByIndex(int)}
     */
    private void editRepeatedTask(Task editedTask, int index) {
        editOptionsForRepeatedTask(editedTask);
        String inputChoice;
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
                    editTaskByIndex(index);
                    break;
                case "2": //Edit time
                    synchronized (this) {
                        editStartAndEndTimes(editedTask, index);
                        setListMutated(true);
                    }
                    log.info("Task times was edited successfully.");
                    editTaskByIndex(index);
                    break;
                case "3": //Change repeat interval
                    int newRepeatInterval = getRepeatIntervalOrStepOutTo(Menus.EDIT_TASK_BY_INDEX, "new", index);
                    synchronized (this) {
                        editedTask.setRepeatInterval(newRepeatInterval);
                        setListMutated(true);
                    }
                    log.info("Task repeat interval was edited successfully.");
                    System.out.println("Repeat interval was edited successfully!");
                    editTaskByIndex(index);
                    break;
                case "4": //Change active state
                    synchronized (this) {
                        editChangeActiveState(editedTask);
                        setListMutated(true);
                    }
                    log.info("Task state was edited successfully.");
                    editTaskByIndex(index);
                    break;
                default:
                    boolean routed = routeIfControlWord(inputChoice, Menus.EDIT_REPEATED_TASK, Menus.VOID, "", index);
                    if(!routed)
                    System.out.println("Incorrect input, please retry.");
            }
        } while(true);
    }

    /**
     * Edit options when the edited task is repeated.
     * Option to make task active/inactive is computed using current task state.
     *
     * @param editedTask actual task that will be edited
     */
    private void editOptionsForRepeatedTask(Task editedTask) {
        System.out.println("\n - Choose what do you want to edit in your task.\n");
        String editTitle = "Edit the title.";
        String editStartTime = "Edit times for the task.";
        String editRepeatInterval = "Edit repeat interval for the task.";
        String isActive;
        if(editedTask.isActive()) {
            isActive = "Make your task inactive.";
        } else {
            isActive = "Make your task active.";
        }

        menuUtil(editTitle, editStartTime, editRepeatInterval, isActive);
    }

    /**
     * Method to edit start and end time while editing repeated task.
     * Entered dates are validated for start date to be BEFORE end date.
     * In case of wrong input user will have to retry input of both dates.
     *
     * User can step out to previous menu {@link #editTaskByIndex(int)}
     *
     * @param editedTask the actual task that will be edited
     * @param index index of the task that will be edited,
     *              used also while stepping out from this method to previous menu {@link #editTaskByIndex(int)}
     */
    private void editStartAndEndTimes(Task editedTask, int index) {
        Date newTaskStart;
        Date newTaskEnd;
        do {
            newTaskStart = getDateOrStepOutTo(Menus.EDIT_TASK_BY_INDEX, "START date for your task",  index);
            newTaskEnd = getDateOrStepOutTo(Menus.EDIT_TASK_BY_INDEX, "END date for your task", index);
            if(newTaskStart.after(newTaskEnd)) {
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
     * Options for editing repeated task are provided by {@link #editOptionsForNonRepeatedTask(Task)}
     *
     * After task was edited, user is returned again on the menu with options to edit same task,
     * as there are several edit options to choose from.
     *
     * @param editedTask the actual task that will be edited
     * @param index index of the task that will be edited,
     *              used also while stepping out from this method to previous menu {@link #editTaskByIndex(int)}
     */
    private void editNonRepeatedTask(Task editedTask, int index) {
        editOptionsForNonRepeatedTask(editedTask);
        String inputChoice;
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
                    editTaskByIndex(index);
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
                    editTaskByIndex(index);
                    break;
                case "3": //Change active state
                    synchronized (this) {
                        editChangeActiveState(editedTask);
                        setListMutated(true);
                    }
                    log.info("Task state was edited successfully.");
                    editTaskByIndex(index);
                    break;
                default:
                    boolean routed = routeIfControlWord(inputChoice, Menus.EDIT_NON_REPEATED_TASK, Menus.VOID, "");
                    if(!routed)
                    System.out.println("Incorrect input, please retry.");
                }
        } while(true);
    }

    /**
    * Edit options when the edited task is non-repeated.
    * Option to make task active/inactive is computed using current task state.
    *
    * @param editedTask actual task that will be edited,
     *                  used to compute menu item about it's current active/inactive state
    */
    private void editOptionsForNonRepeatedTask(Task editedTask) {
        System.out.println("\n - Choose what do you want to edit in your task.\n");
        String editTitle = "Edit the title.";
        String editStartTime = "Edit scheduled time for the task.";
        String isActive;
        if(editedTask.isActive()) {
            isActive = "Make your task inactive.";
        } else {
            isActive = "Make your task active.";
        }

        menuUtil(editTitle, editStartTime, isActive);
    }

    /**
     * Method to make {@code task} active/inactive
     * by using {@link Task#setActive(boolean)} method
     *
     * @param task task, which state is edited
     */
    private void editChangeActiveState(Task task) {
        if(task.isActive()) {
            System.out.println("Task is now INACTIVE");
        } else {
            System.out.println("Task is now ACTIVE");
        }
        task.setActive(!task.isActive());
    }

    /**
     * Method to route user from one menu to another.
     *
     * There are 3 types of predefined statements that can be inputted in most menus:
     *
     * 1. 'menu'        - displays current menu.
     * 2. 'prev'/'back' - routes user to the previous menu.
     * 3. 'exit'/quit'  - exits the application using {@link #exit()} method.
     *
     *
     * @param controlWord   one of predefined statements.
     * @param currentMenu   current menu, user is working in.
     * @param routedToMenu  menu, which will be displayed in case of 'back'/'prev' input.
     * @param message       String that could be used for current menu messages.
     * @param index         used in case we are in edit menu.
     *
     * @return              true, is {@code controlWord} was one of the predefined statements
     *                            and method routed user to any of the menus.
     *                      false, if {@code controlWord} was not a predefined statement.
     */
    private boolean routeIfControlWord(String controlWord, Menus currentMenu, Menus routedToMenu, String message, int...index) {
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
                        System.out.print("Please enter " + message + " in following format 'YYYY-mm-dd HH:mm:ss', you may not enter part after days");
                        return true;

                    case GET_TITLE:
                        System.out.print("\nPlease enter title for your "+ message +" task\n");
                        return true;

                    case CHANGE_TASK_STATE:
                        System.out.print("\nPlease enter 'y' if your task is repeated, enter 'n' otherwise\n");
                        return true;

                    case GET_REPEAT_INTERVAL:
                        System.out.print("\nPlease enter "+ message +" repeat interval for your task in SECONDS\n");
                        return true;

                    case EDIT_NOTIFICATIONS:
                        notificationsMenu();
                        return true;

                    case EXIT:
                        System.out.print("Enter 'y' if you want to save changes made in current session, enter 'n' otherwise");
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
                    case EDIT_NOTIFICATIONS:
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
                        } else if (routedToMenu == Menus.EDIT_TASK_BY_INDEX){
                            editTaskByIndex(index[0]);
                            return true;
                        }
                        return false;

                    case EXIT:
                        switch (routedToMenu) {
                            case CHOOSE_TASKLIST:
                                chooseTaskList();
                                return true;

                            case REMOVE_TASKS:
                                removeTasks();
                                return true;

                            case EDIT_TASK_LIST:
                                editTaskList();
                                return true;

                            case EDIT_REPEATED_TASK:
                            case EDIT_NON_REPEATED_TASK:
                                editTaskByIndex(index[0]);
                            return true;

                            case EDIT_NOTIFICATIONS:
                                editNotifications();
                                return true;

                            default:
                                taskListMain();
                        }
                        return true;
                }
                break;
            case "exit":
            case "quit":
                switch (currentMenu) {
                    case EXIT:
                        return false;
                    default:
                        exitMenu(currentMenu, message, index);
                }
                return false;
        }
        return  false;
    }

    /**
     * Menu, where user can turn on/off the notifications,
     * is formed by {@link #menuUtil(String...)} method.
     *
     * If notifications are currently off, user can only turn them on and vice versa.
     *
     */
    private void notificationsMenu() {
        System.out.println("You can change the state of notifications here.");
        String stateOn = "\nNotifications are currently ON\n";
        String menuItemIfOn = "Turn notifications OFF";

        String stateOff = "\nNotifications are currently OFF\n";
        String menuItemIfOff = "Turn notifications ON";
        if(notificationsAreEnabled) {
            System.out.println(stateOn);
            menuUtil(menuItemIfOn);
        } else {
            System.out.println(stateOff);
            menuUtil(menuItemIfOff);
        }

    }

    /**
     * The actual editing of notifications
     * It's state is flipped every time, when user presses '1'
     * User can use predefined statements, if he types in one of them,
     * {@link #routeIfControlWord(String, Menus, Menus, String, int...)} will route him to desired menu.
     */
    private void editNotifications() {
        notificationsMenu();
        String inputChoice;
        do {
            inputChoice = getTrimmedInput();
            switch (inputChoice) {
                case "1":
                    notificationsAreEnabled = !notificationsAreEnabled;
                    pokeNotificationsManager(notificationsAreEnabled);
                    notificationsMenu();
                    if(notificationsAreEnabled) {
                        log.info("Notifications state was flipped. Notifications are now enabled. " + notificationsAreEnabled);
                    } else {
                        log.info("Notifications state was flipped. Notifications are now disabled. " + notificationsAreEnabled);
                    }
                    break;
                default:
                    boolean routed = routeIfControlWord(inputChoice, Menus.EDIT_NOTIFICATIONS, Menus.VOID, "");
                    if(!routed)
                        System.out.print("Incorrect input, please retry.");
            }
        } while (true);
    }

    /**
     * Every time user enters an app, last state of notification is loaded.
     * In case file is missing, or there was an error, while reading it,
     * return value will be false.
     *
     * @return true,
     *         if file exists and there were no errors during reading.
     *
     *         false,
     *         if file is missing or there was an error, while reading from file.
     */
    private boolean loadNotificationsState() {
        boolean state;
        File oldTasks = new File(SAVED_NOTIFICATIONS_STATE_FILE_NAME);
        try (DataInputStream dos = new DataInputStream(new BufferedInputStream(new FileInputStream(oldTasks)))) {
            state = dos.readBoolean();
        } catch (FileNotFoundException ex) {
            log.warn("Cannot find file for loading notifications state. " + ex);
            state = false;
        } catch (IOException ex) {
            log.warn("IOException happened while loading notifications state. " + ex);
            state = false;
        }
        if(state) log.info("Notifications state was loaded successfully.");
        return state;
    }

    /**
     * Method, used to save notifications state.
     *
     * During next launch of the app notification {@link #loadNotificationsState()}
     * is going to read saved state, if there was error in this method while writing to the file
     * default value of false will be returned by {@link #loadNotificationsState()}.
     *
     * @param state
     *        state of notifications to save.
     */
    private void saveNotificationsState(boolean state) {
        File notificationsState = new File(SAVED_NOTIFICATIONS_STATE_FILE_NAME);
        makeEmptyFileByName(SAVED_NOTIFICATIONS_STATE_FILE_NAME);
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(notificationsState, false)))) {
            dos.writeBoolean(notificationsAreEnabled);
        } catch (FileNotFoundException ex) {
            log.warn("Cannot find file for saving notifications state. " + ex);
        } catch (IOException ex) {
            log.warn("IOException happened while saving notifications state. " + ex);
        }
        log.info("Notifications state was saved successfully.");
    }

    /**
     * Method to start new thread for notifications.
     *
     * If method is called with false argument,
     * and there is running notification thread {@code notifier},
     * that thread will be interrupted.
     *
     * Calling method with true argument will relaunch notification thread.
     *
     * @param state
     *        true, if notifications should be enabled
     *        false, if they should be disabled
     *
     * @see NotificationsManager
     */
    private void pokeNotificationsManager(boolean state) {
        log.info("Notification manager was poked with: " + state + " state.");
        try {
            notifier.interrupt();
            notifier.join();
        } catch (InterruptedException ignored) {
        }
        if(state) {
            notifier = new NotificationsManager();
            notifier.setParentController(this);
            notifier.setDaemon(true);
            notifier.start();
        }
    }

    /**
     * Method for showing exit menu, user can type:
     * - 'menu' to see current menu,
     * - 'back'/'prev' o step out back to previous menu.
     *
     * {@link #routeIfControlWord(String, Menus, Menus, String, int...)} is used to route to correct menu.
     *
     * If user wants to save current session changes by typing 'y' in console,
     * list with tasks will be saved to {@code DEFAULT_STORAGE_FILE_NAME} file and then {@link #exit()} will be called
     *
     * Typing 'n' will exit the app without any saving of current list of task.
     *
     * @param routedToMenu menu to route back if 'back' was typed
     * @param message      needed for menu messages, where uer can possibly step out
     * @param index        needed for menu, where uer can possibly step out
     */
    private void exitMenu(Menus routedToMenu, String message, int...index) {
        System.out.println("\nEnter 'y' if you want to save changes made in current session, enter 'n' otherwise");
        String inputChoice;
        do {
            inputChoice = getTrimmedInput();
            switch (inputChoice) {
                case "y":
                    try {
                        File oldTasks = new File(DEFAULT_STORAGE_FILE_NAME);
                        TaskIO.writeText(taskList, oldTasks);
                    } catch (IOException ex) {
                        log.error("Exception happened while writing to default storage file while saving upon exiting the application. ", ex);
                    }
                    log.info("List of tasks was saved before the exit.");
                    saveNotificationsState(notificationsAreEnabled);
                    System.out.println("Saving...");
                    exit();
                    break;
                case "n":
                    log.info("List of tasks was not saved before the exit.");
                    exit();
                default:
                    boolean routed = routeIfControlWord(inputChoice, Menus.EXIT, routedToMenu, message, index);
                    //above method will resolve predefined words and will route the flow of the program
                    if(!routed)
                        System.out.print("Incorrect input, please retry.");
            }
        } while (true);
    }

    /**
     * Method to exit the application.
     */
    private void exit() {
        log.info("Exiting the app.");
        System.out.println("Exiting...");
        System.exit(0);
    }
}

