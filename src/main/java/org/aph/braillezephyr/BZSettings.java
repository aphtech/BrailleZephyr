/* Copyright (C) 2015 American Printing House for the Blind Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.aph.braillezephyr;

import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * <p>
 * This class handles access to the settings file on the system.  This allows
 * the settings to be restored between executions.
 * </p>
 *
 * @author Mike Gray mgray@aph.org
 */
public final class BZSettings extends BZBase {
    private final File file;

    private final ArrayList<String> recentFiles = new ArrayList<>(31);
    private int recentFilesMax = 31;

    private Point shellSize;
    private boolean shellMaximized;

    /**
     * <p>
     * Creates a new <code>BZSettings</code> object for BZStyledText.
     * </p><p>
     * If <code>fileName</code> is null, then the default file
     * &quot;.braillezephyr.conf&quot; in the user's home directory is tried.
     * </p><p>
     * When <code>useSize</code> is true, then the parent shell of the
     * <code>bzStyledText</code> object's size will be stored and restored.
     * This is used for when the <code>bzStyledText</code> object is or isn't
     * a top level window.
     * </p>
     *
     * @param bzStyledText the bzStyledText object to operate on (cannot be null)
     * @param fileName     the filename of the settings file
     * @param useSize      whether or not to resize the parent of bzStyledText
     */
    public BZSettings(BZStyledText bzStyledText, String fileName, boolean useSize) {
        super(bzStyledText);

        if (fileName == null)
            fileName = System.getProperty("user.home") + File.separator + ".braillezephyr.conf";
        file = new File(fileName);
        readSettings();

        if (useSize) {
            parentShell.addControlListener(new ControlHandler());
            if (shellSize == null)
                shellSize = new Point(640, 480);
            parentShell.setSize(shellSize);
            parentShell.setMaximized(shellMaximized);
        }
    }

    /**
     * <p>
     * Creates a new <code>BZSettings</code> object with the default
     * <code>fileName</code>.
     * </p>
     *
     * @param bzStyledText the bzStyledText to operate on (cannot be null)
     * @param useSize      whether or not to resize the parent of bzStyledText
     * @see #BZSettings(BZStyledText, String, boolean)
     */
    public BZSettings(BZStyledText bzStyledText, boolean useSize) {
        this(bzStyledText, null, useSize);
    }

    /**
     * <p>
     * Creates a new <code>BZSettings</code> object with resizing.
     * </p>
     *
     * @param bzStyledText the bzStyledText to operate on (cannot be null)
     * @param fileName     the filename of the settings file
     * @see #BZSettings(BZStyledText, String, boolean)
     */
    public BZSettings(BZStyledText bzStyledText, String fileName) {
        this(bzStyledText, fileName, true);
    }

    /**
     * <p>
     * Creates a new <code>BZSettings</code> object with the default
     * <code>fileName</code> and resizing.
     * </p>
     *
     * @param bzStyledText the bzStyledText to operate on (cannot be null)
     * @see #BZSettings(BZStyledText, String, boolean)
     */
    public BZSettings(BZStyledText bzStyledText) {
        this(bzStyledText, null);
    }

    ArrayList<String> getRecentFiles() {
        return recentFiles;
    }

    int getRecentFilesMax() {
        return recentFilesMax;
    }

    void removeRecentFile(String fileName) {
        for (String recentFile : recentFiles)
            if (recentFile.equals(fileName)) {
                recentFiles.remove(recentFile);
                return;
            }
    }

    void addRecentFile(String fileName) {
        //   check for duplicates
        removeRecentFile(fileName);

        recentFiles.add(0, fileName);
        if (recentFiles.size() > 6) {
			recentFiles.subList(6, recentFiles.size()).clear();
        }
    }

    private boolean readLine(String line) {
        if (line.isEmpty())
            return true;

        int offset = line.indexOf(' ');
        if (offset < 0)
            return false;

        String value = line.substring(offset + 1);
        if (value.isEmpty())
            return false;

        String[] tokens;
        switch (line.substring(0, offset)) {
            case "size":

                tokens = value.split(" ");
                if (tokens.length != 3)
                    return false;
                shellSize = new Point(Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]));
                shellMaximized = Boolean.parseBoolean(tokens[2]);
                break;

