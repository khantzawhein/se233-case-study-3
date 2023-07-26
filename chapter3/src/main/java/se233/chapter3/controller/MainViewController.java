package se233.chapter3.controller;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import se233.chapter3.Launcher;
import se233.chapter3.model.FileFreq;
import se233.chapter3.model.PDFdocument;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class MainViewController {
    @FXML
    private ListView<String> inputListView;
    @FXML
    private Button startButton;
    @FXML
    private ListView<String> listView;
    private LinkedHashMap<String, ArrayList<FileFreq>> uniqueSets;

    @FXML
    public void initialize() {
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
                String filepath;
                int total_files = dragboard.getFiles().size();

                for (int i = 0; i < total_files; i++) {
                    File file = dragboard.getFiles().get(i);
                    filepath = file.getAbsolutePath();
                    inputListView.getItems().add(filepath);
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });

        startButton.setOnAction(event -> {
            Parent bgRoot = Launcher.stage.getScene().getRoot();
            Task<Void> processTask = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    ProgressIndicator pi = new ProgressIndicator();
                    VBox box = new VBox(pi);
                    box.setAlignment(Pos.CENTER);
                    Launcher.stage.getScene().setRoot(box);

                    try (ExecutorService executor = Executors.newFixedThreadPool(4)) {
                        final ExecutorCompletionService<Map<String, FileFreq>> completionService = new ExecutorCompletionService<>(executor);
                        List<String> inputListViewItems = inputListView.getItems();
                        int total_files = inputListViewItems.size();
                        Map<String, FileFreq>[] wordMap = new Map[total_files];
                        for (String inputListViewItem : inputListViewItems) {
                            try {
                                PDFdocument p = new PDFdocument(inputListViewItem);
                                completionService.submit(new WordMapPageTask(p));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        for (int i = 0; i < total_files; i++) {
                            try {
                                Future<Map<String, FileFreq>> future = completionService.take();
                                wordMap[i] = future.get();
                                System.out.println("Finished " + i + " of " + total_files);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        try {
                            WordMapMergeTask merger = new WordMapMergeTask(wordMap);
                            Future<LinkedHashMap<String, ArrayList<FileFreq>>> future = executor.submit(merger);
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
            popup.getContent().add(popupListView);
            popup.setHeight(popupListView.getItems().size() * 35);
            popup.show(Launcher.stage);
        });
    }


}
