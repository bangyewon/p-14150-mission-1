package com.ll;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Sql {
    //TODO 내부 DB연결 과정이 겹침 리팩터링 필요

    private final SimpleDb simpleDb;
    private final StringBuilder sb = new StringBuilder(); // sql 쌓임

    public Sql(SimpleDb simpleDb) {
        this.simpleDb = simpleDb;
    }

    public Sql append(String command) {
        sb.append(command).append("\n");
        return this;
    }

    public Sql append(String command, Object... params) {
        String replaced = command;

        for (Object param : params) {
            replaced = replaced.replaceFirst("\\?", "'" + param + "'");
        }
        sb.append(replaced).append("\n");
        return this;
    }

    // statment : ? 문자열 치환 불가능 PreparedStatement는 치환 가능 -> 이미 append()에서 치환 후이기에 sttatment로 가능
    public long insert() {

        String sql = sb.toString();

        try (
                Connection connection = simpleDb.getConnection();
                Statement stat = connection.createStatement();
        ) {
            //excuteUpdate() : 행의 개수를 반환
            stat.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
            ResultSet rs = stat.getGeneratedKeys();
            if (rs.next()) { // ResultSet의 첫번째 컬럼 값 : PK
                return rs.getLong(1);
            }
            throw new RuntimeException("PK를 갖고올 수 없습니다.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int update() {
        String sql = sb.toString();
        try (
                Connection connection = simpleDb.getConnection();
                Statement stat = connection.createStatement();
        ) {
            return stat.executeUpdate(sql);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int delete() {
        String sql = sb.toString();
        try (
                Connection connection = simpleDb.getConnection();
                Statement stat = connection.createStatement();
        ) {
            return stat.executeUpdate(sql);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //executeQuery() : 조회 결과가 ResultSet에 들어옴
    public List<Map<String, Object>> selectRows() {
        ArrayList<Map<String, Object>> rows = new ArrayList<>();
        String sql = sb.toString();
        try (
                Connection connection = simpleDb.getConnection();
                Statement stat = connection.createStatement();
                ResultSet rs = stat.executeQuery(sql); // 데이터에 관한 정보
        ) {
            ResultSetMetaData data = rs.getMetaData(); //컬럼에 관한 정보
            int count = data.getColumnCount();
            // 조회 : 결과가 여러줄 나옴 while()
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= count; i++) {
                    // row의 이름과 값 꺼내야 함
                    String name = data.getColumnName(i);
                    Object value = rs.getObject(i);
                    row.put(name, value);
                }
                rows.add(row);
            }
            return rows;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Object> selectRow() {
        Map<String, Object> row = new HashMap<>();
        String sql = sb.toString();
        try (
                Connection connection = simpleDb.getConnection();
                Statement stat = connection.createStatement();
                ResultSet rs = stat.executeQuery(sql);
        ) {
            ResultSetMetaData data = rs.getMetaData();
            int count = data.getColumnCount();
            if (rs.next()) {
                for (int i = 1; i <= count; i++) {
                    String name = data.getColumnName(i);
                    Object value = rs.getObject(i);
                    row.put(name, value);
                }
            }
            return row;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public LocalDateTime selectDatetime() {
        String sql = sb.toString();
        try (
                Connection connection = simpleDb.getConnection();
                Statement stat = connection.createStatement();
                ResultSet rs = stat.executeQuery(sql);
                ) {
            // rs에서 시간 값 꺼내서 LocalDateTime 변환
            if(rs.next()) {
                return rs.getTimestamp(1).toLocalDateTime();
            }
            throw new RuntimeException("결과가 없습니다.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
