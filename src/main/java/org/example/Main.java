package org.example;

import com.couchbase.lite.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.util.ArrayList;

public class Main {

    private static Replicator replicator;

    private static Database database;

    public static void main(String[] args) {
        // Create the frame on the event dispatching thread
        CouchbaseLite.init();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                createAndShowGUI();
            }
        });
    }

    private static void createAndShowGUI() {
        // Create and set up the window
        JFrame frame = new JFrame("Sync Toggle App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create the button
        JButton btnToggleSync = new JButton("Start Sync");

        // Add action listener to button
        btnToggleSync.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Change the button text based on the current state
                if (btnToggleSync.getText().equals("Start Sync")) {
                    try {
                        DatabaseConfiguration config = new DatabaseConfiguration();
                        config.setDirectory("/Users/denisrosa/Downloads");
                        database = new Database("testdb", config);

                        Database.log.getConsole().setDomains(LogDomain.ALL_DOMAINS);
                        Database.log.getConsole().setLevel(LogLevel.VERBOSE);

                        LogFileConfiguration LogCfg = new LogFileConfiguration("/Users/denisrosa/Desktop");
                        LogCfg.setMaxSize(10240);
                        LogCfg.setMaxRotateCount(5);
                        LogCfg.setUsePlaintext(false);
                        Database.log.getFile().setConfig(LogCfg);
                        Database.log.getConsole().setLevel(LogLevel.VERBOSE);

                        java.util.List<Collection> collectionList = new ArrayList<>();

                        for (Scope scope : database.getScopes()) {
                            collectionList.addAll(database.getScope(scope.getName()).getCollections());
                        }

                        // initialize the replicator configuration
                        ReplicatorConfiguration replConfig = new ReplicatorConfiguration(new URLEndpoint(
                                new URI("ws://127.0.0.1:4984/projects"))).addCollections(collectionList, null)
                                .setType(ReplicatorType.PUSH_AND_PULL)
                                .setContinuous(true) // default value
                                .setAutoPurgeEnabled(false)
                                .setAuthenticator(new BasicAuthenticator("demo@example.com", String.valueOf("P@ssw0rd12").toCharArray()));


                        replicator = new Replicator(replConfig);

                        ListenerToken token = replicator.addChangeListener(change -> {
                            CouchbaseLiteException err = change.getStatus().getError();
                            if (err != null) {
                                System.err.println("Error code :: " + err.getCode()+ err.getMessage());
                            } else {
                                System.out.println(change.toString());
                            }
                        });

                        replicator.start(true);


                    } catch (Exception ex) {
                        ex.printStackTrace();
                    } finally {
                        try {
                            if (database != null) {
                                database.close();
                            }
                        } catch (CouchbaseLiteException ex) {
                            throw new RuntimeException(ex);
                        }
                    }

                    btnToggleSync.setText("Stop Sync");
                } else {

                    try {

                        if (replicator != null) {
                            replicator.stop();
                        }
                        if (database != null) {
                            database.close();
                        }

                    } catch (CouchbaseLiteException ex) {
                        throw new RuntimeException(ex);
                    }

                    btnToggleSync.setText("Start Sync");
                }
            }
        });

        // Add the button to the frame
        frame.getContentPane().add(btnToggleSync);

        // Display the window
        frame.pack();
        frame.setVisible(true);
    }
}

