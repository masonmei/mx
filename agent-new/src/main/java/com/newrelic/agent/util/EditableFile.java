package com.newrelic.agent.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EditableFile {
    static final String lineSep = System.getProperty("line.separator");
    public final String comment;
    String filePath;
    String fileAsString;

    public EditableFile(String filestr) throws NullPointerException, FileNotFoundException, IOException {
        if ((filestr == null) || (filestr.equals(""))) {
            throw new NullPointerException("A null or empty string can't become an EditableFile.");
        }

        filePath = filestr;
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("File " + filePath
                                                    + " does not exist, so it can't become an EditableFile.");
        }

        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader in = new BufferedReader(new FileReader(filePath));
            String str;
            while ((str = in.readLine()) != null) {
                sb.append(str + lineSep);
            }
            in.close();
        } catch (FileNotFoundException fnfe) {
            fnfe.getMessage();
            fnfe.printStackTrace();
            throw new FileNotFoundException("Couldn't create EditableFile due to FileNotFoundException");
        } catch (IOException e) {
            e.getMessage();
            e.printStackTrace();
            throw new IOException("Couldn't create EditableFile due to IOException");
        }

        fileAsString = sb.toString();

        if (filePath.endsWith(".bat")) {
            comment = "::";
        } else if (filePath.endsWith(".java")) {
            comment = "//";
        } else {
            comment = "#";
        }
    }

    public String getContents() {
        return fileAsString;
    }

    public String getLocation() {
        return filePath;
    }

    public boolean contains(String regex) {
        if (fileAsString != null) {
            Pattern p = Pattern.compile(regex, 8);
            Matcher m = p.matcher(fileAsString);
            if (m.find()) {
                return true;
            }
        }
        return false;
    }

    public String replaceFirst(String regex, String replacement) {
        return replaceFirst(regex, replacement, true);
    }

    public String replaceFirst(String regex, String replacement, boolean isMultiLine) {
        Pattern p;
        if (isMultiLine) {
            p = Pattern.compile(regex, 8);
        } else {
            p = Pattern.compile(regex);
        }
        Matcher m = p.matcher(fileAsString);
        fileAsString = m.replaceFirst(replacement);
        write();
        return fileAsString;
    }

    public String replaceAll(String regex, String replacement) {
        return replaceAll(regex, replacement, true);
    }

    public String replaceAll(String regex, String replacement, boolean isMultiLine) {
        Pattern p;
        if (isMultiLine) {
            p = Pattern.compile(regex, 8);
        } else {
            p = Pattern.compile(regex);
        }
        Matcher m = p.matcher(fileAsString);
        fileAsString = m.replaceAll(replacement);
        write();
        return fileAsString;
    }

    public String insertBeforeLocator(String regex, String textToInsert, boolean isMultiLine) {
        fileAsString = replaceFirst("(" + regex + ")", textToInsert + lineSep + "$1", isMultiLine);
        write();
        return fileAsString;
    }

    public String insertAfterLocator(String regex, String textToInsert, boolean isMultiLine) {
        fileAsString = replaceFirst("(" + regex + ")", "$1" + lineSep + textToInsert, isMultiLine);
        write();
        return fileAsString;
    }

    public void commentOutFirstLineMatching(String regex) {
        replaceFirst("(" + regex + ")", comment + "$1");
    }

    public void commentOutAllLinesMatching(String regex) {
        replaceAll("(" + regex + ")", comment + "$1");
    }

    public void append(String text) {
        fileAsString = (fileAsString + lineSep + text);
        write();
    }

    public String backup() {
        DateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String filename = filePath + "." + df.format(new Date());
        if (write(filename)) {
            return filename;
        }
        return "";
    }

    private boolean write() {
        return write(filePath);
    }

    private boolean write(String pathToFile) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(pathToFile));
            out.write(fileAsString);
            out.close();
            return true;
        } catch (IOException e) {
            System.out.println("Problem writing file to disk");
            e.getMessage();
            e.printStackTrace();
        }
        return false;
    }
}