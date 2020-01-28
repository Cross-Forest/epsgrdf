package es.uva.gsic.epsgrdf;

import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.geometry.DirectPosition2D;

public class App {
    public static void main(String[] args) throws FactoryException, TransformException {
        CoordinateReferenceSystem sourceCRS = CommonCRS.WGS84.geographic();
        CoordinateReferenceSystem targetCRS = CommonCRS.WGS84.universal(40, 14);  // Get whatever zone is valid for 14°E.
        CoordinateOperation operation = CRS.findOperation(sourceCRS, targetCRS, null);

        // The above lines are costly and should be performed only once before to project many points.
        // In this example, the operation that we got is valid for coordinates in geographic area from
        // 12°E to 18°E (UTM zone 33) and 0°N to 84°N.

        DirectPosition ptSrc = new DirectPosition2D(40, 14);           // 40°N 14°E
        DirectPosition ptDst = operation.getMathTransform().transform(ptSrc, null);

        System.out.println("Source: " + ptSrc);
        System.out.println("Target: " + ptDst);
    }
}

