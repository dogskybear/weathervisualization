package com.adc.disasterforecast.task;

import com.adc.disasterforecast.dao.FeiteDataDAO;
import com.adc.disasterforecast.entity.FeiteDataEntity;
import com.adc.disasterforecast.global.FeiteRegionInfo;
import com.adc.disasterforecast.global.FeiteTaskName;
import com.adc.disasterforecast.global.JsonServiceURL;
import com.adc.disasterforecast.tools.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.ArrayList;
import java.util.List;

@Component
public class FeiteTask {
    // logger for FeiteTask
    private static final Logger logger = LoggerFactory.getLogger(FeiteTask.class);

    // dao Autowired
    @Autowired
    private FeiteDataDAO feiteDataDAO;

    @PostConstruct
    public void countRegionDiff() {
        logger.info(String.format("began task：%s", FeiteTaskName.FEITE_REGION_DIFF));

        JSONObject xh = new JSONObject();
        xh.put("area", FeiteRegionInfo.XH_AREA);
        xh.put("population", FeiteRegionInfo.XH_POPULATION);
        xh.put("acreage", FeiteRegionInfo.XH_ACREAGE);

        JSONObject cm = new JSONObject();
        cm.put("area", FeiteRegionInfo.CM_AREA);
        cm.put("population", FeiteRegionInfo.CM_POPULATION);
        cm.put("acreage", FeiteRegionInfo.CM_ACREAGE);

        JSONArray diffValue = new JSONArray();
        diffValue.add(xh);
        diffValue.add(cm);

        FeiteDataEntity diff = new FeiteDataEntity();
        diff.setName(FeiteTaskName.FEITE_REGION_DIFF);
        diff.setValue(diffValue);

        feiteDataDAO.updateFeiteDataByName(diff);
    }

    @PostConstruct
    public void countRegionRainfallDiff() {
        String baseUrl = JsonServiceURL.AUTO_STATION_JSON_SERVICE_URL + "GetAutoStationDataByDatetime_5mi_SanWei/";

        logger.info(String.format("began task：%s", FeiteTaskName.FEITE_REGION_RAINFALL_DIFF));

        Map<String, JSONObject> xhRainfalls = new HashMap<>();
        Map<String, JSONObject> cmRainfalls = new HashMap<>();

        for (int i = 0; i < 41; i++) {
            String beginDate = DateHelper.getPostponeDateByHour(2013, 10, 6, 19, 0, 0, i);
            String endDate = DateHelper.getPostponeDateByHour(2013, 10, 6, 20, 0, 0, i);

            String url = baseUrl + beginDate + "/" + endDate + "/1";
            JSONObject rainfallJson = HttpHelper.getDataByURL(url);
            JSONArray rainfallData = (JSONArray) rainfallJson.get("Data");

            for (Object obj : rainfallData) {
                JSONObject rainfall = (JSONObject) obj;
                String stationName = (String) rainfall.get("STATIONNAME");

                if (FeiteRegionInfo.XH_STATION_NAME.equals(stationName)) {
                    xhRainfalls.put(endDate, rainfall);
                } else if (FeiteRegionInfo.CM_STATION_NAME.equals(stationName)) {
                    cmRainfalls.put(endDate, rainfall);
                }
            }
        }
        JSONArray alarms = feiteDataDAO.findFeiteDataByName("ALARM_STAGE").getValue();

        for (Object obj : alarms) {
            Map<String, String> alarm = (Map<String, String>) obj;
            String beginDate = alarm.get("beginDate").substring(0, 10) + "0000";
            String endDate = alarm.get("endDate").substring(0, 10) + "0000";
            String alarmId = alarm.get("alarmId");
            countRegionRainfallDiffByAlarmId(beginDate, endDate, alarmId, xhRainfalls, cmRainfalls);
        }
    }

