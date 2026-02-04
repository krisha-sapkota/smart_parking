import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

class ModernButton extends JButton {
    private final Color originalColor;
    private final Color hoverColor;

    public ModernButton(String text, Color bgColor) {
        super(text);
        this.originalColor = bgColor;
        this.hoverColor = bgColor.brighter();

        setBackground(bgColor);
        setForeground(Color.WHITE);
        setFocusPainted(false);
        setBorderPainted(false);
        setFont(new Font("Arial", Font.BOLD, 14));
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setOpaque(true);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bgColor.darker(), 2),
                BorderFactory.createEmptyBorder(12, 25, 12, 25)
        ));

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                setBackground(hoverColor);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(hoverColor.darker(), 2),
                        BorderFactory.createEmptyBorder(12, 25, 12, 25)
                ));
            }
            @Override public void mouseExited(MouseEvent e) {
                setBackground(originalColor);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(originalColor.darker(), 2),
                        BorderFactory.createEmptyBorder(12, 25, 12, 25)
                ));
            }
            @Override public void mousePressed(MouseEvent e) {
                setBackground(originalColor.darker());
            }
        });
    }
}

class ParkingSpotPanel extends JPanel {
    private final ParkingSpot spot;
    private final JLabel iconLabel;
    private final JLabel infoLabel;
    private final JLabel floorLabel;
    private static final Font ICON_FONT = new Font("Arial Emoji", Font.BOLD, 32);
    private static final Font INFO_FONT = new Font("Arial UI", Font.BOLD, 11);
    private static final Font FLOOR_FONT = new Font("Arial UI", Font.BOLD, 10);

    public ParkingSpotPanel(ParkingSpot spot) {
        this.spot = Objects.requireNonNull(spot);
        setLayout(new BorderLayout(0, 0));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        setPreferredSize(new Dimension(120, 100));

        // Floor indicator
        floorLabel = new JLabel("F" + spot.getFloor(), SwingConstants.RIGHT);
        floorLabel.setFont(FLOOR_FONT);
        floorLabel.setForeground(Color.GRAY);
        add(floorLabel, BorderLayout.NORTH);

        // Main icon
        iconLabel = new JLabel("", SwingConstants.CENTER);
        iconLabel.setFont(ICON_FONT);
        add(iconLabel, BorderLayout.CENTER);

        // Info panel at bottom
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setOpaque(false);
        infoLabel = new JLabel("", SwingConstants.CENTER);
        infoLabel.setFont(INFO_FONT);
        infoPanel.add(infoLabel, BorderLayout.CENTER);

        // Add small status indicator
        JLabel statusIndicator = new JLabel("●");
        statusIndicator.setFont(new Font("Arial", Font.BOLD, 8));
        statusIndicator.setForeground(Color.GREEN);
        infoPanel.add(statusIndicator, BorderLayout.EAST);

        add(infoPanel, BorderLayout.SOUTH);

        updateDisplay();
    }

    public void updateDisplay() {
        if (spot.isAvailable()) {
            setBackground(new Color(230, 245, 230)); // Light green
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(46, 204, 113), 2),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)
            ));
            iconLabel.setText("🅿️");
            iconLabel.setForeground(new Color(39, 174, 96));
            infoLabel.setText("<html><center><b>" + spot.getSpotId() + "</b><br><font color='green'>AVAILABLE</font></center></html>");
            setToolTipText("Spot " + spot.getSpotId() + " (Floor " + spot.getFloor() + "): Available");
        } else {
            Vehicle vehicle = spot.getVehicle();
            if (vehicle != null) {
                VehicleType type = vehicle.getVehicleType();
                Color typeColor = type.getColor();

                // Gradient background for occupied spots
                setBackground(new Color(
                        Math.min(typeColor.getRed() + 40, 255),
                        Math.min(typeColor.getGreen() + 40, 255),
                        Math.min(typeColor.getBlue() + 40, 255)
                ));

                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(typeColor.darker(), 2),
                        BorderFactory.createEmptyBorder(5, 5, 5, 5)
                ));

                iconLabel.setText(type.getIcon());
                iconLabel.setForeground(typeColor.darker());

                // Format time since entry
                Duration duration = Duration.between(vehicle.getEntryTime(), LocalDateTime.now());
                String timeStr = formatDurationShort(duration.toMinutes());

                infoLabel.setText("<html><center><b>" + spot.getSpotId() + "</b><br>" +
                        "<font size='2'>" + vehicle.getLicensePlate() + "</font><br>" +
                        "<font size='1' color='gray'>" + timeStr + "</font></center></html>");

                setToolTipText(String.format("Spot %s (Floor %s): %s %s, Owner: %s, Entry: %s, Duration: %s",
                        spot.getSpotId(), spot.getFloor(), type.name(), vehicle.getLicensePlate(),
                        vehicle.getOwnerId(), vehicle.getEntryTime().format(DateTimeFormatter.ofPattern("HH:mm")), timeStr));
            } else {
                setBackground(new Color(255, 230, 230));
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.RED, 2),
                        BorderFactory.createEmptyBorder(5, 5, 5, 5)
                ));
                iconLabel.setText("⚠️");
                iconLabel.setForeground(Color.RED);
                infoLabel.setText("<html><center><b>" + spot.getSpotId() + "</b><br><font color='red'>ERROR</font></center></html>");
                setToolTipText("Spot " + spot.getSpotId() + ": Inconsistent state");
            }
        }

        // Update floor label color based on availability
        floorLabel.setForeground(spot.isAvailable() ? new Color(100, 100, 100) : new Color(50, 50, 50));
    }

    private String formatDurationShort(long totalMinutes) {
        if (totalMinutes < 60) return totalMinutes + "m";
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return hours + "h" + (minutes > 0 ? " " + minutes + "m" : "");
    }
}

public class SmartParkingGUI extends JFrame {
    private static final Logger LOGGER = Logger.getLogger(SmartParkingGUI.class.getName());

    private SmartParkingLotSimulator manager;
    private JTextArea logArea;
    private JPanel gridPanel;
    private Map<String, ParkingSpotPanel> spotPanelsMap;
    private JLabel statusLabel, timeLabel, revenueLabel, floorLabel;
    private JTextField plateField, ownerField, ticketField, searchField;
    private JComboBox<VehicleType> typeBox;
    private JComboBox<String> floorBox;
    private ModernButton parkButton, releaseButton, findButton, reportsButton;
    private ModernButton copyLastTicketButton, autoParkButton, bulkReleaseButton;
    private String lastParkedTicketId = null;

