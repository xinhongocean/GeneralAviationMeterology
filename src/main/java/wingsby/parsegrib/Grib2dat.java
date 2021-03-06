package wingsby.parsegrib;


import com.alibaba.fastjson.JSONObject;
//import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import wingsby.common.CacheDataFrame;
import wingsby.common.CalculateMermory;
import wingsby.common.GFSMem;
import wingsby.common.TimeMangerJob;
import wingsby.common.tools.GFSDateTimeTools;

import java.io.File;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;


/**
 * Created by desktop13 on 2017/12/20.
 */
public class Grib2dat implements TimeMangerJob {

    static final Logger logger=Logger.getLogger(Grib2dat.class);
    //配置文件信息
    static String inPath;
    static String outPath;
    static String fileStyle;
    public static String[] isobaric;
    public static String[] elements;
    public static String[] latlonArea;
    public static String[] fTime;
    static String[] START_PARSE_TIME;
    static float DTIME;
    public static String timeFlag;
    public DecimalFormat decimalFormat = new DecimalFormat("00");

    private static Grib2dat instance;

    private Grib2dat() {
        loadPath();
    }

    public static Grib2dat getInstance() {
        if (instance == null) {
            synchronized (Grib2dat.class) {
                if (instance == null) {
                    instance = new Grib2dat();
                }
            }
        }
        return instance;
    }

    Grib2dat(String inPath, String outPath) {
        setInPath(inPath);
        setOutPath(outPath);
    }

