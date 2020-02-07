package es.uva.gsic.epsgrdf;

import java.util.Optional;

import org.apache.jena.rdf.model.Model;

public class IRI {

    public static final String PREFIX_MEASURES_ONTOLOGY = "smo";
    public static final String PREFIX_POSITION_ONTOLOGY = "spo";
    public static final String PREFIX_EPSG_ONTOLOGY = "epsg";
    public static final String PREFIX_COORDINATE = "coordinate";
    public static final String PREFIX_AXIS_DATA = "data.axis";
    public static final String PREFIX_AXIS_PROPERTY = "ontology.axis";
    public static final String PREFIX_PLOT = "plot";
    public static final String PREFIX_POSITION = "position";
    public static final String PREFIX_UNIT = "unit";
    public static final String PREFIX_GEOSPARQL = "wkt";

    public static final String MEASURES_BASE = "http://crossforest.eu/measures/";
    public static final String POSITION_BASE = "http://crossforest.eu/position/";
    public static final String EPSG_BASE = "http://epsg.w3id.org/";
    public static final String IFN_BASE = "http://crossforest.eu/ifn/";

    public static final String ONTOLOGY_PATH = "ontology/";
    public static final String DATA_PATH = "data/";
    public static final String COORDINATE_PATH = "coordinate/";
    public static final String UNIT_PATH = "unit/";
    public static final String AXIS_PATH = "axis/";
    public static final String CRS_PATH = "crs/";
    public static final String PLOT_PATH = "plot/";
    public static final String POSITION_PATH = "position/";
    public static final String POINT_PATH = "point/";

    public static final String MEASURES_ONTOLOGY = MEASURES_BASE + ONTOLOGY_PATH;
    public static final String POSITION_ONTOLOGY = POSITION_BASE + ONTOLOGY_PATH;
    public static final String EPSG_ONTOLOGY = EPSG_BASE + ONTOLOGY_PATH;
    public static final String GEOSPARQL_ONTOLOGY = "http://www.opengis.net/ont/geosparql#";

    public static final String POSITION_DATA = POSITION_BASE + DATA_PATH;
    public static final String MEASURES_DATA = MEASURES_BASE + DATA_PATH;
    public static final String EPSG_DATA = EPSG_BASE + DATA_PATH;
    public static final String IFN_DATA = IFN_BASE + DATA_PATH;

    public static final String COORDINATE_NAMESPACE = IFN_DATA + COORDINATE_PATH;
    public static final String POINT_NAMESPACE = IFN_DATA + POINT_PATH;
    public static final String AXIS_DATA_NAMESPACE = EPSG_DATA + AXIS_PATH;
    public static final String AXIS_PROPERTY_NAMESPACE = EPSG_ONTOLOGY + AXIS_PATH;
    public static final String PLOT_NAMESPACE = IFN_DATA + PLOT_PATH;
    public static final String POSITION_NAMESPACE = IFN_DATA + POSITION_PATH;
    public static final String CRS_NAMESPACE = EPSG_DATA + AXIS_PATH;
    public static final String UNIT_NAMESPACE = MEASURES_ONTOLOGY + UNIT_PATH;

    public static final String HAS_EPSG_CODE = EPSG_ONTOLOGY + "hasEPSGcode";
    public static final String HAS_CRS = POSITION_ONTOLOGY + "hasCoordinateReferenceSystem";
    public static final String HAS_POSITION = POSITION_ONTOLOGY + "hasPosition";
    public static final String HAS_REFERENCE = POSITION_ONTOLOGY + "hasReference";
    public static final String HAS_COORDINATE = POSITION_ONTOLOGY + "hasCoordinate";
    public static final String HAS_COORDINATE_1 = AXIS_PROPERTY_NAMESPACE + "1";
    public static final String HAS_COORDINATE_2 = AXIS_PROPERTY_NAMESPACE + "2";
    public static final String HAS_COORDINATE_47 = AXIS_PROPERTY_NAMESPACE + "47";
    public static final String HAS_COORDINATE_48 = AXIS_PROPERTY_NAMESPACE + "48";
    public static final String HAS_DIRECTION_IN_GRADIANS = POSITION_ONTOLOGY + "hasDirectionInGradians";
    public static final String HAS_DISTANCE_IN_METERS = POSITION_ONTOLOGY + "hasDistanceInMeters";
    public static final String HAS_AXIS = EPSG_ONTOLOGY + "hasAxis";
    public static final String HAS_VALUE = MEASURES_ONTOLOGY + "hasValue";
    public static final String HAS_UNIT = MEASURES_ONTOLOGY + "hasUnit";
    public static final String AS_WKT = GEOSPARQL_ONTOLOGY + "asWKT";
    public static final String HAS_PERIMETER = POSITION_ONTOLOGY + "hasPerimeter";

    public static final String CLASS_EGOCENTRIC_POSITION = POSITION_ONTOLOGY + "EgocentricPosition";

    public static final String UNIT_DEGREES = UNIT_NAMESPACE + "Degrees";
    public static final String UNIT_METERS = UNIT_NAMESPACE + "Meters";
    public static final String CRS_4326 = CRS_NAMESPACE + "4326";
    public static final String AXIS_1 = AXIS_DATA_NAMESPACE + "1";
    public static final String AXIS_2 = AXIS_DATA_NAMESPACE + "2";
    public static final String AXIS_47 = AXIS_DATA_NAMESPACE + "47";
    public static final String AXIS_48 = AXIS_DATA_NAMESPACE + "48";

    public static final String WKT_LITERAL = GEOSPARQL_ONTOLOGY + "wktLiteral";

    public static void setPrefixes(Model model) {
        model.setNsPrefix(PREFIX_MEASURES_ONTOLOGY, MEASURES_ONTOLOGY);
        model.setNsPrefix(PREFIX_POSITION_ONTOLOGY, POSITION_ONTOLOGY);
        model.setNsPrefix(PREFIX_EPSG_ONTOLOGY, EPSG_ONTOLOGY);
        model.setNsPrefix(PREFIX_GEOSPARQL, GEOSPARQL_ONTOLOGY);
        model.setNsPrefix(PREFIX_COORDINATE, COORDINATE_NAMESPACE);
        model.setNsPrefix(PREFIX_AXIS_DATA, AXIS_DATA_NAMESPACE);
        model.setNsPrefix(PREFIX_AXIS_PROPERTY, AXIS_PROPERTY_NAMESPACE);
        model.setNsPrefix(PREFIX_PLOT, PLOT_NAMESPACE);
        model.setNsPrefix(PREFIX_POSITION, POSITION_NAMESPACE);
        model.setNsPrefix(PREFIX_UNIT, UNIT_NAMESPACE);
    }

    public static String createIriPosition(String name, Optional<String> crs) {
        return POSITION_DATA + "position/" + name + (crs.isPresent() ? "-" + crs.get() : "");
    }

}