package se233.chapter3.controller;

import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.util.Callback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import se233.chapter3.Launcher;
import se233.chapter3.helpers.WordMapList;
import se233.chapter3.model.FileFreq;
import se233.chapter3.model.PDFdocument;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class MainViewController {
    @FXML
    private ListView<File> inputListView;
    @FXML
    private Button startButton;
    @FXML
    private ListView<String> listView;
    @FXML
    private MenuItem menuCloseBtn;
    @FXML
    private AnchorPane anchorPane;
    private LinkedHashMap<String, WordMapList> uniqueSets;
    Logger logger = LogManager.getLogger(MainViewController.class);

    @FXML
    public void initialize() {
        inputListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(File file, boolean empty) {
                super.updateItem(file, empty);
                if (empty || file == null) {
                    setText(null);
                } else {
                    setText(file.getName());
                }
            }
        });

        inputListView.setOnDragOver(event -> {
            Dragboard dragboard = event.getDragboard();

            if (!dragboard.hasFiles()) {
                return;
            }
            final boolean isAccepted = dragboard.getFiles().get(0).getName().toLowerCase().endsWith(".pdf");
            if (isAccepted) {
                event.acceptTransferModes(TransferMode.COPY);
            } else {
                event.consume();
            }
        });

        inputListView.setOnDragDropped(event -> {
            Dragboard dragboard = event.getDragboard();
            boolean success = false;
            if (dragboard.hasFiles()) {
                success = true;
                int total_files = dragboard.getFiles().size();

                for (int i = 0; i < total_files; i++) {
                    File file = dragboard.getFiles().get(i);
                    inputListView.getItems().add(file);
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });


        startButton.setOnAction(event -> {
            Parent bgRoot = Launcher.stage.getScene().getRoot();
            inputListView.getItems().stream().forEach(file -> logger.info(file.getName()));
            Task<Void> processTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    ProgressIndicator pi = new ProgressIndicator();
                    VBox box = new VBox(pi);
                    box.setAlignment(Pos.CENTER);
                    Launcher.stage.getScene().setRoot(box);

                    try (ExecutorService executor = Executors.newFixedThreadPool(4)) {
                        final ExecutorCompletionService<Map<String, FileFreq>> completionService = new ExecutorCompletionService<>(executor);
                        List<File> inputListViewItems = inputListView.getItems();
                        int total_files = inputListViewItems.size();
                        Map<String, FileFreq>[] wordMap = new Map[total_files];
                        for (File inputListViewItem : inputListViewItems) {
                            try {
                                PDFdocument p = new PDFdocument(inputListViewItem.getAbsolutePath());
                                completionService.submit(new WordMapPageTask(p));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        for (int i = 0; i < total_files; i++) {
                            try {
                                Future<Map<String, FileFreq>> future = completionService.take();
                                wordMap[i] = future.get();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        try {
                            WordMapMergeTask merger = new WordMapMergeTask(wordMap);
                            Future<LinkedHashMap<String, WordMapList>> future = executor.submit(merger);
                            uniqueSets = future.get();
                            listView.getItems().clear();
                            listView.getItems().addAll(uniqueSets.keySet());
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            executor.shutdown();
                        }


                    }
                    return null;
                }
            };
            processTask.setOnSucceeded(e -> {
                Launcher.stage.getScene().setRoot(bgRoot);
            });
            Thread thread = new Thread(processTask);
            thread.setDaemon(true);
            thread.start();


        });
        listView.setOnMouseClicked(event -> {
            ArrayList<FileFreq> listOfLinks = uniqueSets.get(listView.getSelectionModel().getSelectedItem());

            ListView<FileFreq> popupListView = new ListView<>();
            LinkedHashMap<FileFreq, String> lookupTable = new LinkedHashMap<>();
            for (FileFreq fileFreq : listOfLinks) {
                lookupTable.put(fileFreq, fileFreq.getPath());
                popupListView.getItems().add(fileFreq);
            }
            popupListView.setPrefHeight(popupListView.getItems().size() * 35);
            popupListView.setOnMouseClicked(innerEvent -> {
                Launcher.hs.showDocument("file:///" + lookupTable.get(popupListView.getSelectionModel().getSelectedItem()));
                popupListView.getScene().getWindow().hide();
            });
            Popup popup = new Popup();
            TextField textField = new TextField();
            textField.setPrefHeight(0);
            textField.setPrefWidth(0);
            textField.setPadding(new Insets(0));
            popup.getContent().addAll(popupListView, textField);
            popup.setHeight(popupListView.getItems().size() * 35);
            popup.show(Launcher.stage);
            textField.requestFocus();
        });

        menuCloseBtn.setOnAction(event -> Launcher.stage.close());
    }


}
