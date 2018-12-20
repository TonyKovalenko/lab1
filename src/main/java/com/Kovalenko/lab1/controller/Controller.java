package com.Kovalenko.lab1.controller;

import com.Kovalenko.lab1.model.ArrayTaskList;
import com.Kovalenko.lab1.model.Task;
import com.Kovalenko.lab1.model.TaskIO;
import com.Kovalenko.lab1.model.TaskList;

import java.io.*;
import java.text.ParseException;
import java.util.Date;

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

    private TaskList taskList;

    /**
     *  Main method to start an application,
     *  it welcomes the user by calling {@link #welcomeMessage()} method
     *  and continues execution by passing the control
     *  to {@link #chooseTaskListMenu()} and {@link #chooseTaskList()} metods
     *
     */
    public void run() {
        welcomeMessage();
        chooseTaskListMenu();
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
        String inputChoice;
        boolean correctInput;
        do {
            inputChoice = getInput();
            switch (inputChoice) {
                case "1":
                    taskList = new ArrayTaskList();
                    System.out.print("New empty list was created.");
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
     * Under construction
     */
    private void taskListMain() {
        System.out.println("\n --- Main menu ---");
        System.out.println(" - Please choose what do you want to do next \u2193 \n");
        //getCollection();
        exit();
    }

    private void showTasks() {

    }

    private void editNotifications() {}

    private void removeTask() {

    }

    private void editTask() {

    }

    private void calendar() {

    }

    private Date selectDates() {
      return null;
    }

    private void addTask() {

    }

    private void saveChanges() {

    }

    private void exit() {
        System.exit(0);
    }

    private void getCollection() {
        for (Task task : taskList) {
            System.out.println(task);
        }
    }
}
