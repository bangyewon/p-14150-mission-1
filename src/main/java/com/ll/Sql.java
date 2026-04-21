package com.ll;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class Sql {

    private final SimpleDb simpleDb;
    private final StringBuilder sb = new StringBuilder(); // sql 쌓임

    public Sql(SimpleDb simpleDb) {
        this.simpleDb = simpleDb;
    }

    public Sql append(String command) {
        return this;
    }

    public Sql append(String command, Object param) {
        return this;
    }

    public long insert() {

        String sql = sb.toString();

        try (
                Connection connection = simpleDb.getConnection();
                Statement stat = connection.createStatement();
        ) {
            //excuteUpdate() : 행의 개수를 반환
            stat.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
            ResultSet rs = stat.getGeneratedKeys();
            if(rs.next()) { // ResultSet의 첫번째 컬럼 값 : PK
                return rs.getLong(1);
            }
            throw new RuntimeException("PK를 갖고올 수 없습니다.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
//    public int update() {
//        return;
//    }
}
