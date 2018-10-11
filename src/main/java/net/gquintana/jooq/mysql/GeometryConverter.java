/*
 * Default License
 */
package net.gquintana.jooq.mysql;

import org.locationtech.jts.geom.CoordinateSequenceFactory;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.impl.CoordinateArraySequenceFactory;
import org.locationtech.jts.io.ByteOrderValues;
import org.locationtech.jts.io.InputStreamInStream;
import org.locationtech.jts.io.OutputStreamOutStream;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.jooq.Converter;
import org.jooq.DataType;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultDataType;

/**
 * Converter JTS Geometry to/from byte array.
 * Inspired by
 * <a
 * href="http://www.dev-garden.org/2011/11/27/loading-mysql-spatial-data-with-jdbc-and-jts-wkbreader/">Loading
 * MySQL Spatial Data with JDBC and JTS WKBReader</a>. Using Object instead
 * of byte[] because of codegen.
 */
public class GeometryConverter implements Converter<Object, Geometry> {
    /**
     * Little endian or Big endian
     */
    private int byteOrder = ByteOrderValues.LITTLE_ENDIAN;
    /**
     * Precision model
     */
    private PrecisionModel precisionModel = new PrecisionModel();
    /**
     * Coordinate sequence factory
     */
    private CoordinateSequenceFactory coordinateSequenceFactory = CoordinateArraySequenceFactory.instance();
    /**
     * Output dimension
     */
    private int outputDimension = 2;
    /**
     * Convert byte array containing SRID + WKB Geometry into Geometry object
     */
    @Override
    public Geometry from(Object databaseObject) {
        final byte[] bytes = (byte[]) databaseObject;            
        if (bytes == null) return null;
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {            
            // Read SRID
            byte[] sridBytes = new byte[4];
            inputStream.read(sridBytes);
            int srid = ByteOrderValues.getInt(sridBytes, byteOrder);

            // Prepare Geometry factory
            GeometryFactory geometryFactory = new GeometryFactory(precisionModel, srid, coordinateSequenceFactory);

            // Read Geometry
            WKBReader wkbReader = new WKBReader(geometryFactory);
            Geometry geometry = wkbReader.read(new InputStreamInStream(inputStream));
            return geometry;
        } catch(IOException | ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Convert Geometry object into byte array containing SRID + WKB Geometry 
     */
    @Override
    public Object to(Geometry userObject) {
        if (userObject == null) return null;
        try(ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            // Write SRID
            byte[] sridBytes = new byte[4];
            ByteOrderValues.putInt(userObject.getSRID(), sridBytes, byteOrder);
            outputStream.write(sridBytes);
            // Write Geometry
            WKBWriter wkbWriter = new WKBWriter(outputDimension, byteOrder);
            wkbWriter.write(userObject, new OutputStreamOutStream(outputStream));
            return outputStream.toByteArray();
        } catch(IOException ioe) {
            throw new IllegalArgumentException(ioe);
        }
    }

    @Override
    public Class fromType() {
        return byte[].class;
    }

    @Override
    public Class<Geometry> toType() {
        return Geometry.class;
    }

    public int getByteOrder() {
        return byteOrder;
    }

    public void setByteOrder(int byteOrder) {
        this.byteOrder = byteOrder;
    }

    public PrecisionModel getPrecisionModel() {
        return precisionModel;
    }

    public void setPrecisionModel(PrecisionModel precisionModel) {
        this.precisionModel = precisionModel;
    }

    public CoordinateSequenceFactory getCoordinateSequenceFactory() {
        return coordinateSequenceFactory;
    }

    public void setCoordinateSequenceFactory(CoordinateSequenceFactory coordinateSequenceFactory) {
        this.coordinateSequenceFactory = coordinateSequenceFactory;
    }

    public int getOutputDimension() {
        return outputDimension;
    }

    public void setOutputDimension(int outputDimension) {
        this.outputDimension = outputDimension;
    }
    /**
     * Manually register de {@link org.jooq.impl.ConvertedDataType} to avoid the dreaded
     * "org.jooq.exception.SQLDialectNotSupportedException: 
     * Type class org.locationtech.jts.geom.Geometry is not supported in dialect SQL99"
     * @return ConvertedDataType
     */
    public DataType<Geometry> registerDataType() {
        DataType<Object> dataType = DefaultDataType.getDefaultDataType(SQLDialect.MYSQL, "geometry");
        return dataType.asConvertedDataType(this);
    }
}
