package src.app.dialogs;

import src.app.AppController;
import src.person.Person;
import src.person.People;
import src.person.RegisteredPerson;
import src.person.OCCCPerson;
import src.date.OCCCDate;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikipediaImportDialog extends JDialog {
    private final AppController appController;
    private JTextField urlField;
    private JCheckBox randomizeStudentIDBox;
    private JCheckBox randomizeGovIDBox;
    private JButton fetchButton;
    private JTextArea previewArea;
    private JButton importButton;
    private JButton exportButton;
    private JComboBox<String> exportFormatBox;
    private JButton cancelButton;
    private JCheckBox debugOutputBox; // Add this field
    // Helper class to store person and metadata for import
    private static class PersonWithMeta {
        Person person;
        String description;
        String tags;
        PersonWithMeta(Person person, String description, String tags) {
            this.person = person;
            this.description = description;
            this.tags = tags;
        }
    }

    private List<PersonWithMeta> scrapedPeople = new ArrayList<>();

    public WikipediaImportDialog(JFrame parent, AppController appController) {
        super(parent, "Import People from Wikipedia", false);
        this.appController = appController;
        setLayout(new BorderLayout(10, 10));
        setSize(700, 500);
        setLocationRelativeTo(parent);
        buildUI();
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setFocusable(true);
        setFocusableWindowState(true);
        setEnabled(true);
        // DO NOT call setVisible(true) here!
    }

    private void buildUI() {
        // Combine all top content into a single vertical panel
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        // Blurb
        JPanel blurbPanel = new JPanel(new BorderLayout());
        JLabel blurbLabel = new JLabel("<html><b>Wikipedia List Import</b><br>Paste a Wikipedia list URL (e.g., <a href='https://en.wikipedia.org/wiki/List_of_computer_scientists'>List of computer scientists</a>) below.<br>This tool will extract names and birthdays from each linked article.<br>Example: https://en.wikipedia.org/wiki/List_of_computer_scientists</html>");
        blurbLabel.setFont(blurbLabel.getFont().deriveFont(Font.PLAIN, 13f));
        blurbPanel.add(blurbLabel, BorderLayout.CENTER);
        topPanel.add(blurbPanel);

        // Input panel
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        urlField = new JTextField();
        urlField.setToolTipText("Paste a Wikipedia list URL here, e.g. https://en.wikipedia.org/wiki/List_of_computer_scientists");
        urlField.setFont(urlField.getFont().deriveFont(Font.PLAIN, 14f));
        urlField.setEnabled(true);
        urlField.setEditable(true);
        inputPanel.add(new JLabel("Wikipedia List URL:"), BorderLayout.WEST);
        inputPanel.add(urlField, BorderLayout.CENTER);
        fetchButton = new JButton("Fetch");
        inputPanel.add(fetchButton, BorderLayout.EAST);
        topPanel.add(inputPanel);

        // Options panel
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        randomizeStudentIDBox = new JCheckBox("Randomize Student IDs");
        randomizeGovIDBox = new JCheckBox("Randomize Government IDs");
        debugOutputBox = new JCheckBox("Show Debug Output"); // Add debug toggle
        optionsPanel.add(randomizeStudentIDBox);
        optionsPanel.add(randomizeGovIDBox);
        optionsPanel.add(debugOutputBox); // Add to panel
        topPanel.add(optionsPanel);

        // Add only one component to BorderLayout.NORTH
        add(topPanel, BorderLayout.NORTH);

        previewArea = new JTextArea(15, 60);
        previewArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(previewArea);
        add(scrollPane, BorderLayout.CENTER); // Fix: actually add the scrollPane to the dialog

        // Action panel
        JPanel actionPanel = new JPanel(new BorderLayout());
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        importButton = new JButton("Import into Current List");
        exportButton = new JButton("Export as New List");
        exportFormatBox = new JComboBox<>(new String[]{".ppl", ".txt", ".json"});
        rightPanel.add(importButton);
        rightPanel.add(exportButton);
        rightPanel.add(exportFormatBox);
        rightPanel.setOpaque(false);
        // Cancel button (bottom left)
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        cancelButton = new JButton("Cancel");
        cancelButton.setBackground(UIManager.getColor("Viewer.background"));
        cancelButton.setForeground(UIManager.getColor("Viewer.foreground"));
        cancelButton.addActionListener(_ -> dispose());
        leftPanel.add(cancelButton);
        leftPanel.setOpaque(false);
        actionPanel.add(leftPanel, BorderLayout.WEST);
        actionPanel.add(rightPanel, BorderLayout.EAST);
        actionPanel.setOpaque(false);
        add(actionPanel, BorderLayout.SOUTH);

        fetchButton.addActionListener(_ -> fetchAndPreview());
        importButton.addActionListener(_ -> doImport());
        exportButton.addActionListener(_ -> doExport());

        SwingUtilities.invokeLater(() -> {
            urlField.setEnabled(true);
            urlField.setEditable(true);
            urlField.requestFocusInWindow();
            urlField.selectAll();
        });
    }

    private void fetchAndPreview() {
        String url = urlField.getText().trim();
        if (url.isEmpty() || !url.startsWith("http")) {
            JOptionPane.showMessageDialog(this, "Please enter a valid Wikipedia URL.", "Invalid URL", JOptionPane.ERROR_MESSAGE);
            return;
        }
        fetchButton.setEnabled(false);
        previewArea.setText("");
        new SwingWorker<List<PersonWithMeta>, String>() {
            private Exception error = null;
            @Override
            protected List<PersonWithMeta> doInBackground() {
                List<PersonWithMeta> peopleWithMeta = new ArrayList<>();
                try {
                    publish("Fetching Wikipedia list page...\n");
                    String html = fetchHtml(url);
                    publish("Parsing names and links from list...\n");
                    List<PersonEntry> entries = extractNamesAndLinksFromWikipediaList(html);
                    // Filter out garbage entries
                    Set<String> forbiddenFirst = new HashSet<>(Arrays.asList("List", "Common", "Dynamic", "Category", "Template", "Portal", "Index", "Outline", "Main", "Help", "Special", "Wikipedia", "Wikidata", "Commons", "File", "Talk", "Draft", "Module", "Book", "Topic", "Glossary", "Timeline", "Bibliography", "See", "References", "External", "Navigation", "Authority", "Control"));
                    Set<String> forbiddenLast = new HashSet<>(Arrays.asList("people", "scientists", "sciences)", "Wikidata", "Commons", "Wikipedia", "articles", "(disambiguation)", "(surname)", "(given", "(name)", "(computer", "(mathematician)", "(engineer)", "(scientist)", "(author)", "(programmer)", "(theorist)", "(inventor)", "(researcher)", "(academic)", "(professor)", "(administrator)", "(biologist)", "(chemist)", "(physicist)", "(statistician)", "(philosopher)", "(linguist)", "(psychologist)", "(sociologist)", "(economist)", "(entrepreneur)", "(businessman)", "(businesswoman)", "(politician)", "(artist)", "(writer)", "(poet)", "(composer)", "(musician)", "(actor)", "(director)", "(producer)", "(journalist)", "(editor)", "(publisher)", "(cartoonist)", "(illustrator)", "(designer)", "(architect)", "(lawyer)", "(judge)", "(doctor)", "(physician)", "(surgeon)", "(dentist)", "(nurse)", "(veterinarian)", "(pharmacist)", "(engineer)", "(pilot)", "(astronaut)", "(athlete)", "(coach)", "(referee)", "(umpire)", "(trainer)", "(manager)", "(owner)", "(executive)", "(administrator)", "(consultant)", "(advisor)", "(counselor)", "(therapist)", "(social", "(activist)", "(advocate)", "(organizer)", "(volunteer)", "(philanthropist)", "(benefactor)", "(patron)", "(supporter)", "(sponsor)", "(donor)", "(contributor)", "(member)", "(participant)", "(attendee)", "(guest)", "(visitor)", "(resident)", "(citizen)", "(immigrant)", "(emigrant)", "(expatriate)", "(refugee)", "(migrant)", "(traveler)", "(tourist)", "(explorer)", "(adventurer)", "(pioneer)", "(settler)", "(colonist)", "(missionary)", "(clergy)", "(priest)", "(minister)", "(pastor)", "(rabbi)", "(imam)", "(monk)", "(nun)", "(bishop)", "(cardinal)", "(pope)", "(saint)", "(martyr)", "(prophet)", "(apostle)", "(disciple)", "(patriarch)", "(matriarch)", "(deacon)", "(elder)", "(preacher)", "(evangelist)", "(missionary)", "(theologian)", "(philosopher)", "(scholar)", "(teacher)", "(student)", "(alumnus)", "(alumna)", "(graduate)", "(undergraduate)", "(postgraduate)", "(fellow)", "(intern)", "(apprentice)", "(trainee)", "(candidate)", "(nominee)", "(winner)", "(finalist)", "(runner-up)", "(champion)", "(medalist)", "(record", "(holder)", "(titleholder)", "(defender)", "(challenger)", "(contender)", "(opponent)", "(rival)", "(competitor)", "(teammate)", "(partner)", "(collaborator)", "(co-author)", "(co-founder)", "(co-inventor)", "(co-owner)", "(co-director)", "(co-producer)", "(co-star)", "(co-host)", "(co-anchor)", "(co-editor)", "(co-pilot)", "(co-driver)", "(co-captain)", "(co-chair)", "(co-president)", "(co-CEO)", "(co-manager)", "(coordinator)", "(facilitator)", "(moderator)", "(mediator)", "(arbitrator)", "(negotiator)", "(representative)", "(delegate)", "(envoy)", "(ambassador)", "(consul)", "(attaché)", "(diplomat)", "(emissary)", "(messenger)", "(herald)", "(spokesperson)", "(press", "(secretary)", "(aide)", "(assistant)", "(associate)", "(colleague)", "(peer)", "(counterpart)", "(successor)", "(predecessor)", "(ancestor)", "(descendant)", "(relative)", "(kin)", "(family)", "(parent)", "(mother)", "(father)", "(child)", "(son)", "(daughter)", "(sibling)", "(brother)", "(sister)", "(uncle)", "(aunt)", "(nephew)", "(niece)", "(cousin)", "(grandparent)", "(grandmother)", "(grandfather)", "(grandchild)", "(grandson)", "(granddaughter)", "(in-law)", "(step", "(half", "(adopted)", "(foster)", "(guardian)", "(ward)", "(godparent)", "(godchild)", "(mentor)", "(mentee)", "(protege)", "(beneficiary)", "(heir)", "(legatee)", "(testator)", "(testatrix)", "(executor)", "(administrator)", "(trustee)", "(custodian)", "(conservator)", "(receiver)", "(liquidator)", "(insolvent)", "(debtor)", "(creditor)", "(claimant)", "(plaintiff)", "(defendant)", "(appellant)", "(respondent)", "(petitioner)", "(complainant)", "(accused)", "(suspect)", "(victim)", "(witness)", "(informant)", "(whistleblower)", "(leaker)", "(source)", "(contact)", "(confidant)", "(confidante)", "(friend)", "(acquaintance)", "(neighbor)", "(stranger)"));
                    List<PersonEntry> filteredEntries = new ArrayList<>();
                    for (PersonEntry entry : entries) {
                        String[] parts = entry.name.split(" ");
                        String first = parts.length > 1 ? parts[0] : entry.name;
                        String last = parts.length > 1 ? parts[parts.length - 1] : "";
                        if (forbiddenFirst.contains(first) || forbiddenLast.contains(last)) continue;
                        filteredEntries.add(entry);
                    }
                    int total = filteredEntries.size();
                    publish("Filtered to " + total + " valid entries. Fetching Wikidata in parallel...\n");
                    Set<String> usedGovIDs = Collections.synchronizedSet(new HashSet<>());
                    Set<String> usedStudentIDs = Collections.synchronizedSet(new HashSet<>());
                    Random rand = new Random();
                    int nThreads = Math.min(16, Runtime.getRuntime().availableProcessors() * 2);
                    java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(nThreads);
                    List<java.util.concurrent.Future<PersonWithMeta>> futures = new ArrayList<>();
                    java.util.concurrent.atomic.AtomicInteger completed = new java.util.concurrent.atomic.AtomicInteger(0);
                    for (PersonEntry entry : filteredEntries) {
                        futures.add(pool.submit(() -> {
                            String[] parts = entry.name.split(" ");
                            String first = parts.length > 1 ? parts[0] : entry.name;
                            String last = parts.length > 1 ? parts[parts.length - 1] : "";
                            int done = completed.incrementAndGet();
                            final String entryName = entry.name;
                            PersonWithMeta pwm = fetchPersonWithMetaFromWikidata(entry.link, first, last, msg -> publish("[" + done + "/" + total + "] " + entryName + ": " + msg + "\n"));
                            // Optionally, fetch description/tags from Wikidata or fallback to HTML as before
                            // ...existing random ID logic...
                            String govID = null, studentID = null;
                            if (randomizeGovIDBox.isSelected()) {
                                do { govID = randomID(rand, 8); } while (usedGovIDs.contains(govID));
                                usedGovIDs.add(govID);
                            }
                            if (randomizeStudentIDBox.isSelected()) {
                                do { studentID = randomID(rand, 8); } while (usedStudentIDs.contains(studentID));
                                usedStudentIDs.add(studentID);
                            }
                            Person p = pwm.person;
                            if (govID != null && studentID != null) {
                                p = new OCCCPerson(new RegisteredPerson(p.getFirstName(), p.getLastName(), p.getDOB(), govID), studentID);
                            } else if (govID != null) {
                                p = new RegisteredPerson(p.getFirstName(), p.getLastName(), p.getDOB(), govID);
                            }
                            return new PersonWithMeta(p, pwm.description, pwm.tags);
                        }));
                    }
                    pool.shutdown();
                    for (java.util.concurrent.Future<PersonWithMeta> f : futures) {
                        try {
                            PersonWithMeta pwm = f.get();
                            if (pwm != null) peopleWithMeta.add(pwm);
                        } catch (Exception e) {
                            // Log error for this entry
                        }
                    }
                    publish("Done fetching all Wikidata.\n");
                    return peopleWithMeta;
                } catch (Exception ex) {
                    error = ex;
                    return Collections.emptyList();
                }
            }
            @Override
            protected void process(List<String> chunks) {
                for (String msg : chunks) {
                    previewArea.append(msg);
                }
            }
            @Override
            protected void done() {
                fetchButton.setEnabled(true);
                try {
                    if (error != null) {
                        previewArea.append("\n");
                        JOptionPane.showMessageDialog(WikipediaImportDialog.this, "Failed to fetch or parse Wikipedia page: " + error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    } else {
                        scrapedPeople = get();
                        previewArea.append("\n--- Preview ---\n");
                        previewArea.append(peoplePreview(scrapedPeople));
                    }
                } catch (Exception ex) {
                    previewArea.append("\n");
                    JOptionPane.showMessageDialog(WikipediaImportDialog.this, "Unexpected error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // Fix: fetchHtml returns String, not void
    private String fetchHtml(String urlStr) throws IOException {
        StringBuilder sb = new StringBuilder();
        try {
            URL url = new URI(urlStr).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
        } catch (Exception e) {
            throw new IOException("Failed to fetch HTML: " + e.getMessage(), e);
        }
        return sb.toString();
    }

    // Helper class to store name and link
    private static class PersonEntry {
        String name;
        String link;
        PersonEntry(String name, String link) {
            this.name = name;
            this.link = link;
        }
    }

    // Improved HTML parsing using a state machine (no regex for nested tags)
    private List<PersonEntry> extractNamesAndLinksFromWikipediaList(String html) {
        List<PersonEntry> entries = new ArrayList<>();
        Set<String> seenNames = new HashSet<>();
        int idx = 0;
        while ((idx = html.indexOf("<li", idx)) != -1) {
            int liStart = html.indexOf('>', idx);
            if (liStart == -1) break;
            int liEnd = html.indexOf("</li>", liStart);
            if (liEnd == -1) break;
            String liContent = html.substring(liStart + 1, liEnd).trim();
            // Only consider <li> that starts with <a href=...>
            if (!liContent.startsWith("<a")) {
                idx = liEnd + 5;
                continue;
            }
            // Find the first <a href="/wiki/...">Name</a>
            int aStart = liContent.indexOf("<a ");
            int hrefStart = liContent.indexOf("href=\"/wiki/", aStart);
            if (aStart == -1 || hrefStart == -1) {
                idx = liEnd + 5;
                continue;
            }
            hrefStart += 6; // move past 'href="'
            int hrefEnd = liContent.indexOf('"', hrefStart + 6);
            if (hrefEnd == -1) {
                idx = liEnd + 5;
                continue;
            }
            String link = liContent.substring(hrefStart, hrefEnd);
            // Only allow links to articles (no ':', '#', '?')
            if (link.contains(":") || link.contains("#") || link.contains("?")) {
                idx = liEnd + 5;
                continue;
            }
            int nameStart = liContent.indexOf('>', hrefEnd) + 1;
            int nameEnd = liContent.indexOf("</a>", nameStart);
            if (nameStart == 0 || nameEnd == -1) {
                idx = liEnd + 5;
                continue;
            }
            String name = liContent.substring(nameStart, nameEnd).trim();
            // Heuristics: at least two capitalized words, no digits, not all caps, not a known non-person
            String[] parts = name.split(" ");
            if (parts.length < 2) {
                idx = liEnd + 5;
                continue;
            }
            boolean allCapitalized = true;
            for (String part : parts) {
                if (part.isEmpty() || !Character.isUpperCase(part.charAt(0))) {
                    allCapitalized = false;
                    break;
                }
            }
            if (!allCapitalized || name.matches(".*\\d.*") || name.equals(name.toUpperCase())) {
                idx = liEnd + 5;
                continue;
            }
            if (name.matches(".*(Award|Bibliography|Fiction|Science|Anthology|Series|Magazine|Journal|Press|Publisher|Encyclopedia|Reference|External|Notes|Further|Reading|Sources|See|Also|Category|Portal|Template|Commons|Wikidata|Wikipedia|Help|Special|File|Talk|Draft|Module|Book|Topic|Glossary|Timeline|Navigation|Authority|Control|Index|Outline|Main).*")) {
                idx = liEnd + 5;
                continue;
            }
            if (link.endsWith("_(disambiguation)")) {
                idx = liEnd + 5;
                continue;
            }
            if (seenNames.contains(name)) {
                idx = liEnd + 5;
                continue;
            }
            seenNames.add(name);
            entries.add(new PersonEntry(name, link));
            idx = liEnd + 5;
        }
        return entries;
    }

    // Helper: Get Wikidata entity ID from Wikipedia article link
    private String getWikidataEntityIdFromWikipedia(String relativeLink) {
        try {
            String title = relativeLink.replace("/wiki/", "");
            String encodedTitle = java.net.URLEncoder.encode(title, "UTF-8");
            String apiUrl = "https://en.wikipedia.org/w/api.php?action=query&prop=pageprops&format=json&titles=" + encodedTitle;
            appendDebug("[DEBUG] Wikipedia API URL: " + apiUrl);
            String json = fetchHtml(apiUrl);
            int redirectsIdx = json.indexOf("\"redirects\"");
            if (redirectsIdx != -1) {
                int toIdx = json.indexOf("\"to\":\"", redirectsIdx);
                if (toIdx != -1) {
                    int toStart = toIdx + 7;
                    int toEnd = json.indexOf('"', toStart);
                    if (toEnd != -1) {
                        String canonicalTitle = json.substring(toStart, toEnd);
                        appendDebug("[DEBUG] Wikipedia redirect: " + title + " -> " + canonicalTitle);
                        String encodedCanonical = java.net.URLEncoder.encode(canonicalTitle, "UTF-8");
                        String apiUrl2 = "https://en.wikipedia.org/w/api.php?action=query&prop=pageprops&format=json&titles=" + encodedCanonical;
                        String json2 = fetchHtml(apiUrl2);
                        int idx2 = json2.indexOf("\"wikibase_item\":\"Q");
                        if (idx2 != -1) {
                            int start2 = idx2 + 18;
                            int end2 = json2.indexOf('"', start2);
                            if (end2 != -1) {
                                String qid2 = "Q" + json2.substring(start2, end2);
                                appendDebug("[DEBUG] Wikipedia article: " + canonicalTitle + " | QID: " + qid2);
                                return qid2;
                            }
                        }
                    }
                }
            }
            int idx = json.indexOf("\"wikibase_item\":\"Q");
            if (idx != -1) {
                int start = idx + 18;
                int end = json.indexOf('"', start);
                if (end != -1) {
                    String qid = "Q" + json.substring(start, end);
                    appendDebug("[DEBUG] Wikipedia article: " + title + " | QID: " + qid);
                    return qid;
                }
            } else {
                appendDebug("[DEBUG] Wikipedia article: " + title + " | QID: NOT FOUND");
            }
        } catch (Exception e) {
            appendDebug("[DEBUG] Exception in getWikidataEntityIdFromWikipedia: " + e.getMessage());
        }
        return null;
    }

    // Debug output to previewArea (internal terminal)
    private void appendDebug(String entryName, String msg) {
        if (debugOutputBox != null && debugOutputBox.isSelected() && previewArea != null) {
            synchronized (previewArea) {
                if (entryName != null && !entryName.isEmpty()) {
                    previewArea.append("[" + entryName + "] ");
                }
                previewArea.append(msg + "\n");
            }
        }
    }
    // Overload for no entry name
    private void appendDebug(String msg) {
        appendDebug(null, msg);
    }

    // Improved: robustly extract all P569 (birth date) 'time' fields after locating 'P569' in Wikidata JSON
    private PersonWithMeta fetchPersonWithMetaFromWikidata(String relativeLink, String first, String last, java.util.function.Consumer<String> logger) {
        String entityId = getWikidataEntityIdFromWikipedia(relativeLink);
        String description = "";
        StringBuilder occcTags = new StringBuilder();
        OCCCDate dob = new OCCCDate(1, 1, 1);
        String intro = "";
        String pageTitle = relativeLink.replace("/wiki/", "");
        if (entityId == null) {
            logger.accept("No Wikidata entity found");
        } else {
            try {
                // Fetch Wikidata entity info
                String props = "claims%7Cdescriptions%7Clabels";
                String apiUrl = "https://www.wikidata.org/w/api.php?action=wbgetentities&ids=" + entityId + "&props=" + props + "&languages=en&format=json";
                String json = fetchHtml(apiUrl);
                String entityKey = "\"" + entityId + "\"";
                int entityIdx = json.indexOf(entityKey);
                if (entityIdx == -1) entityIdx = 0;
                // --- Description (from Wikidata) ---
                int descRoot = json.indexOf("\"descriptions\"", entityIdx);
                if (descRoot != -1) {
                    int enIdx = json.indexOf("\"en\"", descRoot);
                    if (enIdx != -1 && enIdx < json.indexOf("}", descRoot)) {
                        int valIdx = json.indexOf("\"value\"", enIdx);
                        if (valIdx != -1 && valIdx < json.indexOf("}", enIdx)) {
                            int colon = json.indexOf(':', valIdx);
                            int quote1 = json.indexOf('"', colon + 1);
                            int quote2 = json.indexOf('"', quote1 + 1);
                            if (quote1 != -1 && quote2 != -1) {
                                description = json.substring(quote1 + 1, quote2);
                                logger.accept("Wikidata description: " + description);
                            }
                        }
                    }
                }
                // --- Birthday (P569) ---
                int p569Idx = json.indexOf("\"P569\"");
                boolean foundDOB = false;
                if (p569Idx != -1) {
                    int timeIdx = json.indexOf("\"time\"", p569Idx);
                    while (timeIdx != -1) {
                        int quote1 = json.indexOf('"', timeIdx + 7);
                        int quote2 = json.indexOf('"', quote1 + 1);
                        if (quote1 != -1 && quote2 != -1) {
                            String time = json.substring(quote1 + 1, quote2);
                            if (time.length() >= 11) {
                                try {
                                    int year = Integer.parseInt(time.substring(1, 5));
                                    int month = Integer.parseInt(time.substring(6, 8));
                                    int day = Integer.parseInt(time.substring(9, 11));
                                    if (month == 0) month = 1;
                                    if (day == 0) day = 1;
                                    dob = new OCCCDate(day, month, year);
                                    logger.accept("Wikidata birthdate: " + year + "-" + month + "-" + day);
                                    foundDOB = true;
                                    break;
                                } catch (Exception parseEx) {
                                    logger.accept("Wikidata date parse error: " + parseEx.getMessage());
                                }
                            }
                        }
                        // Look for next '"time"' after this one
                        timeIdx = json.indexOf("\"time\"", quote2);
                    }
                    if (!foundDOB) {
                        logger.accept("No valid 'time' field found for P569");
                    }
                } else {
                    logger.accept("No P569 (birthdate) property found");
                }
                // --- Tags (occupations, P106) ---
                Set<String> occcIds = new LinkedHashSet<>();
                int claimsIdxForTags = json.indexOf("\"claims\"", entityIdx);
                int p106Idx = json.indexOf("\"P106\"", claimsIdxForTags);
                if (p106Idx != -1) {
                    int searchIdx = p106Idx;
                    while (true) {
                        int idIdx = json.indexOf("\"id\"", searchIdx);
                        if (idIdx == -1 || idIdx > json.indexOf("\"labels\"", entityIdx)) break;
                        int colon = json.indexOf(':', idIdx);
                        int quote1 = json.indexOf('"', colon + 1);
                        int quote2 = json.indexOf('"', quote1 + 1);
                        if (quote1 == -1 || quote2 == -1) break;
                        String occcId = json.substring(quote1 + 1, quote2);
                        if (!occcId.startsWith("Q")) break;
                        occcIds.add(occcId);
                        searchIdx = quote2 + 1;
                    }
                }
                // Fetch occupation labels robustly
                for (String occcId : occcIds) {
                    // Try to find the label for this occupation QID
                    Pattern occcLabelPattern = Pattern.compile("\"" + occcId + "\"\\s*:\\s*\\{[^}]*?\"en\"\\s*:\\s*\\{[^}]*?\"value\"\\s*:\\s*\"([^\"]+)\"", Pattern.DOTALL);
                    Matcher occcLabelMatcher = occcLabelPattern.matcher(json);
                    if (occcLabelMatcher.find()) {
                        String label = occcLabelMatcher.group(1);
                        occcTags.append("<").append(label).append(">");
                    }
                }
                if (occcTags.length() == 0) {
                    logger.accept("No occupation tags found");
                } else {
                    logger.accept("Wikidata tags: " + occcTags);
                }
            } catch (Exception e) {
                logger.accept("Wikidata error: " + e.getMessage());
            }
            // --- Fetch Wikipedia intro as description ---
            try {
                String apiUrl = "https://en.wikipedia.org/api/rest_v1/page/summary/" + java.net.URLEncoder.encode(pageTitle, "UTF-8");
                String json = fetchHtml(apiUrl);
                int extractIdx = json.indexOf("\"extract\"");
                if (extractIdx != -1) {
                    int colon = json.indexOf(':', extractIdx);
                    int quote1 = json.indexOf('"', colon + 1);
                    int quote2 = json.indexOf('"', quote1 + 1);
                    if (quote1 != -1 && quote2 != -1) {
                        intro = json.substring(quote1 + 1, quote2);
                        if (!intro.isEmpty()) description = intro;
                        logger.accept("Wikipedia intro: " + intro);
                    }
                }
            } catch (Exception e) {
                logger.accept("Wikipedia intro fetch error: " + e.getMessage());
            }
        }
        // Use first/last from arguments (from PersonEntry)
        return new PersonWithMeta(new Person(first, last, dob), description, occcTags.toString());
    }

    private String randomID(Random rand, int len) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(rand.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String peoplePreview(List<PersonWithMeta> people) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (PersonWithMeta pwm : people) {
            Person p = pwm.person;
            sb.append(++count).append(": ").append(p.getFirstName()).append(" ").append(p.getLastName());
            if (p instanceof RegisteredPerson) {
                sb.append(" [GID: ").append(((RegisteredPerson)p).getGovID()).append("]");
            }
            if (p instanceof OCCCPerson) {
                sb.append(" [SID: ").append(((OCCCPerson)p).getStudentID()).append("]");
            }
            sb.append(" [DOB: ").append(p.getDOB()).append("]");
            if (!pwm.description.isEmpty()) sb.append("\n    Desc: ").append(pwm.description);
            if (!pwm.tags.isEmpty()) sb.append("\n    Tags: ").append(pwm.tags);
            sb.append("\n");
        }
        sb.append("Total: ").append(people.size()).append(" people\n");
        return sb.toString();
    }

    private void doImport() {
        if (scrapedPeople == null || scrapedPeople.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No people to import. Fetch a Wikipedia list first.", "Nothing to Import", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int imported = 0;
        for (PersonWithMeta pwm : scrapedPeople) {
            Person p = pwm.person;
            String desc = pwm.description;
            String tags = pwm.tags;
            boolean added = appController.getPeople().add(p, desc, tags);
            if (added) imported++;
        }
        appController.notifyDataChanged(); // Flag data as changed for save prompt
        JOptionPane.showMessageDialog(this, imported + " people imported.", "Import Complete", JOptionPane.INFORMATION_MESSAGE);
        SwingUtilities.invokeLater(() -> dispose());
    }

    private void doExport() {
        if (scrapedPeople == null || scrapedPeople.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No people to export. Fetch a Wikipedia list first.", "Nothing to Export", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String format = exportFormatBox.getSelectedItem().toString();
        JFileChooser fileChooser = new JFileChooser();
        if (format.equals(".json")) {
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON Files", "json"));
        } else if (format.equals(".txt")) {
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Text Files", "txt"));
        } else {
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("People Files", "ppl"));
        }
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                People people = new People();
                for (PersonWithMeta pwm : scrapedPeople) {
                    people.add(pwm.person, pwm.description, pwm.tags);
                }
                if (format.equals(".json")) {
                    src.app.dialogs.Dialogs.exportToJson(people, file, d -> (d != null ? String.format("%02d/%02d/%04d", d.getMonthNumber(), d.getDayOfMonth(), d.getYear()) : "01/01/1900"));
                } else if (format.equals(".txt")) {
                    src.app.dialogs.Dialogs.exportToText(people, file, d -> (d != null ? String.format("%02d/%02d/%04d", d.getMonthNumber(), d.getDayOfMonth(), d.getYear()) : "01/01/1900"));
                } else {
                    try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
                        oos.writeObject(people);
                    }
                }
                JOptionPane.showMessageDialog(this, "Exported " + people.size() + " people.", "Export Complete", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void showDialog(JFrame parent, AppController appController) {
        SwingUtilities.invokeLater(() -> {
            WikipediaImportDialog dialog = new WikipediaImportDialog(parent, appController);
            dialog.setVisible(true); // Only call setVisible(true) after construction
            dialog.urlField.setEnabled(true);
            dialog.urlField.setEditable(true);
            dialog.urlField.requestFocusInWindow();
            dialog.urlField.selectAll();
        });
    }
}
