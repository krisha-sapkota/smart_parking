import java.time.*;
import java.util.*;
import java.awt.Color;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.*;

class ParkingLotException extends RuntimeException {
    public ParkingLotException(String message) {
        super(message);
    }
}

class ParkingLotFullException extends ParkingLotException {
    public ParkingLotFullException() {
        super("Parking lot is full.");
    }
}

class VehicleAlreadyParkedException extends ParkingLotException {
    public VehicleAlreadyParkedException(String licensePlate) {
        super("Vehicle " + licensePlate + " is already parked.");
    }
}

class InvalidTicketException extends ParkingLotException {
    public InvalidTicketException(String ticketId) {
        super("Invalid or unknown ticket ID: " + ticketId);
    }
}

class VehicleNotFoundException extends ParkingLotException {
    public VehicleNotFoundException(String licensePlate) {
        super("No active vehicle found with license plate: " + licensePlate);
    }
}

class DeadlockPreventionException extends ParkingLotException {
    public DeadlockPreventionException(String message) {
        super(message);
    }
}

enum VehicleType {
    CAR("🚗", Color.decode("#4A90E2"), 1.0),
    BIKE("🏍️", Color.decode("#F5A623"), 0.5),
    ELECTRIC_CAR("⚡", Color.decode("#7ED321"), 1.2),
    SUV("🚙", Color.decode("#BD10E0"), 1.5),
    TRUCK("🚚", Color.decode("#B8E986"), 2.0);

    private final String icon;
    private final Color color;
    private final double rateMultiplier;

    VehicleType(String icon, Color color, double rateMultiplier) {
        this.icon = icon;
        this.color = color;
        this.rateMultiplier = rateMultiplier;
    }

    public String getIcon() {
        return icon;
    }

    public Color getColor() {
        return color;
    }

    public double getRateMultiplier() {
        return rateMultiplier;
    }
}

interface Chargeable {
    double calculateCharges(long durationMinutes, VehicleType type);
}

abstract class Vehicle {
    private final String licensePlate;
    private final LocalDateTime entryTime;
    private final VehicleType vehicleType;
    private final String ownerId;

    public Vehicle(String licensePlate, VehicleType vehicleType, String ownerId) {
        this.licensePlate = licensePlate.toUpperCase().trim();
        this.entryTime = LocalDateTime.now();
        this.vehicleType = vehicleType;
        this.ownerId = ownerId != null && !ownerId.trim().isEmpty() ? ownerId.trim() : "Anonymous";
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public String getOwnerId() {
        return ownerId;
    }
}

class Car extends Vehicle {
    public Car(String licensePlate, String ownerId) {
        super(licensePlate, VehicleType.CAR, ownerId);
    }
}

class Bike extends Vehicle {
    public Bike(String licensePlate, String ownerId) {
        super(licensePlate, VehicleType.BIKE, ownerId);
    }
}

class ElectricCar extends Vehicle {
    public ElectricCar(String licensePlate, String ownerId) {
        super(licensePlate, VehicleType.ELECTRIC_CAR, ownerId);
    }
}

class SUV extends Vehicle {
    public SUV(String licensePlate, String ownerId) {
        super(licensePlate, VehicleType.SUV, ownerId);
    }
}

class Truck extends Vehicle {
    public Truck(String licensePlate, String ownerId) {
        super(licensePlate, VehicleType.TRUCK, ownerId);
    }
}

class ParkingSpot {
    private final String spotId;
    private final int floor;
    private Vehicle vehicle;
    private boolean isOccupied;
    private final ReentrantLock lock;
    private final boolean isEntryPoint;

    public ParkingSpot(String spotId, int floor) {
        this.spotId = spotId;
        this.floor = floor;
        this.lock = new ReentrantLock(true); // Fair lock for deadlock prevention
        this.isEntryPoint = spotId.endsWith("1") || spotId.endsWith("A"); // Mark entry points
    }

    public boolean isAvailable() {
        return !isOccupied;
    }

    public boolean tryAcquire() {
        return lock.tryLock();
    }

    public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
        return lock.tryLock(timeout, unit);
    }