    private void countRegionRainfallDiffByAlarmId(String beginDate, String endDate, String alarmId, Map<String, JSONObject> xhRainfalls, Map<String, JSONObject> cmRainfalls) {
        JSONArray xhRainfallsByAlarmId = new JSONArray();
        JSONArray cmRainfallsByAlarmId = new JSONArray();

        int delayHour = 0;
        String date = "";
        while (!date.equals(endDate)) {
            date = DateHelper.getPostponeDateByHour(beginDate, delayHour);

            addRainfall(xhRainfallsByAlarmId, xhRainfalls.get(date), delayHour);
            addRainfall(cmRainfallsByAlarmId, cmRainfalls.get(date), delayHour);

            delayHour++;
        }

        JSONArray rainfallValue = new JSONArray();

        JSONObject xhRainfallById = new JSONObject();
        xhRainfallById.put("area", FeiteRegionInfo.XH_AREA);
        xhRainfallById.put("value", xhRainfallsByAlarmId);

        JSONObject cmRainfallById = new JSONObject();
        cmRainfallById.put("area", FeiteRegionInfo.CM_AREA);
        cmRainfallById.put("value", cmRainfallsByAlarmId);

        rainfallValue.add(xhRainfallById);
        rainfallValue.add(cmRainfallById);

        FeiteDataEntity rainfall = new FeiteDataEntity();
        rainfall.setName(FeiteTaskName.FEITE_REGION_RAINFALL_DIFF);
        rainfall.setValue(rainfallValue);
        rainfall.setAlarmId(alarmId);

        feiteDataDAO.updateFeiteDataByNameAndAlarmId(rainfall);
    }

    private void addRainfall(JSONArray areaRainfalls, JSONObject rainfall, int date) {
        JSONObject jsonObject = new JSONObject();
        double rainHour = Double.parseDouble((String) rainfall.get("RAINHOUR"));

        jsonObject.put("date", date);
        jsonObject.put("value", (int) (rainHour * 10));

        areaRainfalls.add(jsonObject);
    }

    @PostConstruct
    public void countRegionDisasterDiff() {
        logger.info(String.format("began task：%s", FeiteTaskName.FEITE_REGION_DISASTER_NUM_DIFF));
        logger.info(String.format("began task：%s", FeiteTaskName.FEITE_REGION_DISASTER_DENSITY_DIFF));
        logger.info(String.format("began task：%s", FeiteTaskName.FEITE_REGION_DISASTER_TYPE_DIFF));

        JSONArray alarms = feiteDataDAO.findFeiteDataByName("ALARM_STAGE").getValue();

        for (Object obj : alarms) {
            Map<String, String> alarm = (Map<String, String>) obj;
            String beginDate = alarm.get("beginDate");
            String endDate = alarm.get("endDate");
            String alarmId = alarm.get("alarmId");
            countRegionDisasterDiffByAlarmId(beginDate, endDate, alarmId);
        }
    }

    private void countRegionDisasterDiffByAlarmId(String beginDate, String endDate, String alarmId) {
        String url = JsonServiceURL.ALARM_JSON_SERVICE_URL + "GetDisasterHistory/" + beginDate + "/" + endDate;

        JSONObject obj = HttpHelper.getDataByURL(url);

        // 统计两地受灾数
        List<JSONObject> xhDisasters = new ArrayList<>();
        List<JSONObject> cmDisasters = new ArrayList<>();

        JSONArray disasters = (JSONArray) obj.get("Data");
        for (Object disaster : disasters) {
            JSONObject disasterData = (JSONObject) disaster;
            String disasterDistrict = (String) disasterData.get("Disaster_District");

            if (FeiteRegionInfo.XH_DISTRICT.equals(disasterDistrict)) {
                xhDisasters.add(disasterData);
            } else if (FeiteRegionInfo.CM_DISTRICT.equals(disasterDistrict)) {
                cmDisasters.add(disasterData);
            }
        }

        JSONObject xhNumDiff = new JSONObject();
        xhNumDiff.put("area", FeiteRegionInfo.XH_AREA);
        xhNumDiff.put("value", xhDisasters.size());

        JSONObject cmNumDiff = new JSONObject();
        cmNumDiff.put("area", FeiteRegionInfo.CM_AREA);
        cmNumDiff.put("value", cmDisasters.size());

        JSONArray numDiffValue = new JSONArray();
        numDiffValue.add(xhNumDiff);
        numDiffValue.add(cmNumDiff);

        FeiteDataEntity numDiff = new FeiteDataEntity();
        numDiff.setName(FeiteTaskName.FEITE_REGION_DISASTER_NUM_DIFF);
        numDiff.setValue(numDiffValue);
        numDiff.setAlarmId(alarmId);

        feiteDataDAO.updateFeiteDataByNameAndAlarmId(numDiff);

        // 统计两地受灾密度
        JSONObject xhDensityDiff = new JSONObject();
        xhDensityDiff.put("area", FeiteRegionInfo.XH_AREA);
        xhDensityDiff.put("value", ((double) xhDisasters.size()) / FeiteRegionInfo.XH_ACREAGE);

        JSONObject cmDensityDiff = new JSONObject();
        cmDensityDiff.put("area", FeiteRegionInfo.CM_AREA);
        cmDensityDiff.put("value", ((double) cmDisasters.size()) / FeiteRegionInfo.CM_ACREAGE);

        JSONArray densityDiffValue = new JSONArray();
        densityDiffValue.add(xhDensityDiff);
        densityDiffValue.add(cmDensityDiff);

        FeiteDataEntity densityDiff = new FeiteDataEntity();
        densityDiff.setName(FeiteTaskName.FEITE_REGION_DISASTER_DENSITY_DIFF);
        densityDiff.setValue(densityDiffValue);
        densityDiff.setAlarmId(alarmId);

        feiteDataDAO.updateFeiteDataByNameAndAlarmId(densityDiff);

        // 统计两地受灾种类数
        JSONObject xhDisasterType = getAreaDisasterType(FeiteRegionInfo.XH_AREA, xhDisasters);
        JSONObject cmDisasterType = getAreaDisasterType(FeiteRegionInfo.CM_AREA, cmDisasters);

        JSONArray typeDiffValue = new JSONArray();
        typeDiffValue.add(xhDisasterType);
        typeDiffValue.add(cmDisasterType);

        FeiteDataEntity typeDiff = new FeiteDataEntity();
        typeDiff.setName(FeiteTaskName.FEITE_REGION_DISASTER_TYPE_DIFF);
        typeDiff.setValue(typeDiffValue);
        typeDiff.setAlarmId(alarmId);

        feiteDataDAO.updateFeiteDataByNameAndAlarmId(typeDiff);
    }

