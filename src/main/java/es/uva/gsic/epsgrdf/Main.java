package es.uva.gsic.epsgrdf;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.RDF;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.referencing.CRS;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.geom.Geometry;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

import es.uva.gsic.epsgrdf.IRI;

public class Main {
    public static void main(String[] args) {

        // Read input file
        String outputName = "output";
        Model modelInput = ModelFactory.createDefaultModel();
        IRI.setPrefixes(modelInput);
        for (String fileName : args) {
            System.out.println("Reading file " + fileName);
            outputName = outputName + "_" + Paths.get(fileName).getFileName();
            InputStream in = FileManager.get().open(fileName);
            if (in == null) {
                throw new IllegalArgumentException("File: " + fileName + " not found");
            }
            modelInput.read(in, null, "TURTLE");
        }
        outputName = "wgs84.ttl";

        Model modelTemp = ModelFactory.createDefaultModel();
        System.out.println("Creating Geocentring Positions from Egocentric Positions");
        modelTemp.add(transformEgocentricPositions(modelInput));
        modelInput.add(modelTemp);
        System.out.println("Creating WGS84 Positions from Positions in other CRSs");
        modelTemp.add(transformGeocentricPositions(modelInput));
        System.out.println("Transforming WKT to set of triples");
        modelTemp.add(transformWKT(modelInput));

        System.out.println("Adding prefixes to model");
        IRI.setPrefixes(modelTemp);
        try {
            // modelInput.add(modelTemp);
            // modelInput.write(new FileWriter(new File("output.ttl")), "TURTLE");
            System.out.println("Writing " + outputName);
            modelTemp.write(new FileWriter(new File(outputName)), "TURTLE");
            System.out.println(outputName + " is written");
        } catch (IOException e) {
            System.out.println("Exception while writing " + outputName);
            e.printStackTrace();
        }
        System.out.println("THE END");
    }

    static Model transformWKT(Model modelInput) {
        Model modelTemp = ModelFactory.createDefaultModel();
        Property asWKT = modelInput.getProperty(IRI.AS_WKT);
        Property hasCoordinate = modelTemp.createProperty(IRI.HAS_COORDINATE);
        Property hasPerimeter = modelTemp.createProperty(IRI.HAS_PERIMETER);
        Property hasValue = modelInput.getProperty(IRI.HAS_VALUE);
        Property hasUnit = modelInput.getProperty(IRI.HAS_UNIT);
        Resource unitDegrees = modelTemp.createResource(IRI.UNIT_DEGREES);
        Property hasAxis = modelInput.getProperty(IRI.HAS_AXIS);
        Property hasCoordinate106 = modelTemp.createProperty(IRI.HAS_COORDINATE_106);
        Property hasCoordinate107 = modelTemp.createProperty(IRI.HAS_COORDINATE_107);
        Resource axis106 = modelTemp.createResource(IRI.AXIS_106);
        Resource axis107 = modelTemp.createResource(IRI.AXIS_107);
        StmtIterator iterPositions = modelInput.listStatements(null, asWKT, (RDFNode) null);
        WKTReader wktReader = new WKTReader();
        iterPositions.forEachRemaining(statementWKT -> {
            try {
                String positionURI = statementWKT.getSubject().getURI();
                // System.out.println(positionURI);
                Resource position = modelTemp.createResource(positionURI);
                List<Resource> points = new LinkedList<>();

                String wktInput = statementWKT.getLiteral().getLexicalForm();
                Geometry geometry = wktReader.read(wktInput);
                Arrays.stream(geometry.getCoordinates()).forEach(coordinate -> {
                    Literal[] coordinateLiterals = new Literal[2];
                    coordinateLiterals[0] = modelTemp.createTypedLiteral(Double.toString(coordinate.x),
                            XSDDatatype.XSDdecimal);
                    coordinateLiterals[1] = modelTemp.createTypedLiteral(Double.toString(coordinate.y),
                            XSDDatatype.XSDdecimal);
                    Resource point = modelTemp
                            .createResource(IRI.POINT_NAMESPACE + "-" + coordinate.x + "-" + coordinate.y);
                    Resource coordinateX = modelTemp.createResource(IRI.COORDINATE_NAMESPACE + "106-" + coordinate.x);
                    coordinateX.addProperty(hasValue, coordinateLiterals[0]);
                    coordinateX.addProperty(hasUnit, unitDegrees);
                    coordinateX.addProperty(hasAxis, axis106);
                    Resource coordinateY = modelTemp.createResource(IRI.COORDINATE_NAMESPACE + "107-" + coordinate.y);
                    coordinateY.addProperty(hasValue, coordinateLiterals[1]);
                    coordinateY.addProperty(hasUnit, unitDegrees);
                    coordinateY.addProperty(hasAxis, axis107);
                    point.addProperty(hasCoordinate106, coordinateLiterals[0]);
                    point.addProperty(hasCoordinate107, coordinateLiterals[1]);
                    point.addProperty(hasCoordinate, coordinateX);
                    point.addProperty(hasCoordinate, coordinateY);
                    points.add(point);
                });
                RDFList rdfPoints = modelTemp.createList(points.iterator());
                position.addProperty(hasPerimeter, rdfPoints);
            } catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });
        return modelTemp;
    }