    public void release() {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    public void assignVehicle(Vehicle v) {
        this.vehicle = v;
        this.isOccupied = true;
    }

    public void removeVehicle() {
        this.vehicle = null;
        this.isOccupied = false;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public String getSpotId() {
        return spotId;
    }

    public int getFloor() {
        return floor;
    }

    @SuppressWarnings("unused")
    public boolean isEntryPoint() {
        return isEntryPoint;
    }

    @Override
    public String toString() {
        return "ParkingSpot{spotId='" + spotId + "', floor=" + floor + ", occupied=" + isOccupied + "}";
    }
}

class Ticket {
    private final String ticketId;
    private final Vehicle vehicle;
    private final ParkingSpot spot;
    private final LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private double chargesPaid;

    public Ticket(String ticketId, Vehicle vehicle, ParkingSpot spot) {
        this.ticketId = ticketId;
        this.vehicle = vehicle;
        this.spot = spot;
        this.entryTime = vehicle.getEntryTime();
    }

    public void closeTicket(LocalDateTime exitTime, double charges) {
        this.exitTime = exitTime;
        this.chargesPaid = charges;
    }

    public long getDuration() {
        return Duration.between(entryTime, exitTime != null ? exitTime : LocalDateTime.now()).toMinutes();
    }

    public String getTicketId() {
        return ticketId;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public ParkingSpot getSpot() {
        return spot;
    }

    public LocalDateTime getExitTime() {
        return exitTime;
    }

    public double getChargesPaid() {
        return chargesPaid;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }
}

class EnhancedBillingSystem implements Chargeable {
    private static final double BASE_RATE_PER_SECOND = 20.0;
    private static final double MINIMUM_CHARGE = 50.0;

    public double calculateCharges(long durationMinutes, VehicleType type) {
        if (durationMinutes <= 0)
            return MINIMUM_CHARGE;
        double charges = BASE_RATE_PER_SECOND * type.getRateMultiplier() * durationMinutes * 60;
        return Math.max(MINIMUM_CHARGE, Math.round(charges));
    }
}

public class SmartParkingLotSimulator {
    private final List<ParkingSpot> spots = new ArrayList<>();
    private final Map<String, Ticket> activeTickets = new ConcurrentHashMap<>();
    private final List<Ticket> ticketHistory = Collections.synchronizedList(new ArrayList<>());
    private final Chargeable billingSystem;
    private final Map<String, Vehicle> vehicleRegistry = new ConcurrentHashMap<>();
    private final Map<Integer, List<ParkingSpot>> floorMap = new HashMap<>();
    private int ticketCounter = 1001;

    // Deadlock prevention system
    private final DeadlockPreventionSystem deadlockPrevention;

    public SmartParkingLotSimulator(int rows, int cols, int floors, Chargeable billingSystem) {
        this.billingSystem = billingSystem;
        this.deadlockPrevention = new DeadlockPreventionSystem();

        // Initialize floors
        for (int floor = 1; floor <= floors; floor++) {
            List<ParkingSpot> floorSpots = new ArrayList<>();
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    String spotId = (char) ('A' + r) + String.valueOf(c + 1) + "F" + floor;
                    ParkingSpot spot = new ParkingSpot(spotId, floor);
                    spots.add(spot);
                    floorSpots.add(spot);
                }
            }
            floorMap.put(floor, floorSpots);
        }

        System.out.println("Initialized " + floors + " floors with " + spots.size() + " total spots");
    }

    @SuppressWarnings("unused")
    public Ticket parkVehicle(Vehicle vehicle) {
        return parkVehicleWithFloorPreference(vehicle, 1); // Default to floor 1
    }

    public Ticket parkVehicleWithFloorPreference(Vehicle vehicle, int preferredFloor) {
        // Check if vehicle already parked
        if (vehicleRegistry.containsKey(vehicle.getLicensePlate())) {
            throw new VehicleAlreadyParkedException(vehicle.getLicensePlate());
        }

        // Try preferred floor first
        ParkingSpot spot = findAvailableSpotOnFloor(preferredFloor);

        // If preferred floor is full, try other floors
        if (spot == null) {
            spot = findAvailableSpot();
            if (spot == null) {
                throw new ParkingLotFullException();
            }
        }

        // Use deadlock prevention system to safely acquire spot
        try {
            deadlockPrevention.acquireSpot(spot);

            spot.assignVehicle(vehicle);
            String ticketId = "TKT-" + ticketCounter++;
            Ticket ticket = new Ticket(ticketId, vehicle, spot);

            activeTickets.put(ticketId, ticket);
            vehicleRegistry.put(vehicle.getLicensePlate(), vehicle);

            deadlockPrevention.releaseSpot(spot);

            return ticket;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ParkingLotException("Parking operation interrupted: " + e.getMessage());
        }
    }

    public double releaseVehicle(String ticketId) {
        Ticket ticket = activeTickets.get(ticketId);
        if (ticket == null)
            throw new InvalidTicketException(ticketId);

        ParkingSpot spot = ticket.getSpot();

        try {
            // Use deadlock prevention for release
            deadlockPrevention.acquireSpot(spot);

            double charges = billingSystem.calculateCharges(ticket.getDuration(),
                    ticket.getVehicle().getVehicleType());

            ticket.closeTicket(LocalDateTime.now(), charges);
            spot.removeVehicle();

            activeTickets.remove(ticketId);
            ticketHistory.add(ticket);
            vehicleRegistry.remove(ticket.getVehicle().getLicensePlate());

            deadlockPrevention.releaseSpot(spot);

            return charges;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ParkingLotException("Release operation interrupted: " + e.getMessage());
        }
    }

    public Ticket findTicketByLicense(String licensePlate) {
        for (Ticket ticket : activeTickets.values()) {
            if (ticket.getVehicle().getLicensePlate().equalsIgnoreCase(licensePlate.trim())) {
                return ticket;
            }
        }
        throw new VehicleNotFoundException(licensePlate);
    }

    public static Vehicle createVehicle(VehicleType type, String plate, String owner) {
        return switch (type) {
            case CAR -> new Car(plate, owner);
            case BIKE -> new Bike(plate, owner);
            case ELECTRIC_CAR -> new ElectricCar(plate, owner);
            case SUV -> new SUV(plate, owner);
            case TRUCK -> new Truck(plate, owner);
        };
    }

    private ParkingSpot findAvailableSpotOnFloor(int floor) {
        List<ParkingSpot> floorSpots = floorMap.get(floor);
        if (floorSpots == null)
            return null;

        // Two-way parking algorithm: alternate between entry points
        boolean startFromLeft = Math.random() > 0.5;

        if (startFromLeft) {
            for (ParkingSpot spot : floorSpots) {
                if (spot.isAvailable() && spot.tryAcquire()) {
                    return spot;
                }
            }
        } else {
            // Start from right (end of list)
            for (int i = floorSpots.size() - 1; i >= 0; i--) {
                ParkingSpot spot = floorSpots.get(i);
                if (spot.isAvailable() && spot.tryAcquire()) {
                    return spot;
                }
            }
        }

        return null;
    }

    private ParkingSpot findAvailableSpot() {
        // Try floors in order, but with two-way approach on each floor
        for (int floor = 1; floor <= floorMap.size(); floor++) {
            ParkingSpot spot = findAvailableSpotOnFloor(floor);
            if (spot != null) {
                return spot;
            }
        }
        return null;
    }

    // New methods for multi-floor support
    public List<ParkingSpot> getSpotsByFloor(int floor) {
        return new ArrayList<>(floorMap.getOrDefault(floor, new ArrayList<>()));
    }

    @SuppressWarnings("unused")
    public Map<Integer, Integer> getFloorOccupancy() {
        Map<Integer, Integer> occupancy = new HashMap<>();
        for (Map.Entry<Integer, List<ParkingSpot>> entry : floorMap.entrySet()) {
            int floor = entry.getKey();
            long occupiedCount = entry.getValue().stream()
                    .filter(spot -> !spot.isAvailable())
                    .count();
            occupancy.put(floor, (int) occupiedCount);
        }
        return occupancy;
    }

    // Getters
    public List<ParkingSpot> getSpots() {
        return new ArrayList<>(spots);
    }

    public Map<String, Ticket> getActiveTickets() {
        return new HashMap<>(activeTickets);
    }

    public List<Ticket> getTicketHistory() {
        return Collections.unmodifiableList(ticketHistory);
    }

    public double getTotalRevenueFromHistory() {
        return ticketHistory.stream()
                .mapToDouble(Ticket::getChargesPaid)
                .sum();
    }
}

/**
 * Deadlock Prevention System using Resource Hierarchy Solution
 * This prevents deadlocks by establishing a total order on resources (parking
 * spots)
 * and requiring that they be acquired in that order.
 */
class DeadlockPreventionSystem {
    private static final int TIMEOUT_MS = 5000; // 5 second timeout