    private JSONObject getAreaDisasterType(String area, List<JSONObject> disasters) {
        int[] disasterType = new int[7];
        String[] disasterTypeName = {"树倒", "河水上涨", "农田作物", "房屋进水", "小区进水", "高空坠物", "其他"};

        for (JSONObject disaster : disasters) {
            String code = (String) disaster.get("Disaster_Code");
            if ("2".equals(code)) {
                disasterType[0] += 1;
            } else {
                String description = (String) disaster.get("Disaster_Description");
                if (description.contains("河水")) {
                    disasterType[1] += 1;
                } else if (description.contains("田地") || description.contains("农田")) {
                    disasterType[2] += 1;
                } else if (description.contains("房屋") || description.contains("家")) {
                    disasterType[3] += 1;
                } else if (description.contains("小区")) {
                    disasterType[4] += 1;
                } else if (description.contains("坠")) {
                    disasterType[5] += 1;
                } else {
                    disasterType[6] += 1;
                }
            }
        }

        JSONArray disasterTypeList = new JSONArray();

        for (int i = 0; i < disasterType.length; i++) {
            JSONObject obj = new JSONObject();
            obj.put("type", disasterTypeName[i]);
            obj.put("value", disasterType[i]);
            disasterTypeList.add(obj);
        }

        JSONObject areaDisasterType = new JSONObject();
        areaDisasterType.put("area", area);
        areaDisasterType.put("value", disasterTypeList);

        return areaDisasterType;
    }

    @PostConstruct
    public void getRainfallTop10() {
        String baseUrl = JsonServiceURL.AUTO_STATION_JSON_SERVICE_URL + "GetAutoStationDataByDatetime_5mi_SanWei/";
        String type = "1";
        logger.info(String.format("began task：%s", FeiteTaskName.FEITE_RAINFALL_TOP10));

        HashMap<String, LinkedList<Double> > hs =  new HashMap<>();
        for (int i = 0; i < FeiteRegionInfo.Hours; i++){
            String date = DateHelper.getPostponeDateByHour(2013, 10, 7, 13, 0, 0, i);
            String url = baseUrl + date + "/" + date + "/" + type;
            JSONObject rainfallJson = HttpHelper.getDataByURL(url);
            JSONArray rainfallData = (JSONArray) rainfallJson.get("Data");

            for (Object obj : rainfallData) {
                JSONObject rainfall = (JSONObject) obj;
                LinkedList<Double> allRainfall = hs.get(rainfall.get("STATIONNAME"));
                if (allRainfall == null) allRainfall = new LinkedList<>();
                allRainfall.add(Double.parseDouble((String)rainfall.get("RAINHOUR")));
                hs.put((String) rainfall.get("STATIONNAME"), allRainfall);
            }
        }
        JSONArray rainfallTop10 = new JSONArray();
        int cnt = 10;
        Iterator iter = hs.entrySet().iterator();
        while (iter.hasNext()){
            Map.Entry entry = (Map.Entry) iter.next();
            String siteName = (String)entry.getKey();
            LinkedList<Double> rainfallVal = (LinkedList<Double>)entry.getValue();
            Collections.sort(rainfallVal);
            JSONObject rainfallTopBySite = new JSONObject();
            rainfallTopBySite.put("site", siteName);
            rainfallTopBySite.put("value", rainfallVal.getLast());
            rainfallTop10.add(rainfallTopBySite);
            if ((--cnt) < 0) break;
        }

        FeiteDataEntity rainfall = new FeiteDataEntity();
        rainfall.setName(FeiteTaskName.FEITE_RAINFALL_TOP10);
        rainfall.setValue(rainfallTop10);
        feiteDataDAO.updateFeiteDataByName(rainfall);
    }

