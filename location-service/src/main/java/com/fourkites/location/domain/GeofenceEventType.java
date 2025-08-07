package com.fourkites.location.domain;

/**
 * Sealed hierarchy for geofence event types using Java 21
 * Provides type-safe event handling with exhaustive pattern matching
 */
public sealed interface GeofenceEventType 
    permits GeofenceEventType.Enter, 
            GeofenceEventType.Exit, 
            GeofenceEventType.Dwell {
    
    String eventName();
    String description();
    boolean requiresNotification();
    
    record Enter(
        String geofenceId,
        java.time.Instant timestamp,
        double latitude,
        double longitude
    ) implements GeofenceEventType {
        @Override
        public String eventName() {
            return "GEOFENCE_ENTER";
        }
        
        @Override
        public String description() {
            return "Entered geofence " + geofenceId + " at " + timestamp;
        }
        
        @Override
        public boolean requiresNotification() {
            return true;
        }
    }
    
    record Exit(
        String geofenceId,
        java.time.Instant timestamp,
        double latitude,
        double longitude,
        java.time.Duration dwellTime
    ) implements GeofenceEventType {
        @Override
        public String eventName() {
            return "GEOFENCE_EXIT";
        }
        
        @Override
        public String description() {
            return "Exited geofence " + geofenceId + " after " + dwellTime.toMinutes() + " minutes";
        }
        
        @Override
        public boolean requiresNotification() {
            return true;
        }
    }
    
    record Dwell(
        String geofenceId,
        java.time.Instant timestamp,
        java.time.Duration dwellTime,
        double latitude,
        double longitude
    ) implements GeofenceEventType {
        @Override
        public String eventName() {
            return "GEOFENCE_DWELL";
        }
        
        @Override
        public String description() {
            return "Dwelling in geofence " + geofenceId + " for " + dwellTime.toMinutes() + " minutes";
        }
        
        @Override
        public boolean requiresNotification() {
            return dwellTime.toMinutes() > 30; // Notify if dwelling more than 30 minutes
        }
    }
    
    // Pattern matching helper for processing events
    static void processEvent(GeofenceEventType event, java.util.function.Consumer<String> notifier) {
        switch (event) {
            case Enter e when e.requiresNotification() -> {
                notifier.accept("Vehicle entered " + e.geofenceId());
            }
            case Exit e when e.requiresNotification() -> {
                notifier.accept("Vehicle exited " + e.geofenceId() + " after " + e.dwellTime().toMinutes() + " minutes");
            }
            case Dwell e when e.requiresNotification() -> {
                notifier.accept("Vehicle dwelling in " + e.geofenceId() + " for " + e.dwellTime().toMinutes() + " minutes");
            }
            default -> {
                // No notification required
            }
        }
    }
}