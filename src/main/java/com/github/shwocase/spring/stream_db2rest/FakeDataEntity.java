package com.github.shwocase.spring.stream_db2rest;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Objects;
import java.util.UUID;

@Entity
public class FakeDataEntity {

    @Id
    private UUID id;
    private String field1;
    private String field2;
    private String field3;

    public FakeDataEntity() {
    }

    public FakeDataEntity(String field1, String field2, String field3) {
        this.id = UUID.randomUUID();
        this.field1 = field1;
        this.field2 = field2;
        this.field3 = field3;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getField1() {
        return field1;
    }

    public void setField1(String field1) {
        this.field1 = field1;
    }

    public String getField2() {
        return field2;
    }

    public void setField2(String field2) {
        this.field2 = field2;
    }

    public String getField3() {
        return field3;
    }

    public void setField3(String field3) {
        this.field3 = field3;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FakeDataEntity that = (FakeDataEntity) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(field1, that.field1) &&
                Objects.equals(field2, that.field2) &&
                Objects.equals(field3, that.field3);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, field1, field2, field3);
    }
}