    @PostConstruct
    public void getGaleTop10() {
        String baseUrl = JsonServiceURL.AUTO_STATION_JSON_SERVICE_URL + "GetAutoStationDataByDatetime_5mi_SanWei/";
        String type = "1";
        logger.info(String.format("began task：%s", FeiteTaskName.FEITE_GALE_TOP10));

        HashMap<String, LinkedList<Double>> hs = new HashMap<>();
        for (int i = 0; i < FeiteRegionInfo.Hours; i++) {
            String date = DateHelper.getPostponeDateByHour(2013, 10, 7, 13, 0, 0, i);
            String url = baseUrl + date + "/" + date + "/" + type;
            JSONObject GaleJson = HttpHelper.getDataByURL(url);
            JSONArray GaleData = (JSONArray) GaleJson.get("Data");

            for (Object obj : GaleData) {
                JSONObject Gale = (JSONObject) obj;
                LinkedList<Double> allGale = hs.get(Gale.get("STATIONNAME"));
                if (allGale == null) allGale = new LinkedList<>();
                allGale.add(Double.parseDouble((String) Gale.get("WINDSPEED")));
                hs.put((String) Gale.get("STATIONNAME"), allGale);
            }
        }
        JSONArray GaleTop10 = new JSONArray();
        int cnt = 10;
        Iterator iter = hs.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String siteName = (String) entry.getKey();
            LinkedList<Double> rainfallVal = (LinkedList<Double>) entry.getValue();
            Collections.sort(rainfallVal);
            JSONObject GaleTopBySite = new JSONObject();
            GaleTopBySite.put("site", siteName);
            GaleTopBySite.put("value", rainfallVal.getLast());
            GaleTop10.add(GaleTopBySite);
            if ((--cnt) < 0) break;
        }

