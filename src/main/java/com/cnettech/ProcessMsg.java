package com.cnettech;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.ibatis.session.SqlSession;

import com.cnettech.util.Common;
import com.cnettech.util.Log4j;
import com.cnettech.util.SqlSessionFactoryManager;

public class ProcessMsg {
    private static Properties pros = Common.getProperties();
    private static long ELAPSE_TIME_LIMIT = 600000;
    private String MON_CPU = pros.getProperty("mon.cpu");
    private String MON_MEM = pros.getProperty("mon.mem");
    private String MON_HDD = pros.getProperty("mon.hdd");
    private SqlSession sqlSession = null;
    private String MainServer = "";
    private String BackupServer = "";
    private Map<String, Object> ErrorCode = new HashMap<String, Object>();
    // 최초 업데이트 타임 세팅
    private long UpdateTime_tbl_system_select_main_backup = -1;
    private long UpdateTime_tbl_dash_mon_code_rel_select = -1;

    ProcessMsg() {
    }

    /**
     * Check에서 보내는 메세지 분석 함수
     * 
     * 
     * 전송내용 :
     * SYSINCNET!@,01,LOCAL,,,,1024,768,001~C:010~,00000,001~ONOTEPAD.EXE|5.1.2600~,014,011,0,,1234
     * 
     * @param ip  : 요청 IP
     * @param msg : 요청 메세지
     * 
     */
    public void Msg(String SendIp, String msg) {
        // String[] value = msg.split(",");
        tbl_system_select_main_backup();
        tbl_dash_mon_code_rel_select();
        if (msg.contains("SYSINCNET!@")) {
            // Check쪽 자료호출
            String[] value = msg.split(",");
            String sSystemCode = value[1];
            sSystemCode = MainServer.contains(SendIp) ? sSystemCode + "0" : sSystemCode + "1";
            App(sSystemCode, msg);
        } else if (msg.contains("STA")){
            String[] value = msg.split("|");
            // 녹취쪽 자료호출
            String sSystemCode = value[2];
            sSystemCode = MainServer.contains(SendIp) ? sSystemCode + "0" : sSystemCode + "1";
            Rec(sSystemCode, msg);
        }
    }

    private void App(String sSystemCode, String msg) {
        String[] value = msg.split(",");
        String sProcess = value[10];
        String sAllPro = "";
        String sNotPro = "";
        String[] sProcessValue = sProcess.split("~");
        String sHdd = value[8];
        String sHddStr = "";
        String[] sHddValue = sHdd.split("~");
        String sCpu = value[11];
        String sMem = value[12];
        String sErrCode = "";

        // System.out.printf("System Code : %s \r\n", sSystemCode);
        // System.out.printf("Total Process Count : %s \r\n",
        // Integer.parseInt(sProcessValue[0]));
        for (int i = 1; i < sProcessValue.length; i++) {
            // System.out.printf("%s\r\n", sProcessValue[i]);
            String[] sTemp = sProcessValue[i].split("\\|");
            // System.out.printf("Process Name %s, Version : %s\r\n", sTemp[0], sTemp[1]);
            sAllPro = String.format("%s,%s", sAllPro, sTemp[0]);
            if (sTemp[0].substring(0, 1).equals("X"))
                sNotPro = String.format("%s,%s", sNotPro, sTemp[0].substring(1));
        }

        if (!sAllPro.equals(""))
            sAllPro = sAllPro.substring(1);

        if (!sNotPro.equals(""))
            sNotPro = sNotPro.substring(1);

        // Process 이상유무 체크
        if (!sNotPro.equals(""))
            sErrCode = String.format("%s,301", sErrCode);

        // System.out.printf("Cpu : %s(%d) \r\n", sCpu, Integer.parseInt(sCpu));
        // System.out.printf("Mem : %s(%d) \r\n", sMem, Integer.parseInt(sMem));

        // System.out.printf("Total Hdd Count : %d\r\n",
        // Integer.parseInt(sHddValue[0]));

        for (int i = 1; i < sHddValue.length; i++) {
            String[] sTemp = sHddValue[i].split(":");
            /// System.out.printf("Drive Name : %s, Remain : %d\r\n", sTemp[0],
            /// Integer.parseInt(sTemp[1]));
            sHddStr = String.format("%s,%s:%d", sHddStr, sTemp[0], 100 - Integer.parseInt(sTemp[1]));
            // HDD 오류 처리
            if (100 - Integer.parseInt(sTemp[1]) > Integer.parseInt(MON_HDD))
                if (sErrCode.indexOf("302") < 0)
                    sErrCode = String.format("%s,302", sErrCode);
        }

        if (!sHddStr.equals(""))
            sHddStr = sHddStr.substring(1);

        // CPU 이상유무 체크
        if (Integer.parseInt(sCpu) > Integer.parseInt(MON_CPU))
            sErrCode = String.format("%s,303", sErrCode);

        // 메모리 이상유무 체크
        if (Integer.parseInt(sMem) > Integer.parseInt(MON_MEM))
            sErrCode = String.format("%s,304", sErrCode);

        if (sErrCode.equals("")) {
            sErrCode = "000";
        } else {
            sErrCode = sErrCode.substring(1);
        }

        String sSql = "";
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dayformatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        DateTimeFormatter timeformatter = DateTimeFormatter.ofPattern("HHmmss");

        sSql = "UPDATE tbl_dash_Monitoring SET Mon_Date='" + now.format(dayformatter) + "',Mon_Time='"
                + now.format(timeformatter) + "',";
        sSql += "Mon_Cpu='" + Integer.parseInt(sCpu) + "',Mon_Mem='" + Integer.parseInt(sMem) + "',Mon_Hdd='" + sHddStr
                + "',Mon_Process='" + sAllPro + "',Mon_Check='" + sErrCode + "' " +
                "WHERE Mon_System='" + sSystemCode + "'";

        System.out.printf("Query : %s\r\n", sSql);
        // System.out.println(sNotPro);
        // String system_code, String cpu, String mem, String hdd, String process,
        // String alarm
        tbl_dash_Monitoring_update(sSystemCode, sCpu, sMem, sHddStr, sAllPro, sErrCode);

        for (String sErrValue : sErrCode.split(",")) {
            sSql = "INSERT INTO tbl_dash_monlist(Mon_System,Mon_Date,Mon_Time,Mon_Local_Phone,Mon_Process,Mon_Alram) VALUES ('"
                    + sSystemCode + "','" + now.format(dayformatter) + "','" +
                    now.format(timeformatter) + "','','" + (sErrValue.equals("301") ? sNotPro : "") + "','" + sErrValue
                    + "')";
            System.out.printf("Query : %s\r\n", sSql);

            tbl_dash_monlist_insert(sSystemCode, "", (sErrValue.equals("301") ? sNotPro : ""), sErrValue);
        }
    }