    static Model transformEgocentricPositions(Model modelInput) {
        Model modelTemp = ModelFactory.createDefaultModel();

        Resource egocentricPosition = modelInput.getResource(IRI.CLASS_EGOCENTRIC_POSITION);
        Property hasReference = modelInput.getProperty(IRI.HAS_REFERENCE);
        Property hasCRS = modelInput.getProperty(IRI.HAS_CRS);
        Property hasEPSGcode = modelInput.getProperty(IRI.HAS_EPSG_CODE);
        Property hasCoordinate = modelTemp.createProperty(IRI.HAS_COORDINATE);
        Property hasCoordinate2 = modelInput.getProperty(IRI.HAS_COORDINATE_2);
        Property hasCoordinate1 = modelInput.getProperty(IRI.HAS_COORDINATE_1);
        Property hasDirectionInGradians = modelInput.getProperty(IRI.HAS_DIRECTION_IN_GRADIANS);
        Property hasDistanceInMeters = modelInput.getProperty(IRI.HAS_DISTANCE_IN_METERS);
        Property hasValue = modelInput.getProperty(IRI.HAS_VALUE);
        Property hasUnit = modelInput.getProperty(IRI.HAS_UNIT);
        Property hasAxis = modelInput.getProperty(IRI.HAS_AXIS);
        Resource unitMeters = modelTemp.createResource(IRI.UNIT_METERS);
        Resource axis2 = modelTemp.createResource(IRI.AXIS_2);
        Resource axis1 = modelTemp.createResource(IRI.AXIS_1);
        Property hasPosition = modelInput.getProperty(IRI.HAS_POSITION);

        StmtIterator iterPositions = modelInput.listStatements(null, RDF.type, egocentricPosition);
        iterPositions.forEachRemaining(statementIsEgocentricPosition -> {
            Resource oldPosition = statementIsEgocentricPosition.getSubject();
            Resource referencePosition = oldPosition.getProperty(hasReference).getObject().asResource();
            Resource crs = referencePosition.getProperty(hasCRS).getObject().asResource();
            int crsCode = crs.getProperty(hasEPSGcode).getInt();
            double[] referenceCoordinates = new double[2];
            Literal[] coordinateLiterals = new Literal[2];
            referenceCoordinates[0] = referencePosition.getProperty(hasCoordinate1).getInt();
            referenceCoordinates[1] = referencePosition.getProperty(hasCoordinate2).getInt();
            double direction = oldPosition.getProperty(hasDirectionInGradians).getInt();
            double distance = oldPosition.getProperty(hasDistanceInMeters).getDouble();

            double[] newCoordinates = egocentric2geocentric(referenceCoordinates, direction, distance);
            coordinateLiterals[0] = modelTemp.createTypedLiteral(Double.toString(newCoordinates[0]),
                    XSDDatatype.XSDdecimal);
            coordinateLiterals[1] = modelTemp.createTypedLiteral(Double.toString(newCoordinates[1]),
                    XSDDatatype.XSDdecimal);

            Resource coordinate1 = modelTemp.createResource(IRI.COORDINATE_NAMESPACE + "1-" + newCoordinates[0]);
            coordinate1.addProperty(hasValue, coordinateLiterals[0]);
            coordinate1.addProperty(hasUnit, unitMeters);
            coordinate1.addProperty(hasAxis, axis1);
            Resource coordinate2 = modelTemp.createResource(IRI.COORDINATE_NAMESPACE + "2-" + newCoordinates[1]);
            coordinate2.addProperty(hasValue, coordinateLiterals[1]);
            coordinate2.addProperty(hasUnit, unitMeters);
            coordinate2.addProperty(hasAxis, axis2);

            Resource newPosition = modelTemp.createResource(oldPosition.getURI() + "-" + crsCode);
            newPosition.addProperty(hasCRS, crs);
            newPosition.addProperty(hasCoordinate, coordinate1);
            newPosition.addProperty(hasCoordinate, coordinate2);
            newPosition.addProperty(hasCoordinate1, coordinateLiterals[0]);
            newPosition.addProperty(hasCoordinate2, coordinateLiterals[1]);

            // Add the position to the spatial entity
            StmtIterator iterSpatialEntities = modelInput.listStatements(null, hasPosition, oldPosition);
            iterSpatialEntities.forEachRemaining(statementHasPosition -> {
                String spatialEntityUri = statementHasPosition.getSubject().getURI();
                Resource spatialEntity = modelTemp.createResource(spatialEntityUri);
                spatialEntity.addProperty(hasPosition, newPosition);
                newPosition.addProperty(hasCRS, crs);
            });
        });

        return modelTemp;
    }

