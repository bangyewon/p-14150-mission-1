package com.ll;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

public class SimpleDb {
    private Connection connection;
    private boolean inTransaction = false; // 트랜잭션 중일땐 connection을 계속 반환해야하기에 트랜잭션 중인지 여부
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

    public void run(String command, Object... parmas) {
        try (
                Connection connection = DriverManager.getConnection(url, username, password);
                PreparedStatement pstmt = connection.prepareStatement(command);
        ) {
            // 문자열 치환 아닌 PreparedStatement 사용
            for (int i = 0; i < parmas.length; i++) {
                pstmt.setObject(i + 1, parmas[i]);
            }
            pstmt.execute();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Sql genSql() {
        return new Sql(this);
    }

    /**
     * 트랜잭션 아님 -> getConnection()이 새 커넥션 생성 -> 끝나면 releaseConnection()이 닫음
     * 트랜잭션 중 -> getConnection()이 공유 커넥션 반환 -> 끝나도 releaseConnection()은 닫지 않음 - rollbacok,commit이 닫도록 수정
     */
    public Connection getConnection() {
        try {
            if (!inTransaction) {
                return DriverManager.getConnection(url, username, password);
            }

            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(url, username, password);
            }

            return connection;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 반환 여부에 대해서 SimpleDb가 결정하기 위함
    public void releaseConnection(Connection connection) {
        try {
            if (connection == null || connection.isClosed()) return;
            // 현재 트랜잭션에서 관리 중인 공유 커넥션인지 확인
            if (inTransaction && this.connection == connection)
                return;
            connection.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void startTransaction() {
        try {
            connection = getConnection();
            connection.setAutoCommit(false); // AutoCommit을 끄는 순간 sql -> 트랜잭션으로 묶임
            inTransaction = true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void rollback() {
        // connection연결이 되어있다면 롤백 가능
        try {
            if (connection != null && !connection.isClosed()) {
                connection.rollback();
                connection.setAutoCommit(true);
                connection.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            connection = null;
            inTransaction = false;
        }
    }

    public void commit() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.commit();
                connection.setAutoCommit(true);
                connection.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            connection = null;
            inTransaction = false;

        }
    }

    public boolean isInTransaction() {
        return inTransaction;
    }
}