    /**
     * STA | 채널번호 | 장비 번호 | 내선번호 | 내용 | 오류 코드 |
     * 
     * @param ip
     * @param msg
     */
    private void Rec(String sSystemCode, String msg) {
        String[] value = msg.split(",");
        String sLocalNo = value[3];
        String sMsg = value[4];
        String sErrCode = value[5];

        // 에러코드집합에 시스템 코드에 포함되어 있는지 확인후 없으면 리턴함
        if (!ErrorCode.containsKey(sSystemCode))
            return;

        // 에러코드집합에 해당 오류코드가 포함되어 있는지 확인후 없으면 리턴함            
        if (!ErrorCode.get(sSystemCode).toString().contains(sErrCode))
            return;

        // tbl_dash_Monitoring_update(sSystemCode, sCpu, sMem, sHddStr, sAllPro,
        // sErrCode);

        // for (String sErrValue : sErrCode.split(",")) {
        // sSql = "INSERT INTO
        // tbl_dash_monlist(Mon_System,Mon_Date,Mon_Time,Mon_Local_Phone,Mon_Process,Mon_Alram)
        // VALUES ('"
        // + sSystemCode + "','" + now.format(dayformatter) + "','" +
        // now.format(timeformatter) + "','','" + (sErrValue.equals("301") ? sNotPro :
        // "") + "','" + sErrValue
        // + "')";
        // System.out.printf("Query : %s\r\n", sSql);

        // tbl_dash_monlist_insert(sSystemCode, "", (sErrValue.equals("301") ? sNotPro :
        // ""), sErrValue);
        // }
    }

    public void tbl_dash_monlist_insert(String systemCode, String localphone, String program, String ErrorCode) {
        // SqlSession sqlSession = null;
        Map<String, Object> argMap = new HashMap<String, Object>();

        try {
            // DB Connection
            if (sqlSession == null)
                sqlSession = SqlSessionFactoryManager.getSqlSessionFactory().openSession(true);
            argMap.clear();
            argMap.put("system_code", systemCode);
            argMap.put("local_phone", localphone);
            argMap.put("process", program);
            argMap.put("alarm", ErrorCode);

            int count = sqlSession.insert("dashboard.tbl_dash_monlist_insert", argMap);

            Log4j.log.info("readChannel : " + count);

            // List<Map<String, Object>> list = sqlSession.selectList("channel.selectList",
            // argMap);
            // int index = 0;
            // for (Map<String, Object> item : list) {
            // index++;
            // Log4j.log.info("index :" + index
            // + " phone_num :" + item.get("phone_num")
            // + " phone_ip :" + item.get("phone_ip")
            // + " channel :" + item.get("channel")
            // + " system_code :" + item.get("system_code"));

            // // if(mapDN.containsKey(item.get("phone_num").toString()) != true)
            // // {
            // // Channel Chan = new Channel();
            // // Chan.sDN = item.get("phone_num").toString();
            // // Chan.bEnabled = true;
            // // Chan.bRegister = false;
            // // mapDN.put(item.get("phone_num").toString(), Chan);
            // // }
            // }
        } catch (Exception e) {
            Log4j.log.error(e.getMessage());
        } finally {
            // if (sqlSession != null)
            // sqlSession.close();
        }
    }