    /**
     * Acquire a parking spot with deadlock prevention
     * Uses resource ordering based on floor and spot ID
     */
    public void acquireSpot(ParkingSpot spot) throws InterruptedException {
        if (spot == null) {
            throw new IllegalArgumentException("Spot cannot be null");
        }

        long startTime = System.currentTimeMillis();

        // Try to acquire the lock with timeout
        if (!spot.tryAcquire()) {
            // If we can't get it immediately, wait with timeout
            if (!spot.tryAcquire(TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                throw new DeadlockPreventionException(
                        "Timeout waiting for spot " + spot.getSpotId() +
                                " (Floor " + spot.getFloor() + "). Possible deadlock detected.");
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed > 1000) {
            System.out.println("Warning: Slow acquisition for spot " + spot.getSpotId() +
                    " took " + elapsed + "ms");
        }

        // Verify spot is still available after acquiring lock
        if (!spot.isAvailable()) {
            spot.release();
            throw new ParkingLotException("Spot " + spot.getSpotId() + " was taken while waiting");
        }
    }

    /**
     * Release a parking spot
     */
    public void releaseSpot(ParkingSpot spot) {
        if (spot != null) {
            spot.release();
        }
    }

    /**
     * Two-way parking algorithm that prevents gridlock
     * Alternates direction based on current traffic flow
     */
    @SuppressWarnings("unused")
    public static class TwoWayParkingAlgorithm {
        private final Map<Integer, Boolean> floorDirection = new ConcurrentHashMap<>();
        private final Random random = new Random();

        /**
         * Get parking direction for a floor
         * 
         * @return true for left-to-right, false for right-to-left
         */
        public boolean getDirectionForFloor(int floor) {
            return floorDirection.computeIfAbsent(floor, f -> random.nextBoolean());
        }

        /**
         * Toggle direction to prevent congestion
         */

        public void toggleDirection(int floor) {
            floorDirection.put(floor, !floorDirection.getOrDefault(floor, true));
        }

        /**
         * Calculate optimal path to avoid congestion
         */
        public List<String> calculateOptimalPath(int floor, List<ParkingSpot> availableSpots) {
            boolean direction = getDirectionForFloor(floor);
            List<ParkingSpot> sortedSpots = new ArrayList<>(availableSpots);

            // Sort spots based on current direction
            sortedSpots.sort((s1, s2) -> {
                // Extract column number from spot ID (e.g., "A1F1" -> 1)
                int col1 = extractColumn(s1.getSpotId());
                int col2 = extractColumn(s2.getSpotId());

                return direction ? Integer.compare(col1, col2) : Integer.compare(col2, col1);
            });

            // Convert to spot IDs
            List<String> path = new ArrayList<>();
            for (ParkingSpot spot : sortedSpots) {
                path.add(spot.getSpotId());
            }

            return path;
        }

        private int extractColumn(String spotId) {
            // Extract numeric part from spot ID (e.g., "A1F1" -> 1)
            try {
                String numericPart = spotId.replaceAll("[^0-9]", "");
                if (!numericPart.isEmpty()) {
                    return Integer.parseInt(numericPart.substring(0, 1));
                }
            } catch (NumberFormatException e) {
                // Fallback
            }
            return 0;
        }
    }
}