    //主逻辑方法parseGrib
    public boolean parseGrib() {
        logger.info("[dataprocess]开始处理数据");
//        List<String> fileNames_0=ChooseDir.getFileList(inPath,fileStyle);
        String[] datePath = getDatePath(inPath);
        if (datePath == null) return false;
        List<String> fileNames = new ArrayList<>();
        if (datePath.length >= 1) {
            List<String> fileNames1 = ChooseDir.getFilterFileList(datePath[0], fileStyle);//文件选择
            if (fileNames1 != null) fileNames.addAll(fileNames1);
        }
        if (datePath.length >= 2) {
            List<String> fileNames2 = ChooseDir.getFilterFileList(datePath[1], fileStyle);//文件选择
            if (fileNames2 != null) fileNames.addAll(fileNames2);
        }

        if (fileNames == null) {
            return false;
        }
        for (String fileName : fileNames) {
            if(cashIsExist( fileName ))continue;    //对cash重复部分不解析
            System.out.println("正在解析：" + fileName);
            try {
                long start = System.currentTimeMillis(); /////////////////////////////////////////////////////
                for (ElementName elementName : ElementName.values()) {
//                    if(!elementName.geteName().equals("Vertical_velocity_pressure_isobaric")) continue;
                    if(elementName.name().equals("PLCB")){
                        System.out.println();
                    }
                    System.out.println("正在解析：：：" + elementName.geteName());//////////////////////////////////////////////////
                    if (elementName.getType().equals( "isobaric") ){                   //多层
                        for (String isobaricName : isobaric) {
//                            if(isobaricName.equals("0975"))
                            System.out.println("正在解析：：：" + isobaricName);///////////////////////////////
                            CacheDataFrame cacheDataFrame = CacheDataFrame.getInstance();
                            GFSMem gfsMem = ReadGrib.getInstance().readGrib(fileName, elementName, isobaricName);
                            if (gfsMem != null) {
//                              String datName = fileName.split("\\\\")[fileName.split("\\\\").length - 1];
                                String datName = new File(fileName).getName();
                                String date = datName.substring(0, 10);
                                String VTI = datName.substring(datName.length() - 3);
//                              if(gfsMem.getWidth()==0)gfsMem.set
                                cacheDataFrame.pushData(gfsMem, date + VTI + "_" + isobaricName + "_" + elementName);
                            }
                        }
                    } else if(elementName.getType().equals( "surface")){                     //单层
                        CacheDataFrame cacheDataFrame = CacheDataFrame.getInstance();
                        if(elementName.equals(ElementName.TCDC)){
                            System.out.println();
                        }
                        GFSMem gfsMem = ReadGrib.getInstance().readGrib(fileName, elementName, "surface");
//                      String datName = fileName.split("\\\\")[fileName.split("\\\\").length - 1];
                        String datName = new File(fileName).getName();
                        String date = datName.substring(0, 10);
                        String VTI = datName.substring(datName.length() - 3);
                        cacheDataFrame.pushData(gfsMem, date + VTI + "_" + "9999" + "_" + elementName);
                    }
                }
//              CacheDataFrame cacheDataFrame = CacheDataFrame.getInstance();
                long end = System.currentTimeMillis();
                System.out.println("读取45数组：" + (end - start));
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.gc();
        }
        return true;
    }

    public boolean parseDat() {
        String[] datePath = getDatePath(Config.inPath_JBDB);
        List<String> fileNames = new ArrayList<>();
        List<String> fileNames1 = ChooseDir.getFilterFileList_JBDB(datePath[0], fileStyle);//文件选择
        List<String> fileNames2 = ChooseDir.getFilterFileList_JBDB(datePath[1], fileStyle);//文件选择
        if(fileNames1!=null)fileNames.addAll(fileNames1);
        if(fileNames2!=null)fileNames.addAll(fileNames2);
        if (fileNames == null) {
            return false;
        }
        long start = System.currentTimeMillis(); /////////////////////////////////////////////////////
        for (String fileName : fileNames) {
            System.out.println("正在解析：" + fileName);   ///////////////////////////////
            ElementName elementName = (fileName.contains("JB")) ? ElementName.JB : ElementName.DB;
            for (String isobaricName : Config.getIsobaric()) {
//                System.out.println("正在解析：：：" + isobaricName);///////////////////////////////
                CacheDataFrame cacheDataFrame = CacheDataFrame.getInstance();
                GFSMem gfsMem = ReadGrib.getInstance().readDat(fileName, elementName, isobaricName);  /////////
                String datName = new File(fileName).getName();
                String date = datName.substring(11, 24);
                cacheDataFrame.pushData(gfsMem, date + "_" + isobaricName + "_" + elementName);
            }
        }
        long end = System.currentTimeMillis();
        System.out.println("读取一个dat文件夹：耗时" + (end - start));
        return true;
    }


    //加载配置文件加载路径
    public static void loadPath() {
        inPath = Config.getInPath();
        outPath = Config.getOutPath();
        fileStyle = Config.getFileStyle();
        isobaric = Config.getIsobaric();
        latlonArea = Config.getLatlonArea();
        fTime = Config.getfTime();
        START_PARSE_TIME = Config.getStartParseTime();
        DTIME = Config.getDTIME();
    }

    public void setInPath(String inPath) {
        this.inPath = inPath;
    }

    public void setOutPath(String outPath) {
        this.outPath = outPath;
    }

    //都是前一天的文件
    public String[] getDatePath(String inpath) {
        Calendar c = Calendar.getInstance();//可以对每个时间域单独修改
        DateTime useDate = new DateTime(c.getTimeInMillis());
        String path = inpath + "/" + GFSDateTimeTools.getGFSDateTime(useDate, 0);
        String path2 = inpath + "/" + GFSDateTimeTools.getGFSDateTime(useDate, 12);
        return new String[]{path, path2};
    }

    //对缓存设置成去重功能
    private boolean cashIsExist(String fileName){
        CacheDataFrame cacheDataFrame = CacheDataFrame.getInstance();
        Map<String, CalculateMermory> map = null;
        Boolean flag = false;   //不存在

        String datName = new File(fileName).getName();
        String date = datName.substring(0, 10);
        String VTI = datName.substring(datName.length() - 3);
        try {
            Field field1=cacheDataFrame.getClass().getDeclaredField("map");
            field1.setAccessible(true);
            map = (Map)field1.get(cacheDataFrame);
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (String key:map.keySet()) {
            if(key.contains(date+VTI)) {
                flag = true;
                break;
            }
        }
        return flag;
    }
    @Override
    public void doJob() {
        try {
            logger.info("[dataprocess]开始处理数据");
            parseGrib();
            parseDat();
            JSONObject jsonObject=new JSONObject();
            jsonObject.put("total",Runtime.getRuntime().totalMemory()/1024./1024.+"MB");
            jsonObject.put("max",Runtime.getRuntime().maxMemory()/1024./1024.+"MB");
            jsonObject.put("free",Runtime.getRuntime().freeMemory()/1024./1024.+"MB");
            logger.info("[memory]"+jsonObject.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
