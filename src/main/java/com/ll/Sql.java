package com.ll;

import java.lang.reflect.Field;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class Sql {
    //TODO 겹치는 부분 공통화 리팩터링 예정

    private final SimpleDb simpleDb;
    private final StringBuilder sb = new StringBuilder(); // sql 쌓임

    @FunctionalInterface
    private interface ResultSetHandler<T> {
        T handle(ResultSet rs) throws Exception;
    }

    private <T> T executeQuery(ResultSetHandler<T> handler) {
        String sql = sb.toString();
        Connection connection = simpleDb.getConnection();

        try (
                Statement stat = connection.createStatement();
                ResultSet rs = stat.executeQuery(sql);
        ) {
            return handler.handle(rs);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            simpleDb.releaseConnection(connection);
        }
    }

    private Map<String, Object> mapCurrentRow(ResultSet rs) throws Exception {
        Map<String, Object> row = new HashMap<>();
        ResultSetMetaData data = rs.getMetaData(); //컬럼에 관한 정보
        int count = data.getColumnCount();

        for (int i = 1; i <= count; i++) {
            String name = data.getColumnName(i);
            Object value = rs.getObject(i);

            if (value instanceof Timestamp timestamp) {
                value = timestamp.toLocalDateTime();
            }

            row.put(name, value);
        }

        return row;
    }

    private <T> T mapCurrentRow(ResultSet rs, Class<T> cls) throws Exception {
        T obj = cls.getDeclaredConstructor().newInstance();
        ResultSetMetaData data = rs.getMetaData();
        int count = data.getColumnCount();

        for (int i = 1; i <= count; i++) {
            String fieldName = data.getColumnName(i);
            Object value = rs.getObject(i);

            if (value instanceof Timestamp timestamp) {
                value = timestamp.toLocalDateTime();
            }

            Field field = cls.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        }

        return obj;
    }

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

    public Sql appendIn(String command, Object... params) {
        StringJoiner sj = new StringJoiner(", ");
        for (Object param : params) {
            sj.add("'" + param + "'");
        }
        String replaced = command.replaceFirst("\\?", sj.toString());
        sb.append(replaced).append("\n");
        return this;
    }

    // statment : ? 문자열 치환 불가능 PreparedStatement는 치환 가능 -> 이미 append()에서 치환 후이기에 statement로 가능
    public long insert() {
        String sql = sb.toString();
        Connection connection = simpleDb.getConnection();

        try (
                Statement stat = connection.createStatement();
        ) {
            //excuteUpdate() : 행의 개수를 반환
            stat.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
            // ResultSet의 첫번째 컬럼 값 : PK
            try (ResultSet rs = stat.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }

            throw new RuntimeException("PK를 갖고올 수 없습니다.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (!simpleDb.isInTransaction()) {
                try {
                    if (connection != null && !connection.isClosed()) {
                        connection.close();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public int update() {
        String sql = sb.toString();
        Connection connection = simpleDb.getConnection();
        try (
                Statement stat = connection.createStatement();
        ) {
            return stat.executeUpdate(sql);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            simpleDb.releaseConnection(connection);
        }
    }

    public int delete() {
        String sql = sb.toString();
        Connection connection = simpleDb.getConnection();
        try (
                Statement stat = connection.createStatement();
        ) {
            return stat.executeUpdate(sql);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            simpleDb.releaseConnection(connection);
        }
    }

    //executeQuery() : 조회 결과가 ResultSet에 들어옴
    public List<Map<String, Object>> selectRows() {
        return executeQuery(rs -> {
            List<Map<String, Object>> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(mapCurrentRow(rs));
            }
            return rows;
        });
    }

    /**
     * 1. SQL 조회 결과 받기
     * 2. 각 행을 Article 객체로 변환
     * 3. 리스트로 반환
     */
    public <T> List<T> selectRows(Class<T> cls) {
        return executeQuery(rs -> {
            List<T> rows = new ArrayList<>();

            while (rs.next()) {
                rows.add(mapCurrentRow(rs, cls));
            }

            return rows;
        });
    }


    public Map<String, Object> selectRow() {
        return executeQuery(rs -> {
            if (!rs.next()) {
                return new HashMap<>();
            }
            return mapCurrentRow(rs);

        });
    }

    public <T> T selectRow(Class<T> cls) {
        return executeQuery(rs -> {
            if (!rs.next()) {
                return cls.getDeclaredConstructor().newInstance();
            }
            return mapCurrentRow(rs, cls);
        });
    }

    public LocalDateTime selectDatetime() {
        return executeQuery(rs -> {
            if (rs.next()) {
                return rs.getTimestamp(1).toLocalDateTime();
            }
            throw new RuntimeException("조회 결과 없어요.");
        });
    }

    public Long selectLong() {
        return executeQuery(rs ->
        {
            if (rs.next()) {
                rs.getLong(1);
            }
            throw new RuntimeException("조회 결과가 없어요.");
        });
    }

    public String selectString() {
        return executeQuery(rs -> {
            if (rs.next()) {
                rs.getString(1);
            }
            throw new RuntimeException("조회 결과 없어요.");
        });
    }

    public Boolean selectBoolean() {
        return executeQuery(rs -> {
            if (rs.next()) {
                return rs.getBoolean(1);
            }
            throw new RuntimeException("조회 결과 없어요.");
        });
    }

    /**
     * sql 내용 받음 -> 조회 후 id꺼내서 리스트로 변환
     */
    public List<Long> selectLongs() {
        List<Long> foundIds = new ArrayList<>();
        return executeQuery(rs -> {
            ResultSetMetaData data = rs.getMetaData();
            int count = data.getColumnCount();
            while (rs.next()) {
                long result = 0;
                for (int i = 1; i <= count; i++) {
                    result = rs.getLong(i);
                }
                foundIds.add(result);
            }
            throw new RuntimeException("조회 결과 없어요.");
        });
    }

}
