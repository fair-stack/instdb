//package cn.cnic.instdb.utils;
//
//
//import com.alibaba.excel.EasyExcel;
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.extern.slf4j.Slf4j;
//
//
//import java.sql.Connection;
//import java.sql.SQLException;
//import java.util.Date;
//import java.util.List;
//import java.util.stream.Collectors;
//
//
//@Data
//@AllArgsConstructor
//@Slf4j
//public class ReadExcelThread extends Thread {
//
//    private String path;
//    private String isHeader;
//    private String sheetName;
//    private Integer sheetNum;
//
//
//    @Override
//    public void run() {
//
//        try {
//            EasyExcel.read(path, new NoModelDataListener("","",null)).sheet("Sheet1").headRowNumber(1).doRead();
//        } catch (Exception e) {
//
//        }
//
//    }
//}
