package org.easyframe.tutorial.lesson2;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import jef.database.DbClient;
import jef.database.DbClientBuilder;

import org.easyframe.tutorial.lesson2.entity.Student;
import org.easyframe.tutorial.lesson2.entity.StudentToLesson;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class Case3 {
    private static DbClient db;

    @BeforeClass
    public static void setup() throws SQLException {
        db = new DbClientBuilder().build();
        db.createTable(Student.class);
    }

    @Test()
    public void testAllRecords_error() throws SQLException {
        // 按学号顺序查出全部学生
        Student st = new Student();
        st.setName(null);
        try {
            List<Student> all = db.select(st);
            System.out.println("共有学生" + all.size());
        } catch (NullPointerException e) {
            e.printStackTrace();
            throw e;
        }

    }

    @Test
    public void testSelectCombineKey() throws SQLException {
        
        StudentToLesson s=new StudentToLesson();
        s.setLessionId(100);
        s.setCreateTime(new Date());
        db.select(s);
        

    }

    @AfterClass
    public static void close() {
        if (db != null)
            db.close();
    }

}
