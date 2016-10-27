package com.minhld.jobex;

/**
 * this interface holds an abstraction of data parser. developer needs to override
 * this method to have the suitable parser for their data
 *
 * Created by minhld on 11/23/2015.
 */
public interface JobDataParser {
    /**
     * get class definition of the data object which you use to exchange
     * to other devices
     *
     * @return
     */
    public Class getDataClass();

    /**
     * read data object from local file
     *
     * @param path
     * @return
     * @throws Exception
     */
    public Object loadObject(String path) throws Exception;

    /**
     * convert data object to byte array - serialize object to bytes
     *
     * @param objData
     * @return
     * @throws Exception
     */
    public byte[] parseObjectToBytes(Object objData) throws Exception;

    /**
     * convert from byte array to data object - deserialize byte array to object<br>
     * some object type cannot be deserialized, for example Bitmap
     *
     * @param byteData
     * @return
     * @throws Exception
     */
    public Object parseBytesToObject(byte[] byteData) throws Exception;

    /**
     * this function will get one small piece of a data object. the position
     * and size of the part depends on the firstOffset and the lastOffset.
     *
     * @param objData
     * @param firstOffset (percentage - integer) the first offset of the data piece (0 -> 100)
     * @param lastOffset (percentage - integer) the last offset of the data piece (0 -> 100)
     * @return
     */
    public byte[] getPartFromObject(Object objData, int firstOffset, int lastOffset);

    /**
     * create the placeholder to accumulate the results from other devices
     * sending back to requester
     *
     * @param dataObject
     * @return
     */
    public Object createPlaceHolder(Object dataObject);

    /**
     * merge the partial data object to the placeholder, in other words, final result.
     *
     * @param placeholderObj
     * @param partObj
     * @param firstOffset (percentage - integer) the first offset of the piece (0 -> 100)
     * @param lastOffset (percentage - integer) the last offset of the piece (0 -> 100)
     * @return
     */
    public Object copyPartToHolder(Object placeholderObj, byte[] partObj, int firstOffset, int lastOffset);

    /**
     * destroyObject the data object from the memory. If the object is input/output stream
     * object, then it will be closed. If it is a bitmap, it will be recycled etc...
     *
     * @param data
     */
    public void destroyObject(Object data);

    /**
     * check if the data object is really destroyed or not
     *
     * @param data
     * @return
     */
    public boolean isObjectDestroyed(Object data);
}