        FeiteDataEntity gale = new FeiteDataEntity();
        gale.setName(FeiteTaskName.FEITE_GALE_TOP10);
        gale.setValue(GaleTop10);
        feiteDataDAO.updateFeiteDataByName(gale);
    }
    /**
    * @Description 预警（使用手动导出的数据）
    * @Author lilin
    * @Create 2017/11/16 22:25
    **/

    @PostConstruct
    public void getWarning() {
        // String url = JsonServiceURL.ALARM_JSON_SERVICE_URL + "/GetWeatherWarnningByDatetime/20131006200000/20131008120000";
        logger.info(String.format("began task：%s", FeiteTaskName.FEITE_WARNING));

        //JSONObject obj = HttpHelper.getDataByURL(url);
        JSONObject obj = WarningHelper.getWarningContent();
        JSONArray resultArray = new JSONArray();

        JSONArray warnings = (JSONArray) obj.get("Data");
        for (int i = 0; i < warnings.size(); i++) {
            JSONObject warning = (JSONObject) warnings.get(i);
            String date = (String) warning.get("FORECASTDATE");
            String weather = (String) warning.get("TYPE");
            String level = (String) warning.get("LEVEL");
            JSONObject resultObject = new JSONObject();
            resultObject.put("date", DateHelper.getWarningDate(date));
            resultObject.put("weather", WarningHelper.getWarningWeather(weather));
            resultObject.put("level", WarningHelper.getWarningLevel(level));
            resultObject.put("ID", "ID" + String.valueOf(i + 1));
            resultArray.add(resultObject);
        }

        FeiteDataEntity warningsData = new FeiteDataEntity();
        warningsData.setName(FeiteTaskName.FEITE_WARNING);
        warningsData.setValue(resultArray);
        feiteDataDAO.updateFeiteDataByName(warningsData);
    }

    /**
    * @Description 雨量累计&大风监测
    * @Author lilin
    * @Create 2017/11/16 17:51
    **/

    @PostConstruct
    public void countRainfallAndMonitorWind() {
        String url = JsonServiceURL.AUTO_STATION_JSON_SERVICE_URL + "/GetAutoStationDataByDatetime_5mi_SanWei/20131006200000/20131008120000/1";
        logger.info(String.format("began task：%s", FeiteTaskName.FEITE_RAINFALL_TOTAL + " & " + FeiteTaskName.FEITE_GALE_TOTAL));

        JSONObject obj = HttpHelper.getDataByURL(url);

        JSONArray rainfallValueArray = new JSONArray();
        JSONArray windValueArray = new JSONArray();

        JSONArray autoStationDataArray = (JSONArray) obj.get("Data");
        for (int i = 0; i < autoStationDataArray.size(); i++) {
            JSONObject autoStationData = (JSONObject) autoStationDataArray.get(i);
            String rainfallValue = (String) autoStationData.get("RAINHOUR");
            String windSpeedValue = (String) autoStationData.get("WINDSPEED");
            double rainfallValueNum = Double.valueOf(rainfallValue);
            double windSpeedValueNum = Double.valueOf(windSpeedValue);
            if (rainfallValueNum >= 0) {
                JSONObject rainfallValueObject = new JSONObject();
                rainfallValueObject.put("value", rainfallValueNum);
                rainfallValueObject.put("level", RainfallHelper.getRainfallLevel(rainfallValue));
                rainfallValueArray.add(rainfallValueObject);
            }
            if (windSpeedValueNum >= 0) {
                JSONObject windValueObject = new JSONObject();
                windValueObject.put("value", windSpeedValueNum);
                windValueObject.put("level", WindHelper.getWindLevel(windSpeedValue));
                windValueArray.add(windValueObject);
            }
        }

        FeiteDataEntity rainfallTotalData = new FeiteDataEntity();
        rainfallTotalData.setName(FeiteTaskName.FEITE_RAINFALL_TOTAL);
        rainfallTotalData.setValue(rainfallValueArray);
        feiteDataDAO.updateFeiteDataByName(rainfallTotalData);

        FeiteDataEntity galeTotalData = new FeiteDataEntity();
        galeTotalData.setName(FeiteTaskName.FEITE_GALE_TOTAL);
        galeTotalData.setValue(windValueArray);
        feiteDataDAO.updateFeiteDataByName(galeTotalData);
    }

    /**
    * @Description 报灾情况
    * @Author lilin
    * @Create 2017/11/16 21:10
    **/
    @PostConstruct
    public void countDisasterReports() {
        String url = JsonServiceURL.ALARM_JSON_SERVICE_URL + "/GetDisasterDetailData_Geliku/20131006200000/20131008120000";
        logger.info(String.format("began task：%s", FeiteTaskName.FEITE_DISASTER_TOTAL));

        JSONObject obj = HttpHelper.getDataByURL(url);

        JSONObject resultObject = new JSONObject();
        JSONArray rainArray = new JSONArray();
        JSONArray windArray = new JSONArray();
        JSONArray resultArray = new JSONArray();

        JSONArray disasterReports = (JSONArray) obj.get("Data");
        int allNum = disasterReports.size();
        int rainNum = 0;
        int windNum = 0;
        // 房屋进水
        int FWJSNum = 0;
        // 道路积水
        int DLJSNum = 0;
        // 小区积水
        int XQJSNum = 0;
        // 车辆进水
        int CLJSNum = 0;
        // 厂区、商铺进水
        int CQSPJSNum = 0;
        // 其他
        int OtherNum = 0;
        // 树木倒伏
        int SMDFNum = 0;
        // 广告牌受损
        int GGPSSNum = 0;
        // 房屋受损
        int FWSSNum = 0;
        // 电线断裂
        int DXDLNum = 0;
        // 信号灯受损
        int XHDSSNum = 0;
        // 构筑物受损
        int GZWSSNum = 0;
        for (int i = 0; i < disasterReports.size(); i++) {
            JSONObject disasterReport = (JSONObject) disasterReports.get(i);
            long disasterType = (long) disasterReport.get("CODE_DISASTER");
            String caseAddr = (String) disasterReport.get("CASE_ADDR");
            String caseDesc = (String) disasterReport.get("CASE_DESC");
            if (disasterType == 2) {
                windNum++;
                String windDisasterType = DisasterTypeHelper.getWindDisasterType(caseAddr, caseDesc);
                if ("树木倒伏".equals(windDisasterType)) {
                    SMDFNum++;
                } else if ("广告牌受损".equals(windDisasterType)) {
                    GGPSSNum++;
                } else if ("房屋受损".equals(windDisasterType)) {
                    FWSSNum++;
                } else if ("电线断裂".equals(windDisasterType)) {
                    DXDLNum++;
                } else if ("信号灯受损".equals(windDisasterType)) {
                    XHDSSNum++;
                } else if ("构筑物受损".equals(windDisasterType)) {
                    GZWSSNum++;
                }
            }
            if (disasterType == 1) {
                rainNum++;
                String rainstormDisasterType = DisasterTypeHelper.getRainstormDisasterType(caseAddr, caseDesc);
                if ("房屋进水".equals(rainstormDisasterType)) {
                    FWJSNum++;
                } else if ("道路积水".equals(rainstormDisasterType)) {
                    DLJSNum++;
                } else if ("小区积水".equals(rainstormDisasterType)) {
                    XQJSNum++;
                } else if ("车辆进水".equals(rainstormDisasterType)) {
                    CLJSNum++;
                } else if ("厂区、商铺进水".equals(rainstormDisasterType)) {
                    CQSPJSNum++;
                } else {
                    OtherNum++;
                }
            }
        }
        JSONObject totalValue = new JSONObject();
        totalValue.put("all", allNum);
        totalValue.put("rain", rainNum);
        totalValue.put("wind", windNum);
        resultObject.put("total", totalValue);

        JSONObject FWJSObject = new JSONObject();
        JSONObject DLJSObject = new JSONObject();
        JSONObject XQJSObject = new JSONObject();
        JSONObject CLJSObject = new JSONObject();
        JSONObject CQSPJSObject = new JSONObject();
        JSONObject OtherObject = new JSONObject();

        JSONObject SMDFObject = new JSONObject();
        JSONObject GGPSSObject = new JSONObject();
        JSONObject FWSSObject = new JSONObject();
        JSONObject DXDLObject = new JSONObject();
        JSONObject XHDSSObject = new JSONObject();
        JSONObject GZWSSObject = new JSONObject();

        FWJSObject.put("type", "房屋进水");
        FWJSObject.put("value", FWJSNum + "");
        DLJSObject.put("type", "道路积水");
        DLJSObject.put("value", DLJSNum + "");
        XQJSObject.put("type", "小区积水");
        XQJSObject.put("value", XQJSNum + "");
        CLJSObject.put("type", "车辆进水");
        CLJSObject.put("value", CLJSNum + "");
        CQSPJSObject.put("type", "厂区、商铺进水");
        CQSPJSObject.put("value", CQSPJSNum + "");
        OtherObject.put("type", "其他");
        OtherObject.put("value", OtherNum + "");

        rainArray.add(FWJSObject);
        rainArray.add(DLJSObject);
        rainArray.add(XQJSObject);
        rainArray.add(CLJSObject);
        rainArray.add(CQSPJSObject);
        rainArray.add(OtherObject);

        resultObject.put("rain", rainArray);

        SMDFObject.put("type", "树木倒伏");
        SMDFObject.put("value", SMDFNum + "");
        GGPSSObject.put("type", "广告牌受损");
        GGPSSObject.put("value", GGPSSNum + "");
        FWSSObject.put("type", "房屋受损");
        FWSSObject.put("value", FWSSNum + "");
        DXDLObject.put("type", "电线断裂");
        DXDLObject.put("value", DXDLNum + "");
        XHDSSObject.put("type", "信号灯受损");
        XHDSSObject.put("value", XHDSSNum + "");
        GZWSSObject.put("type", "构筑物受损");
        GZWSSObject.put("value", GZWSSNum + "");

        windArray.add(SMDFObject);
        windArray.add(GGPSSObject);
        windArray.add(FWSSObject);
        windArray.add(DXDLObject);
        windArray.add(XHDSSObject);
        windArray.add(GZWSSObject);

        resultObject.put("wind", windArray);

        resultArray.add(resultObject);

        FeiteDataEntity disasterReportsData = new FeiteDataEntity();
        disasterReportsData.setName(FeiteTaskName.FEITE_DISASTER_TOTAL);
        disasterReportsData.setValue(resultArray);
        feiteDataDAO.updateFeiteDataByName(disasterReportsData);
    }
}
