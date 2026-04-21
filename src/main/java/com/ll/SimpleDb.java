package com.ll;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

public class SimpleDb {
    private String host;
    private String username;
    private String password;
    private String dbName;
    private boolean mode;
    private String url;


    public SimpleDb(String host, String username, String password, String dbName) {
        this.host = host;
        this.username = username;
        this.password = password;
        this.dbName = dbName;
        //3306 포트를 다른 점유하고 있어서 포워딩 한 상태입니다.
        this.url = "jdbc:mysql://" + host + ":3304/" + dbName
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul";
    }

    public void setDevMode(boolean mode) {
        this.mode = mode;
    }

    public void run(String command) {
        /* java.sql.* 을 통해서 로직 구현 - DriverManager
         * 1. DB연결 + 실행도구 생성
         * 2. sql실행
         * */
        try (
                Connection connection = DriverManager.getConnection(url, username, password);
                Statement stat = connection.createStatement();
        ) {
            stat.execute(command);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void run(String command,Object... parmas) {
        try (
                Connection connection = DriverManager.getConnection(url, username, password);
                PreparedStatement pstmt = connection.prepareStatement(command);
        ) {
            // 문자열 치환 아닌 PreparedStatement 사용
            for(int i = 0; i < parmas.length; i++) {
                pstmt.setObject(i + 1,parmas[i]);
            }
            pstmt.execute();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public Sql genSql() {
        return new Sql(this);
    }

    public Connection getConnection() {
        try {
            return DriverManager.getConnection(url, username, password);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
