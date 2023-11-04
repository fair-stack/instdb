package cn.cnic.instdb.exception;

/**
 * @Description Exception enumeration
 * @Date 2019-08-16 14:20
 * @author jmal
 */
public enum ExceptionType {

    /***
     * Other abnormalities
     */
    SYSTEM_ERROR(-1, "Other abnormalities"),
    /***
     * success
     */
    SYSTEM_SUCCESS(0, "true"),
    /***
     * Missing parameter
     */
    MISSING_PARAMETERS(1, "Missing parameter"),
    /***
     * Incorrect time format
     */
    UNPARSEABLE_DATE(2, "Incorrect time format"),
    /***
     * lackHeader
     */
    MISSING_HEADERTERS(3, "lackHeader"),
    /***
     * The resource already exists
     */
    EXISTING_RESOURCES(4, "The resource already exists"),
    /***
     * Not logged in or login timeout
     */
    LOGIN_EXCEPRION(5, "Not logged in or login timeout"),
    /***
     * The issue of uploading data from the local system to the platform
     */
    UPLOAD_LSC_EXCEPRION(6, "The issue of uploading data from the local system to the platform"),

    /***
     * file does not exist
     */
    FILE_NOT_FIND(7, "file does not exist"),

    /**
     * No permission
     */
    PERMISSION_DENIED(8, "No permission"),

    /**
     * Custom exception
     */
    CUSTOM_EXCEPTION(9, "Custom exception"),

    /**
     * Wrong parameter value
     */
    PARAMETERS_VALUE(10, "Wrong parameter value"),

    /**
     * Wrong parameter value
     */
    OFFLINE(11, "off-line"),

    /**
     * user does not exist
     */
    USER_NOT_FIND(12, "user does not exist"),

    /***
     * directory does not exist
     */
    DIR_NOT_FIND(13, "directory does not exist"),

    /***
     * Decompression failed
     */
    FAIL_DECOMPRESS(14, "Decompression failed"),

    /***
     * Unrecognized file
     */
    UNRECOGNIZED_FILE(15, "Unrecognized file"),

    /***
     * Merge file failed
     */
    FAIL_MERGA_FILE(16, "Merge file failed"),

    /***
     * Delete file failed
     */
    FAIL_DELETE_FILE(17, "Delete file failed"),

    /***
     * Failed to upload file
     */
    FAIL_UPLOAD_FILE(18, "Failed to upload file"),

    /***
     * Prohibit access
     */
    ACCESS_FORBIDDEN(19, "Prohibit access"),

    /***
     * Space is full
     */
    SPACE_FULL(20, "Space is full");


    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    private int code;
    private String msg;

    ExceptionType(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