    static double[] egocentric2geocentric(double[] reference, double direction, double distance) {
        double[] newCoordinates = new double[2];
        double radians = Math.PI / 2.0 - direction * Math.PI / 200.0;
        newCoordinates[0] = Math.round((reference[0] + distance * Math.cos(radians)) * 100.0) / 100.0;
        newCoordinates[1] = Math.round((reference[1] + distance * Math.sin(radians)) * 100.0) / 100.0;
        return newCoordinates;
    }

    static Model transformGeocentricPositions(Model modelInput) {
        Model modelTemp = ModelFactory.createDefaultModel();
        Property hasCRS = modelInput.getProperty(IRI.HAS_CRS);
        Property hasPosition = modelInput.getProperty(IRI.HAS_POSITION);
        Property hasEPSGcode = modelInput.getProperty(IRI.HAS_EPSG_CODE);
        Resource crs4326 = modelInput.createResource(IRI.CRS_4326);
        Property hasCoordinate = modelTemp.createProperty(IRI.HAS_COORDINATE);
        Property hasValue = modelInput.getProperty(IRI.HAS_VALUE);
        Property hasUnit = modelInput.getProperty(IRI.HAS_UNIT);
        Resource unitDegrees = modelTemp.createResource(IRI.UNIT_DEGREES);
        Property hasAxis = modelInput.getProperty(IRI.HAS_AXIS);
        Property hasCoordinate106 = modelTemp.createProperty(IRI.HAS_COORDINATE_106);
        Property hasCoordinate107 = modelTemp.createProperty(IRI.HAS_COORDINATE_107);
        Resource axis106 = modelTemp.createResource(IRI.AXIS_106);
        Resource axis107 = modelTemp.createResource(IRI.AXIS_107);

        StmtIterator iterPositions = modelInput.listStatements(null, hasCRS, (RDFNode) null);
        iterPositions.forEachRemaining(statementHasCRS -> {
            if (!statementHasCRS.getObject().asResource().equals(crs4326)) {
                Resource oldPosition = statementHasCRS.getSubject();
                Resource newPosition = modelTemp.createResource(oldPosition.getURI() + "-4326");
                Resource oldCRS = statementHasCRS.getObject().asResource();
                int oldCRScode = oldCRS.getProperty(hasEPSGcode).getInt();
                double[] oldCoordinates = new double[2];
                double[] newCoordinates = new double[2];
                Literal[] coordinateLiterals = new Literal[2];

                StmtIterator iterCoordinates = modelInput.listStatements(oldPosition, hasCoordinate, (RDFNode) null);
                iterCoordinates.forEachRemaining(statementHasCoordinate -> {
                    Resource oldCoordinate = statementHasCoordinate.getObject().asResource();
                    double oldCoordinateValue = oldCoordinate.getProperty(hasValue).getObject().asLiteral().getDouble();
                    Resource oldAxis = oldCoordinate.getProperty(hasAxis).getObject().asResource();
                    switch (oldAxis.getURI()) {
                    case IRI.AXIS_1:
                        oldCoordinates[0] = oldCoordinateValue;
                        break;
                    case IRI.AXIS_2:
                        oldCoordinates[1] = oldCoordinateValue;
                        break;
                    default:
                        System.out.println(oldAxis.getURI());
                        // todo: throw exception
                        break;
                    }
                });
                CoordinateReferenceSystem sourceCRS, targetCRS;
                CoordinateOperation operation;
                DirectPosition ptSrc, ptDst;
                try {
                    sourceCRS = CRS.forCode("EPSG:" + oldCRScode);
                    targetCRS = CRS.forCode("EPSG:4326");
                    operation = CRS.findOperation(sourceCRS, targetCRS, null);
                    ptSrc = new DirectPosition2D(oldCoordinates[0], oldCoordinates[1]);
                    ptDst = operation.getMathTransform().transform(ptSrc, null);
                    newCoordinates[0] = Math.round(ptDst.getCoordinate()[0] * 1000000) / 1000000.0;
                    newCoordinates[1] = Math.round(ptDst.getCoordinate()[1] * 1000000) / 1000000.0;
                    Resource coordinate106 = modelTemp.createResource(IRI.IFN_DATA + "coordinate/106-" + newCoordinates[0]);
                    Resource coordinate107 = modelTemp.createResource(IRI.IFN_DATA + "coordinate/107-" + newCoordinates[1]);
                    newPosition.addProperty(hasCoordinate, coordinate106);
                    newPosition.addProperty(hasCoordinate, coordinate107);
                    coordinateLiterals[0] = modelTemp.createTypedLiteral(Double.toString(newCoordinates[0]),
                            XSDDatatype.XSDdecimal);
                    coordinateLiterals[1] = modelTemp.createTypedLiteral(Double.toString(newCoordinates[1]),
                            XSDDatatype.XSDdecimal);
                    newPosition.addProperty(hasCoordinate106, coordinateLiterals[0]);
                    newPosition.addProperty(hasCoordinate107, coordinateLiterals[1]);
                    coordinate106.addProperty(hasValue, coordinateLiterals[0]);
                    coordinate106.addProperty(hasUnit, unitDegrees);
                    coordinate106.addProperty(hasAxis, axis106);
                    coordinate107.addProperty(hasValue, coordinateLiterals[1]);
                    coordinate107.addProperty(hasUnit, unitDegrees);
                    coordinate107.addProperty(hasAxis, axis107);
                } catch (FactoryException | MismatchedDimensionException | TransformException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                StmtIterator iterSpatialEntities = modelInput.listStatements(null, hasPosition,
                        statementHasCRS.getSubject());
                iterSpatialEntities.forEachRemaining(statementHasPosition -> {
                    String spatialEntityUri = statementHasPosition.getSubject().getURI();
                    Resource spatialEntity = modelTemp.createResource(spatialEntityUri);
                    spatialEntity.addProperty(hasPosition, newPosition);
                    newPosition.addProperty(hasCRS, crs4326);
                });
            }
        });

        return modelTemp;
    }
}
