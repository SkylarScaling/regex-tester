package com.wheezy.apps.regextest;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class RegExTester extends Application
{
  FXMLLoader fxmlLoader;

  @Override
  public void start(Stage primaryStage)
  {
    try
    {
      // Read file fxml and draw interface.
      fxmlLoader = new FXMLLoader(getClass().getResource("RegExTester.fxml"));
      Parent root = fxmlLoader.load();

      Scene scene = new Scene(root);
      scene.getStylesheets().add(RegExTester.class.getResource("regex-highlighting.css").toExternalForm());

      primaryStage.setTitle("RegEx Tester");
      primaryStage.setScene(scene);
      primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>()
      {
        @Override
        public void handle(WindowEvent t)
        {
          Platform.exit();
          System.exit(0);
        }
      });
      primaryStage.show();

    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  @Override
  public void stop()
  {
    RegExTesterController controller = fxmlLoader.getController();
    controller.shutdown();
  }

  public static void main(String[] args)
  {
    launch(args);
  }
}