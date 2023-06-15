// You can use this file as a starting point for your dictionary client
// The file contains the code for command line parsing and it also
// illustrates how to read and partially parse the input typed by the user.
// Although your main class has to be in this file, there is no requirement that you
// use this template or hav all or your classes in this file.

import java.io.*;
import java.io.IOException;
import java.lang.System;
import java.net.Socket;
import java.util.Arrays;

//
// This is an implementation of a simplified version of a command
// line dictionary client. The only argument the program takes is
// -d which turns on debugging output.
//

public class CSdict {

  static final int MAX_LEN = 255;
  static Boolean debugOn = false;

  private static final int PERMITTED_ARGUMENT_COUNT = 1;
  private static String command;
  private static String[] arguments;
  private static Socket socket = null;
  private static PrintWriter out = null;
  private static BufferedReader in = null;
  private static String error903 = "903 Incorrect number of arguments.";
  private static String error904 = "904 Invalid argument.";
  private static String error910 =
    "910 Supplied command not expected at this time.";
  private static String error925 =
    "925 Control connection I/O error, closing control connection.";
  private static String selDict = "*";

  public static void main(String[] args) {
    // Verify command line arguments

    if (args.length == PERMITTED_ARGUMENT_COUNT) {
      debugOn = args[0].equals("-d");
      if (debugOn) {
        System.out.println("Debugging output enabled");
      } else {
        System.out.println(
          "902 Invalid command line option - Only -d is allowed"
        );
        return;
      }
    } else if (args.length > PERMITTED_ARGUMENT_COUNT) {
      System.out.println(
        "901 Too many command line options - Only -d is allowed"
      );
      return;
    }

    // Example code to read command line input and extract arguments.
    while (true) {
      byte cmdString[] = new byte[MAX_LEN];
      int len;

      try {
        System.out.print("317dict> ");
        System.in.read(cmdString);

        // Convert the command string to ASII
        String inputString = new String(cmdString, "ASCII");

        // Split the string into words
        String[] inputs = inputString.trim().split("( |\t)+");

        String first = "";
        for (String input : inputs) {
          if (!input.equals("") && !input.equals(" ")) {
            first = input;
            break;
          }
        }

        // continue to next loop if the input is an empty line or starts with #
        if (
          inputs.length == 0 || first.startsWith("#") || first.equals("")
        ) continue;

        // Set the command
        command = inputs[0].toLowerCase().trim();
        // Remainder of the inputs is the arguments.
        arguments = Arrays.copyOfRange(inputs, 1, inputs.length);
      } catch (IOException exception) {
        System.err.println(
          "998 Input error while reading commands, terminating."
        );
        terminateConnections();
        System.exit(-1);
      }

      try {
        boolean openOrQuit = command.equals("open") || command.equals("quit");

        /*
         * If command is not open or quit, check connection first. If socket is not
         * connected, print error message and go to next loop.
         */
        if (!openOrQuit && !isConnected()) {
          System.out.println(error910);
          continue;
        }

        switch (command) {
          case "open":
            // if connection is established, print error message
            if (isConnected()) {
              System.out.println(error910);
            } else {
              openServerPort(arguments);
            }
            break;
          case "dict":
            getAllDicts(arguments);
            break;
          case "set":
            setDictionaries(arguments);
            break;
          case "define":
            defineWord(arguments);
            break;
          case "match":
            matchWord(arguments);
            break;
          case "prefixmatch":
            prefixMatchWord(arguments);
            break;
          case "close":
            disconnect(arguments);
            break;
          case "quit":
            terminateConnections();
            System.exit(0);
            break;
          default:
            System.out.println("900 Invalid command.");
        }
      } catch (IOException e) {
        System.out.println(error925);
        terminateConnections();
      } catch (Exception e) {
        System.out.println("999 Processing error. " + e.getMessage() + ".");
      }
    }
  }

  // Opens a new TCP/IP connection to a dictionary server
  public static void openServerPort(String[] arguments) throws IOException {
    if (arguments.length != 2) {
      System.out.println(error903);
      return;
    }

    String server = arguments[0];
    String portAsString = arguments[1];

    try {
      int port = Integer.parseInt(arguments[1]);
      selDict = "*";
      socket = new Socket(server, port);
      out = new PrintWriter(socket.getOutputStream(), true);
      in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      String s = in.readLine();
      if (debugOn) {
        printStatus(s);
      }
    } catch (NumberFormatException e) {
      System.out.println(error904);
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      System.out.println(
        "920 Control connection to " +
        server +
        " on port " +
        portAsString +
        " failed to open."
      );
    }
  }

