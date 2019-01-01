package com.Kovalenko.lab1.model;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Class for serialization of Task collections,
 * contains method to work with various I/O streams
 * @see Task
 * @see TaskList
 * @see ArrayTaskList
 * @see LinkedTaskList
 *
 * @author Anton Kovalenko
 * @version 1.0
 * @since 12-9-2018
 *
 */
public final class TaskIO {

    private TaskIO() {}

    /**
     * Method to put Task collection {@code tasks} into OutputStream in following format:
     * Number of Tasks -> Title length -> Title -> 0 or 1 whether is active or not ->
     * -> interval of repetition (if is repeated then put time of start and time of ending)
     *                           (if is not repeated then put a time of notification)
     * @see Task
     *
     * @param tasks collection of Task, we want to serialize into OutputStream
     * @param out OutputStream, to serialize the collection in
     */
    public static void write(TaskList tasks, OutputStream out) throws IOException {
        Iterator<Task> iter = tasks.iterator();
        Task currentTask;
        DataOutputStream dos = new DataOutputStream(out);
        try {
            dos.writeInt(tasks.size()); //number of tasks
            while(iter.hasNext()) {
                currentTask = iter.next();

                dos.writeInt(currentTask.getTitle().length()); // title length
                dos.writeChars(currentTask.getTitle());        // title in byte array
                dos.writeInt(currentTask.isActive() ? 1 : 0);  // 0 or 1 if active or not
                dos.writeInt(currentTask.getRepeatInterval()); // write repeat interval
                if(currentTask.isRepeated()) {
                    dos.writeLong(currentTask.getStartTime().getTime()); // task is repeated, so put start time
                    dos.writeLong(currentTask.getEndTime().getTime());   // and end time
                } else {
                    dos.writeLong(currentTask.getTime().getTime());      // task is non repeated, so put the time of notification
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new IOException("IOException happened, while writing to OutputStream from collection");
        }
    }

    /**
     * Method to get Tasks  {@code tasks} from InputStream in following format:
     * Number of Tasks -> Title length -> Title -> 0 or 1 whether is active or not ->
     * -> interval of repetition (if is repeated then put time of start and time of ending)
     *                           (if is not repeated then put a time of notification)
     * then - converting tasks into Task objects and adding them into collection
     * @see Task
     *
     * @param tasks collection of Task, we want to be filled from InputStream
     * @param in InputStream, to fill the collection from
     */
    public static void read(TaskList tasks, InputStream in) throws IOException {
        DataInputStream dis = new DataInputStream(in);
        Task currentTask;
        int titleLength;
        String title;              // title for future Task
        boolean active, repeated;  // flags for future Task
        Date time, start, end;     // dates for future Task
        int repeat;
        char [] bufferForTitle;
        try {
            dis.readInt(); // read task count (ignored, as we use collections that can expand by themselves)
            while(dis.available() > 0) {
                titleLength = dis.readInt(); // title length
                bufferForTitle = new char[titleLength]; // buffer for title

                for (int i = 0; i < titleLength; i++) {
                    bufferForTitle[i] = dis.readChar(); // read title as
                }

                title = new String(bufferForTitle); // convert char array to string
                active = (dis.readInt() != 0); // if task is active - 1 else 0
                repeat = dis.readInt();
                if (repeat == 0) {
                    time = new Date(dis.readLong());
                    currentTask = new Task(title, time);
                    currentTask.setActive(active);
                    tasks.add(currentTask);
                } else {
                    start = new Date(dis.readLong());
                    end = new Date(dis.readLong());
                    currentTask = new Task(title, start, end, repeat);
                    currentTask.setActive(active);
                    tasks.add(currentTask);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new IOException("IOException happened, while reading from InputStream to collection");
        }
    }

    /**
     * Method to put tasks {@code tasks} to file using OutputStream class,
     * with the help of {@link #write(TaskList, OutputStream)} method
     * @see Task
     *
     * @param tasks collection of Task, we want to be put into File
     * @param file File, to put task collection in
     */
    public static void writeBinary(TaskList tasks, File file) throws IOException {
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file))){
            write(tasks, out);
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            throw new FileNotFoundException("Specified file was not found");
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new IOException("IOexception happened while writing into file");
        }
    }

    /**
     * Method to read Tasks {@code tasks} from File
     * with the help of method {@link #read(TaskList, InputStream)}
     * @see Task
     *
     * @param tasks collection of Task, we want to be filled from file
     * @param file File, to fill the collection from
     */
    public static void readBinary(TaskList tasks, File file) throws IOException {
        try (InputStream in = new BufferedInputStream(new FileInputStream(file))){
            read(tasks, in);
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            throw new FileNotFoundException("Specified file was not found");
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new IOException("IOexception happened while writing into file");
        }
    }

    /**
     * Method to put Task collection {@code tasks} into Writer in following possible formats:
     * "Task title" at [2014-06-28 18:00:13.000];
     * "Very ""Good"" title" at [2013-05-10 20:31:20.001] inactive;
     * "Other task" from [2010-06-01 08:00:00.000] to [2010-09-01 00:00:00.000] every [1 day].
     *
     * @see Task
     *
     * @param tasks collection of tasks, we want to serialize into Writer
     * @param out Writer, to serialize the collection in
     */
    public static void write(TaskList tasks, Writer out) {
        Iterator<Task> iter = tasks.iterator();
        Task currentTask;
        //PrintWriter pOut = new PrintWriter(out);
        StringBuilder lineToWrite = new StringBuilder();
        try {
            while (iter.hasNext()) {
                currentTask = iter.next();
                lineToWrite = new StringBuilder();
                lineToWrite.append("\"");
                lineToWrite.append(doubleTheQuotes(currentTask.getTitle()));
                lineToWrite.append("\"");

                if (!currentTask.isRepeated()) {
                    lineToWrite.append(" at ");
                    lineToWrite.append(getStringFromDate(currentTask.getTime()));
                } else {
                    lineToWrite.append(" from ");
                    lineToWrite.append(getStringFromDate(currentTask.getStartTime()));
                    lineToWrite.append(" to ");
                    lineToWrite.append(getStringFromDate(currentTask.getEndTime()));
                    lineToWrite.append(" every ");
                    lineToWrite.append(getStringFromRepeatInterval(currentTask.getRepeatInterval()));
                }

                if (!currentTask.isActive()) {
                    lineToWrite.append(" inactive");
                    if (iter.hasNext()) {
                        lineToWrite.append(";\n");
                    } else {
                        lineToWrite.append(".");
                        //lineToWrite.insert(lineToWrite.lastIndexOf(" "),".");
                    }
                    out.write(lineToWrite.toString());
                    continue;
                }

                if (iter.hasNext()) {
                    lineToWrite.append(";\n");
                } else {
                    lineToWrite.append(".");
                }
                out.write(new String(lineToWrite));
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            //throw new IOException("IOException happened while writing to Writer from collection");
        } finally {
            try {
                out.close();
            } catch (IOException ignored) {

            }
        }

    }

    /**
     * Method to get tasks  {@code tasks} from Reader in following possible formats:
     * "Task title" at [2014-06-28 18:00:13.000];
     * "Very ""Good"" title" at [2013-05-10 20:31:20.001] inactive;
     * "Other task" from [2010-06-01 08:00:00.000] to [2010-09-01 00:00:00.000] every [1 day].
     * then - converting them into Task objects and adding them into collection
     * @see Task
     *
     * @param tasks collection of Task, we want to be filled from Reader
     * @param in Reader, to fill the collection from
     */
    public static void read(TaskList tasks, Reader in) throws IOException, ParseException {
        BufferedReader bufferedReader = new BufferedReader(in);
        String currentLine;
        Task currentTask;
        String title;              // title for future Task
        int lastQuoteIndex;
        String repetition;
        try {
//            while (bufferedReader.read() != -1) {
//                System.out.print((char)bufferedReader.read());
//            }
            currentLine = bufferedReader.readLine();
//            Scanner scan = new Scanner(in)
//            scan.useDelimiter(Pattern.compile(";"));
//            while (scan.hasNext()) {
//                currentLine = scan.next();
//
//            }
            while (currentLine != null) {
                lastQuoteIndex = currentLine.lastIndexOf('"');
                title = currentLine.substring(1, lastQuoteIndex);

                repetition = currentLine.substring(lastQuoteIndex + 1, currentLine.indexOf('['));
                if(repetition.equals(" at ")) {
                    currentTask = parseAsNonRepeated(title, currentLine, lastQuoteIndex);
                } else if (repetition.equals(" from ")) {
                    currentTask = parseAsRepeated(title, currentLine, lastQuoteIndex);
                } else {
                    throw new ParseException("Unknown Task format in reader", lastQuoteIndex + 1);
                }
                tasks.add(currentTask);
                currentLine = bufferedReader.readLine();
            }
            if (tasks instanceof LinkedTaskList) {
                ((LinkedTaskList) tasks).reverse();
            }
        } catch (IOException|ParseException ex) {
            throw ex;
        } finally {
            try {
                bufferedReader.close();
            } catch (IOException ignored) {

            }
        }
    }

    /**
     * Method to read tasks {@code tasks} from file
     * with the help of method {@link #read(TaskList, Reader)}
     * @see Task
     *
     * @param tasks collection of Task, we want to be filled from file
     * @param file File, to fill the collection from
     */
    public static void readText(TaskList tasks, File file) throws IOException, ParseException {
        try (Reader in = new BufferedReader(new FileReader(file))){
            read(tasks, in);
        } catch (IOException ex) {
            throw ex;
        }
    }

    private static Task parseAsNonRepeated(String title, String currentLine, int lastQuoteIndex) throws ParseException {

        int indexOfFirstSquareBracket = currentLine.indexOf("[", lastQuoteIndex);
        String stringDate = currentLine.substring(indexOfFirstSquareBracket + 1, currentLine.lastIndexOf("]"));
        Date actualDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").parse(stringDate);

        Task taskToReturn = new Task(title, actualDate);
        if(currentLine.lastIndexOf("]") + 1 == currentLine.lastIndexOf(";")
               || currentLine.lastIndexOf("]") + 1 == currentLine.lastIndexOf("." )) {
            taskToReturn.setActive(true);
        } else {
            taskToReturn.setActive(false);
        }

        return taskToReturn;
    }

    private static Task parseAsRepeated(String title, String currentLine,int lastQuoteIndex) throws ParseException {

        int indexOfFirstSquareBracket = currentLine.indexOf("[", lastQuoteIndex);
        int indexOfSecondSquareBracket = currentLine.indexOf("]", indexOfFirstSquareBracket);
        String stringData = currentLine.substring(indexOfFirstSquareBracket + 1, indexOfSecondSquareBracket);
        Date startDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").parse(stringData);

        int indexOfThirdSquareBracket = currentLine.indexOf("[", indexOfSecondSquareBracket);
        int indexOfFourthSquareBracket = currentLine.indexOf("]", indexOfThirdSquareBracket);
        stringData = currentLine.substring(indexOfThirdSquareBracket + 1, indexOfFourthSquareBracket);
        Date endDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").parse(stringData);

        int indexOfFifthSquareBracket = currentLine.indexOf("[", indexOfFourthSquareBracket);
        int indexOfSixthSquareBracket = currentLine.indexOf("]", indexOfFifthSquareBracket);
        stringData = currentLine.substring(indexOfFifthSquareBracket + 1, indexOfSixthSquareBracket);
        int repeatInterval = parseRepeatInterval(stringData);

        Task taskToReturn = new Task(title, startDate, endDate, repeatInterval);
        if(currentLine.lastIndexOf("]") + 1 == currentLine.lastIndexOf(";")
               || currentLine.lastIndexOf("]") + 1 == currentLine.lastIndexOf("." )) {
            taskToReturn.setActive(true);
        } else {
            taskToReturn.setActive(false);
        }

        return taskToReturn;
    }

    private static int parseRepeatInterval(String data) throws NumberFormatException {
        int days = 0, hours = 0, minutes = 0, seconds = 0;
        List<String> allMatches = new ArrayList<>();
        Matcher m = Pattern.compile("\\d+\\s\\w").matcher(data);
        while (m.find()) {
            allMatches.add(m.group());
        }

        try {
            for (String s : allMatches) {
                switch (s.substring(s.length() - 1)) {
                    case "d":
                        days = Integer.parseInt(s.substring(0, s.indexOf(" ")));
                        break;
                    case "h":
                        hours = Integer.parseInt(s.substring(0, s.indexOf(" ")));
                        break;
                    case "m":
                        minutes = Integer.parseInt(s.substring(0, s.indexOf(" ")));
                        break;
                    case "s":
                        seconds = Integer.parseInt(s.substring(0, s.indexOf(" ")));
                        break;
                }
            }
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
            throw new NumberFormatException("Integer parse error while parsing repeat interval");
        }

        return ((days * 86400) + (hours * 3600) + (minutes * 60) + seconds);
    }

    /**
     * Method to put tasks {@code tasks} to file using Writer class,
     * with the help of {@link #write(TaskList, Writer)} method
     * @see Task
     *
     * @param tasks collection of tasks, we want to be put into File
     * @param file File, to put tasks in
     */
    public static void writeText(TaskList tasks, File file) throws IOException {
        try (Writer out = new BufferedWriter(new FileWriter(file))){
            write(tasks, out);
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            throw new FileNotFoundException("Specified file was not found");
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new IOException("IOexception happened while writing into file");
        }
    }

    private static String doubleTheQuotes(final String value) {
        if(value.indexOf('"') == -1) {
            return value;
        }
        StringBuilder builderString = new StringBuilder(value);
        int indexOfQuote = builderString.indexOf("\"", 0);
        while( indexOfQuote != -1) {
            builderString.insert(indexOfQuote, "\"");
            indexOfQuote = builderString.indexOf("\"", indexOfQuote + 2);
        }
        return new String(builderString);
    }

    private static String getStringFromDate(final Date date) {
        StringBuilder builderString = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String dateString = sdf.format(date);
        builderString.append("[");
        builderString.append(dateString);
        builderString.append("]");
        return new String(builderString);
    }

    private static String getStringFromRepeatInterval(final int repeatInterval) {
        StringBuilder builderString = new StringBuilder();
        int days = repeatInterval / 86400;
        int hours =  (repeatInterval / 3600) % 24;
        int minutes = (repeatInterval / 60) % 60;
        int seconds = repeatInterval % 60;
        builderString.append("[");


        switch (days) {
            case 0:
                break;
            case 1:
                builderString.append(1);
                builderString.append(" day ");
                break;
            default:
                builderString.append(days);
                builderString.append(" days ");
        }

        switch (hours) {
            case 0:
                break;
            case 1:
                builderString.append(1);
                builderString.append(" hour ");
                break;
            default:
                builderString.append(hours);
                builderString.append(" hours ");
        }

        switch (minutes) {
            case 0:
                break;
            case 1:
                builderString.append(1);
                builderString.append(" minute ");
                break;
            default:
                builderString.append(minutes);
                builderString.append(" minutes ");
        }


        switch (seconds) {
            case 0:
                break;
            case 1:
                builderString.append(1);
                builderString.append(" second ");
                break;
            default:
                builderString.append(seconds);
                builderString.append(" seconds ");
        }

        builderString.insert(builderString.lastIndexOf(" "),"]");
        String s = new String(builderString);
        return s.trim();
    }
}
