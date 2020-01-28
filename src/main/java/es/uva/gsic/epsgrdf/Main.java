package es.uva.gsic.epsgrdf;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.RDF;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.referencing.CRS;
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
        Model modelInput = ModelFactory.createDefaultModel();
        IRI.setPrefixes(modelInput);
        for (String fileName : args) {
            System.out.println("Reading file " + fileName);
            InputStream in = FileManager.get().open(fileName);
            if (in == null) {
                throw new IllegalArgumentException("File: " + fileName + " not found");
            }
            modelInput.read(in, null, "TURTLE");
        }

        Model modelTemp = transformEgocentricPositions(modelInput);
        modelTemp.add(transformGeocentricPositions(modelInput));

        System.out.println("Writing output.ttl");
        IRI.setPrefixes(modelTemp);
        try {
            // modelInput.add(modelTemp);
            // modelInput.write(new FileWriter(new File("output.ttl")), "TURTLE");
            modelTemp.write(new FileWriter(new File("output.ttl")), "TURTLE");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static Model transformEgocentricPositions(Model modelInput) {
        Model modelTemp = ModelFactory.createDefaultModel();

        Resource egocentricPosition = modelInput.getResource(IRI.CLASS_EGOCENTRIC_POSITION);
        Property hasReference = modelInput.getProperty(IRI.HAS_REFERENCE);
        Property hasCRS = modelInput.getProperty(IRI.HAS_CRS);
        Property hasEPSGcode = modelInput.getProperty(IRI.HAS_EPSG_CODE);
        Property hasCoordinate = modelTemp.createProperty(IRI.HAS_COORDINATE);
        Property hasCoordinate47 = modelInput.getProperty(IRI.HAS_COORDINATE_47);
        Property hasCoordinate48 = modelInput.getProperty(IRI.HAS_COORDINATE_48);
        Property hasDirectionInGradians = modelInput.getProperty(IRI.HAS_DIRECTION_IN_GRADIANS);
        Property hasDistanceInMeters = modelInput.getProperty(IRI.HAS_DISTANCE_IN_METERS);
        Property hasValue = modelInput.getProperty(IRI.HAS_VALUE);
        Property hasUnit = modelInput.getProperty(IRI.HAS_UNIT);
        Property hasAxis = modelInput.getProperty(IRI.HAS_AXIS);
        Resource unitMeters = modelTemp.createResource(IRI.UNIT_METERS);
        Resource axis47 = modelTemp.createResource(IRI.AXIS_47);
        Resource axis48 = modelTemp.createResource(IRI.AXIS_48);

        StmtIterator iterPositions = modelInput.listStatements(null, RDF.type, egocentricPosition);
        iterPositions.forEachRemaining(statementIsEgocentricPosition -> {
            Resource oldPosition = statementIsEgocentricPosition.getSubject();
            Resource referencePosition = oldPosition.getProperty(hasReference).getObject().asResource();
            Resource crs = referencePosition.getProperty(hasCRS).getObject().asResource();
            int crsCode = crs.getProperty(hasEPSGcode).getInt();
            double[] referenceCoordinates = new double[2];
            referenceCoordinates[0] = referencePosition.getProperty(hasCoordinate48).getInt();
            referenceCoordinates[1] = referencePosition.getProperty(hasCoordinate47).getInt();
            double direction = oldPosition.getProperty(hasDirectionInGradians).getInt();
            double distance = oldPosition.getProperty(hasDistanceInMeters).getDouble();
            
            double[] newCoordinates = egocentric2geocentric(referenceCoordinates, direction, distance);
            Resource coordinate48 = modelTemp.createResource(IRI.IFN_DATA + "coordinate/48-" + newCoordinates[0]);
            coordinate48.addLiteral(hasValue, newCoordinates[0]);
            coordinate48.addProperty(hasUnit, unitMeters);
            coordinate48.addProperty(hasAxis, axis48);
            Resource coordinate47 = modelTemp.createResource(IRI.IFN_DATA + "coordinate/47-" + newCoordinates[1]);
            coordinate47.addLiteral(hasValue, newCoordinates[1]);
            coordinate47.addProperty(hasUnit, unitMeters);
            coordinate47.addProperty(hasAxis, axis47);

            Resource newPosition = modelTemp.createResource(oldPosition.getURI() + "-" + crsCode);
            newPosition.addProperty(hasCRS, crs);
            newPosition.addProperty(hasCoordinate, coordinate48);
            newPosition.addProperty(hasCoordinate, coordinate47);
            newPosition.addLiteral(hasCoordinate48, newCoordinates[0]);
            newPosition.addLiteral(hasCoordinate47, newCoordinates[1]);

        });

        return modelTemp;
    }

    static double[] egocentric2geocentric(double[] reference, double direction, double distance) {
        double[] newCoordinates = new double[2];
        double radians = Math.PI / 2 - direction * Math.PI / 200;
        newCoordinates[0] = Math.floor(reference[0] + distance * Math.sin(radians));
        newCoordinates[1] = Math.floor(reference[1] + distance * Math.cos(radians));
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
        Property hasCoordinate1 = modelTemp.createProperty(IRI.HAS_COORDINATE_1);
        Property hasCoordinate2 = modelTemp.createProperty(IRI.HAS_COORDINATE_2);
        Resource axis1 = modelTemp.createResource(IRI.AXIS_1);
        Resource axis2 = modelTemp.createResource(IRI.AXIS_2);

        StmtIterator iterPositions = modelInput.listStatements(null, hasCRS, (RDFNode) null);
        System.out.println("Adding triples");
        iterPositions.forEachRemaining(statementHasCRS -> {
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
                case IRI.AXIS_48:
                    oldCoordinates[0] = oldCoordinateValue;
                    break;
                case IRI.AXIS_47:
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
                Resource coordinate1 = modelTemp.createResource(IRI.IFN_DATA + "coordinate/1-" + newCoordinates[0]);
                Resource coordinate2 = modelTemp.createResource(IRI.IFN_DATA + "coordinate/2-" + newCoordinates[1]);
                newPosition.addProperty(hasCoordinate, coordinate1);
                newPosition.addProperty(hasCoordinate, coordinate2);
                coordinateLiterals[0] = modelTemp.createTypedLiteral(Double.toString(newCoordinates[0]),
                        XSDDatatype.XSDdecimal);
                coordinateLiterals[1] = modelTemp.createTypedLiteral(Double.toString(newCoordinates[1]),
                        XSDDatatype.XSDdecimal);
                newPosition.addProperty(hasCoordinate1, coordinateLiterals[0]);
                newPosition.addProperty(hasCoordinate2, coordinateLiterals[1]);
                coordinate1.addProperty(hasValue, coordinateLiterals[0]);
                coordinate1.addProperty(hasUnit, unitDegrees);
                coordinate1.addProperty(hasAxis, axis1);
                coordinate2.addProperty(hasValue, coordinateLiterals[1]);
                coordinate2.addProperty(hasUnit, unitDegrees);
                coordinate2.addProperty(hasAxis, axis2);
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
        });

        return modelTemp;
    }
}