            case "charsPerLine":
                bzStyledText.setCharsPerLine(Integer.parseInt(value));
                break;
            case "lineMarginBell":
                bzStyledText.setLineMarginBell(Integer.parseInt(value));
                break;
            case "lineMarginFileName":

                try {
                    bzStyledText.loadLineMarginFileName(value);
                } catch (FileNotFoundException exception) {
                    logError("Unable to open line margin sound file", exception);
                } catch (IOException exception) {
                    logError("Unable to read line margin sound file", exception);
                } catch (UnsupportedAudioFileException ignore) {
                    logError("Sound file unsupported for line margin bell", value);
                } catch (LineUnavailableException ignore) {
                    logError("Line unavailable for line margin bell", value);
                }
                break;

            case "lineEndFileName":

                try {
                    bzStyledText.loadLineEndFileName(value);
                } catch (FileNotFoundException exception) {
                    logError("Unable to open line end sound file", exception);
                } catch (IOException exception) {
                    logError("Unable to read line end sound file", exception);
                } catch (UnsupportedAudioFileException ignore) {
                    logError("Sound file unsupported for line end bell", value);
                } catch (LineUnavailableException ignore) {
                    logError("Line unavailable for line end bell", value);
                }
                break;

            case "linesPerPage":
                bzStyledText.setLinesPerPage(Integer.parseInt(value));
                break;
            case "pageMarginBell":
                bzStyledText.setPageMarginBell(Integer.parseInt(value));
                break;
            case "pageMarginFileName":

                try {
                    bzStyledText.loadPageMarginFileName(value);
                } catch (FileNotFoundException exception) {
                    logError("Unable to open page margin sound file", exception);
                } catch (IOException exception) {
                    logError("Unable to read page margin sound file", exception);
                } catch (UnsupportedAudioFileException ignore) {
                    logError("Sound file unsupported for page margin bell", value);
                } catch (LineUnavailableException ignore) {
                    logError("Line unavailable for page margin bell", value);
                }
                break;

            case "brailleText.visible":
                bzStyledText.setBrailleVisible(Boolean.parseBoolean(value));
                break;
            case "brailleText.font":

                //   find offset for fileName
                offset = value.indexOf(' ') + 1;
                if (offset < 1 || offset == value.length())
                    return false;
                offset = value.indexOf(' ', offset) + 1;
                if (offset < 1 || offset == value.length())
                    return false;
                tokens = value.split(" ");
                if (tokens.length < 3)
                    return false;
                bzStyledText.setBrailleFont(new Font(parentShell.getDisplay(),
                        value.substring(offset),
                        Integer.parseInt(tokens[0]),
                        Integer.parseInt(tokens[1])));
                break;

            case "asciiText.visible":
                bzStyledText.setAsciiVisible(Boolean.parseBoolean(value));
                break;
            case "asciiText.font":

                //   find offset for fileName
                offset = value.indexOf(' ') + 1;
                if (offset < 1 || offset == value.length())
                    return false;
                offset = value.indexOf(' ', offset) + 1;
                if (offset < 1 || offset == value.length())
                    return false;
                tokens = value.split(" ");
                if (tokens.length < 3)
                    return false;
                bzStyledText.setAsciiFont(new Font(parentShell.getDisplay(),
                        value.substring(offset),
                        Integer.parseInt(tokens[0]),
                        Integer.parseInt(tokens[1])));
                break;

            case "recentFilesMax":
                recentFilesMax = Integer.parseInt(value);
                break;
            case "recentFile":

                if (recentFiles.size() < recentFilesMax)
                    recentFiles.add(value);
                break;//TODO:  rereading settings?

