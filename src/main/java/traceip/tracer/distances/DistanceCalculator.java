package traceip.tracer.distances;

/**
 * Method Object para encapsular el calculo de la distancia entre 2 geolocalizaciones
 */
public class DistanceCalculator {

    private final GeoLocation loc1;
    private final GeoLocation loc2;

    public DistanceCalculator(GeoLocation loc1, GeoLocation loc2) {
        this.loc1 = loc1;
        this.loc2 = loc2;
    }

    public Long calculate() {
        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(loc2.getLatitude() - loc1.getLatitude());
        double lonDistance = Math.toRadians(loc2.getLongitude() - loc1.getLongitude());
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(loc1.getLatitude())) * Math.cos(Math.toRadians(loc2.getLatitude()))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c;

        distance = Math.pow(distance, 2);

        return Math.round(Math.sqrt(distance));
    }
}