  // print all the dictionaries on server
  public static void getAllDicts(String[] arguments) throws Exception {
    if (arguments.length != 0) {
      System.out.println(error903);
      return;
    }

    String command = "SHOW DATABASES";

    if (debugOn) {
      printClientCommand(command);
    }

    out.println(command);

    // read status response
    String s = in.readLine();
    if (debugOn) {
      printStatus(s);
    }

    // read text response
    s = in.readLine();

    while (!s.startsWith("250 ok")) {
      System.out.println(s);
      s = in.readLine();
    }

    // print 250 ok
    if (debugOn) {
      printStatus(s);
    }
  }

  public static void setDictionaries(String[] arguments) throws Exception {
    if (arguments.length != 1) {
      System.out.println(error903);
      return;
    }

    selDict = arguments[0];
  }

  public static void defineWord(String[] arguments) throws Exception {
    if (arguments.length != 1) {
      System.out.println(error903);
      return;
    }
    String commandDefine = "DEFINE " + selDict + " " + arguments[0];

    if (debugOn) {
      printClientCommand(commandDefine);
    }

    out.println(commandDefine);

    // read status response
    String s = in.readLine();
    if (debugOn) {
      printStatus(s);
    }

    if (s.startsWith("150")) {
      // read text response
      s = in.readLine();

      while (!s.startsWith("250 ok")) {
        if (s.startsWith("151")) {
          if (debugOn) {
            printStatus(s);
          }
          System.out.println(atReplace(s));
        } else {
          System.out.println(s);
        }
        s = in.readLine();
      }

      // print 250 ok
      if (debugOn) {
        printStatus(s);
      }
    } else if (s.startsWith("552")) {
      System.out.println("**No definition found**");
      String commandMatch = "MATCH " + selDict + " . " + arguments[0];
      matchRes(commandMatch, "default");
    } else if (s.startsWith("550")) {
      throw new Exception("Invalid database");
    }
  }

  // this method disconnect socket from server, and call terminateSocket
  public static void disconnect(String[] arguments) throws Exception {
    if (arguments.length != 0) {
      System.out.println(error903);
      return;
    }

    String command = "QUIT";

    if (debugOn) {
      printClientCommand(command);
    }
    out.println(command);

    String s = in.readLine();

    if (debugOn) {
      printStatus(s);
    }
    terminateConnections();
  }

  // helper method to read match response
  private static void matchRes(String commandMatch, String strategy)
    throws Exception {
    if (debugOn) {
      printClientCommand(commandMatch);
    }

    out.println(commandMatch);

    // read status response
    String s = in.readLine();
    if (debugOn) {
      printStatus(s);
    }

    if (s.startsWith("152")) {
      // read text response
      s = in.readLine();
      while (!s.startsWith("250 ok")) {
        System.out.println(s);
        s = in.readLine();
      }

      // print 250 ok
      if (debugOn) {
        printStatus(s);
      }
    } else if (s.startsWith("552")) {
      if (strategy == "default") {
        System.out.println("***No matches found***");
      } else {
        System.out.println("****No matching word(s) found****");
      }
    } else if (s.startsWith("550")) {
      throw new Exception("Invalid database");
    } else if (s.startsWith("551")) {
      throw new Exception("Invalid strategy");
    }
  }

  // execute MATCH WORD command
  private static void matchWord(String[] arguments) throws Exception {
    if (!isConnected()) {
      System.out.println(error910);
      return;
    }

    if (arguments.length != 1) {
      System.out.println(error903);
      return;
    }

    String commandMatch = "MATCH " + selDict + " exact " + arguments[0];
    matchRes(commandMatch, "exact");
  }

  // execute prefixMatchWord
  private static void prefixMatchWord(String[] arguments) throws Exception {
    if (!isConnected()) {
      System.out.println(error910);
      return;
    }

    if (arguments.length != 1) {
      System.out.println(error903);
      return;
    }

    String commandMatch = "MATCH " + selDict + " prefix " + arguments[0];
    matchRes(commandMatch, "exact");
  }

  private static void terminateConnections() {
    try {
      if (socket != null && !socket.isClosed()) {
        socket.shutdownInput();
        socket.shutdownOutput();
        socket.close();
      }
    } catch (Exception e) {}
  }

  private static boolean isConnected() {
    return socket != null && socket.isConnected() && !socket.isClosed();
  }

  private static String atReplace(String s) {
    String[] arr = s.split(" ", 3);
    return "@ " + arr[2];
  }

  private static void printClientCommand(String command) {
    System.out.println("--> " + command);
  }

  private static void printStatus(String status) {
    System.out.println("<-- " + status);
  }
}