            default:
                return false;
        }

        return true;
    }

    boolean readSettings() {
        if (!file.exists()) {
            logMessage("Settings file not found:  " + file.getPath());
            return false;
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                try {
                    if (!readLine(line))
                        logError("Unknown setting, line #" + lineNumber, line + " -- " + file.getPath(), false);
                } catch (NumberFormatException ignored) {
                    logError("Bad setting value, line #" + lineNumber, line + " -- " + file.getPath(), false);
                } finally {
                    lineNumber++;
                }
            }
        } catch (FileNotFoundException exception) {
            logError("Unable to open settings file for reading", exception);
        } catch (IOException exception) {
            logError("Unable to read settings file", exception);
        } finally {
            try {
                if (reader != null)
                    reader.close();
            } catch (IOException exception) {
                logError("Unable to close settings file", exception);
            }
        }

        return true;
    }

    private void writeLines(PrintWriter writer) {
        String fileName;

        if (shellSize != null)
            writer.println("size " + shellSize.x + ' ' + shellSize.y + ' ' + shellMaximized);

        writer.println("charsPerLine " + bzStyledText.getCharsPerLine());
        writer.println("lineMarginBell " + bzStyledText.getLineMarginBell());
        fileName = bzStyledText.getLineMarginFileName();
        if (fileName != null)
            writer.println("lineMarginFileName " + fileName);
        fileName = bzStyledText.getLineEndFileName();
        if (fileName != null)
            writer.println("lineEndFileName " + fileName);

        writer.println("linesPerPage " + bzStyledText.getLinesPerPage());
        writer.println("pageMarginBell " + bzStyledText.getPageMarginBell());
        fileName = bzStyledText.getPageMarginFileName();
        if (fileName != null)
            writer.println("pageMarginFileName " + fileName);

        writer.println();

        writer.println("brailleText.visible " + bzStyledText.getBrailleVisible());
        FontData fontData = bzStyledText.getBrailleFont().getFontData()[0];
        writer.println("brailleText.font "
                + fontData.getHeight() + ' '
                + fontData.getStyle() + ' '
                + fontData.getName());

        writer.println();

        writer.println("asciiText.visible " + bzStyledText.getAsciiVisible());
        fontData = bzStyledText.getAsciiFont().getFontData()[0];
        writer.println("asciiText.font "
                + fontData.getHeight() + ' '
                + fontData.getStyle() + ' '
                + fontData.getName());

        writer.println();

        writer.println("recentFilesMax " + recentFilesMax);
        for (String recentFile : recentFiles)
            writer.println("recentFile " + recentFile);

        writer.println();
    }

    boolean writeSettings() {
        try {
            if (!file.exists()) {
                logMessage("Creating settings file:  " + file.getPath());
                file.createNewFile();
            }
        } catch (IOException exception) {
            logError("Unable to create settings file", exception);
            return false;
        }

        try (PrintWriter writer = new PrintWriter(file)) {
            writeLines(writer);
        } catch (FileNotFoundException exception) {
            logError("Unable to open settings file for writing", exception);
            return false;
        }

        return true;
    }

    private class ControlHandler implements ControlListener {
        /**
         * @see CheckMaximizeThread
         */
        private volatile boolean checkingMaximize;

        private Point prevShellSize;

        @Override
        public void controlResized(ControlEvent ignored) {
            prevShellSize = shellSize;
            shellSize = parentShell.getSize();

            if (!checkingMaximize) {
                checkingMaximize = true;
                parentShell.getDisplay().timerExec(100, new CheckMaximizeThread());
            }

        }

        @Override
        public void controlMoved(ControlEvent ignored) {
        }

        /**
         * <p>
         * The getMaximized method does not work with some window managers
         * inside the controlResized method.  It needs to be called after the
         * controlResized method returns.  Unlike the AdjustOtherThread object,
         * there is no corresponding event for which to wait.  So the thread for
         * this class is run inside controlResized with a delay and it then
         * checks getMaximized.
         * </p>
         */
        private class CheckMaximizeThread implements Runnable {
            @Override
            public void run() {
                shellMaximized = parentShell.getMaximized();
                if (shellMaximized)
                    shellSize = prevShellSize;
                checkingMaximize = false;
            }
        }
    }
}