    private java.util.Timer highlightEffectTimer;

    private static final int LOT_ROWS = 3;
    private static final int LOT_COLS = 8;
    private static final int FLOORS = 3;
    private static final DateTimeFormatter LOG_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String CURRENCY_SYMBOL = "PKR ";

    public SmartParkingGUI() {
        super();
        try {
            this.manager = new SmartParkingLotSimulator(LOT_ROWS, LOT_COLS, FLOORS, new EnhancedBillingSystem());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "CRITICAL ERROR: Could not initialize parking manager", e);
            JOptionPane.showMessageDialog(null,
                    String.format("<html><b>CRITICAL ERROR:</b><br>Could not initialize parking manager.<br>%s<br><br>Exiting.</html>",
                            e.getMessage()),
                    "Startup Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "System L&F not available. Using default.", e);
        }

        setTitle("🚗 Smart Parking Management System - 3 Floors (PKR Billing)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(245, 247, 250));

        initComponents();

        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        JPanel centerAreaPanel = createCenterAreaPanel();
        add(centerAreaPanel, BorderLayout.CENTER);

        initializeParkingGrid();
        setupActionListeners();

        pack();
        setMinimumSize(new Dimension(1400, 850));
        setSize(1400, 900);
        setLocationRelativeTo(null);

        startClockAndStatusTimer();
        updateOverallDisplay();

        logMessage("🚀 System Initialized. Parking Lot: " + FLOORS + " floors × " + LOT_ROWS + "×" + LOT_COLS + " spots. Billing in " + CURRENCY_SYMBOL, "INFO");
        setVisible(true);
    }

    private void initComponents() {
        plateField = createStyledTextField();
        ownerField = createStyledTextField();
        typeBox = new JComboBox<>(VehicleType.values());
        styleComboBox(typeBox);

        ticketField = createStyledTextField();
        searchField = createStyledTextField();

        // Initialize floor selector
        floorBox = new JComboBox<>();
        for (int i = 1; i <= FLOORS; i++) {
            floorBox.addItem("Floor " + i);
        }
        styleComboBox(floorBox);

        // Color scheme for buttons
        parkButton = new ModernButton("🚗 Park Vehicle", new Color(46, 204, 113));
        releaseButton = new ModernButton("🚪 Release Vehicle", new Color(231, 76, 60));
        findButton = new ModernButton("🔍 Find Vehicle", new Color(52, 152, 219));
        reportsButton = new ModernButton("📊 View Reports", new Color(155, 89, 182));
        copyLastTicketButton = new ModernButton("📋 Copy Last Ticket", new Color(241, 196, 15));
        autoParkButton = new ModernButton("🤖 Auto-Park", new Color(52, 73, 94));
        bulkReleaseButton = new ModernButton("📤 Bulk Release", new Color(230, 126, 34));

        timeLabel = new JLabel("Time: --:--:--", SwingConstants.RIGHT);
        timeLabel.setFont(new Font("Arial", Font.BOLD, 13));

        statusLabel = new JLabel("Status: Loading...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 13));

        revenueLabel = new JLabel("Revenue: " + CURRENCY_SYMBOL + "0.00", SwingConstants.LEFT);
        revenueLabel.setFont(new Font("Arial", Font.BOLD, 14));

        floorLabel = new JLabel("Floor 1", SwingConstants.CENTER);
        floorLabel.setFont(new Font("Arial", Font.BOLD, 16));
        floorLabel.setForeground(new Color(52, 152, 219));

        gridPanel = new JPanel(new GridLayout(LOT_ROWS, LOT_COLS, 10, 10));
        gridPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        gridPanel.setBackground(Color.WHITE);

        spotPanelsMap = new HashMap<>();

        logArea = new JTextArea(15, 45);
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setBackground(new Color(25, 25, 35));
        logArea.setForeground(new Color(200, 220, 240));
        logArea.setCaretColor(Color.WHITE);
    }

    private JTextField createStyledTextField() {
        JTextField field = new JTextField(12);
        field.setFont(new Font("Arial", Font.PLAIN, 13));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        return field;
    }

    private void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setFont(new Font("Arial", Font.PLAIN, 13));
        comboBox.setBackground(Color.WHITE);
        comboBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setBackground(new Color(30, 40, 60));
        header.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 25));

        // Title with icon
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titlePanel.setOpaque(false);

        JLabel iconLabel = new JLabel("🏢");
        iconLabel.setFont(new Font("Arial Emoji", Font.PLAIN, 36));

        JLabel title = new JLabel("Smart Parking Management System");
        title.setFont(new Font("Arial", Font.BOLD, 28));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("3-Floor Parking with Deadlock Prevention");
        subtitle.setFont(new Font("Arial", Font.ITALIC, 14));
        subtitle.setForeground(new Color(200, 220, 255));

        titlePanel.add(iconLabel);
        titlePanel.add(Box.createHorizontalStrut(15));

        JPanel textPanel = new JPanel(new BorderLayout(0, 0));
        textPanel.setOpaque(false);
        textPanel.add(title, BorderLayout.NORTH);
        textPanel.add(subtitle, BorderLayout.SOUTH);
        titlePanel.add(textPanel);

        header.add(titlePanel, BorderLayout.WEST);

        // Info panel
        JPanel infoPanel = new JPanel(new GridLayout(2, 3, 15, 5));
        infoPanel.setOpaque(false);

        Font infoFont = new Font("Arial", Font.PLAIN, 13);
        Font infoFontBold = new Font("Arial", Font.BOLD, 13);

        // First row
        JLabel revLabel = new JLabel("Total Revenue:", SwingConstants.RIGHT);
        revLabel.setFont(infoFont);
        revLabel.setForeground(Color.WHITE);
        infoPanel.add(revLabel);

        revenueLabel.setFont(infoFontBold);
        revenueLabel.setForeground(new Color(46, 204, 113));
        revenueLabel.setHorizontalAlignment(SwingConstants.LEFT);
        infoPanel.add(revenueLabel);

