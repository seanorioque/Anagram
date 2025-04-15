package com.example.anagramfinalprojectdaa;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AnagramFinderController implements Initializable {

    @FXML
    private TextField wordTextField;

    @FXML
    private Button searchButton;

    @FXML
    private ListView<String> resultsListView;

    private ArrayList<String> fileWords = new ArrayList<>();
    private WebView definitionView;
    private Stage definitionStage;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadWordsFromFile();

        // Set up the definition stage
        definitionStage = new Stage();
        definitionStage.setTitle("Word Definition");
        definitionView = new WebView();
        Scene definitionScene = new Scene(new BorderPane(definitionView), 600, 400);
        definitionStage.setScene(definitionScene);

        // Click on a result to show definition
        resultsListView.setOnMouseClicked(e -> {
            String selectedWord = resultsListView.getSelectionModel().getSelectedItem();
            if (selectedWord != null && !selectedWord.startsWith("No anagrams found")) {
                showDefinition(selectedWord);
            }
        });
    }

    @FXML
    private void handleSearchAction() {
        findAnagrams();
    }

    private void loadWordsFromFile() {
        try {
            File file = new File("src/words.txt");
            Scanner scan = new Scanner(file);

            while (scan.hasNextLine()) {
                fileWords.add(scan.nextLine().trim());
            }
            scan.close();
        } catch (FileNotFoundException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("File Error");
            alert.setHeaderText("Words File Not Found");
            alert.setContentText("Could not find the words.txt file in the src directory.");
            alert.showAndWait();
        }
    }

    private void findAnagrams() {
        String targetWord = wordTextField.getText().trim();
        if (targetWord.isEmpty()) {
            return;
        }

        // Clear previous results
        resultsListView.getItems().clear();

        for (String word : fileWords) {
            if (isAnagram(targetWord, word)) {
                resultsListView.getItems().add(word);
            }
        }

        if (resultsListView.getItems().isEmpty()) {
            resultsListView.getItems().add("No anagrams found for \"" + targetWord + "\"");
        }
    }

    private boolean isAnagram(String word1, String word2) {
        if (word1.length() != word2.length()) return false;

        ArrayList<Character> w1 = new ArrayList<>();
        ArrayList<Character> w2 = new ArrayList<>();

        for (char c : word1.toLowerCase().toCharArray()) {
            w1.add(c);
        }
        for (char c : word2.toLowerCase().toCharArray()) {
            w2.add(c);
        }

        Collections.sort(w1);
        Collections.sort(w2);

        return w1.equals(w2);
    }

    private void showDefinition(String word) {
        try {
            // Using the Free Dictionary API
            URL url = new URL("https://api.dictionaryapi.dev/api/v2/entries/en/" + word);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String response = reader.lines().collect(Collectors.joining());
                reader.close();

                // Parse the JSON response and create HTML
                String htmlContent = formatDefinitionAsHtml(response, word);

                // Load the HTML content into the WebView
                definitionView.getEngine().loadContent(htmlContent);
                definitionStage.setTitle("Definition of \"" + word + "\"");
                definitionStage.show();
            } else {
                definitionView.getEngine().loadContent("<html><body><h2>Definition not found for \"" +
                        word + "\"</h2><p>Sorry, we couldn't find a definition for this word.</p></body></html>");
                definitionStage.show();
            }
        } catch (Exception e) {
            definitionView.getEngine().loadContent("<html><body><h2>Error</h2><p>Failed to load definition: " +
                    e.getMessage() + "</p></body></html>");
            definitionStage.show();
        }
    }

    private String formatDefinitionAsHtml(String jsonResponse, String word) {
        StringBuilder html = new StringBuilder();
        html.append("<html><head><style>");
        html.append("body { font-family: 'Segoe UI', Arial, sans-serif; margin: 20px; background-color: #f8f9fa; }");
        html.append("h1 { color: #3a539b; border-bottom: 2px solid #e0e0e0; padding-bottom: 10px; }");
        html.append("h3 { color: #2c406e; margin-top: 20px; margin-bottom: 5px; }");
        html.append(".definition { margin-left: 20px; line-height: 1.5; }");
        html.append(".example { margin-left: 40px; font-style: italic; color: #666666; background-color: #f0f2f5; padding: 8px; border-left: 3px solid #3a539b; }");
        html.append(".phonetic { color: #669966; font-size: 16px; margin-bottom: 20px; }");
        html.append("ol { padding-left: 25px; }");
        html.append("li { margin-bottom: 15px; }");
        html.append(".word-type { display: inline-block; background-color: #e8eaf6; color: #3a539b; padding: 5px 10px; border-radius: 15px; font-weight: bold; margin-right: 10px; }");
        html.append(".synonyms, .antonyms { margin-top: 8px; color: #444444; }");
        html.append("a { color: #3a539b; text-decoration: none; }");
        html.append("a:hover { text-decoration: underline; }");
        html.append("</style></head><body>");

        try {
            // Simple parsing of the JSON response using regex
            html.append("<h1>").append(word).append("</h1>");

            // Extract phonetics
            Pattern phoneticPattern = Pattern.compile("\"text\":\"([^\"]+)\"");
            Matcher phoneticMatcher = phoneticPattern.matcher(jsonResponse);

            html.append("<p class='phonetic'>Pronunciation: ");
            boolean foundPhonetic = false;
            while (phoneticMatcher.find()) {
                html.append(phoneticMatcher.group(1)).append(" ");
                foundPhonetic = true;
            }
            if (!foundPhonetic) {
                html.append("Not available");
            }
            html.append("</p>");

            // Extract parts of speech
            Pattern posPattern = Pattern.compile("\"partOfSpeech\":\"([^\"]+)\"");
            Matcher posMatcher = posPattern.matcher(jsonResponse);

            int posIndex = 0;
            while (posMatcher.find()) {
                String partOfSpeech = posMatcher.group(1);
                html.append("<h3><span class='word-type'>").append(partOfSpeech).append("</span></h3>");

                // Find the definitions section for this part of speech
                int startPos = posMatcher.end();
                int nextPosStart = jsonResponse.indexOf("\"partOfSpeech\"", startPos);
                if (nextPosStart == -1) {
                    nextPosStart = jsonResponse.length();
                }

                String definitionsSection = jsonResponse.substring(startPos, nextPosStart);

                // Extract definitions
                Pattern defPattern = Pattern.compile("\"definition\":\"([^\"]+)\"");
                Matcher defMatcher = defPattern.matcher(definitionsSection);

                html.append("<ol>");
                while (defMatcher.find()) {
                    String definition = defMatcher.group(1);
                    html.append("<li><div class='definition'>").append(definition).append("</div>");

                    // Find example for this definition if available
                    int defEnd = defMatcher.end();
                    int exampleStart = definitionsSection.indexOf("\"example\":", defEnd);
                    if (exampleStart != -1 && exampleStart < definitionsSection.indexOf("\"definition\":", defEnd)) {
                        Pattern examplePattern = Pattern.compile("\"example\":\"([^\"]+)\"");
                        Matcher exampleMatcher = examplePattern.matcher(definitionsSection.substring(defEnd));
                        if (exampleMatcher.find()) {
                            String example = exampleMatcher.group(1);
                            html.append("<div class='example'>\"").append(example).append("\"</div>");
                        }
                    }

                    html.append("</li>");
                }
                html.append("</ol>");

                // Extract synonyms
                Pattern synPattern = Pattern.compile("\"synonyms\":\\[(.*?)\\]");
                Matcher synMatcher = synPattern.matcher(definitionsSection);
                if (synMatcher.find()) {
                    String synonymsJson = synMatcher.group(1);
                    if (!synonymsJson.isEmpty()) {
                        html.append("<div class='synonyms'><strong>Synonyms: </strong>");
                        Pattern wordPattern = Pattern.compile("\"([^\"]+)\"");
                        Matcher wordMatcher = wordPattern.matcher(synonymsJson);
                        boolean first = true;
                        while (wordMatcher.find()) {
                            if (!first) html.append(", ");
                            html.append(wordMatcher.group(1));
                            first = false;
                        }
                        html.append("</div>");
                    }
                }

                // Extract antonyms
                Pattern antPattern = Pattern.compile("\"antonyms\":\\[(.*?)\\]");
                Matcher antMatcher = antPattern.matcher(definitionsSection);
                if (antMatcher.find()) {
                    String antonymsJson = antMatcher.group(1);
                    if (!antonymsJson.isEmpty()) {
                        html.append("<div class='antonyms'><strong>Antonyms: </strong>");
                        Pattern wordPattern = Pattern.compile("\"([^\"]+)\"");
                        Matcher wordMatcher = wordPattern.matcher(antonymsJson);
                        boolean first = true;
                        while (wordMatcher.find()) {
                            if (!first) html.append(", ");
                            html.append(wordMatcher.group(1));
                            first = false;
                        }
                        html.append("</div>");
                    }
                }

                posIndex++;
            }

            // Extract source URLs
            Pattern sourcePattern = Pattern.compile("\"sourceUrls\":\\[\"([^\"]+)\"");
            Matcher sourceMatcher = sourcePattern.matcher(jsonResponse);
            if (sourceMatcher.find()) {
                String sourceUrl = sourceMatcher.group(1);
                html.append("<p><small>Source: <a href='")
                        .append(sourceUrl)
                        .append("' target='_blank'>")
                        .append(sourceUrl)
                        .append("</a></small></p>");
            }

        } catch (Exception e) {
            html.append("<h2>Error parsing definition</h2>");
            html.append("<p>").append(e.getMessage()).append("</p>");
            html.append("<pre>").append(jsonResponse).append("</pre>");
        }

        html.append("</body></html>");
        return html.toString();
    }
}