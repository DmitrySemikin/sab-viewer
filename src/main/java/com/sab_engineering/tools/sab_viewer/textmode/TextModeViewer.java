package com.sab_engineering.tools.sab_viewer.textmode;

import com.sab_engineering.tools.sab_viewer.controller.ViewerSettings;
import com.sab_engineering.tools.sab_viewer.io.LineContent;
import com.sab_engineering.tools.sab_viewer.io.LineStatistics;
import com.sab_engineering.tools.sab_viewer.io.Reader;
import com.sab_engineering.tools.sab_viewer.io.Scanner;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Vector;

// this just some simple demo for the scanner to show and test the components a real GUI would need
public class TextModeViewer {
    private final static int ROWS = 40;
    private final static int COLUMNS = 80;

    private static final List<LineContent> LINE_CONTENTS = new Vector<>(ROWS); // synchronized vector is useful after all
    private static final List<LineStatistics> LINE_STATISTICS = new Vector<>(100000);

    private static IOException scannerException = null;

    @SuppressWarnings("BusyWait")
    public static void view(String fileName) {
        Charset charset = StandardCharsets.UTF_8;
        Thread scannerThread = new Thread(
                () -> {
                    try {
                        Scanner.scanFile(fileName, charset, LINE_CONTENTS::add, ROWS, COLUMNS, LINE_STATISTICS::add);
                    } catch (IOException ioException) {
                        scannerException = ioException;
                    }
                },
                "Scanner"
        );
        scannerThread.start();

        int printedSize = 0;
        while (scannerThread.isAlive() || printedSize < LINE_CONTENTS.size()) {
            while (printedSize < LINE_CONTENTS.size()) {
                LineContent newLine = LINE_CONTENTS.get(printedSize);
                printedSize += 1;

                System.out.println(newLine.getVisibleContent());
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                scannerThread.interrupt();
            }
        }

        if (scannerException != null) {
            throw new UncheckedIOException("Unable to scan file '" + fileName + "': " + scannerException.getClass().getSimpleName(), scannerException);
        }

        try {
            if (LINE_STATISTICS.size() > ROWS) {
                Reader reader = new Reader(fileName, charset);
                List<LineContent> lineContents = reader.readSpecificLines(LINE_STATISTICS.subList(LINE_STATISTICS.size() / 2 - 5, LINE_STATISTICS.size() / 2 + 5), new ViewerSettings(ROWS, COLUMNS, LINE_STATISTICS.size() / 2 - 5, 10));
                System.out.println("=============Middle lines, starting from column 10 =============");
                for (LineContent lineContent : lineContents) {
                    System.out.println(lineContent.getVisibleContent());
                }

                lineContents = reader.readSpecificLines(LINE_STATISTICS.subList(LINE_STATISTICS.size() - 10, LINE_STATISTICS.size()), new ViewerSettings(ROWS, COLUMNS, LINE_STATISTICS.size() - 10, 0));
                System.out.println("=============Last lines=============");
                for (LineContent lineContent : lineContents) {
                    System.out.println(lineContent.getVisibleContent());
                }
            }
        } catch (IOException ioException) {
            throw new UncheckedIOException("Unable to read file '" + fileName + "': " + ioException.getClass().getSimpleName(), ioException);
        }

        System.out.println("File " + fileName + " contained " + LINE_STATISTICS.size() + " lines with " + LINE_STATISTICS.stream().mapToLong(LineStatistics::getLengthInBytes).sum() + " characters (excluding new line characters)");
    }

}
