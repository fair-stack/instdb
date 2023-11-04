package cn.cnic.instdb.ftp.minimalftp.api;

public interface SpaceListener {

    String saveSpaceId(String username, String spaceId);

    String auth(String username, String path);

    void renameFile(String username, String sourceFileName, String targetFileName);

    void mkdirFile(String username, String fileName);

    void deleteFile(String username, String fileName, String type);

    void uploadFile(String username, String fileName);

    void downloadFile(String username, String fileName);
}