    public void tbl_dash_Monitoring_update(String system_code, String cpu, String mem, String hdd, String process,
            String alarm) {
        // SqlSession sqlSession = null;
        Map<String, Object> argMap = new HashMap<String, Object>();

        try {
            // DB Connection
            if (sqlSession == null)
                sqlSession = SqlSessionFactoryManager.getSqlSessionFactory().openSession(true);
            argMap.clear();
            argMap.put("system_code", system_code);
            argMap.put("cpu", cpu);
            argMap.put("mem", mem);
            argMap.put("hdd", hdd);
            argMap.put("process", process);
            argMap.put("alarm", alarm);

            int count = sqlSession.update("dashboard.tbl_dash_Monitoring_update", argMap);

            Log4j.log.info("readChannel : " + count);

        } catch (Exception e) {
            Log4j.log.error(e.getMessage());
        } finally {
            // if (sqlSession != null)
            // sqlSession.close();
        }
    }

    public void tbl_system_select_main_backup() {
        // SqlSession sqlSession = null;
        try {
            long now = System.currentTimeMillis();
            long timeElapsed = now - UpdateTime_tbl_system_select_main_backup;
            // System.out.printf("%s : %d\r\n","경과시간",timeElapsed);
            // System.out.printf("%s : %d\r\n","지금시간",now);
            // System.out.printf("%s : %d\r\n","요청시간",UpdateTime);
            // 업데이트후 10분이 지나면 다시 자료를 로딩한다
            if (timeElapsed < ELAPSE_TIME_LIMIT && timeElapsed > 0)
                return;

            // DB Connection
            if (sqlSession == null)
                sqlSession = SqlSessionFactoryManager.getSqlSessionFactory().openSession(true);

            List<Map<String, Object>> list = sqlSession.selectList("dashboard.tbl_system_select_main_backup");

            int index = 0;
            MainServer = "";
            BackupServer = "";
            for (Map<String, Object> item : list) {
                index++;
                Log4j.log.info("index :" + index
                        + " system_ip :" + item.get("system_ip")
                        + " backup_ip :" + item.get("backup_ip"));
                MainServer += item.get("system_ip") + "|";
                BackupServer += (item.get("backup_ip") == null ? "" : item.get("backup_ip")) + "|";
            }
            System.out.println("Main Server : " + MainServer);
            System.out.println("Backup Server : " + BackupServer);

            UpdateTime_tbl_system_select_main_backup = System.currentTimeMillis();

        } catch (Exception e) {
            Log4j.log.error(e.getMessage());
        }
    }

    public void tbl_dash_mon_code_rel_select() {
        // SqlSession sqlSession = null;
        try {
            long now = System.currentTimeMillis();
            long timeElapsed = now - UpdateTime_tbl_dash_mon_code_rel_select;
            // System.out.printf("%s : %d\r\n","경과시간",timeElapsed);
            // System.out.printf("%s : %d\r\n","지금시간",now);
            // System.out.printf("%s : %d\r\n","요청시간",UpdateTime);
            // 업데이트후 10분이 지나면 다시 자료를 로딩한다
            if (timeElapsed < ELAPSE_TIME_LIMIT && timeElapsed > 0)
                return;

            // DB Connection
            if (sqlSession == null)
                sqlSession = SqlSessionFactoryManager.getSqlSessionFactory().openSession(true);

            List<Map<String, Object>> list = sqlSession.selectList("dashboard.tbl_dash_mon_code_rel_select");

            int index = 0;
            MainServer = "";
            BackupServer = "";
            ErrorCode.clear();
            for (Map<String, Object> item : list) {
                index++;
                Log4j.log.info("index :" + index
                        + " Mon_System :" + item.get("Mon_System")
                        + " Mon_Code :" + item.get("Mon_Code"));

                if (ErrorCode.containsKey(item.get("Mon_System").toString())) {
                    ErrorCode.replace(item.get("Mon_System").toString(),
                            ErrorCode.get(item.get("Mon_System").toString()) + "|" + item.get("Mon_Code").toString());
                } else {
                    ErrorCode.put(item.get("Mon_System").toString(), item.get("Mon_Code"));
                }
            }

            for (String strKey : ErrorCode.keySet()) {
                String strValue = ErrorCode.get(strKey).toString();
                System.out.println(strKey + ":" + strValue);
            }

            UpdateTime_tbl_dash_mon_code_rel_select = System.currentTimeMillis();

        } catch (Exception e) {
            Log4j.log.error(e.getMessage());
        }
    }
}
