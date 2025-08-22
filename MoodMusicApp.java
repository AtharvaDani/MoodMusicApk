/*
 * MoodMusicApp.java
 *
 * Simple Swing Mood-Based Music Recommender
 * - Recommend a mini-playlist for a mood (up to 5 songs)
 * - Open song in browser (YouTube search)
 * - Add / view / remove favorites (stored in user's home folder)
 *
 * Save as: MoodMusicApp.java
 */

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MoodMusicApp extends JFrame {
    // Simple Song model
    public static class Song {
        public final String title;
        public final String url;

        public Song(String title, String url) {
            this.title = title;
            this.url = url;
        }

        @Override
        public String toString() { return title; }
    }

    // Main DB
    private final Map<String, List<Song>> db = new LinkedHashMap<>();

    // UI models
    private final DefaultListModel<Song> playlistModel = new DefaultListModel<>();
    private final JList<Song> playlistList = new JList<>(playlistModel);

    // Components
    private JComboBox<String> moodBox;
    private JTextField searchField;
    private JLabel statusLabel;
    private JButton openBtn;
    private JButton addFavBtn;

    // Favorites file
    private final File favFile = new File(System.getProperty("user.home"), ".moodmusic_favs.txt");

    public MoodMusicApp() {
        super("🎵 Mood Music — Recommender");
        buildDatabase();
        initUI();
    }

    private void buildDatabase() {
        db.put("Happy", Arrays.asList(
                new Song("Golden – HUNTR/X", "https://www.youtube.com/results?search_query=HUNTRX+Golden"),
                new Song("Ordinary – Alex Warren", "https://www.youtube.com/results?search_query=Ordinary+Alex+Warren"),
                new Song("Flowers – Miley Cyrus (2023)", "https://www.youtube.com/results?search_query=Flowers+Miley+Cyrus")
        ));

        db.put("Energetic", Arrays.asList(
                new Song("Just Keep Watching – Tate McRae", "https://www.youtube.com/results?search_query=Just+Keep+Watching+Tate+McRae"),
                new Song("Born Again – Lisa ft. Doja Cat & Raye", "https://www.youtube.com/results?search_query=Born+Again+Lisa+Doja+Cat+Raye"),
                new Song("Levitating – Dua Lipa (2020)", "https://www.youtube.com/results?search_query=Levitating+Dua+Lipa")
        ));

        db.put("Chill", Arrays.asList(
                new Song("Weightless – Marconi Union", "https://www.youtube.com/results?search_query=Weightless+Marconi+Union"),
                new Song("Blinding Lights – The Weeknd (2020)", "https://www.youtube.com/results?search_query=Blinding+Lights+The+Weeknd"),
                new Song("As It Was – Harry Styles (2022)", "https://www.youtube.com/results?search_query=As+It+Was+Harry+Styles")
        ));

        db.put("Romantic", Arrays.asList(
                new Song("Manchild – Sabrina Carpenter", "https://www.youtube.com/results?search_query=Manchild+Sabrina+Carpenter"),
                new Song("What I Want – Morgan Wallen ft. Tate McRae", "https://www.youtube.com/results?search_query=What+I+Want+Morgan+Wallen+Tate+McRae"),
                new Song("Save Your Tears – The Weeknd (2021)", "https://www.youtube.com/results?search_query=Save+Your+Tears+The+Weeknd")
        ));

        db.put("Hit Singles", Arrays.asList(
                new Song("Vampire – Olivia Rodrigo (2023)", "https://www.youtube.com/results?search_query=Vampire+Olivia+Rodrigo"),
                new Song("Espresso – Sabrina Carpenter (2024)", "https://www.youtube.com/results?search_query=Espresso+Sabrina+Carpenter"),
                new Song("Ordinary – Alex Warren", "https://www.youtube.com/results?search_query=Ordinary+Alex+Warren")
        ));
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(720, 420);
        setLocationRelativeTo(null);

        JPanel main = new JPanel();
        main.setBorder(new EmptyBorder(14, 14, 14, 14));
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        add(main);

        JLabel title = new JLabel("Mood Music Recommender");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        main.add(title);

        main.add(Box.createRigidArea(new Dimension(0, 12)));

        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.X_AXIS));
        controls.setAlignmentX(Component.LEFT_ALIGNMENT);

        moodBox = new JComboBox<>(db.keySet().toArray(new String[0]));
        moodBox.setMaximumSize(new Dimension(200, 28));
        controls.add(moodBox);
        controls.add(Box.createRigidArea(new Dimension(10, 0)));

        JButton recommendBtn = new JButton("🎧 Recommend");
        recommendBtn.addActionListener(this::onRecommend);
        controls.add(recommendBtn);
        controls.add(Box.createRigidArea(new Dimension(12, 0)));

        searchField = new JTextField();
        searchField.setMaximumSize(new Dimension(240, 28));
        searchField.setToolTipText("Search for artist/song");
        searchField.addActionListener(e -> onSearch(null));
        controls.add(searchField);
        controls.add(Box.createRigidArea(new Dimension(8, 0)));

        JButton searchBtn = new JButton("Search");
        searchBtn.addActionListener(this::onSearch);
        controls.add(searchBtn);

        main.add(controls);
        main.add(Box.createRigidArea(new Dimension(0, 12)));

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.X_AXIS));

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        JLabel playlistLabel = new JLabel("Playlist");
        playlistLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        listPanel.add(playlistLabel);

        playlistList.setVisibleRowCount(8);
        playlistList.setFixedCellWidth(360);
        playlistList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(playlistList);
        listPanel.add(scroll);

        center.add(listPanel);
        center.add(Box.createRigidArea(new Dimension(12, 0)));

        JPanel actionPanel = new JPanel();
        actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.Y_AXIS));
        actionPanel.setPreferredSize(new Dimension(280, 200));

        openBtn = new JButton("▶ Open Selected");
        openBtn.addActionListener(e -> openSelected());
        openBtn.setEnabled(false);

        addFavBtn = new JButton("♡ Add to Favorites");
        addFavBtn.addActionListener(e -> addSelectedToFavorites());
        addFavBtn.setEnabled(false);

        JButton showFavBtn = new JButton("★ Show Favorites");
        showFavBtn.addActionListener(e -> showFavoritesDialog());

        actionPanel.add(openBtn);
        actionPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        actionPanel.add(addFavBtn);
        actionPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        actionPanel.add(showFavBtn);

        center.add(actionPanel);

        main.add(center);
        main.add(Box.createRigidArea(new Dimension(0, 12)));

        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(Color.DARK_GRAY);
        main.add(statusLabel);

        playlistList.addListSelectionListener(e -> {
            boolean sel = playlistList.getSelectedIndex() != -1;
            openBtn.setEnabled(sel);
            addFavBtn.setEnabled(sel);
        });

        onRecommend(null);
    }

    private void onRecommend(ActionEvent e) {
        String mood = (String) moodBox.getSelectedItem();
        List<Song> pool = new ArrayList<>(db.getOrDefault(mood, Collections.emptyList()));

        if (pool.size() < 5) {
            for (List<Song> list : db.values()) {
                for (Song s : list) {
                    if (pool.size() >= 5) break;
                    if (!pool.contains(s)) pool.add(s);
                }
                if (pool.size() >= 5) break;
            }
        }

        Collections.shuffle(pool);
        List<Song> pick = pool.subList(0, Math.min(5, pool.size()));
        updatePlaylist(pick);
        statusLabel.setText("Recommended " + pick.size() + " songs for \"" + mood + "\"");
    }

    private void updatePlaylist(List<Song> songs) {
        playlistModel.clear();
        for (Song s : songs) playlistModel.addElement(s);
        if (!songs.isEmpty()) {
            playlistList.setSelectedIndex(0);
            openBtn.setEnabled(true);
            addFavBtn.setEnabled(true);
        }
    }

    private void onSearch(ActionEvent e) {
        String q = searchField.getText().trim();
        if (q.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Type an artist, song or mood to search.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Song s = new Song(q, "https://www.youtube.com/results?search_query=" + URLEncoder.encode(q, StandardCharsets.UTF_8));
        updatePlaylist(Collections.singletonList(s));
        statusLabel.setText("Search results for: " + q);
    }

    private void openSelected() {
        Song s = playlistList.getSelectedValue();
        if (s == null) return;
        try {
            if (s.url != null && !s.url.isBlank()) {
                Desktop.getDesktop().browse(new URI(s.url));
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not open link: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addSelectedToFavorites() {
        Song s = playlistList.getSelectedValue();
        if (s == null) return;
        try {
            Set<String> lines = loadFavLines();
            String line = escape(s.title) + "|" + escape(s.url == null ? "" : s.url);
            if (lines.contains(line)) {
                statusLabel.setText("Already in favorites");
                return;
            }
            try (FileWriter fw = new FileWriter(favFile, true);
                 BufferedWriter bw = new BufferedWriter(fw)) {
                bw.write(line);
                bw.newLine();
            }
            statusLabel.setText("Added to favorites: " + s.title);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to add favorite: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Set<String> loadFavLines() throws IOException {
        Set<String> out = new LinkedHashSet<>();
        if (!favFile.exists()) return out;
        try (BufferedReader br = new BufferedReader(new FileReader(favFile))) {
            String ln;
            while ((ln = br.readLine()) != null) {
                if (!ln.isBlank()) out.add(ln);
            }
        }
        return out;
    }

    private void showFavoritesDialog() {
        try {
            List<Song> favs = new ArrayList<>();
            for (String ln : loadFavLines()) {
                String[] p = ln.split("\\|", 2);
                String t = p.length > 0 ? p[0] : "";
                String u = p.length > 1 ? p[1] : "";
                favs.add(new Song(t, u));
            }

            if (favs.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No favorites yet.", "Favorites", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            DefaultListModel<Song> favModel = new DefaultListModel<>();
            for (Song s : favs) favModel.addElement(s);
            JList<Song> favList = new JList<>(favModel);
            favList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            JScrollPane pane = new JScrollPane(favList);
            pane.setPreferredSize(new Dimension(480, 240));

            JButton openFav = new JButton("Open");
            JButton removeFav = new JButton("Remove");
            JPanel pBtns = new JPanel();
            pBtns.add(openFav);
            pBtns.add(removeFav);

            openFav.addActionListener(ev -> {
                Song se = favList.getSelectedValue();
                if (se != null) {
                    try {
                        Desktop.getDesktop().browse(new URI(se.url));
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, "Could not open favorite: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });

            removeFav.addActionListener(ev -> {
                Song se = favList.getSelectedValue();
                if (se != null) {
                    favModel.removeElement(se);
                    try (BufferedWriter bw = new BufferedWriter(new FileWriter(favFile))) {
                        for (int i = 0; i < favModel.size(); i++) {
                            Song s = favModel.get(i);
                            bw.write(escape(s.title) + "|" + escape(s.url == null ? "" : s.url));
                            bw.newLine();
                        }
                        statusLabel.setText("Favorites updated");
                    } catch (IOException ioex) {
                        JOptionPane.showMessageDialog(this, "Failed to update favorites: " + ioex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });

            JPanel content = new JPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.add(pane);
            content.add(pBtns);

            JOptionPane.showMessageDialog(this, content, "Your Favorites", JOptionPane.PLAIN_MESSAGE);

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Failed to load favorites: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("\n", " ").replace("\r", " ");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MoodMusicApp app = new MoodMusicApp();
            app.setVisible(true);
        });
    }
}
