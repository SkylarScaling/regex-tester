package com.wheezy.apps.regextest;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.BadLocationException;

import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.reactfx.Subscription;

import javafx.application.Platform;
import javafx.fxml.FXML;

public class RegExTesterController
{

  @FXML
  private StyleClassedTextArea exampleTextArea;

  @FXML
  private StyleClassedTextArea regExTextField;

  @FXML
  private StyleClassedTextArea resultsTextArea;

  private ExecutorService executor;
  private Subscription regExSub;
  private Subscription exampleTextSub;

  @FXML
  public void initialize()
  {
    executor = Executors.newSingleThreadExecutor();
    exampleTextArea.setParagraphGraphicFactory(LineNumberFactory.get(exampleTextArea));
    
    exampleTextArea.textProperty().addListener((ov, oldv, newv) -> {
      computeHighlighting();
    });
    
    regExTextField.textProperty().addListener((ov, oldv, newv) -> {
      computeHighlighting();
    });
  }

  private void testRegex() throws BadLocationException
  {
    String text = exampleTextArea.getText();

    // This fixes possible issues with EOL characters by replacing them with
    // Unix-style EOLs.
    text = text.replaceAll("\r\n", "\n");

    exampleTextArea.replaceText(text);

    String pattern = regExTextField.getText();
    if (pattern.length() == 0)
    {
      return;
    }

    resultsTextArea.replaceText("");

    if (text.length() > 0)
    {
      ArrayList<ArrayList<Integer>> highlightPositions = new ArrayList<>();
      int count = 0;

      StringBuilder sb = new StringBuilder();

      ArrayList<String> regexGroups = getRegexGroups(pattern);

      try
      {
        Pattern r = Pattern.compile(pattern);
        Matcher matcher = r.matcher(text);
        while (matcher.find())
        {
          count++;

          ArrayList<Integer> currentHighlightPositions = new ArrayList<>();
          currentHighlightPositions.add(matcher.start());
          currentHighlightPositions.add(matcher.end());
          highlightPositions.add(currentHighlightPositions);

          System.out.println("Highlight range: (" + matcher.start() + ", " + matcher.end() + ")");
          exampleTextArea.setStyleClass(matcher.start(), matcher.end(), "highlight");

          sb.append(String.format("Match %d\n", count));
          for (int i = 0; i <= matcher.groupCount(); i++)
          {
            if (matcher.start(i) >= 0 && matcher.end(i) >= 0)
            {
              String matchPosString = String.format("pos. %d", matcher.start(i));
              matchPosString += String.format(", length %d", matcher.end(i) - matcher.start(i));
              sb.append(String.format("   Group%d (%s): %s   =>   '%s'\n", i, matchPosString, regexGroups.get(i),
                  matcher.group(i)));
            }
          }
          sb.append("- - - - -\n");
        }
      }
      catch (Exception e)
      {
        resultsTextArea.replaceText(e.getMessage());
        resultsTextArea.setStyleClass(0, resultsTextArea.getText().length(), "red");
        return;
      }

      if (count > 0)
      {
        resultsTextArea.replaceText(sb.toString());
        resultsTextArea.setStyleClass(0, resultsTextArea.getText().length(), "blue");
      }
      else
      {
        resultsTextArea.replaceText("NO MATCH!");
        resultsTextArea.setStyleClass(0, resultsTextArea.getText().length(), "red");
      }
    }

  }

  private static ArrayList<String> getRegexGroups(String rx)
  {
    ArrayList<String> rxGroups = new ArrayList<>();
    Stack<Integer> openingBracketPositions = new Stack<>();
    List<Integer> closingBracketPositions = new ArrayList<>();
    List<RegexGroup> matchingGroups = new ArrayList<>();

    rxGroups.add(rx); // Group0 is always the entire regex string;

    char[] rxChars = rx.toCharArray();

    for (int i = 0; i < rxChars.length; i++)
    {
      String stringBefore = new String(rxChars, 0, i);
      char curChar = rxChars[i];
      if (curChar == '(' && !hasEscapeSequence(stringBefore))
      {
        openingBracketPositions.push(i);
      }
      else if (curChar == ')' && !hasEscapeSequence(stringBefore))
      {
        closingBracketPositions.add(i);
      }
    }

    while (openingBracketPositions.size() > 0)
    {
      Integer[] currentGroup = new Integer[2];
      currentGroup[0] = openingBracketPositions.pop();

      for (int i = 0; i < closingBracketPositions.size(); i++)
      {
        if (closingBracketPositions.get(i) > currentGroup[0])
        {
          currentGroup[1] = closingBracketPositions.get(i);
          closingBracketPositions.remove(i);
          break;
        }
      }

      if (currentGroup[1] != null)
      {
        RegexGroup curRegexGroup = new RegexGroup(currentGroup[0], currentGroup[1], rx);
        matchingGroups.add(curRegexGroup);
      }
    }

    matchingGroups.sort(RegexGroup::compare);

    matchingGroups.forEach((group) -> rxGroups.add(group.toString()));

    return rxGroups;
  }

  private static boolean hasEscapeSequence(String s)
  {
    int backslashCount = 0;
    if (s.length() > 0)
    {
      char[] stringChars = s.toCharArray();

      for (int i = stringChars.length - 1; i >= 0; --i)
      {
        if (stringChars[i] == '\\')
        {
          ++backslashCount;
        }
        else
        {
          break;
        }
      }
    }

    return backslashCount % 2 == 1;
  }

  private void computeHighlighting()
  {
    Platform.runLater(new Runnable()
    {
      @Override
      public void run()
      {
        try
        {
          testRegex();
        }
        catch (BadLocationException e)
        {
          e.printStackTrace();
        }
      }
    });
  }

  public void shutdown()
  {
    regExSub.unsubscribe();
    exampleTextSub.unsubscribe();
    executor.shutdown();
  }
}