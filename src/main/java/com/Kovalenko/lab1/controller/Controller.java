package com.Kovalenko.lab1.controller;

import com.Kovalenko.lab1.model.*;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Controller class of TaskManager
 *
 * @author Anton Kovalenko
 * @version 1.0
 * @since 19.12.2018
 *
 * @see Task
 * @see TaskList
 * @see TaskIO
 */
public class Controller {

    private ArrayTaskList taskList;

    /**
     *  Main method to start an application,
     *  it welcomes the user by calling {@link #welcomeMessage()} method
     *  and continues execution by passing the control
     *  to {@link #chooseTaskListMenu()} and {@link #chooseTaskList()} metods
     *
     */
    public void run() {
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
        // System.out.println("=!= You can always type: 'quit' or 'exit' to exit with saving changes =!=\n" +
        //                   "=!=                      'menu' to view you current menu              =!=");
    }

    /**
     * Method for printing the menu, by using which user can choose,
     * from where he wants all the tasks to be loaded from
     *
     * Menu is printed by using {@link #menuUtil(String...)} method
     */
    private void chooseTaskListMenu()  {
        System.out.println("\n - Please choose a further action by typing it's number in following menu \u2193 \n");
        String menuItemCreateEmptyTaskList = "Create new empty list of tasks.";
        String menuItemLoadTaskListFromFile = "Load list of tasks from existing file.";
        String menuItemUseLastSavedTaskList = "Continue with last saved list of tasks.";

        menuUtil(menuItemCreateEmptyTaskList, menuItemLoadTaskListFromFile, menuItemUseLastSavedTaskList);
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
    private String getInput() {
        System.out.print("\n>>> Your input: ");
        String input = "";
        BufferedReader buff  = new BufferedReader(new InputStreamReader(System.in));
        try {
            input = buff.readLine();
        } catch (IOException e) {
            System.out.print("Cannot read input because of internal error, try retrying the input.");
        }
        if(input == null) {
            System.out.println("Null was returned when reading user's input");
        }
         return input.trim();
    }

//    private void fileTypeMenu() {
//        System.out.println("\n> What type of file is your desired one?");
//        String fileInnBinaryFormat = "Binary file.";
//        String fileInStringFormat = "Text file.";
//        menu(fileInnBinaryFormat, fileInStringFormat);
//    }
//
//   private String selectFileTypeOrGoBack() {
//        String inputtedFileType;
//        fileTypeMenu();
//        boolean correctInput = true;
//        do {
//            inputtedFileType = getInput();
//            switch (inputtedFileType) {
//                case "menu":
//                    fileTypeMenu();
//                    break;
//                case "prev":
//                    chooseTaskListMenu();
//                    chooseTaskList();
//                    break;
//                case "exit":
//                    exit();
//                case "quit":
//                    exit();
//                default:
//                    System.out.print("Incorrect input, please retry.");
//            }
//        } while (correctInput);
//        return "binary";
//    }

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
     *                           'exit', 'quit' - will exit the application
     *
     *                           'menu' - will display current menu and will wait for user input
     *
     * In case of wrong input, user will be asked to retry.
     */
    private void chooseTaskList() {
        chooseTaskListMenu();
        String inputChoice;
        boolean correctInput;
        do {
            inputChoice = getInput();
            switch (inputChoice) {
                case "1":
                    taskList = new ArrayTaskList();
                    System.out.println("New empty list was created.");
                    taskListMain();
                    break;
                case "2":
                    loadFromUserSpecifiedFile();
                    taskListMain();
                case "3":
                    loadFromLastSavedFile();
                    taskListMain();
                    break;
                case "menu":
                    chooseTaskListMenu();
                    break;
                case "exit":
                    System.out.println("Saving changes, exiting...");
                    exit();
                    break;
                case "quit":
                    System.out.println("Saving changes, quiting...");
                    exit();
                    break;
                default:
                    System.out.print("Incorrect input, please retry.");
            }
            correctInput = true;
        } while (correctInput);
    }

    /**
     * - Method for adding tasks to {@code taskList}
     * by loading collection of Tasks from
     * default storage text file with {@code defaultFileName} name
     *
     * - In case of last saved storage file is missing,
     * FileNotFoundException will be thrown by {@link com.Kovalenko.lab1.model.TaskIO#readText(TaskList, File)} method
     * So instead new empty default storage file will be created {@link #createEmptyFileByName(String)},
     * and {@code taskList} will be empty too.
     *
     * - In case of last saved storage file is corrupted,
     * i.e. format of any Task inside the file is not one of such templates:
     *
     *    "Task title" at [2014-06-28 18:00:13.000];
     *    "Very ""Good"" title" at [2013-05-10 20:31:20.001] inactive; (quotes in titles are doubled)
     *    "Other task" from [2010-06-01 08:00:00.000] to [2010-09-01 00:00:00.000] every [1 day].
     *
     * ParseException will be thrown by {@link com.Kovalenko.lab1.model.TaskIO#readText(TaskList, File)} method
     * So instead new empty default file will be created, and {@code taskList} will be empty too.
     *
     * - In case of any IOException while reading from default storage file,
     * there will be empty {@code taskList} created
     *
     * @see TaskIO
     */
    private void loadFromLastSavedFile() {
        final String defaultStorageFileName = "myTasks.txt";
        File defaultStorageFile = new File(defaultStorageFileName);
        boolean errorHappened = false;
        taskList = new ArrayTaskList();
        try {
            TaskIO.readText(taskList, defaultStorageFile);
        } catch (FileNotFoundException ex) {
            errorHappened = true;
            try {
                if (defaultStorageFile.createNewFile()) {
                    createEmptyFileByName(defaultStorageFileName);
                    System.out.println("Seems like the last saved file is missing, "
                                           + "so new empty file was created and list of tasks is now empty");
                }
            } catch (IOException ignored) {

            }
        } catch (ParseException ex) {
            errorHappened = true;
            taskList = new ArrayTaskList();
            createEmptyFileByName(defaultStorageFileName);
            System.out.println("Seems like the content of last saved file is corrupted, "
                                + "so new empty file was created and list of tasks is now empty");
        } catch (IOException ex) {
            errorHappened = true;
            System.out.println("Error happened while reading from file, list of tasks is now empty");
            taskList = new ArrayTaskList();
        }
        if(!errorHappened) {
            System.out.println("File was loaded successfully! ");
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
    private void createEmptyFileByName(String name) {
        File defaultFile = new File(name);
        try {
            PrintWriter writer = new PrintWriter(defaultFile);
            writer.print("");
            writer.close();
        } catch (IOException ignored) {
        }
    }

    /**
     * - Method for adding tasks to {@code taskList}
     * by loading collection of Tasks from
     * user specified {@code inputtedFilePath} text file .
     *
     * - In case of user specified file is missing,
     * FileNotFoundException will be thrown by {@link com.Kovalenko.lab1.model.TaskIO#readText(TaskList, File)} method.
     * So user will be redirected to previous menu {@link #chooseTaskListMenu()}.
     *
     * - In case of user specified file is corrupted,
     * i.e. format of any Task inside the file is not one of such templates:
     *
     *    "Task title" at [2014-06-28 18:00:13.000];
     *    "Very ""Good"" title" at [2013-05-10 20:31:20.001] inactive; (quotes in titles are doubled)
     *    "Other task" from [2010-06-01 08:00:00.000] to [2010-09-01 00:00:00.000] every [1 day].
     *
     * ParseException will be thrown by {@link com.Kovalenko.lab1.model.TaskIO#readText(TaskList, File)} method.
     * So user will be redirected to previous menu {@link #chooseTaskListMenu()}.
     *
     * - In case of any IOException while reading from user specified file,
     * user will be redirected to previous menu {@link #chooseTaskListMenu()}.
     *
     * @see TaskIO
     */
    private void loadFromUserSpecifiedFile() {
        String inputtedFilePath;
        boolean errorHappened = false;
        System.out.println("\n - Please enter path to the file with tasks.");
        inputtedFilePath = getInput();
        try {
            taskList = new ArrayTaskList();
            TaskIO.readText(taskList, new File(inputtedFilePath));
        } catch (FileNotFoundException ex) {
            System.out.println("\nSorry, but the specified file was not found. Returning you to previous menu.");
            errorHappened = true;
        } catch (ParseException ex) {
            System.out.println("\nSorry, but the specified file contains incorrect tasks format inside. Returning you to previous menu.");
            errorHappened = true;
        } catch (IOException ex) {
            System.out.println("\nSorry, the specified file can't be read properly. Returning you to previous menu.");
            errorHappened = true;
        } finally {
            if(errorHappened){
                chooseTaskListMenu();
                chooseTaskList();
            }
        }
        System.out.println("File was loaded successfully.");
    }

    /**
     * Main menu, printed using {@link #menuUtil(String...)}
     *
     * Here user can choose what to do next with collection
     *
     */
    private void taskListMainMenu() {
        System.out.println("\n---------- Main menu -----------");
        System.out.println("\n - Please choose what do you want to do next \u2193 \n");
        String menuItemViewTasks = "View all your tasks.";
        String menuItemAddTask = "Add a new task to list.";
        String menuItemRemoveTask = "Remove tasks from list.";
        String menuItemEditTask = "Edit task in list.";
        String menuItemViewCalendar = "View calendar.";
        String menuItemEditNotifications = "Edit notifications.";

        menuUtil(menuItemViewTasks, menuItemAddTask, menuItemRemoveTask, menuItemEditTask, menuItemViewCalendar, menuItemEditNotifications);
        //getCollection();
        //exit();
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
     *                           'back', 'prev' - will return to previous menu
     *
     *                           'exit', 'quit' - will exit the application
     *
     *                           'menu' - will display current menu and will wait for user input
     *
     * In case of wrong input, user will be asked to retry.
     */
    private void taskListMain() {
        taskListMainMenu();
        String inputChoice;
        boolean correctInput;
        do {
            inputChoice = getInput();
            switch (inputChoice) {
                case "1":
                    //View all tasks
                    viewTasks();
                    taskListMainMenu();
                    break;
                case "2":
                    //Add new task
                    System.out.println("Add new task");
                    break;
                case "3":
                    //Remove task from list
                    removeTasks();
                    break;
                case "4":
                    //Edit task in list
                    System.out.println("Edit task in list");
                    break;
                case "5":
                    //View calendar
                    calendar();
                    break;
                case "6":
                    //Edit notifications
                    System.out.println("Edit notifications");
                    break;
                case "menu":
                    taskListMainMenu();
                    break;
                case "prev":
                case "back":
                    chooseTaskList();
                    break;
                case "exit":
                    System.out.println("Saving changes, exiting...");
                    exit();
                    break;
                case "quit":
                    System.out.println("Saving changes, quiting...");
                    exit();
                    break;
                default:
                    System.out.print("Incorrect input, please retry.");
            }
            correctInput = true;
        } while (correctInput);
    }

    /**
     * Method for viewing current list of tasks
     *
     * If it is empty, there will be a message about it,
     * if not, list will be displayed on the screen.
     *
     * After viewing user should press ENTER button,
     * this will redirect him to previous menu {@link #taskListMain()}
     */
    private void viewTasks() {
        if(checkIfEmptyCollection()) {
            System.out.println("Your list of tasks is empty at the moment. Nothing to view.");
        } else {
            System.out.println("List of all tasks is displayed below.\n");
            viewCollection();
        }
        System.out.println("\nHit ENTER to go to previous menu.");
        waitForEnterButton();
    }

    /**
     * Util method, waits for ENTER press, then returns
     */
    private void waitForEnterButton() {
        Scanner scan = new Scanner(System.in);
        scan.nextLine();
    }

    /**
     * Method to check if collection is empty( i.e. has no task inside)
     * {@link TaskList#size()}
     *
     * @return true,  if collection is empty
     *         false, if there are any tasks in collection
     * @see TaskList
     */
    private boolean checkIfEmptyCollection() {
        return taskList.size() == 0;
    }

    /**
     * Method to show remove menu to user,
     * it will display all tasks description if list of tasks is not empty,
     * otherwise user will be notified about it and proposed to hit ENTER key,
     * which will return him to previous menu {@link #taskListMain()}.
     *
     * There is an option to delete several tasks at once if user enters task's
     * menu indexes, separated by spaces. But still, this input will be validated.
     *
     */
    private void removeTasks() {
        if(checkIfEmptyCollection()) {
            System.out.println("Your list of tasks is empty at the moment. Nothing to remove.");
            System.out.println("\nHit ENTER to go to previous menu.");
            waitForEnterButton();
            taskListMain();
        } else {
            int[] indexesToRemoveTasksFrom = null;
            removeTasksMenu();
            String inputChoice;
            boolean correctInput = false;
            do {
                inputChoice = getInput();
                try {
                    indexesToRemoveTasksFrom = parseNeededRemoveIndexes(inputChoice);
                    checkForInvalidRemovalIndexes(indexesToRemoveTasksFrom);
                } catch (NumberFormatException ex) {
                    switch (inputChoice) {
                        case "menu":
                            removeTasksMenu();
                            break;
                        case "prev":
                        case "back":
                            taskListMain();
                            break;
                        case "exit":
                            System.out.println("Saving changes, exiting...");
                            exit();
                            break;
                        case "quit":
                            System.out.println("Saving changes, quiting...");
                            exit();
                            break;
                        default:
                            System.out.print("Incorrect input, please retry.");
                            correctInput = true;
                            continue;
                    }
                } catch (IndexOutOfBoundsException ex) {
                    System.out.print(ex.getMessage());
                    indexesToRemoveTasksFrom = null;
                    correctInput = true;
                    continue;
                }

                if(indexesToRemoveTasksFrom != null) {
                    removeTasksByGivenIndexes(indexesToRemoveTasksFrom);
                }
                correctInput = true;
            } while (correctInput);
        }
    }

    /**
     * Menu, that will be displayed, when user wants to delete tasks from collection
     *
     * Tasks from collection will be displayed in following format:
     *
     * #1   Task description
     * #2   Task description
     * ...  ...
     *
     * Such output is formed in {@link #menuOutOfCollection(TaskList)} method.
     *
     * Remove logic relies on the fact that actual index of task in collection is (#i - 1),
     */
    private void removeTasksMenu() {
        String[] collectionItemsAsMenu = menuOutOfCollection(taskList);
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
     * @return String array, each element of which is corresponding task description in collection.
     *
     * @see TaskList
     */
    private String[] menuOutOfCollection(TaskList tasks) {
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
     *              in the format of tasks indexes in menu, separated by spaces
     * @return int[] indexes, by which tasks will be deleted from collection
     * @throws NumberFormatException in case of any parsing error from String input to int,
     *                               using above defined format
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
     *                                   Keep in mind, that tasks in the menu are shown from 1 to i,
     *                                   but we validate indexes shifting them 1 position to the left (i.e. i - 1).
     *                                   User will be notified about wrong indexes by a message, containing all
     *                                   wrong ones.
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
            throw new IndexOutOfBoundsException("There were invalid removal indexes in your input " + Collections.singletonList(invalidIndexes) + ", please retry. ");
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
    private void removeTasksByGivenIndexes(int[] indexes) {
        System.out.println("Are you sure you want to remove tasks " + Arrays.toString(indexes)
                               + "? (This action can't be undone.)\n "
                               + " - To confirm - type 'y', to cancel - type 'n' ");
        String inputChoice;
        boolean correctInput = false;
        do {
            inputChoice = getInput();
            String trimmedInput = inputChoice.trim();
            switch (trimmedInput) {
                case "y":
                    removeByIndexesConfirmed(indexes);
                    System.out.println(" --- Tasks were deleted successfully! --- ");
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
            correctInput = true;
        } while (correctInput);
    }

    /**
     *
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

    private void calendar() {
        Date startDate;
        Date endDate;
        if(checkIfEmptyCollection()) {
            System.out.println("Your list of tasks is empty at the moment. Can't create calendar");
            System.out.println("\nHit ENTER to go to previous menu.");
            waitForEnterButton();
            taskListMain();
        } else {
            boolean correctInput = true;
            do {
                //System.out.println("in enter section");
                startDate = getDateOrGoBack("start date");
                endDate = getDateOrGoBack("end date");
                if(endDate.before(startDate)) {
                    correctInput = false;
                    System.out.println("End date is before start date, please retry your input.");
                }
            } while (!correctInput);
            renderCalendar(startDate, endDate);
        }

    }

    private Date getDateOrGoBack(String name) {
        System.out.print("\nPlease enter " + name + " for calendar in following format 'yyyy-mm-dd'\n");
        String inputChoice;
        Date actualDate = new Date();
        boolean correctInput;
        do {
            correctInput = true;
            inputChoice = getInput();
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                sdf.setLenient(false);
                actualDate = sdf.parse(inputChoice.trim());
            } catch (ParseException ex) {
                correctInput = false;
                switch (inputChoice) {
                    case "menu":
                        System.out.print("Please enter " + name + " for calendar in following format 'YYYY-mm-dd'");
                        break;
                    case "prev":
                    case "back":
                        taskListMain();
                        break;
                    case "exit":
                        System.out.println("Saving changes, exiting...");
                        exit();
                        break;
                    case "quit":
                        System.out.println("Saving changes, quiting...");
                        exit();
                        break;
                    default:
                        System.out.print("You've entered " + name + " in invalid format, please retry.");
                }
            }
            if(correctInput) {
                return actualDate;
            }
        } while (true);
    }

    private void renderCalendar(Date from, Date to) {
        System.out.println("Tasks, contained between start: "
                               + from.toString() + " and end: "
                               + to.toString() + " dates are shown below.\n");
        SortedMap<Date, Set<Task>> calendar = Tasks.calendar(taskList, from, to);
        calendar.forEach((K, V) -> {
            System.out.println(" --- " + K + " --- ");
            for (Task task : V) {
                System.out.println(task);
            }
            System.out.println(" ------------------------------------\n");
        } );
        System.out.println("Hit ENTER to go to previous menu.");
        waitForEnterButton();
        taskListMain();
    }

    private void editNotifications() {}

    private void editTask() {

    }


    private void addTask() {

    }

    private void saveChanges() {

    }

    private void exit() {
        System.exit(0);
    }

    private void viewCollection() {
        for (Task task : taskList) {
            System.out.println(task);
        }
    }
}