        timeLabel.setFont(infoFont);
        timeLabel.setForeground(new Color(200, 220, 255));
        infoPanel.add(timeLabel);

        // Second row
        JLabel statusText = new JLabel("System Status:", SwingConstants.RIGHT);
        statusText.setFont(infoFont);
        statusText.setForeground(Color.WHITE);
        infoPanel.add(statusText);

        statusLabel.setFont(infoFontBold);
        statusLabel.setForeground(new Color(52, 152, 219));
        statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
        infoPanel.add(statusLabel);

        JLabel floorTitle = new JLabel("Current Floor:", SwingConstants.RIGHT);
        floorTitle.setFont(infoFont);
        floorTitle.setForeground(Color.WHITE);
        infoPanel.add(floorTitle);

        header.add(infoPanel, BorderLayout.CENTER);

        // Floor indicator on right
        JPanel floorPanel = new JPanel(new BorderLayout());
        floorPanel.setOpaque(false);
        floorLabel.setFont(new Font("Arial", Font.BOLD, 18));
        floorLabel.setForeground(new Color(52, 152, 219));
        floorPanel.add(floorLabel, BorderLayout.CENTER);
        header.add(floorPanel, BorderLayout.EAST);

        return header;
    }

    private JPanel createCenterAreaPanel() {
        JPanel centerArea = new JPanel(new BorderLayout(15, 15));
        centerArea.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        centerArea.setOpaque(false);

        // Control panel at top
        JPanel controlPanel = createControlPanel();
        centerArea.add(controlPanel, BorderLayout.NORTH);

        // Main grid area with floor selector
        JPanel gridContainer = new JPanel(new BorderLayout(0, 10));
        gridContainer.setOpaque(false);

        // Floor navigation
        JPanel floorNav = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        floorNav.setOpaque(false);

        for (int i = 1; i <= FLOORS; i++) {
            JButton floorBtn = createFloorButton(i);
            floorNav.add(floorBtn);
        }

        gridContainer.add(floorNav, BorderLayout.NORTH);

        // Grid panel in scroll pane
        JScrollPane gridScrollPane = new JScrollPane(gridPanel);
        gridScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 220), 2),
                "Parking Layout - Floor 1",
                TitledBorder.CENTER, TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 16), new Color(30, 40, 60)
        ));
        gridScrollPane.getViewport().setBackground(Color.WHITE);
        gridScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        gridScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        gridContainer.add(gridScrollPane, BorderLayout.CENTER);
        centerArea.add(gridContainer, BorderLayout.CENTER);

        // Log panel on right
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setPreferredSize(new Dimension(400, 0));

        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 220), 2),
                "Activity Log",
                TitledBorder.CENTER, TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 14), new Color(30, 40, 60)
        ));

        // Log controls
        JPanel logControls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        logControls.setOpaque(false);
        JButton clearLogBtn = new ModernButton("🗑️ Clear Log", new Color(100, 100, 120));
        clearLogBtn.setPreferredSize(new Dimension(120, 35));
        clearLogBtn.addActionListener(_ -> logArea.setText(""));
        logControls.add(clearLogBtn);

        logPanel.add(logScrollPane, BorderLayout.CENTER);
        logPanel.add(logControls, BorderLayout.SOUTH);

        centerArea.add(logPanel, BorderLayout.EAST);

        return centerArea;
    }

    private JButton createFloorButton(int floorNum) {
        JButton floorBtn = new JButton("Floor " + floorNum);
        floorBtn.setFont(new Font("Arial", Font.BOLD, 12));
        floorBtn.setBackground(floorNum == 1 ? new Color(52, 152, 219) : new Color(220, 220, 220));
        floorBtn.setForeground(floorNum == 1 ? Color.WHITE : Color.BLACK);
        floorBtn.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        floorBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        floorBtn.addActionListener(_ -> {
            showFloor(floorNum);
            // Update button colors
            for (Component comp : floorBtn.getParent().getComponents()) {
                if (comp instanceof JButton btn) {
                    btn.setBackground(btn.getText().equals("Floor " + floorNum) ?
                            new Color(52, 152, 219) : new Color(220, 220, 220));
                    btn.setForeground(btn.getText().equals("Floor " + floorNum) ?
                            Color.WHITE : Color.BLACK);
                }
            }
        });
        return floorBtn;
    }

    private JPanel createControlPanel() {
        JPanel controlOuter = new JPanel();
        controlOuter.setLayout(new BoxLayout(controlOuter, BoxLayout.Y_AXIS));
        controlOuter.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(new Color(180, 190, 210), 2),
                        "Parking Operations Console",
                        TitledBorder.LEFT, TitledBorder.TOP,
                        new Font("Arial", Font.BOLD, 16), new Color(40, 50, 70)
                ),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        controlOuter.setBackground(new Color(250, 252, 255));

        // Row 1: Park Vehicle
        JPanel parkPanel = createParkPanel();
        controlOuter.add(parkPanel);
        controlOuter.add(Box.createVerticalStrut(10));

        // Row 2: Release & Search
        JPanel releaseSearchPanel = createReleaseSearchPanel();
        controlOuter.add(releaseSearchPanel);
        controlOuter.add(Box.createVerticalStrut(10));

        // Row 3: Additional Controls
        JPanel extraPanel = createExtraControlsPanel();
        controlOuter.add(extraPanel);

        return controlOuter;
    }

    private JPanel createParkPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setOpaque(false);

        panel.add(new JLabel("License Plate:"));
        panel.add(plateField);

        panel.add(new JLabel("Owner ID:"));
        panel.add(ownerField);

        panel.add(new JLabel("Vehicle Type:"));
        panel.add(typeBox);

        panel.add(new JLabel("Preferred Floor:"));
        panel.add(floorBox);

        panel.add(parkButton);
        panel.add(autoParkButton);

        return panel;
    }

    private JPanel createReleaseSearchPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setOpaque(false);

        panel.add(new JLabel("Ticket ID:"));
        panel.add(ticketField);

        panel.add(releaseButton);
        panel.add(copyLastTicketButton);
        panel.add(bulkReleaseButton);

        panel.add(Box.createHorizontalStrut(30));

        panel.add(new JLabel("Search Plate:"));
        panel.add(searchField);
        panel.add(findButton);

        return panel;
    }

    private JPanel createExtraControlsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setOpaque(false);

        panel.add(reportsButton);

        // Add quick stats
        JLabel statsLabel = new JLabel();
        statsLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        statsLabel.setForeground(new Color(100, 100, 100));
        panel.add(Box.createHorizontalStrut(20));
        panel.add(statsLabel);

        // Update stats in real-time
        javax.swing.Timer statsTimer = new javax.swing.Timer(2000, _ -> {
            int total = manager.getSpots().size();
            int occupied = manager.getActiveTickets().size();
            statsLabel.setText(String.format("Total: %d | Occupied: %d | Available: %d",
                    total, occupied, total - occupied));
        });
        statsTimer.setRepeats(true);
        statsTimer.start();

        return panel;
    }

    private void initializeParkingGrid() {
        if (gridPanel == null) {
            logMessage("CRITICAL: gridPanel is null in initializeParkingGrid. UI cannot be built.", "ERROR");
            return;
        }
        showFloor(1);
    }

    private void showFloor(int floor) {
        gridPanel.removeAll();
        spotPanelsMap.clear();

        List<ParkingSpot> spots = manager.getSpotsByFloor(floor);
        for (ParkingSpot spot : spots) {
            ParkingSpotPanel panel = new ParkingSpotPanel(spot);
            spotPanelsMap.put(spot.getSpotId(), panel);
            gridPanel.add(panel);
        }

        gridPanel.revalidate();
        gridPanel.repaint();
        floorLabel.setText("Floor " + floor);

        // Update the scroll pane title
        Container parent = gridPanel.getParent().getParent();
        if (parent instanceof JScrollPane scrollPane) {
            ((TitledBorder) scrollPane.getBorder()).setTitle("Parking Layout - Floor " + floor);
            scrollPane.repaint();
        }
    }

    private void setupActionListeners() {
        parkButton.addActionListener(this::actionParkVehicle);
        releaseButton.addActionListener(this::actionReleaseVehicle);
        findButton.addActionListener(this::actionFindVehicle);
        reportsButton.addActionListener(this::actionViewReports);
        copyLastTicketButton.addActionListener(this::actionCopyLastTicketId);
        autoParkButton.addActionListener(this::actionAutoPark);
        bulkReleaseButton.addActionListener(this::actionBulkRelease);

        plateField.addActionListener(this::actionParkVehicle);
        ticketField.addActionListener(this::actionReleaseVehicle);
        searchField.addActionListener(this::actionFindVehicle);
    }

    private void actionParkVehicle(ActionEvent e) {
        String plate = plateField.getText().trim().toUpperCase();
        String owner = ownerField.getText().trim();

        if (plate.length() < 3 || plate.length() > 10) {
            showErrorDialog("License plate must be 3-10 characters.", plateField);
            return;
        }

        VehicleType type = (VehicleType) typeBox.getSelectedItem();
        if (type == null) {
            showErrorDialog("Please select a vehicle type.", typeBox);
            return;
        }

        try {
            Vehicle vehicle = SmartParkingLotSimulator.createVehicle(type, plate, owner);

            // Get preferred floor from selection
            String floorSelection = (String) floorBox.getSelectedItem();
            int preferredFloor = 1;
            if (floorSelection != null && !floorSelection.isEmpty()) {
                preferredFloor = Integer.parseInt(floorSelection.replace("Floor ", ""));
            }

            Ticket ticket = manager.parkVehicleWithFloorPreference(vehicle, preferredFloor);

            this.lastParkedTicketId = ticket.getTicketId();

            // Use emoji based on success
            String emoji = "✅";
            if (ticket.getSpot().getFloor() != preferredFloor) {
                emoji = "🔄"; // Changed floor
                logMessage(emoji + " PARKED: " + plate + " (" + type.name() + ") in " +
                        ticket.getSpot().getSpotId() + " (Floor " + ticket.getSpot().getFloor() +
                        ") - Preferred floor was full", "INFO");
            } else {
                logMessage(emoji + " PARKED: " + plate + " (" + type.name() + ") in " +
                        ticket.getSpot().getSpotId() + " (Floor " + ticket.getSpot().getFloor() + ")", "SUCCESS");
            }

            logTicketId(ticket.getTicketId());
            clearParkInputFields();

            // Show which floor it's on
            showFloor(ticket.getSpot().getFloor());
            highlightSpotOnGrid(ticket.getSpot().getSpotId(), true);

        } catch (ParkingLotFullException | VehicleAlreadyParkedException ex) {
            showErrorDialog(ex.getMessage(), plateField);
            logMessage("❌ Park FAILED: " + plate + ". Reason: " + ex.getMessage(), "WARN");
        } catch (Exception ex) {
            showErrorDialog("Unexpected error during parking: " + ex.getMessage(), null);
            logMessage("❌ Park FAILED (Unexpected): " + plate + ". " + ex.getMessage(), "ERROR");
            LOGGER.log(Level.SEVERE, "Parking failed", ex);
        }

        updateOverallDisplay();
    }

    private void actionReleaseVehicle(ActionEvent e) {
        String ticketId = ticketField.getText().trim();
        if (ticketId.isEmpty()) {
            showErrorDialog("Ticket ID cannot be empty.", ticketField);
            return;
        }

        try {
            Ticket ticketToConfirm = manager.getActiveTickets().get(ticketId);
            if (ticketToConfirm == null) throw new InvalidTicketException(ticketId);

            // Enhanced confirmation dialog
            int confirm = JOptionPane.showConfirmDialog(this,
                    String.format("<html><b>Confirm Vehicle Release</b><br><br>" +
                                    "Vehicle: %s %s<br>" +
                                    "Spot: %s (Floor %d)<br>" +
                                    "Ticket ID: %s<br>" +
                                    "Entry Time: %s<br><br>" +
                                    "Are you sure you want to release this vehicle?</html>",
                            ticketToConfirm.getVehicle().getVehicleType().getIcon(),
                            ticketToConfirm.getVehicle().getLicensePlate(),
                            ticketToConfirm.getSpot().getSpotId(),
                            ticketToConfirm.getSpot().getFloor(),
                            ticketId,
                            ticketToConfirm.getEntryTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))),
                    "Confirm Release", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

            if (confirm != JOptionPane.YES_OPTION) {
                logMessage("⚠️ Release cancelled by user for Ticket: " + ticketId, "INFO");
                return;
            }

            double charges = manager.releaseVehicle(ticketId);
            Ticket releasedTicket = manager.getTicketHistory().stream()
                    .filter(t -> t.getTicketId().equals(ticketId))
                    .findFirst()
                    .orElse(null);

            if (releasedTicket != null) {
                logMessage(String.format("✅ RELEASED: %s from %s. Charges: %s%.0f",
                        releasedTicket.getVehicle().getLicensePlate(),
                        releasedTicket.getSpot().getSpotId(),
                        CURRENCY_SYMBOL, charges), "SUCCESS");

                showBillingSummaryDialog(releasedTicket, charges);
                ticketField.setText("");

                // Show the floor where spot was released
                showFloor(releasedTicket.getSpot().getFloor());
            } else {
                logMessage("❌ CRITICAL ERROR: Released ticket " + ticketId + " not found in history.", "ERROR");
            }
        } catch (InvalidTicketException ex) {
            showErrorDialog(ex.getMessage(), ticketField);
            logMessage("⚠️ Release FAILED: " + ticketId + ". Reason: " + ex.getMessage(), "WARN");
        } catch (Exception ex) {
            showErrorDialog("Unexpected error during release: " + ex.getMessage(), ticketField);
            logMessage("❌ Release FAILED (Unexpected): " + ticketId + ". " + ex.getMessage(), "ERROR");
            LOGGER.log(Level.SEVERE, "Release failed", ex);
        }

        updateOverallDisplay();
    }

    private void actionFindVehicle(ActionEvent e) {
        String plate = searchField.getText().trim().toUpperCase();
        if (plate.isEmpty()) {
            showErrorDialog("License plate for search cannot be empty.", searchField);
            return;
        }

        try {
            Ticket ticket = manager.findTicketByLicense(plate);
            Vehicle v = ticket.getVehicle();

            String info = String.format("<html><div style='font-family: Arial; padding: 10px;'>" +
                            "<h3 style='color: #3498db;'>🚗 Vehicle Found</h3>" +
                            "<table border='0' cellpadding='5'>" +
                            "<tr><td><b>Plate:</b></td><td>%s %s</td></tr>" +
                            "<tr><td><b>Type:</b></td><td>%s</td></tr>" +
                            "<tr><td><b>Owner:</b></td><td>%s</td></tr>" +
                            "<tr><td><b>Location:</b></td><td>Spot %s (Floor %d)</td></tr>" +
                            "<tr><td><b>Ticket ID:</b></td><td>%s</td></tr>" +
                            "<tr><td><b>Entry Time:</b></td><td>%s</td></tr>" +
                            "<tr><td><b>Duration:</b></td><td>%s</td></tr>" +
                            "</table></div></html>",
                    v.getVehicleType().getIcon(), v.getLicensePlate(),
                    v.getVehicleType().name(),
                    v.getOwnerId(),
                    ticket.getSpot().getSpotId(), ticket.getSpot().getFloor(),
                    ticket.getTicketId(),
                    v.getEntryTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    formatDuration(Duration.between(v.getEntryTime(), LocalDateTime.now()).toMinutes()));

            JOptionPane.showMessageDialog(this, info, "Vehicle Information", JOptionPane.INFORMATION_MESSAGE);
            logMessage("🔍 Found vehicle: " + plate + " in spot " + ticket.getSpot().getSpotId() + " (Floor " + ticket.getSpot().getFloor() + ")", "INFO");

            highlightSpotOnGrid(ticket.getSpot().getSpotId(), true);
            showFloor(ticket.getSpot().getFloor());
            searchField.setText("");
        } catch (VehicleNotFoundException ex) {
            showErrorDialog(ex.getMessage(), searchField);
            logMessage("⚠️ Search: " + ex.getMessage(), "WARN");
        } catch (Exception ex) {
            showErrorDialog("Unexpected error during search: " + ex.getMessage(), searchField);
            logMessage("❌ Search FAILED (Unexpected): " + plate + ". " + ex.getMessage(), "ERROR");
            LOGGER.log(Level.SEVERE, "Search failed", ex);
        }
    }

    private void actionViewReports(ActionEvent e) {
        createAndShowReportsDialog();
    }

    private void actionCopyLastTicketId(ActionEvent e) {
        if (lastParkedTicketId != null && !lastParkedTicketId.isEmpty()) {
            StringSelection stringSelection = new StringSelection(lastParkedTicketId);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, null);
            logMessage("📋 Copied to clipboard: " + lastParkedTicketId, "INFO");
            ticketField.setText(lastParkedTicketId);
            ticketField.requestFocusInWindow();
            ticketField.selectAll();
            logMessage("Ticket ID auto-filled in release field.", "INFO");
        } else {
            logMessage("⚠️ No recent ticket ID to copy.", "WARN");
            JOptionPane.showMessageDialog(this,
                    "<html>No vehicle has been parked yet in this session.<br>Park a vehicle first to get a ticket ID.</html>",
                    "No Ticket ID", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void actionAutoPark(ActionEvent e) {
        // Auto-park a random vehicle for demo/testing
        String[] plates = {"ABC123", "XYZ789", "DEF456", "GHI789", "JKL012"};
        String[] owners = {"John Doe", "Jane Smith", "Bob Johnson", "Alice Brown", "Charlie Wilson"};
        VehicleType[] types = VehicleType.values();

        Random rand = new Random();
        String plate = plates[rand.nextInt(plates.length)] + rand.nextInt(100);
        String owner = owners[rand.nextInt(owners.length)];
        VehicleType type = types[rand.nextInt(types.length)];

        plateField.setText(plate);
        ownerField.setText(owner);
        typeBox.setSelectedItem(type);
        floorBox.setSelectedIndex(rand.nextInt(FLOORS));

        // Trigger park action
        actionParkVehicle(e);
    }

    private void actionBulkRelease(ActionEvent e) {
        int count = manager.getActiveTickets().size();
        if (count == 0) {
            JOptionPane.showMessageDialog(this, "No vehicles to release.", "Bulk Release", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                String.format("Release all %d parked vehicles? This will generate %s revenue.",
                        count, CURRENCY_SYMBOL),
                "Confirm Bulk Release", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            double totalRevenue = 0;
            List<String> ticketIds = new ArrayList<>(manager.getActiveTickets().keySet());

            for (String ticketId : ticketIds) {
                try {
                    double charges = manager.releaseVehicle(ticketId);
                    totalRevenue += charges;
                    logMessage("Released: " + ticketId + " - " + CURRENCY_SYMBOL + charges, "INFO");
                } catch (Exception ex) {
                    logMessage("Failed to release: " + ticketId + " - " + ex.getMessage(), "ERROR");
                }
            }

            JOptionPane.showMessageDialog(this,
                    String.format("<html><b>Bulk Release Complete</b><br><br>" +
                                    "Released: %d vehicles<br>" +
                                    "Total Revenue: <font color='green'><b>%s%.0f</b></font></html>",
                            count, CURRENCY_SYMBOL, totalRevenue),
                    "Bulk Release Summary", JOptionPane.INFORMATION_MESSAGE);

            updateOverallDisplay();
        }
    }

    private void updateOverallDisplay() {
        int total = manager.getSpots().size();
        int occupied = manager.getActiveTickets().size();

        statusLabel.setText(String.format("Spots: %d Available | %d Occupied | %d Total",
                total - occupied, occupied, total));

        revenueLabel.setText(String.format("Revenue: %s%.0f",
                CURRENCY_SYMBOL, manager.getTotalRevenueFromHistory()));

        // Update all spot panels
        for (ParkingSpotPanel panel : spotPanelsMap.values()) {
            panel.updateDisplay();
        }

        // Update floor occupancy display
        updateFloorOccupancy();
    }

    private void updateFloorOccupancy() {
        // This method would update floor-specific statistics
        // Could be enhanced to show occupancy per floor
    }

    private void clearParkInputFields() {
        plateField.setText("");
        ownerField.setText("");
        typeBox.setSelectedIndex(0);
        plateField.requestFocusInWindow();
    }

    private void startClockAndStatusTimer() {
        javax.swing.Timer clockTimer = new javax.swing.Timer(1000, _ -> {
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            timeLabel.setText("Time: " + time);
        });
        clockTimer.start();
    }

    private void highlightSpotOnGrid(String spotId, boolean highlight) {
        ParkingSpotPanel panel = spotPanelsMap.get(spotId);
        if (panel != null) {
            if (highlight) {
                panel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.ORANGE, 3),
                        BorderFactory.createEmptyBorder(2, 2, 2, 2)
                ));

                if (highlightEffectTimer != null)
                    highlightEffectTimer.cancel();

                highlightEffectTimer = new java.util.Timer();
                highlightEffectTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        SwingUtilities.invokeLater(() -> highlightSpotOnGrid(spotId, false));
                    }
                }, 3000);
            } else {
                panel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.GRAY, 1),
                        BorderFactory.createEmptyBorder(5, 5, 5, 5)
                ));
            }
        }
    }

    private void logMessage(String message, String type) {
        String timestamp = LocalDateTime.now().format(LOG_TIMESTAMP_FORMAT);

        // Simple text logging (HTML not supported in JTextArea)
        logArea.append(String.format("[%s] %s: %s\n", timestamp, type, message));
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void logTicketId(String ticketId) {
        String timestamp = LocalDateTime.now().format(LOG_TIMESTAMP_FORMAT);
        logArea.append(String.format("[%s] 🎫 TICKET ISSUED: %s\n", timestamp, ticketId));
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void showErrorDialog(String message, Component fieldToFocus) {
        logMessage(message, "ERROR-UI");

        JOptionPane.showMessageDialog(this,
                "<html><div style='font-family: Arial; padding: 10px;'>" +
                        "<h3 style='color: #e74c3c;'>⚠️ Error</h3>" +
                        "<p>" + message + "</p></div></html>",
                "Input Error", JOptionPane.ERROR_MESSAGE);

        if (fieldToFocus instanceof JTextField textField) {
            SwingUtilities.invokeLater(() -> {
                textField.requestFocusInWindow();
                textField.selectAll();
            });
        } else if (fieldToFocus != null) {
            SwingUtilities.invokeLater(fieldToFocus::requestFocusInWindow);
        }
    }

    private String formatDuration(long totalMinutes) {
        if (totalMinutes < 0) return "0m";
        long d = TimeUnit.MINUTES.toDays(totalMinutes);
        long h = TimeUnit.MINUTES.toHours(totalMinutes) % 24;
        long m = totalMinutes % 60;
        if (d > 0) return String.format("%dd %dh %dm", d, h, m);
        if (h > 0) return String.format("%dh %dm", h, m);
        return String.format("%dm", m);
    }

    private void showBillingSummaryDialog(Ticket ticket, double charges) {
        Vehicle v = ticket.getVehicle();
        String summary = String.format(
                "<html><body style='font-family: Arial; padding: 15px;'>" +
                        "<div style='text-align: center;'>" +
                        "<h2 style='color: #27ae60; margin-top: 0;'>✅ Vehicle Released</h2>" +
                        "<div style='background: #f8f9fa; padding: 15px; border-radius: 8px; margin: 15px 0;'>" +
                        "<table border='0' cellpadding='8' style='margin: 0 auto;'>" +
                        "<tr><td align='right'><b>Plate:</b></td><td>%s %s</td></tr>" +
                        "<tr><td align='right'><b>Type:</b></td><td>%s</td></tr>" +
                        "<tr><td align='right'><b>Owner:</b></td><td>%s</td></tr>" +
                        "<tr><td align='right'><b>Spot:</b></td><td>%s (Floor %d)</td></tr>" +
                        "<tr><td align='right'><b>Entry:</b></td><td>%s</td></tr>" +
                        "<tr><td align='right'><b>Exit:</b></td><td>%s</td></tr>" +
                        "<tr><td align='right'><b>Duration:</b></td><td>%s</td></tr>" +
                        "</table></div>" +
                        "<div style='background: #27ae60; color: white; padding: 15px; border-radius: 8px;'>" +
                        "<h3 style='margin: 0;'>TOTAL CHARGES: %s%.0f</h3>" +
                        "</div>" +
                        "</div></body></html>",
                v.getVehicleType().getIcon(), v.getLicensePlate(),
                v.getVehicleType().name(),
                v.getOwnerId(),
                ticket.getSpot().getSpotId(), ticket.getSpot().getFloor(),
                ticket.getEntryTime().format(DateTimeFormatter.ofPattern("MM-dd HH:mm")),
                ticket.getExitTime().format(DateTimeFormatter.ofPattern("MM-dd HH:mm")),
                formatDuration(ticket.getDuration()),
                CURRENCY_SYMBOL, charges
        );

        JOptionPane.showMessageDialog(this, summary, "Billing Summary", JOptionPane.INFORMATION_MESSAGE);
    }

    private void createAndShowReportsDialog() {
        JDialog reportsDialog = new JDialog(this, "Parking System Reports", true);
        reportsDialog.setSize(900, 700);
        reportsDialog.setLocationRelativeTo(this);
        reportsDialog.setLayout(new BorderLayout());

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(30, 40, 60));
        header.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel title = new JLabel("📊 Parking System Reports");
        title.setFont(new Font("Arial", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);

        JLabel timestamp = new JLabel("Generated: " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        timestamp.setFont(new Font("Arial", Font.PLAIN, 12));
        timestamp.setForeground(Color.LIGHT_GRAY);
        header.add(timestamp, BorderLayout.EAST);

        reportsDialog.add(header, BorderLayout.NORTH);

        // Tabbed content
        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPane.setFont(new Font("Arial", Font.BOLD, 13));

        tabbedPane.addTab("🏢 Current Status", createReportPanel_CurrentStatus());
        tabbedPane.addTab("💰 Revenue Details", createReportPanel_Revenue());
        tabbedPane.addTab("📜 Parking History", createReportPanel_History());
        tabbedPane.addTab("📈 Floor Analysis", createReportPanel_FloorAnalysis());

        reportsDialog.add(tabbedPane, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton exportButton = new ModernButton("📥 Export Report", new Color(52, 152, 219));
        exportButton.addActionListener(_ -> JOptionPane.showMessageDialog(reportsDialog,
                "Report export feature would be implemented here.",
                "Export Report", JOptionPane.INFORMATION_MESSAGE));

        JButton closeButton = new ModernButton("✕ Close", new Color(100, 100, 100));
        closeButton.addActionListener(_ -> reportsDialog.dispose());

        footer.add(exportButton);
        footer.add(Box.createHorizontalStrut(10));
        footer.add(closeButton);

        reportsDialog.add(footer, BorderLayout.SOUTH);
        reportsDialog.setVisible(true);
    }

    private JComponent createReportPanel_CurrentStatus() {
        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Overall stats
        int totalSpots = manager.getSpots().size();
        int occupied = manager.getActiveTickets().size();
        double occupancyRate = totalSpots > 0 ? (occupied * 100.0 / totalSpots) : 0.0;

        JPanel statsPanel = new JPanel(new GridLayout(2, 2, 15, 15));
        statsPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        addStatCard(statsPanel, "Total Spots", String.valueOf(totalSpots), new Color(52, 152, 219));
        addStatCard(statsPanel, "Occupied Spots", String.valueOf(occupied), new Color(231, 76, 60));
        addStatCard(statsPanel, "Available Spots", String.valueOf(totalSpots - occupied), new Color(46, 204, 113));
        addStatCard(statsPanel, "Occupancy Rate", String.format("%.1f%%", occupancyRate),
                occupancyRate > 80 ? new Color(230, 126, 34) : new Color(155, 89, 182));

        content.add(statsPanel, BorderLayout.NORTH);

        // Vehicle type breakdown
        String[] cols = {"Vehicle Type", "Icon", "Count", "Percentage"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int column) {
                return column == 2 ? Integer.class : String.class;
            }
        };

        Map<VehicleType, Long> typeCounts = new EnumMap<>(VehicleType.class);
        manager.getActiveTickets().values().forEach(t ->
                typeCounts.merge(t.getVehicle().getVehicleType(), 1L, Long::sum));

        for (VehicleType vt : VehicleType.values()) {
            long count = typeCounts.getOrDefault(vt, 0L);
            double percentage = occupied > 0 ? (count * 100.0 / occupied) : 0.0;
            model.addRow(new Object[]{
                    vt.name(), vt.getIcon(), count,
                    String.format("%.1f%%", percentage)
            });
        }

        JTable table = new JTable(model);
        styleTable(table);
        table.setRowHeight(30);

        content.add(new JScrollPane(table), BorderLayout.CENTER);
        return content;
    }

    private JComponent createReportPanel_Revenue() {
        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        double totalRevenue = manager.getTotalRevenueFromHistory();

        // Revenue summary
        JPanel summaryPanel = new JPanel(new BorderLayout());
        summaryPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JLabel summaryLabel = new JLabel(String.format(
                "<html><div style='text-align: center;'>" +
                        "<h2 style='color: #27ae60; margin: 0;'>Total Revenue Generated</h2>" +
                        "<h1 style='color: #2c3e50; margin: 10px 0;'>%s%.0f</h1>" +
                        "<p style='color: #7f8c8d;'>From all completed parking sessions</p>" +
                        "</div></html>", CURRENCY_SYMBOL, totalRevenue));
        summaryLabel.setHorizontalAlignment(SwingConstants.CENTER);

        summaryPanel.add(summaryLabel, BorderLayout.CENTER);
        content.add(summaryPanel, BorderLayout.NORTH);

        // Revenue by vehicle type
        String[] cols = {"Vehicle Type", "Icon", "Rate Multiplier", "Revenue (" + CURRENCY_SYMBOL + ")", "Percentage"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        Map<VehicleType, Double> revenuePerType = new EnumMap<>(VehicleType.class);
        manager.getTicketHistory().forEach(t ->
                revenuePerType.merge(t.getVehicle().getVehicleType(), t.getChargesPaid(), Double::sum));

        for (VehicleType vt : VehicleType.values()) {
            double revenue = revenuePerType.getOrDefault(vt, 0.0);
            double percentage = totalRevenue > 0 ? (revenue * 100.0 / totalRevenue) : 0.0;
            model.addRow(new Object[]{
                    vt.name(), vt.getIcon(), vt.getRateMultiplier() + "x",
                    String.format("%.0f", revenue),
                    String.format("%.1f%%", percentage)
            });
        }

        JTable table = new JTable(model);
        styleTable(table);
        table.setRowHeight(30);

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        content.add(new JScrollPane(table), BorderLayout.CENTER);
        return content;
    }

    private JComponent createReportPanel_History() {
        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM-dd HH:mm");
        String[] cols = {"Ticket", "Plate", "Type", "Spot", "Floor", "Entry", "Exit", "Duration", "Charges ("+ CURRENCY_SYMBOL +")"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        List<Ticket> historyCopy = new ArrayList<>(manager.getTicketHistory());
        Collections.reverse(historyCopy);

        for (Ticket t : historyCopy) {
            Vehicle v = t.getVehicle();
            model.addRow(new Object[]{
                    t.getTicketId(),
                    v.getLicensePlate(),
                    v.getVehicleType().getIcon(),
                    t.getSpot().getSpotId(),
                    t.getSpot().getFloor(),
                    t.getEntryTime().format(dtf),
                    t.getExitTime() != null ? t.getExitTime().format(dtf) : "N/A",
                    formatDuration(t.getDuration()),
                    String.format("%.0f", t.getChargesPaid())
            });
        }

        JTable table = new JTable(model);
        styleTable(table);
        table.setRowHeight(25);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        int[] widths = {100, 80, 40, 50, 50, 90, 90, 80, 100};
        for (int i = 0; i < widths.length && i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        content.add(new JScrollPane(table), BorderLayout.CENTER);
        return content;
    }

    private JComponent createReportPanel_FloorAnalysis() {
        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Calculate floor statistics
        Map<Integer, Integer> floorCapacity = new HashMap<>();
        Map<Integer, Integer> floorOccupied = new HashMap<>();
        Map<Integer, Double> floorRevenue = new HashMap<>();

        for (ParkingSpot spot : manager.getSpots()) {
            int floor = spot.getFloor();
            floorCapacity.put(floor, floorCapacity.getOrDefault(floor, 0) + 1);
            if (!spot.isAvailable()) {
                floorOccupied.put(floor, floorOccupied.getOrDefault(floor, 0) + 1);
            }
        }

        for (Ticket ticket : manager.getTicketHistory()) {
            int floor = ticket.getSpot().getFloor();
            floorRevenue.put(floor, floorRevenue.getOrDefault(floor, 0.0) + ticket.getChargesPaid());
        }

        // Create floor statistics table
        String[] cols = {"Floor", "Total Spots", "Occupied", "Available", "Occupancy Rate", "Revenue (" + CURRENCY_SYMBOL + ")"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        for (int floor = 1; floor <= FLOORS; floor++) {
            int capacity = floorCapacity.getOrDefault(floor, 0);
            int occupied = floorOccupied.getOrDefault(floor, 0);
            int available = capacity - occupied;
            double occupancyRate = capacity > 0 ? (occupied * 100.0 / capacity) : 0.0;
            double revenue = floorRevenue.getOrDefault(floor, 0.0);

            model.addRow(new Object[]{
                    "Floor " + floor,
                    capacity,
                    occupied,
                    available,
                    String.format("%.1f%%", occupancyRate),
                    String.format("%.0f", revenue)
            });
        }

        JTable table = new JTable(model);
        styleTable(table);
        table.setRowHeight(30);

        content.add(new JScrollPane(table), BorderLayout.CENTER);

        // Add recommendation based on floor occupancy
        JPanel recommendationPanel = createRecommendationPanel(floorCapacity, floorOccupied);
        content.add(recommendationPanel, BorderLayout.SOUTH);

        return content;
    }

    private JPanel createRecommendationPanel(Map<Integer, Integer> floorCapacity, Map<Integer, Integer> floorOccupied) {
        JPanel recommendationPanel = new JPanel(new BorderLayout());
        recommendationPanel.setBorder(BorderFactory.createTitledBorder("Recommendations"));
        recommendationPanel.setBackground(Color.WHITE);

        StringBuilder recommendations = new StringBuilder("<html><b>Parking Recommendations:</b><ul>");

        for (int floor = 1; floor <= FLOORS; floor++) {
            int capacity = floorCapacity.getOrDefault(floor, 0);
            int occupied = floorOccupied.getOrDefault(floor, 0);
            double occupancyRate = capacity > 0 ? (occupied * 100.0 / capacity) : 0.0;

            if (occupancyRate > 90) {
                recommendations.append("<li>Floor ").append(floor).append(" is <font color='red'>nearly full</font> (").append(String.format("%.1f%%", occupancyRate)).append(")</li>");
            } else if (occupancyRate < 30) {
                recommendations.append("<li>Floor ").append(floor).append(" has <font color='green'>plenty of space</font> (").append(String.format("%.1f%%", occupancyRate)).append(")</li>");
            }
        }

        recommendations.append("</ul></html>");

        JLabel recommendationLabel = new JLabel(recommendations.toString());
        recommendationLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        recommendationPanel.add(recommendationLabel, BorderLayout.CENTER);

        return recommendationPanel;
    }

    private void addStatCard(JPanel panel, String title, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(color);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color.darker(), 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setForeground(Color.WHITE);

        JLabel valueLabel = new JLabel(value, SwingConstants.CENTER);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 24));
        valueLabel.setForeground(Color.WHITE);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        panel.add(card);
    }

    private void styleTable(JTable table) {
        table.setFont(new Font("Arial", Font.PLAIN, 12));
        table.setRowHeight(25);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(240, 240, 245));
        table.getTableHeader().setForeground(new Color(50, 50, 50));
        table.setGridColor(new Color(220, 220, 220));
        table.setSelectionBackground(new Color(52, 152, 219));
        table.setSelectionForeground(Color.WHITE);
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(1, 1));
    }

    @SuppressWarnings("unused")
    static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new SmartParkingGUI();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "FATAL ERROR DURING GUI INITIALIZATION", e);
                JOptionPane.showMessageDialog(null,
                        String.format("<html><b>A critical error occurred during application startup:</b><br><br>" +
                                "%s<br><br>The application will now exit.</html>", e.getMessage()),
                        "Fatal Startup Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}